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
package org.grails.ide.eclipse.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import junit.framework.AssertionFailedError;

import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.SourceFolderJob;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.core.FileUtil;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.commands.test.AbstractCommandTest;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.grails.ide.eclipse.ui.internal.properties.GrailsInstallPropertyPage;

/**
 * @author Kris De Volder
 */
public class GrailsProjectVersionFixerTest extends AbstractCommandTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GrailsProjectVersionFixer.DEBUG = true;
		GrailsCommandUtils.DEBUG = true; // Want to see info about refresh dependencies ops being executed.
		
		GroovyCompilerVersionCheck.testMode();
//		GrailsCoreActivator.getDefault().setKeepGrailsRunning(true);
		GrailsProjectVersionFixer.globalAskToUpgradeAnswer = true;
		GrailsProjectVersionFixer.globalAskToConvertToGrailsProjectAnswer = true;
		StsTestUtil.setAutoBuilding(true);
		System.out.println("Waiting for autobuild...");
		StsTestUtil.waitForAutoBuild();
		System.out.println("Done autobuild");
		
		// Ensure the test project doesn't exist in the workspace before test begins
		StsTestUtil.deleteAllProjects();
		//clearGrailsState(); //Workaround for http://jira.grails.org/browse/GRAILS-7655 (ivy cache corruption)
	}
	
	@Override
	protected void tearDown() throws Exception {
		GrailsProjectVersionFixer.testMode(); // reset "precanned answers" for other test runs.
		super.tearDown();
		GrailsProjectVersionFixer.DEBUG = false;
		GrailsCommandUtils.DEBUG = false;
	}
	
//	public void testImportVeryOldProjectAsLinkedProject() throws Throwable {
//		final URL zipFileURL = this.getClass().getClassLoader().getResource("OldTestApp.zip");
//		final String projectName = "OldTestApp";
//		
//		importAsLinkedProject(zipFileURL, projectName);
//		
//		/////////////////////////////////////////////////////////////////////////////////////////////////
//		//Check a few things about this test project
//		checkImportedProject();
//	}

	/**
	 * Tests whether our 'ensureDefaultGrailsVersion' test utility method works properly.
	 */
	public void testEnsureDefaultGrailsVersion() throws Throwable {
		//Yes, we swithc back and forth quite a few times. This ensures that all code paths
		//through 'ensureDefaultGrailsVersion' get executed (different paths when install 
		//was already configured).
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS_PREVIOUS);
		assertEquals(GrailsVersion.PREVIOUS_PREVIOUS, GrailsVersion.getDefault());
		
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS);
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getDefault());
		
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		assertEquals(GrailsVersion.MOST_RECENT, GrailsVersion.getDefault());
		
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS);
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getDefault());
		
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS_PREVIOUS);
		assertEquals(GrailsVersion.PREVIOUS_PREVIOUS, GrailsVersion.getDefault());
	}

	/**
	 * @param zipFileURL
	 * @param projectName
	 * @throws Throwable
	 */
	public void importAsLinkedProject(final URL zipFileURL,
			final String projectName) throws Throwable {
		WorkspaceJob atomic = new WorkspaceJob("Create linked project from zip") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				try {
					// Create project folder from zipfile in some random place outside the workspace
					File unzipLoc = FileUtil.createTempDirectory();
					ZipFileUtil.unzip(zipFileURL, unzipLoc, new NullProgressMonitor());
					
					IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
					project = workspaceRoot.getProject(projectName);
					IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
					desc.setLocation(new Path(unzipLoc.toString()).append(project.getName()));
					project.create(desc, null);
					project.open(null);
//					project.refreshLocal(IResource.DEPTH_INFINITE, null);
					
				} catch (IOException e) {
					return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "bad", e);
				}
				return Status.OK_STATUS;
			}
		};
		IStatus status = atomic.run(new NullProgressMonitor());
		assertOK(status);
		assertTrue(project.isAccessible());
		assertTrue(project.getFolder("grails-app").exists());
	}
	
	public static void assertOK(IStatus status) throws Throwable {
		if (status.isOK()) return;
		if (status.getException()!=null)
			throw status.getException();
		throw new AssertionFailedError(status.getMessage());
	}
	
	public void pl() throws Throwable {
		final URL zipFileURL = this.getClass().getClassLoader().getResource("OldTestApp.zip");
		final String projectName = "OldTestApp";
		
		importProject(zipFileURL, projectName);
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		checkImportedProject();
	}

//	/**
//	 * Test for the case were a project using an older version of grails is being imported, where the
//	 * older version is not configured in the workspace and the user asks to upgrade the project.
//	 */
//	public void testImportNotSoOldProject() throws Throwable {
//		final URL zipFileURL = this.getClass().getClassLoader().getResource("NotSoOldTestApp.zip");
//		final String projectName = "NotSoOldTestApp";
//		
//		importProject(zipFileURL, projectName);
//		
//		/////////////////////////////////////////////////////////////////////////////////////////////////
//		//Check a few things about this test project
//		checkImportedProject();
//	}
	
	/**
	 * Test for the case where we import a project that doesn't have eclipse specific files committed.
	 * So all configuration of Eclipse stuff has to be done by our fixer.
	 * <p>
	 * This test is a kind of 'proxy' for importing projects from SVN/CVS, where none of the Eclipse settings
	 * files have been committed.
	 * <p>
	 * The project being imported has a version that is the same as the default Grails install of the
	 * workspace.
	 */
	public void testImportBareBonesProjectWithMatchingVersion() throws Throwable {
		GrailsVersion version = GrailsVersion.MOST_RECENT;
		ensureDefaultGrailsVersion(version);
		final URL zipFileURL = getProjectZip("bareBonesProject", version);
		final String projectName = "testProject";
		
		importProject(zipFileURL, projectName);
		
		// This test is intended to check importing of project who's grails version is the same as
		// the workspace default install...
		assertEquals(GrailsVersion.getDefault(), GrailsVersion.getGrailsVersion(project));
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		checkImportedProject();
	}

	/**
	 * Test for the case where we import a project that doesn't have eclipse specific files committed.
	 * So all configuration of Eclipse stuff has to be done by our fixer.
	 * <p>
	 * The project being imported has a version that is older than the default Grails install of the
	 * workspace.
	 */
	public void testImportBareBonesProjectWithOlderVersion() throws Throwable {
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);

		final URL zipFileURL = getProjectZip("bareBonesProject", GrailsVersion.PREVIOUS);
		final String projectName = "testProject";

		importProject(zipFileURL, projectName);

		// This test is intended to check importing of project who's grails version is older than
		// the workspace default install...
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getGrailsVersion(project));

		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		checkImportedProject();
	}
	
	/**
	 * Checks a bunch of stuff about the "very old test project" once it has been imported in
	 * the workspace.
	 * 
	 * @throws Throwable
	 */
	private void checkImportedProject() throws Exception {

		//Check project config, like classpath related stuff. 
		// The config may not be right initially... but should eventually become correct as a background
		// refresh dependency job should get scheduled. 
		// ACondition
		new ACondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("Checking project config...");
				IJavaProject javaProject = JavaCore.create(project);
				
				assertDefaultOutputFolder(javaProject);
				assertTrue(project.hasNature(JavaCore.NATURE_ID)); // Should have Java Nature at this point
				assertTrue(GroovyNature.hasGroovyNature(project)); // Should also have Groovy nature
				assertTrue(GrailsNature.isGrailsAppProject(project)); // Should look like a Grails app to grails tooling
				
				///////////////////////////////////
				// Check resolved classpath stuff
				IClasspathEntry[] classPath = javaProject.getResolvedClasspath(false);
				
				// A whole bunch of libraries should be there, check for just a few of those
				
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "/jsse.jar", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "grails-core", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "grails-bootstrap", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "groovy-all", classPath);
//				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "servlet-api", classPath);
				
//				System.out.println(">>>Resolved classpath");
//				for (IClasspathEntry entry : classPath) {
//					System.out.println(kindString(entry.getEntryKind())+": "+entry.getPath());
//				}
//				System.out.println("<<<Resolved classpath");
				
				///////////////////////////////////
				// Check raw classpath stuff
				classPath = javaProject.getRawClasspath();
//				for (IClasspathEntry classpathEntry : classPath) {
//					System.out.println(kindString(classpathEntry.getEntryKind())+": "+classpathEntry.getPath());
//				}

				//The usual source folders:
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "src/java", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "src/groovy", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/conf", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/controllers", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/domain", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/services", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/taglib", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "test/integration", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "test/unit", classPath);

				//The classpath containers for Java and Grails
				assertClassPathEntry(IClasspathEntry.CPE_CONTAINER, "org.eclipse.jdt.launching.JRE_CONTAINER", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_CONTAINER, "org.grails.ide.eclipse.core.CLASSPATH_CONTAINER", classPath);
				
				//Installed plugins source folders
				assertDefaultPluginSourceFolders(project);
				
				System.out.println("Checking project config => OK!");
				return true;
			}

		}.waitFor(120000);
	}
	
	/**
	 * Relates to https://issuetracker.springsource.com/browse/STS-1518. Fix for this bug revamped representation
	 * of plugin source folders in STS projects. This creates a need to clean up legacy linked source folders.
	 * <p>
	 * This assert checks whether there is anything looking like a legacy source folder 
	 * @throws Exception 
	 * @throws CoreException 
	 */
	public void testCleanupLegacyLinkedSourceFolders() throws Exception {
		final GrailsVersion version = GrailsVersion.V_1_3_6;
		ensureDefaultGrailsVersion(version);
		final String projectName = "gTunes"; // This project was zipped before we converted to the new source folder layout
		final URL zipFileURL = getProjectZip(projectName, version);
		
		importProject(zipFileURL, projectName);
		
		new ACondition() {
			@Override
			public boolean test() throws Exception {
				assertNoLegacyLinkedFolders();
				return true;
			}
		}.waitFor(120000);
	}
	
	private void assertNoLegacyLinkedFolders() throws CoreException {
		IResource[] children = project.members(true);
		boolean seenALink = false;
		for (IResource r : children) {
			if (r.isLinked()) {
				seenALink = true;
				// The only thing we expect to see is a single link to the toplevel of the grails plugins folder
				assertEquals("Legacy link not cleaned up?", SourceFolderJob.PLUGINS_FOLDER_LINK, r.getName());
			}
		}
		assertTrue("No linked resources found in project. Expected a link called: "+SourceFolderJob.PLUGINS_FOLDER_LINK, seenALink);
	}
	
	public void testSTS1604UserChangesProjectSpecificGrailsInstall() throws Throwable {
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>testSTS1604UserChangesProjectSpecificGrailsInstall");
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS);
		final String projectName = "gTunes";
		final URL zipFileURL = getProjectZip(projectName, GrailsVersion.PREVIOUS);
		importProject(zipFileURL, projectName);
		
		//Double check that project was created ok with correct version numbers in both eclipse settings and application.properties
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getGrailsVersion(project));
		checkImportedProject();

		GrailsInstallPropertyPage.setInstall(project, true, GrailsVersion.MOST_RECENT.getInstall().getName());

		new ACondition() {
			@Override
			public boolean test() throws Exception {
				assertEquals("Project eclipse conf not correct", GrailsVersion.MOST_RECENT, GrailsVersion.getEclipseGrailsVersion(project));
				assertEquals("Project not upgraded", GrailsVersion.MOST_RECENT, GrailsVersion.getGrailsVersion(project));
				return true;
			}
		}.waitFor(120000);
		checkImportedProject();
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<testSTS1604UserChangesProjectSpecificGrailsInstall");
	}
	
	public void testChangeDefaultGrails() throws Exception {
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS);
		final String projectName = "gTunes";
		final URL zipFileURL = getProjectZip(projectName, GrailsVersion.PREVIOUS);
		importProject(zipFileURL, projectName);
		
		//Double check that project was created ok with correct version numbers in both eclipse settings and application.properties
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(GrailsVersion.PREVIOUS, GrailsVersion.getGrailsVersion(project));
		
		GrailsProjectVersionFixer.globalAskToUpgradeAnswer = true;
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT); 
		
		// Project should get upgraded... eventually
		new ACondition() {
			public boolean test() throws Exception {
				ACondition.assertJobManagerIdle();
				assertEquals("Eclipse Grails version", GrailsVersion.MOST_RECENT, GrailsVersion.getEclipseGrailsVersion(project));
				assertEquals("application.properties Grails version", GrailsVersion.MOST_RECENT, GrailsVersion.getGrailsVersion(project));
				assertTrue("Project should inherit default install", GrailsInstallManager.inheritsDefaultInstall(project));
				
				return true;
			}
		}.waitFor(180000);
		
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS); 
		
		// Project should get downgraded... eventually
		new ACondition() {
			public boolean test() throws Exception {
				ACondition.assertJobManagerIdle();
				assertEquals("Eclipse Grails version", GrailsVersion.PREVIOUS, GrailsVersion.getEclipseGrailsVersion(project));
				assertEquals("application.properties Grails version", GrailsVersion.PREVIOUS, GrailsVersion.getGrailsVersion(project));
				assertTrue("Project should inherit default install", GrailsInstallManager.inheritsDefaultInstall(project));
				
				return true;
			}
		}.waitFor(120000);
		
		assertTrue("Test project for this test should be configured to inherit the default install", GrailsInstallManager.inheritsDefaultInstall(project));
		
	}
	
	public void testImportProjectWithSameNameAsDeletedProject() throws Exception {
		//When a project is deleted, stale classpath data of this project should not end up in a new project that may have the same name.
		final String projectName = "gTunes";

		//Get gTunes project with older version in workspace
		GrailsVersion version = GrailsVersion.PREVIOUS;
		ensureDefaultGrailsVersion(version);
		URL zipFileURL = getProjectZip(projectName, version);
		importProject(zipFileURL, projectName);
		assertEquals(version, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(version, GrailsVersion.getGrailsVersion(project));
		checkImportedProject();
		
		project.delete(true, true, new NullProgressMonitor());
		
		version = GrailsVersion.MOST_RECENT;
		ensureDefaultGrailsVersion(version);
		zipFileURL = getProjectZip(projectName, version);
		importProject(zipFileURL, projectName);
		assertEquals(version, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(version, GrailsVersion.getGrailsVersion(project));
		checkImportedProject();
	}
	
	public void testSTS1799ImportSpringSecurityACLSample() throws Exception {
		//Sample project from http://burtbeckwith.github.com/grails-spring-security-acl/docs/manual/index.html
		// But with upgraded plugins to most recent version available now and compatible with Grails 1.3.7
		final GrailsVersion version = GrailsVersion.V_1_3_7;
//		assertTrue("This test's test project needs updating to a more recent Grails version", 
//				version.equals(GrailsVersion.MOST_RECENT) || version.equals(GrailsVersion.PREVIOUS));
		String projectName = "grails-contacts";
		
		ensureDefaultGrailsVersion(version);
		
		System.out.println(">>>>> Grails installs known to STS");
		Collection<IGrailsInstall> installs = GrailsCoreActivator.getDefault().getInstallManager().getAllInstalls();
		for (IGrailsInstall install : installs) {
			System.out.println("--------------------------");
			System.out.println("name          = '"+install.getName()+"'");
			System.out.println("version       = '"+install.getVersion()+"'");
			System.out.println("versionString = '"+install.getVersionString()+"'");
			System.out.println("home          = '"+install.getHome()+"'");
			System.out.println("isDefault     = '"+install.isDefault()+"'");
		}
		System.out.println("<<<<< Grails installs known to STS ====");
		
		URL zipFileURL = getProjectZip(projectName, version);
		importProject(zipFileURL, projectName);
		new ACondition() {	//Need ACondition on build server because initially will be '<unknown>' grails version
			//until version fixer has run and fixed the problem.
			@Override
			public boolean test() throws Exception {
				assertEquals(version, GrailsVersion.getEclipseGrailsVersion(project));
				return true;
			}
		}.waitFor(3000);
		assertEquals(version, GrailsVersion.getGrailsVersion(project));
		checkImportedProject();
		StsTestUtil.assertNoErrors(project);
	}
	
	public void testImportProjectWithUnknownEclipseGrailsVersion() throws Exception {
		//The project being imported has following setup
		//  - uses project specific Grails install
		//  - Grails install name = 'Bogus X.X.X' 
		//This will cause GrailsVersion.getEclipseGrailsVersion() to return an 'unknown' Grails install.
		final GrailsVersion version = GrailsVersion.MOST_RECENT;
		String projectName = "BogusEclipseVersion";
		
		ensureDefaultGrailsVersion(version);
		
		URL zipFileURL = getProjectZip(projectName, version);
		importProject(zipFileURL, projectName);
		new ACondition() {
			@Override
			public boolean test() throws Exception {
				assertEquals(version, GrailsVersion.getEclipseGrailsVersion(project));
				return true;
			}
		}
		.waitFor(3000);
		assertEquals(version, GrailsVersion.getGrailsVersion(project));
		checkImportedProject();
		StsTestUtil.assertNoErrors(project);
	}
	
	//TODO: add a test for case "user changes global default" with multiple projects in workspace using global default
	
}
