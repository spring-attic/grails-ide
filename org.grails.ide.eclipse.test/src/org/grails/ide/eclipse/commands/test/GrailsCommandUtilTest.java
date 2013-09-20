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

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.classpath.SourceFolderJob;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsLaunchUtils;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Tests for (portions of) the {@link GrailsCommandUtils} class. 
 * @author Kris De Volder
 * @created 2010-08-04
 */
public class GrailsCommandUtilTest extends AbstractCommandTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GrailsProjectVersionFixer.testMode();
		GroovyCompilerVersionCheck.testMode();
		//Ensure prior test projects don't exist
		StsTestUtil.deleteAllProjects();
		//clearGrailsState(); //Workaround for http://jira.grails.org/browse/GRAILS-7655 (ivy cache corruption)
	}
	
	/**
	 * This test will fail on the buildsite if proxy system properties are not what we expect.
	 * This may indicate a problem with proxy settings in the build, or it may indicate that
	 * proxy situation on the build server has changed. In the latter case, this test should
	 * be updated.
	 */
	// DISABLED!!!
	public void _testProxy() {
//		if (StsTestUtil.isOnBuildSite()) {
			Map<String, String> props = GrailsCoreActivator.getDefault().getLaunchSystemProperties();
		//	assertProp(props, "http.proxyHost", "proxy.eng.vmware.com");
		//	assertProp(props, "http.proxyPort", "3128");
		//	assertProp(props, "https.proxyHost", "proxy.eng.vmware.com");
		//	assertProp(props, "https.proxyPort", "3128");
			//Proxy settings no longer required on build server and should *not* be set
			assertProp(props, "http.proxyHost", null);
			assertProp(props, "http.proxyPort", null);
			assertProp(props, "https.proxyHost", null);
			assertProp(props, "https.proxyPort", null);
//		}
	}

	private void assertProp(Map<String, String> props, String k, String v) {
		assertEquals(k, v, props.get(k));
	}

	/**
	 * Test to see if we can use the "eclipsifyProject" method to "eclipsify" a very bare-bones
	 * imported grails project that is missing just about *everything* in the way of configuration files.
	 * <p>
	 * This bare bones project is created by deleting all Eclipse related stuff and the target directory
	 * from a grails project and then importing this into eclipse as a "general" project. Thus it has the
	 * minimum, of configuration to make it a valid Eclipse project but nothing more.
	 */
	public void testEclipsifyBareBonesProject() throws Exception {
		GrailsVersion version = GrailsVersion.MOST_RECENT;
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run this for snapshot builds. Too much work to create test projects for moving target.
			return;
		}
		ensureDefaultGrailsVersion(version); // The version of the BareBones project
		URL bareBonesURL = getProjectZip("bareBonesProject", version);

		// Create bare bones project from zip file
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		project = workspaceRoot.getProject("testProject");
		IPath unzipLoc = workspaceRoot.getLocation();
		ZipFileUtil.unzip(bareBonesURL, unzipLoc.toFile(), new NullProgressMonitor());
		project.create(new NullProgressMonitor());
		assertTrue(project.exists());
		project.open(new NullProgressMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(project.isAccessible());
		assertTrue(project.isAccessible());
		assertTrue(project.getFolder("grails-app").exists());
		
		GrailsCommandUtils.eclipsifyProject(null, true, project);
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		
		assertTrue(project.hasNature(JavaCore.NATURE_ID)); // Should have Java Nature at this point
		assertTrue(GroovyNature.hasGroovyNature(project)); // Should also have Groovy nature

		IJavaProject javaProject = JavaCore.create(project);
		
		assertDefaultOutputFolder(javaProject);
		
		IClasspathEntry[] classPath = javaProject.getRawClasspath();
		for (IClasspathEntry classpathEntry : classPath) {
			System.out.println(kindString(classpathEntry.getEntryKind())+": "+classpathEntry.getPath());
		}
		
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
		assertBareBonesProjectUserPluginSourceFolders(version);
		assertDefaultPluginSourceFolders(project);
		
		assertCharset(project);
	}

	/**
	 * Asserts that all message.properties files have the proper charsets
     * @param project
	 * @throws CoreException 
     */
    private void assertCharset(IProject project) throws CoreException {
        IFolder i18nFolder = project.getFolder("grails-app/i18n");
        internalAssertCharset(i18nFolder);
        i18nFolder = project.getFolder(".link_to_grails_plugins/feeds-1.5/grails-app/i18n");
        if (i18nFolder.exists()) {
            internalAssertCharset(i18nFolder);
        }
    }

    /**
     * @param i18nFolder
     * @throws CoreException
     */
    public void internalAssertCharset(IFolder i18nFolder) throws CoreException {
        assertEquals("Invalid charset encoding", "UTF-8", i18nFolder.getDefaultCharset(true));
        for (IResource member : i18nFolder.members()) {
            if (member.getType() == IResource.FILE) {
                assertEquals("Invalid charset encoding", "UTF-8", ((IFile) member).getCharset(true));
            } else {
                assertEquals("Invalid charset encoding", "UTF-8", ((IFolder) member).getDefaultCharset(true));            
            }
        }
    }

    /**
	 * Check some expected source folders for user installed plugins that are
	 * contained in the 'barebones' zipped test project.
	 */
	private void assertBareBonesProjectUserPluginSourceFolders(
			GrailsVersion version) throws JavaModelException {
		String feedsVersion;
		if (GrailsVersion.V_2_3_.compareTo(version)<=0) {
			feedsVersion = "1.6";
		} else {
			feedsVersion = "1.5";
		}
		assertPluginSourceFolder(project, "feeds-"+feedsVersion, "grails-app", "taglib");
		assertPluginSourceFolder(project, "feeds-"+feedsVersion, "src", "groovy");
	}

	/**
	 * Test for https://issuetracker.springsource.com/browse/STS-1348
	 * <p>
	 * Does eclipsifyProject work also if the project is linked rather than physically in the workspace.
	 */
	public void testEclipsifyLinkedProject() throws Exception {
		GrailsVersion version = GrailsVersion.MOST_RECENT;
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run this for SS builds. Too much work to create test projects for moving target.
			return;
		}
		ensureDefaultGrailsVersion(version); // The version of the BareBones project
		URL bareBonesURL = getProjectZip("bareBonesProject", version);

		// Create bare bones project from zip file
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		
		File unzipLoc = StsTestUtil.createTempDirectory();
		ZipFileUtil.unzip(bareBonesURL, unzipLoc, new NullProgressMonitor());
		
		project = workspaceRoot.getProject("testProject");
		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		desc.setLocation(new Path(unzipLoc.toString()).append(project.getName()));
		project.create(desc, null);
		project.open(null);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		
		assertTrue(project.isAccessible());
		assertTrue(project.getFolder("grails-app").exists());
		
		GrailsCommandUtils.eclipsifyProject(null, true, project);
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		checkProjectBasics();
		
		assertBareBonesProjectUserPluginSourceFolders(version);
	}

	private void checkProjectBasics() throws CoreException, JavaModelException {
		assertTrue(project.hasNature(JavaCore.NATURE_ID)); // Should have Java Nature at this point
		assertTrue(GroovyNature.hasGroovyNature(project)); // Should also have Groovy nature

		IJavaProject javaProject = JavaCore.create(project);
		
		assertDefaultOutputFolder(javaProject);
		
		IClasspathEntry[] classPath = javaProject.getRawClasspath();
		for (IClasspathEntry classpathEntry : classPath) {
			System.out.println(kindString(classpathEntry.getEntryKind())+": "+classpathEntry.getPath());
		}
		
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
		
		assertCharset(project);
	}
	
	public void testCreateAndEclipsifyLinkedProject() throws Exception {
		String projName = "test-linked-project";
		GrailsCommand createApp = GrailsCommandFactory.createApp(projName);
		final File projectLoc = new File(StsTestUtil.createTempDirectory(), projName);
		createApp.setPath(projectLoc.getParent());
		createApp.synchExec();
		GrailsCommandUtils.eclipsifyProject(null, true, new Path(projectLoc.getAbsolutePath()));
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		
		checkProjectBasics();
	}
	
	/**
	 * This test isn't actually testing stuff from {@link GrailsCommandUtils}, but it is in here because
	 * it uses some similar checks that a project is properly setup after doing a "raw" import of project
	 * created outside eclipse using grails commands.
	 * <p>
	 * A "raw" import means simply pulling in a project from files from somewhere outside eclipse and doing 
	 * nothing more than adding them as a new project to the workspace. As of Grails 1.3.5 this should produce
	 * a correctly configured grails project (grails 1.3.5 produces correct .project etc. files). So there
	 * should be no need to call 'eclipsify' on such projects.
	 */
	public void testImportExistingProject() throws Throwable {
		GrailsProjectVersionFixer.globalAskToConvertToGrailsProjectAnswer = true;
		System.out.println(">>>>>>>>>>>>>>>>>>>testImportExistingProject");
		GroovyCompilerVersionCheck.testMode();
		ensureDefaultGrailsVersion(GrailsVersion.V_1_3_5); // GrailsVersion of test project!
		StsTestUtil.setAutoBuilding(true);
		System.out.println("Waiting for autobuild...");
		StsTestUtil.waitForAutoBuild();
		System.out.println("Done autobuild");
		
		URL bareBonesURL = this.getClass().getClassLoader().getResource("external-project.zip");

		String projectName = "External";
		// Create bare bones project from zip file
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		
		IPath unzipLoc = workspaceRoot.getLocation();
		ZipFileUtil.unzip(bareBonesURL, unzipLoc.toFile(), new NullProgressMonitor());
		
		project = workspaceRoot.getProject(projectName);
		project.create(null);
		
		if (!project.isOpen()) {
			project.open(null);
		}
		
		assertTrue(project.isAccessible());
		assertTrue(project.getFolder("grails-app").exists());
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		
		assertTrue(project.hasNature(JavaCore.NATURE_ID)); // Should have Java Nature at this point
		assertTrue(GroovyNature.hasGroovyNature(project)); // Should also have Groovy nature

		final IJavaProject javaProject = JavaCore.create(project);
				
		//Next we check the classpath related stuff. 
		// The classpath may not be right initially... but should eventually become correct as a background
		// refresh dependency job should get scheduled. 
		// ACondition
		new ACondition() {
			@Override
			public boolean test() throws Exception {
				System.out.println("Checking project config...");
				assertEquals("/"+project.getName()+"/target-eclipse/classes", javaProject.getOutputLocation().toString());
				
				///////////////////////////////////
				// Check resolved classpath stuff
				IClasspathEntry[] classPath = javaProject.getResolvedClasspath(false);
				
				// A whole bunch of libraries should be there, check for just a few of those

				// this one will fail on macs
//				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "jre/lib/rt.jar", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "grails-core", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "grails-bootstrap", classPath);
				assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "groovy-all", classPath);
				//assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "servlet-api", classPath);
				
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

		        assertCharset(project);

				System.out.println("Checking project config => OK!");
				return true;
			}
		}.waitFor(60000);

		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<testImportExistingProject");
	}
	
	/**
	 * User's can modify where the Grails stores the plugins for their project by setting a magic
	 * variable inside their BuildConfig.groovy. This test verifies whether refresh dependencies properly
	 * picks up on a changed plugin directory (change is reflected in the link).
	 * @throws Exception 
	 */
	public void testChangingGrailsProjectPluginsDir() throws Exception {
		project = ensureProject( this.getClass().getSimpleName());
		
		DependencyData data = PerProjectDependencyDataCache.get(project);
		String pluginsDir = data.getPluginsDirectory();
		
		assertPluginsLink(pluginsDir);
		
		File newPluginsDir = StsTestUtil.createTempDirectory();
		setBuildConfigVariable(project, "grails.project.plugins.dir", newPluginsDir.getCanonicalPath());
		
		GrailsCommandUtils.refreshDependencies(JavaCore.create(project), true);
		
		data = PerProjectDependencyDataCache.get(project);
		pluginsDir = data.getPluginsDirectory();
		assertEquals("Editing BuildConfig.groovy to set 'grails.project.plugins.dir' didn't impact STS depenendency data", 
				newPluginsDir.getCanonicalPath(), pluginsDir);
		assertPluginsLink(pluginsDir);
		assertCharset(project);
	}

	protected void setBuildConfigVariable(IProject project, String varName, String value) throws IOException, CoreException {
		IFile buildConfig = project.getFile("grails-app/conf/BuildConfig.groovy");
		File file = buildConfig.getLocation().toFile();
		String contents = FileUtils.readFileToString(file);
		int locationOfVar = contents.indexOf(varName+"=");
		if (locationOfVar>=0) {
			// Try to replace existing def
			int endOfLine = contents.indexOf('\n', locationOfVar);
			contents = contents.substring(0, locationOfVar) 
				+ varDef(varName, value)
				+ contents.substring(endOfLine);
		} else {
			// Insert new def at start of file
			contents = varDef(varName, value) + "\n" + contents;
		}
		FileUtils.writeStringToFile(file, contents);
		buildConfig.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
	}

	/**
	 * Relates to https://issuetracker.springsource.com/browse/STS-1832 and
	 * https://issuetracker.springsource.com/browse/STS-1799
	 * <p>
	 * Spring security ACL plugin has a script that copies classes from the plugin
	 * into the user's code. This causes duplicate class def in STS.
	 * <p>
	 * However, simply adding exclusions filters to the CP will cause
	 * trouble for users who do not run the copying script.
	 * @throws CoreException 
	 */
	public void _testSpringSecurityACLClassPathExclusions() throws Exception {
		if (GrailsVersion.MOST_RECENT.equals(GrailsVersion.V_2_1_0_revisit)) {
			//This test is known to fail since Grails 2.0 RC1: still failing in Grails 2.0 final
			//See: http://jira.grails.org/browse/GRAILS-8198
			return;
		}
		
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		project = ensureProject("ACLTest");
		final IJavaProject javaProject = JavaCore.create(project);
		
		//First try without running the script... there should be no exclusions in the CP
		
		System.out.println(">>> Installing plugin 'spring-security-acl' ...");
		System.out.println(GrailsCommandFactory.installPlugin(project, "spring-security-acl").synchExec());
		System.out.println("<<< Installing plugin 'spring-security-acl'");
		GrailsCommandUtils.refreshDependencies(javaProject, false);
		
		IClasspathEntry[] rawClassPath = javaProject.getRawClasspath();
		IClasspathEntry entry = assertPluginSourceFolder(rawClassPath, "spring-security-acl", "grails-app/domain");
		assertNoExclusions(entry);
		StsTestUtil.assertNoErrors(project);
		
		// Now try running the copying script...
		System.out.println(">>> Running 's2-create-acl-domains' command ...");
		System.out.println(GrailsCommand.forTest(project, "s2-create-acl-domains").synchExec());
		System.out.println("<<< Running 's2-create-acl-domains' command ");
		GrailsCommandUtils.refreshDependencies(javaProject, false);
		
		rawClassPath = javaProject.getRawClasspath();
		entry = assertPluginSourceFolder(rawClassPath, "spring-security-acl", "grails-app/domain");
		assertAclDomainExclusions(entry);
		StsTestUtil.assertNoErrors(project);
	}

	private void assertAclDomainExclusions(IClasspathEntry entry) {
		assertExclusions(entry, "**/AclClass.groovy",
				"**/AclEntry.groovy",
				"**/AclObjectIdentity.groovy",
				"**/AclSid.groovy");
	}
	
	/**
	 * This test does the same as the one above, but it uses GrailsLaunchUtils to run the
	 * grails command (the method called by the grails command prompt). 
	 * <p>
	 * Purpose of this test is to see that the launcher logic is doing the right thing
	 * w.r.t. running refresh dependencies after executing the command.
	 */
	public void _testSpringSecurityACLClassPathExclusionsLaunch() throws Exception {
		if (GrailsVersion.MOST_RECENT.equals(GrailsVersion.V_2_1_0_revisit)) {
			//This test is known to fail in RC1:
			//See: http://jira.grails.org/browse/GRAILS-8198
			return;
		}
		
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		project = ensureProject("ACLTest2");
		final IJavaProject javaProject = JavaCore.create(project);
		
		//First try without running the script... there should be no exclusions in the CP
		
		System.out.println(">>> Installing plugin 'spring-security-acl' ...");
		System.out.println(GrailsCommandFactory.installPlugin(project, "spring-security-acl").synchExec());
		System.out.println("<<< Installing plugin 'spring-security-acl'");
		GrailsCommandUtils.refreshDependencies(javaProject, false);
		
		IClasspathEntry[] rawClassPath = javaProject.getRawClasspath();
		IClasspathEntry entry = assertPluginSourceFolder(rawClassPath, "spring-security-acl", "grails-app/domain");
		assertNoExclusions(entry);
		StsTestUtil.assertNoErrors(project);
		
		// Now try running the copying script...

		GrailsLaunchUtils.launch(javaProject, "s2-create-acl-domains");
		
		//The launch.. in this case is run asynchronously so we must wait a bit before we can hope the command
		//to finish and the refresh to run.
		new ACondition() {
			@Override
			public boolean test() throws Exception {
				IClasspathEntry[] rawClassPath = javaProject.getRawClasspath();
				IClasspathEntry entry = assertPluginSourceFolder(rawClassPath, "spring-security-acl", "grails-app/domain");
				assertAclDomainExclusions(entry);
				return true;
			}
		}.waitFor(90000);
		StsTestUtil.assertNoErrors(project);
	}
	
	private void assertExclusions(IClasspathEntry entry, String... expecteds) {
		IPath[] actuals = entry.getExclusionPatterns();
		String[] actualStr = new String[actuals.length];
		for (int i = 0; i < actualStr.length; i++) {
			actualStr[i] = actuals[i].toString();
		}
		Arrays.sort(actualStr);
		Arrays.sort(expecteds);
		assertArrayEquals(expecteds, actualStr);
	}

	private void assertNoExclusions(IClasspathEntry entry) {
		IPath[] exclusions = entry.getExclusionPatterns();
		if (exclusions==null || exclusions.length==0) {
			return; //OK
		} else { //Problem
			StringBuilder msg = new StringBuilder("Expected no exclusions but found:\n");
			for (IPath iPath : exclusions) {
				msg.append(iPath+"\n");
			}
			fail(msg.toString());
		}
	}

	private IClasspathEntry assertPluginSourceFolder(IClasspathEntry[] cp, String pluginName, String pathString) {
		for (IClasspathEntry e : cp) {
			if (e.getEntryKind()==IClasspathEntry.CPE_SOURCE) {
				String path = e.getPath().toString();
				if (path.contains(pluginName) && path.endsWith(pathString)) {
					return e;
				}
			}
		}
		fail("Couldn't find source folder '"+pathString+"' for plugin '"+pluginName);
		return null; // unreachable
	}

	private String varDef(String varName, String value) {
		value = value.replace("\\", "\\\\"); // Likely in windows paths
		//TODO: escape other stuff inside the value if that is needed 
		return varName+"=\""+value+"\"";
	}

	private void assertPluginsLink(String pluginsDir) {
		IFolder link = project.getFolder(SourceFolderJob.PLUGINS_FOLDER_LINK);
		assertTrue("The "+SourceFolderJob.PLUGINS_FOLDER_LINK+" doesn't exist", link!=null && link.exists());
		assertEquals(SourceFolderJob.PLUGINS_FOLDER_LINK+" points to wrong location", pluginsDir, link.getLocation().toString());
	}

}
