/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands.test;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.LaunchListenerManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.junit.Assert;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.internal.configurator.ConfiguratorImporter;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.test.GrailsTestsActivator;
import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @created Sep 30, 2010
 */
public abstract class AbstractCommandTest extends GrailsTest {

    public static class EchoStream implements IStreamListener {
    
    	private PrintStream out;
    
    	public EchoStream(PrintStream out) {
    		this.out = out;
    	}
    
    	public void streamAppended(String text, IStreamMonitor monitor) {
    		out.print(text);
    	}
    
    }

    public static void assertDefaultOutputFolder(IJavaProject javaProject)
			throws JavaModelException {
				assertEquals("/"+javaProject.getProject().getName()+"/"+GrailsCommandUtils.DEFAULT_GRAILS_OUTPUT_FOLDER, 
						javaProject.getOutputLocation().toString());
			}

	ConfiguratorImporter blah = null;
    boolean wasAutoBuilding;
	protected IProject project;
    
    public AbstractCommandTest() {
        super();
    }

    public AbstractCommandTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
    	super.setUp();
    	
        //Ensure Java compliance level is set to something that supports generics
        GrailsTest.setJava15Compliance();
        
    	// Configuring Grails installs happens automatically (or it should!) but it runs
    	// in some Jobs, so it may take a while.
    	ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
    	wasAutoBuilding = StsTestUtil.getWorkspace().isAutoBuilding();
    	StsTestUtil.setAutoBuilding(false);
    	assertFalse(StsTestUtil.getWorkspace().isAutoBuilding());
    	StsTestUtil.waitForAutoBuild();
    }

	@Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    	StsTestUtil.setAutoBuilding(wasAutoBuilding);
    	
    	// avoid running this condition on the joint grails-sts build because it is leading to no class def errors.
    	if (!GrailsTestsActivator.isJointGrailsTest()) {
        	new ACondition() {
        		@Override
        		public boolean test() throws Exception {
        			assertFalse("Launch listeners not cleaned up!", LaunchListenerManager.isMemoryLeaked());
        			
        			Thread[] threads = StsTestUtil.getAllThreads();
        			for (Thread thread : threads) {
        				if (thread.getName().contains("Stream Monitor")) {
        					fail("A debug UI 'Stream Monitor' thread was left running\n"
        							+StsTestUtil.getStackDumps());
        				}
        			}
        			return true;
        		}
        	}.waitFor(180000);
    	}    	
    }
	
	
    protected String TEST_PROJECT_NAME = this.getClass().getSimpleName();

    protected void assertResourceExists(String path) {
    	IResource rsrc = StsTestUtil.getResource(path);
    	assertTrue("Resource doesn't exist: "+rsrc, rsrc.exists());
    }

    protected boolean isOnClasspath(IProject project, IProject maybeOnClasspath)
            throws JavaModelException {
        IJavaProject jProject = JavaCore.create(project);

        IClasspathEntry[] entries = jProject.getRawClasspath();
        for (IClasspathEntry entry : entries) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                if (entry.getPath().equals(maybeOnClasspath.getFullPath())) {
                    return true;
                }
            }
        }
        return false;
    }

	public void importProject(final URL zipFileURL, final String projectName) throws Exception {
		final Job atomic = new Job("Create project from zip") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject existing = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (existing.exists()) {
						existing.delete(true, true, new NullProgressMonitor());
					}
					// Create project from zip file
					IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
					IPath unzipLoc = workspaceRoot.getLocation();
					ZipFileUtil.unzip(zipFileURL, unzipLoc.toFile(), new NullProgressMonitor());
					project = workspaceRoot.getProject(projectName);
					project.create(null);
					
					if (!project.isOpen()) {
						project.open(null);
					}
				} catch (Exception e) {
					return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "bad", e);
				}
				return Status.OK_STATUS;
			}

		};
		atomic.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		atomic.schedule();
		new ACondition("Wait for import") {
			@Override
			public boolean test() throws Exception {
				IStatus status = atomic.getResult();
				assertNotNull("Import job not yet complete", status);
				assertStatusOK(status);
				return true;
			}
		}.waitFor(60000);
		assertTrue(project.isAccessible());
		assertTrue(project.getFolder("grails-app").exists());
	}

	protected void assertDefaultPluginSourceFolders(IProject project)
			throws JavaModelException {
				GrailsVersion version = GrailsVersion.getGrailsVersion(project);
				if (GrailsVersion.V_2_0_0.compareTo(version)<=0) {
					//TODO: what should we expect here for Grails 1.4 or above?
				} else {
					assertPluginSourceFolder(project, "tomcat-"+GrailsVersion.getDefault(), "src", "groovy");
				}
			}

    /**
     * Attachs Stream listeners to the process associated with launch. The listener echo output and error output
     * to System.err / System.out
     * @param launch
     * @throws IOException 
     */
    protected void dumpOutput(ILaunch launch) throws IOException {
    	IProcess process = launch.getProcesses()[0];
    	IStreamsProxy streams = process.getStreamsProxy();
    	streams.getOutputStreamMonitor().addListener(new AbstractCommandTest.EchoStream(System.out));
    	streams.getErrorStreamMonitor().addListener(new AbstractCommandTest.EchoStream(System.err));
    	streams.write("n\n"); // Satisfy potential UAA dialog that pops up
    }

    String getPageContent(URL url) throws Exception {
    	Object content = null;
    	content = url.getContent();
    	assertTrue("Couldn't get content for: " + url, content != null);
    	InputStream in = (InputStream) content;
    	BufferedReader read = new BufferedReader(new InputStreamReader(in));
    	String s = null;
    	StringBuffer result = new StringBuffer();
    	while ((s = read.readLine()) != null) {
    		result.append(s);
    		result.append("\n");
    	}
    	return result.toString();
    }

}