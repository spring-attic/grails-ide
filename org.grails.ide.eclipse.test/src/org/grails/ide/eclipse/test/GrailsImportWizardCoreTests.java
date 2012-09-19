/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import junit.framework.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.commands.test.AbstractCommandTest;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.wizard.GrailsImportWizardCore;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.LiveVariable;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * @author Kris De Volder
 */
public class GrailsImportWizardCoreTests extends AbstractCommandTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GroovyCompilerVersionCheck.testMode();
		ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS);
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		StsTestUtil.deleteAllProjects();
//		GrailsCoreActivator.getDefault().setKeepGrailsRunning(true);
	}

	/**
	 * Creates an app with Gails 'create-app' command. The app is only created on the file system. It is not imported
	 * into the workspace.
	 */
	public File createTestApp(File location, String name) throws CoreException {
		File project = new File(location, name);
		GrailsCommand createApp = GrailsCommandFactory.createApp(name);
		createApp.setPath(location.toString());
		ILaunchResult result = createApp.synchExec();
		System.out.println(result);
		Assert.assertTrue(""+result, result.isOK());
		return project;
	}
	
	/**
	 * Test importing a project with 'copy resources' option disabled.
	 */
	public void testImportCopied() throws Exception {
		File baseDir = StsTestUtil.createTempDirectory("test", "dir");
		String name = this.getClass().getSimpleName(); 
		File projectLoc = createTestApp(baseDir, name);

		// Create wizard and fill in the values
		GrailsImportWizardCore wiz = new GrailsImportWizardCore();
		wiz.location.setValue(projectLoc);
		wiz.copyToWorkspace.setValue(true);

		//Check some of the validation states and computed values
		assertTrue(wiz.locationValidator.getValue().isOk());
		assertEquals(wiz.projectGrailsVersion.getValue(), GrailsVersion.MOST_RECENT);
		assertEquals(wiz.grailsInstall.getValue().getVersion(), GrailsVersion.MOST_RECENT);
		assertEquals(name, wiz.getProjectName());

		//Perform import
		assertTrue(wiz.perform(new NullProgressMonitor()));

		//Check imported project 
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertEquals(Platform.getLocation().append(name), project.getLocation());
		assertNoErrors(project);
		
	}
	
	/**
	 * Test importing a project with 'copy resources in active' option active.
	 */
	public void testImportLinked() throws Exception {
		File baseDir = StsTestUtil.createTempDirectory("test", "dir");
		String name = this.getClass().getSimpleName(); 
		File projectLoc = createTestApp(baseDir, name);

		// Create wizard and fill in the values
		GrailsImportWizardCore wiz = new GrailsImportWizardCore();
		wiz.location.setValue(projectLoc);
		wiz.copyToWorkspace.setValue(false);

		//Check some of the validation states and computed values
		assertTrue(wiz.locationValidator.getValue().isOk());
		assertEquals(wiz.projectGrailsVersion.getValue(), GrailsVersion.MOST_RECENT);
		assertEquals(wiz.grailsInstall.getValue().getVersion(), GrailsVersion.MOST_RECENT);
		assertEquals(name, wiz.getProjectName());

		//Perform import
		assertTrue(wiz.perform(new NullProgressMonitor()));
		
		//Check imported project 
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertEquals(projectLoc.toString(), project.getLocation().toString());
		assertNoErrors(project);
	}
	
	/**
	 * Test importing a project that is already in the workspace location.
	 */
	public void testImportAlreadyInWorkspace() throws Exception {
		File baseDir = Platform.getLocation().toFile(); 
		String name = this.getClass().getSimpleName(); 
		File projectLoc = createTestApp(baseDir, name);

		// Create wizard and fill in the values
		GrailsImportWizardCore wiz = new GrailsImportWizardCore();
		wiz.location.setValue(projectLoc);
		wiz.copyToWorkspace.setValue(false);

		//Check some of the validation states and computed values
		assertTrue(wiz.locationValidator.getValue().isOk());
		assertEquals(wiz.projectGrailsVersion.getValue(), GrailsVersion.MOST_RECENT);
		assertEquals(wiz.grailsInstall.getValue().getVersion(), GrailsVersion.MOST_RECENT);
		assertEquals(name, wiz.getProjectName());

		//Perform import
		assertTrue(wiz.perform(new NullProgressMonitor()));
		
		//Check imported project 
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertEquals(projectLoc.toString(), project.getLocation().toString());
		assertNoErrors(project);
	}

	public void testValidators() throws Exception {
		GrailsImportWizardCore wiz = new GrailsImportWizardCore();
		
		//////////////////////////////////////////
		/// Test location validator
		LiveExpression<ValidationResult> val = wiz.locationValidator;
		
		LiveVariable<File> loc = wiz.location;
		assertError(val, "Specify the location");
		
		loc.setValue(new File("/this/isbogus"));
		assertError(val, "doesn't exist");
		
		File tmpFile = File.createTempFile("test", "txt");
		loc.setValue(tmpFile);
		assertError(val, "is not a directory");
		
		File tmpDir = StsTestUtil.createTempDirectory();
		loc.setValue(tmpDir);
		assertError(val, "doesn't look like a Grails project");
		
		File fakeProject = new File(tmpDir, "fake");
		fakeProject.mkdir();
		new File(fakeProject, "grails-app").mkdir();
		File appPropsFile = new File(fakeProject, "application.properties");
		touch(appPropsFile);
		loc.setValue(fakeProject); //this project should be 'good enough' to fool the validator
		assertTrue(val.getValue().isOk());
		
		ResourcesPlugin.getWorkspace().getRoot().getProject(fakeProject.getName()).create(new NullProgressMonitor());
		loc.setValue(null); loc.setValue(fakeProject);
		assertError(val, "already exists in the workspace");
		
		//////////////////////////////////////////////////
		// Copy to workspace validator
		
		//Remove 'fakeProject' from ws but leave it on the file system
		ResourcesPlugin.getWorkspace().getRoot().getProject("fake").delete(false, true, new NullProgressMonitor());
		
		val = wiz.copyToWorkspaceValidator;
		LiveVariable<Boolean> copy = wiz.copyToWorkspace;
		copy.setValue(true);
		assertError(val, "Can not copy project into workspace because '"+Platform.getLocation()+"/fake"+"' already exists");
		
		copy.setValue(false);
		assertTrue(val.getValue().isOk());

		//Now we also test if the validation state is refreshed as needed when we set the location last instead of first.
		loc.setValue(null);
		assertTrue(val.getValue().isOk());
		copy.setValue(true);
		assertTrue(val.getValue().isOk()); //Still ok, because loc not yet set (validator only active if loc is known).
		loc.setValue(fakeProject);
		assertError(val, "Can not copy project into workspace because '"+Platform.getLocation()+"/fake"+"' already exists");
		
		
		//////////////////////////////////////////////////////
		/// Mavenized validator
		val = wiz.mavenValidator;
		
		assertTrue(val.getValue().isOk());
		File pomFile = new File(fakeProject, "pom.xml");
		touch(pomFile);
		loc.setValue(null); loc.setValue(fakeProject);
		assertError(val,"Mavenized project");
		wiz.ignoreMavenWarning.setValue(true);
		assertTrue(val.getValue().isOk());
		
		
		//////////////////////////////////////////////////////
		/// Install validator
		val = wiz.installValidator;
		
		wiz.grailsInstall.setValue(GrailsVersion.MOST_RECENT.getInstall());
		assertError(val, "Unable to determine Grails version for the project");
		
		Properties props = new Properties();
		props.put("app.grails.version", GrailsVersion.MOST_RECENT.toString());
		FileWriter writer = new FileWriter(appPropsFile);
		try {
			props.store(writer, "Duh!");
		} finally {
			writer.close();
		}
		kick(loc);
		assertTrue(val.getValue().isOk());
		
		wiz.grailsInstall.setValue(null);
		assertError(val, "No Grails install selected");

		wiz.grailsInstall.setValue(GrailsVersion.PREVIOUS.getInstall());
		assertError(val, "does not match install version");

		kick(loc); // correct install should get auto selected clearing the error.
		assertEquals(GrailsVersion.MOST_RECENT.getInstall(), wiz.grailsInstall.getValue());  
		assertTrue(val.getValue().isOk());
	}

	/**
	 * Set and reset a variable's value to force listeners to fire.
	 */
	private <T> void kick(LiveVariable<T> var) {
		T value = var.getValue();
		var.setValue(null);
		var.setValue(value);
	}

	private void touch(File appPropsFile) throws IOException {
		FileWriter writer = new FileWriter(appPropsFile);
		writer.close();
	}

	private void assertError(LiveExpression<ValidationResult> val, String msgFragment) {
		assertEquals(IStatus.ERROR, val.getValue().status);
		assertContains(msgFragment, val.getValue().msg);
	}
	
}
