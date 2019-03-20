/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands.test;

import static org.grails.ide.eclipse.commands.GrailsCommandFactory.createDomainClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.runtime.shared.DependencyFileFormat;
import org.grails.ide.eclipse.runtime.shared.SharedLaunchConstants;
import org.grails.ide.eclipse.test.GrailsTestsActivator;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Tests for the GrailsCommand class.
 * 
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @created 2010-08-04
 */
public class GrailsCommandTest extends AbstractCommandTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GroovyCompilerVersionCheck.testMode();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
//		GrailsCoreActivator.getDefault().setKeepGrailsRunning(true);
	}
	
    /**
     * Relates to https://issuetracker.springsource.com/browse/STS-1874. Example code from
     * https://issuetracker.springsource.com/browse/STS-1880.
     */
    public void testAgentBasedReloading() throws Exception {
        GrailsVersion version = GrailsVersion.MOST_RECENT;
        if (version.compareTo(GrailsVersion.V_2_0_0)>=0 && !GrailsTestsActivator.isJointGrailsTest()) {
            ensureDefaultGrailsVersion(version);
            final String projectName = TEST_PROJECT_NAME;
            project = ensureProject(projectName);
            createResource(project, "grails-app/controllers/ReloadableController.groovy", 
                    "class ReloadableController\n" + 
                            "{\n" + 
                            "   def index = { render \"hello world\" }\n" + 
                    "}");
            StsTestUtil.assertNoErrors(project); // Forces build and checks for compile errors in project.

            final int port = StsTestUtil.findFreeSocketPort();
            ILaunchConfigurationWorkingCopy launchConf = (ILaunchConfigurationWorkingCopy) GrailsLaunchConfigurationDelegate.getLaunchConfiguration(project, "-Dserver.port="+port+" run-app", false);
            
            ILaunch launch = launchConf.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
            dumpOutput(launch);
            final URL url = new URL("http://localhost:"+port+"/"+projectName+"/reloadable/index");
            try {
                new ACondition("hello world") {
                    public boolean test() throws Exception {
                        String page = getPageContent(url);
                        assertEquals("hello world\n", page);
                        return true;
                    }
                }.waitFor(5*60*1000);

                createResource(project, "grails-app/controllers/ReloadableController.groovy", 
                        "class ReloadableController\n" + 
                                "{\n" + 
                                "   def index = { render \"goodbye world\" }\n" + 
                        "}");

                new ACondition("goodbye world") {
                    public boolean test() throws Exception {
                        String page = getPageContent(url);
                        assertEquals("goodbye world\n", page);
                        return true;
                    }
                }.waitFor(30000); // updating contents with SpringLoaded should be faster?
            } finally {
                launch.terminate();
            }
        } else {
            System.out.println("Skipping this test");
        }
    }
    public void testInPlacePluginDependency() throws Exception {
		IProject plugin = ensureProject(this.getClass().getSimpleName()+"-"+"plug-in", true);
		IProject nonPlugin = ensureProject(this.getClass().getSimpleName()+"-"+"NonPlugin");
		IJavaProject jNonPlugin = JavaCore.create(nonPlugin);

		// at first, should not have a dependency and should not be on classpath
		assertFalse("Dependency should not exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));

		GrailsPluginUtil.addPluginDependency(nonPlugin, plugin);

		// dependency should exist, but not on classpath until refresh
		// dependencies is run
		assertTrue("Dependency should exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));

		GrailsCommandUtils.refreshDependencies(jNonPlugin, false);
		assertTrue("Should be on classpath", isOnClasspath(nonPlugin, plugin));
		assertTrue("Dependency should exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));

		// now remove dependency
		GrailsPluginUtil.removePluginDependency(nonPlugin, plugin);

		// dependency should not exist, but still on classpath until refresh
		// dependencies is run
		assertFalse("Dependency should not exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertTrue("Should be on classpath", isOnClasspath(nonPlugin, plugin));

		GrailsCommandUtils.refreshDependencies(jNonPlugin, false);
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));
		assertFalse("Dependency should not exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));

		// try one more time for good show
		GrailsPluginUtil.addPluginDependency(nonPlugin, plugin);

		// dependency should exist, but not on classpath until refresh
		// dependencies is run
		assertTrue("Dependency should exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));

		GrailsCommandUtils.refreshDependencies(jNonPlugin, false);
		assertTrue("Should be on classpath", isOnClasspath(nonPlugin, plugin));
	}

	/**
	 * Execute a command that runs "inside" a Grails project.
	 */
	public void testCreateDomainClass() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		GrailsCommand cmd = createDomainClass(proj, "gTunes.Song");
		ILaunchResult result = cmd.synchExec();
		System.out.println(result.getOutput());
		GrailsTest.assertRegexp("Created.*Song", result.getOutput());
		proj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		assertResourceExists(TEST_PROJECT_NAME+"/grails-app/domain/gTunes/Song.groovy");
		// assertResourceExists(TEST_PROJECT_NAME+"/test/unit/gTunes/SongTests.groovy");
	}

	public void testBogusCommand() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		GrailsCommand cmd = GrailsCommand.forTest(proj,"create-domain")
				.addArgument("gTunes.Album");
		try {
			cmd.synchExec();
			fail("Should have an exception, but didn't have one");
		} catch (CoreException e) {
			if (e.getStatus() instanceof MultiStatus) {
				MultiStatus m = (MultiStatus) e.getStatus();
				if (m.getChildren().length == 2) {
					assertContains("Script 'CreateDomain' not found",
							m.getChildren()[0].getMessage());
					return;
				}
			}
			fail("Incorrect exception thrown.  Status:\n" + e.getMessage());
		}
	}

	/**
	 * Test the hook-up of DependencyExtractingBuildListener as a
	 * "BuildListener" to a grails process.
	 */
	public void testBuildListener() throws Exception {
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		
		GrailsCommand cmd = GrailsCommand.forTest(proj, "compile");
		File tmpFile = File.createTempFile("testListener", ".log");
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		assertFalse(tmpFile.exists());

		// Typical way to pass info to build listener in external process is by
		// setting system properties
		cmd.setSystemProperty(SharedLaunchConstants.DEPENDENCY_FILE_NAME_PROP,
				tmpFile.toString());
		cmd.attachBuildListener(SharedLaunchConstants.DependencyExtractingBuildListener_CLASS);

		cmd.synchExec();

		checkDependencyFile(proj, tmpFile);
	}

	/**
	 * Repeat the above check, but instead of using the "nuts and bolts" just
	 * use enableRefreshDependencyFile(). This should do the same thing (so the
	 * generated dependency file should pass the same checks).
	 */
	public void testBuildListener2() throws Exception {
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		
		GrailsCommand cmd = GrailsCommand.forTest(proj, "compile");
		cmd.enableRefreshDependencyFile();
		File tmpFile = GrailsClasspathUtils.getDependencyDescriptor(proj);
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		assertFalse(tmpFile.exists());

		cmd.synchExec();

		checkDependencyFile(proj, tmpFile);
	}

	/**
	 * Do a few simple checks to see if the contents of a generated dependency
	 * file looks ok. These checks are by no means comprehensive, but it is
	 * better than nothing.
	 * 
	 * @throws IOException
	 */
	protected void checkDependencyFile(IProject project, File file) throws IOException {
		// ///////////////////////////////////////
		// Check the generated file...
		assertTrue(file.exists());

		DependencyData depData = DependencyFileFormat.read(file);

		String dotGrailsFolder = new File(GrailsCoreActivator.getDefault().getUserHome() + "/"
				+ ".grails/" + grailsVersion()).getCanonicalPath();

		// Check plugins directory points where it should
		String pluginsDirectory = depData.getPluginsDirectory();
		if (GrailsVersion.V_2_3_.compareTo(GrailsVersion.MOST_RECENT) <=0) { 
			//Grails 2.3 has moved the plugin area into the project area. It's no longer inside
			// the .grails folder.
			assertEquals(project.getLocation()+"/target/work/plugins",
					pluginsDirectory);
		} else {
			assertEquals(dotGrailsFolder + "/projects/"+TEST_PROJECT_NAME+"/plugins",
					pluginsDirectory);
		}

		Set<String> sources = depData.getSources();
		for (String string : sources) {
			System.out.println(string);
		}
		String[] expectedPlugins = new String[] {
				"tomcat", 
				"hibernate" 
		};
// Checkinf for these makes the tests unstable? No real way to know what is expected here
// It varies depending on plugins and grails version.
		
//		for (String pluginName : expectedPlugins) {
//			for (String javaGroovy : new String[] { "java", "groovy" }) {
//				String expect = pluginsDirectory + "/" + pluginName + "-"
//						+ grailsVersion() + "/src/" + javaGroovy;
//				assertTrue("Missing source entry: " + expect,
//						sources.contains(expect));
//			}
//		}

		Set<String> pluginsXmls = depData.getPluginDescriptors();
		for (String string : pluginsXmls) {
			System.out.println(string);
		}
		for (String pluginName : expectedPlugins) {
			assertPluginXml(pluginName, pluginsXmls);
		}

		// TODO: KDV: (depend) add some more checks of the contents of the file
		// (i.e. check for a few basic jar dependencies that should always be
		// there.)
	}

	private void assertPluginXml(String pluginName, Set<String> pluginsXmls) {
		for (String string : pluginsXmls) {
			if (string.contains(pluginName) && string.endsWith("plugin.xml")) {
				return; //OK, found a entry for that plugin
			}
		}
		fail("No plugin.xml file found for plugin "+pluginName);
	}

	/**
	 * Test to see if after changing application.properties and doing refresh
	 * dependencies, the project source folders look correct.
	 * 
	 * @throws Exception
	 */
	public void testEditedApplicationProperties() throws Exception {
	    // application.properties no longer supported in 2.3 and greater
	    if (GrailsVersion.V_2_3_.compareTo(GrailsVersion.MOST_RECENT) <= 0) {
	        return;
	    }
	    
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		IFile eclipsePropFile = proj.getFile("application.properties");
		File propFile = eclipsePropFile.getLocation().toFile();

		// Modify the props file add two plugins
		Properties props = new Properties();
		props.load(new FileInputStream(propFile)); // Need to close this reader?
		props.put("plugins.feeds", "1.5");
		props.put("plugins.spring-security-core", "1.2.7.3");
		props.store(new FileOutputStream(propFile), "#Grails metadata file");
		eclipsePropFile.refreshLocal(IResource.DEPTH_ZERO,
				new NullProgressMonitor());

		refreshDependencies(proj);

		// Check that the plugins linked source folders are now there.
		assertPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
		assertPluginSourceFolder(proj, "spring-security-core-1.2.7.3", "src", "groovy");

		// /////////////////////////////////////////////////////////////
		// Now modify the version of the plugins and try this again

		props.put("plugins.feeds", "1.6");
		props.put("plugins.spring-security-core", "1.2.7.2");
		props.store(new FileOutputStream(propFile), "#Grails metadata file");
		eclipsePropFile.refreshLocal(IResource.DEPTH_ZERO,
				new NullProgressMonitor());

		// Refresh dependencies
		// GrailsClient.DEBUG_PROCESS = true;
		refreshDependencies(proj);
		// GrailsClient.DEBUG_PROCESS = false;

		// Check that the linked source folders of the replaced version are no
		// longer there.
		assertAbsentPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
		assertAbsentPluginSourceFolder(proj, "spring-security-core-1.2.7.3", "src", "groovy");

		// Check that the linked source folders of the new versions are there.
		assertPluginSourceFolder(proj, "feeds-1.6", "src", "groovy");
		assertPluginSourceFolder(proj, "spring-security-core-1.2.7.2", "src", "groovy");
		
		// /////////////////////////////////////////////////////////////
		// Now remove the plugins and try this again

		props.remove("plugins.feeds");
		props.remove("plugins.spring-security-core");
		props.store(new FileOutputStream(propFile), "#Grails metadata file");
		eclipsePropFile.refreshLocal(IResource.DEPTH_ZERO,
				new NullProgressMonitor());

		// Refresh dependencies
		refreshDependencies(proj);

		// Check that the linked source folders of the replaced version are no
		// longer there.
		assertAbsentPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
		assertAbsentPluginSourceFolder(proj, "spring-security-core-1.2.7.3", "src", "groovy");

		// Check that the linked source folders of the new versions are also no
		// longer there.
		assertAbsentPluginSourceFolder(proj, "feeds-1.6", "src", "groovy");
		assertAbsentPluginSourceFolder(proj, "spring-security-core-1.2.7.2", "src", "groovy");
	}
	
   public void testEditedBuildConfig() throws Exception {
        // only test in 2.3 and greater
        if (GrailsVersion.V_2_3_.compareTo(GrailsVersion.MOST_RECENT) > 0) {
            return;
        }
        ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
        
        final IProject proj = ensureProject(TEST_PROJECT_NAME);
        IFile buildConfig = proj.getFile("grails-app/conf/BuildConfig.groovy");
        String origContents = GrailsTest.getContents(buildConfig);
//        System.out.println("=== BuildConfig.groovy ====");
//        System.out.println(origContents);
//        System.out.println("=== BuildConfig.groovy ====");

        // Modify the props file add two plugins
        // A bit of a hack, but this is a simple way to add plugins to the build
        String newContents = origContents.replace("plugins {\n", "plugins {"
        		+ "\n\t\tcompile \":feeds:1.6\"\n");
        GrailsTest.setContents(buildConfig, newContents);
        
        refreshDependencies(proj);

        // Check that the plugins linked source folders are now there.
        new ACondition("installed feeds-1.6") {
			@Override
			public boolean test() throws Exception {
		        assertPluginSourceFolder(proj, "feeds-1.6", "src", "groovy");
				return true;
			}
		}.waitFor(60000);
      
        // /////////////////////////////////////////////////////////////
        // Now remove the plugins and try this again

//	        props.remove("plugins.feeds");
//	        props.remove("plugins.spring-security-core");
        GrailsTest.setContents(buildConfig, origContents);

        // Refresh dependencies
        refreshDependencies(proj);

        // Check that the linked source folders of the replaced version are no
        // longer there.
        new ACondition("removed feeds") {
        	@Override
        	public boolean test() throws Exception {
        		// Check that the linked source folders of the plugin are no
        		// longer there.
        		assertAbsentPluginSourceFolder(proj, "feeds-1.6", "src", "groovy");

        		return true;
        	}
        }.waitFor(10000);

    }

	   
	private void refreshDependencies(IProject proj) throws CoreException {
		try {
			GrailsCommandUtils.refreshDependencies(JavaCore.create(proj), true);
		} catch (Exception e) {
			// as of Grails 2.3, we really should only need to run refresh dependencies once
			// but below that, we need to do it twice
	        if (GrailsVersion.V_2_3_.compareTo(GrailsVersion.MOST_RECENT) > 0) {
		        try {
		        	GrailsCommandUtils.refreshDependencies(JavaCore.create(proj), true);
		        } catch (Exception e2) {
		        	throw new RuntimeException(e2);
		        }
	        } else {
	        	throw new RuntimeException(e);
	        }
		}
	}
	
	
	public void testTagLibsFromPlugin() throws Exception {
	    
	    IProject proj = ensureProject(TEST_PROJECT_NAME);
	    ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(proj));
	    
        if (GrailsVersion.V_2_3_.compareTo(GrailsVersion.MOST_RECENT) > 0) {
            // below 2.3 install-plugin command works
    	    GrailsCommand cmd = GrailsCommand.forTest(proj, "install-plugin")
    	    		.addArgument("feeds")
    	    		.addArgument("1.6");
    	    cmd.synchExec();
        } else {
            // after 2.3, must edit build config
            IFile buildConfig = proj.getFile("grails-app/conf/BuildConfig.groovy");
            String origContents = GrailsTest.getContents(buildConfig);

            // Modify the props file add two plugins
            // A bit of a hack, but this is a simple way to add plugins to the build
            String newContents = origContents.replace("plugins {\n", "plugins {\n\t\tcompile \":feeds:1.6\"\n");
            GrailsTest.setContents(buildConfig, newContents);
        }
        refreshDependencies(proj);
		
	    assertPluginSourceFolder(proj, "feeds-1.6", "src", "groovy");
	    
	    // now also check to see that the tag is available
	    PerProjectTagProvider provider = GrailsCore.get().connect(proj, PerProjectTagProvider.class);
	    assertNotNull("feeds:meta tag not installed", provider.getDocumentForTagName("feed:meta"));
    }
	
	public void testOutputLimit() throws Exception {
	    IProject proj = ensureProject(TEST_PROJECT_NAME);
	    ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(proj));
	    
		GrailsCommand cmd = GrailsCommand.forTest("help");
		ILaunchResult result = cmd.synchExec();
//		String allOutput = result.getOutput();
		
		int orgLimit = GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit();
		try {
			GrailsCoreActivator.getDefault().setGrailsCommandOutputLimit(100);
			result = cmd.synchExec();
			assertEquals(100, result.getOutput().length());
						
		} finally {
			GrailsCoreActivator.getDefault().setGrailsCommandOutputLimit(orgLimit);
		}
	}
	
	// /**
	// * Test grails command to build "exploded" war file inside the target
	// directory (rather than a ".war" archive). To do this
	// * we need to somehow set additional properties in the grails build
	// settings that we do not ordinarily have access to)
	// * from the command line (only can be set in the BuildSettings.groovy
	// config file). So the this test implicitly checks
	// * the mechanism hacked-up here to set properties in BuildSettings)
	// */
	// public void testExplodedWar() throws Exception {
	// IProject proj = ensureProject(TEST_PROJECT_NAME);
	// GrailsCommand cmd = new GrailsCommand(proj, "dev war");
	// cmd.
	// }

	/**
	 * What's the default grails version that we expect new projects to be
	 * created with.
	 */
	private String grailsVersion() {
		return GrailsCoreActivator.getDefault().getInstallManager()
				.getDefaultGrailsInstall().getVersionString();
	}
	
}
