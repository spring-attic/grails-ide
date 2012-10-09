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
package org.grails.ide.eclipse.longrunning.test;

import static org.grails.ide.eclipse.commands.GrailsCommandFactory.createDomainClass;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.test.GrailsCommandTest;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.ConsoleProvider;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Inherits all the tests from GrailsCommandTest, but runs them with "keep running" option enabled.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class LongRunningGrailsTest extends GrailsCommandTest {
	
	static {
		GrailsTest.clearGrailsState();
	}

	private ConsoleProvider savedConsoleProvider;
	
	/**
	 * Sets the input that will be used for the next command execution.
	 */
	private void expectedInteraction(QuestionAnswer... qas) {
		GrailsProcessManager.consoleProvider = new TestConsoleProvider(qas);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		savedConsoleProvider = GrailsProcessManager.consoleProvider;
		GrailsCoreActivator.getDefault().setKeepGrailsRunning(true);
	}
	
	/**
	 * Most basic test that simply runs a single command and prints the result.
	 */
	public void testOneCommand() throws Exception {
		//GrailsClient.DEBUG_PROCESS = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
		System.out.println(GrailsCommandFactory.createApp("frullBot").addArgument("--stacktrace").synchExec());
	}
	
	public void testCommandWithEnvParam() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		GrailsCommand cmd = GrailsCommandFactory.war(proj, "dev", null);
		ILaunchResult result = cmd.synchExec();
		System.out.println(result);
	}

	/**
	 * Test to see if input is forwarded properly to the command.
	 */
	public void testCommandWithInput()throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);

		GrailsProcessManager.getInstance().shutDown();
		//GrailsClient.DEBUG_PROCESS = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
		
		expectedInteraction(/*empty*/);
		
		GrailsCommand cmd = createDomainClass(proj, "gTunes.Bong");
		ILaunchResult result = cmd.synchExec();
		System.out.println(result.getOutput());
		assertContains("Created file grails-app/domain/gTunes/Bong", result.getOutput());
		proj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		assertResourceExists(TEST_PROJECT_NAME+"/grails-app/domain/gTunes/Bong.groovy");
		
		//GrailsProcessManager.getInstance().shutDown();
		//GrailsClient.DEBUG_PROCESS = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";

		expectedInteraction(
				new QuestionAnswer(
						"Bong.groovy already exists. Overwrite?[y,n]",
						"y"
				),
				new QuestionAnswer(
						"BongTests.groovy already exists. Overwrite?[y,n]",
						"y"
				)
		);
		cmd = createDomainClass(proj, "gTunes.Bong");
		result = cmd.synchExec();
		assertAllQuestionsAnswered();
		
		System.out.println(result.getOutput());
		assertContains("Created file grails-app/domain/gTunes/Bong", result.getOutput());
		proj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		assertResourceExists(TEST_PROJECT_NAME+"/grails-app/domain/gTunes/Bong.groovy");
		
		// Try again, make sure it works more than once (could potentially break because of state issues)
		
		expectedInteraction(
				new QuestionAnswer(
						"Bong.groovy already exists. Overwrite?[y,n]",
						"y"
				),
				new QuestionAnswer(
						"BongTests.groovy already exists. Overwrite?[y,n]",
						"y"
				)
		);
		cmd = createDomainClass(proj, "gTunes.Bong");
		result = cmd.synchExec();
		assertAllQuestionsAnswered();
		
		System.out.println(result.getOutput());
		assertContains("Created file grails-app/domain/gTunes/Bong", result.getOutput());
		proj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		assertResourceExists(TEST_PROJECT_NAME+"/grails-app/domain/gTunes/Bong.groovy");
		
	}
	
	private void assertAllQuestionsAnswered() {
		((TestConsoleProvider)GrailsProcessManager.consoleProvider).assertAllQuestionsAnswered();
	}

	/**
	 * If a project's Grails version is changed does the long running process use the right version?
	 */
	public void testChangeGrailsVersion() throws Exception {
		boolean wasEnabled = GrailsProjectVersionFixer.isEnabled();
		try {
			GrailsProjectVersionFixer.setEnabled(false);

			ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS_PREVIOUS);
			ensureDefaultGrailsVersion(GrailsVersion.PREVIOUS);
			ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);

			IProject project = ensureProject(getClass().getSimpleName());

			doUpgrade(project, GrailsVersion.MOST_RECENT, GrailsVersion.PREVIOUS_PREVIOUS);
			doUpgrade(project, GrailsVersion.PREVIOUS_PREVIOUS, GrailsVersion.PREVIOUS);
			doUpgrade(project, GrailsVersion.PREVIOUS, GrailsVersion.MOST_RECENT);
		} finally {
			GrailsProjectVersionFixer.setEnabled(wasEnabled);
			//All this up-and-down grading is likely to compromise the project's integrity. Ensure we create new one for next test.
			StsTestUtil.deleteAllProjects();
			GrailsTest.clearGrailsState();
		}
	}

	private void doUpgrade(IProject project, GrailsVersion fromVersion, GrailsVersion toVersion) throws CoreException {
		//Verify initial project version state
		assertEquals(fromVersion, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(fromVersion, GrailsVersion.getGrailsVersion(project));
		
		//Change install in eclipse, brings Eclipse / Grails version out of synch
		IGrailsInstall install = toVersion.getInstall();
		GrailsInstallManager.setGrailsInstall(project, false, install.getName());
		
		//Verify expected project version state (i.e. out of synch)
		assertEquals(toVersion, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(fromVersion, GrailsVersion.getGrailsVersion(project));

		//Run upgrade command
		GrailsCommand upgrade = GrailsCommandFactory.upgrade(project);
		ILaunchResult result = upgrade.synchExec();
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		
		// Check whether correct version of Grails was used...
		String welcome = (GrailsVersion.V_2_0_.compareTo(toVersion)<=0) ? "Loading Grails ": "Welcome to Grails ";
		assertContains(welcome+toVersion, result.getOutput());; 
		
		//Verify expected project version state (i.e. back in synch)
		assertEquals(toVersion, GrailsVersion.getEclipseGrailsVersion(project));
		assertEquals(toVersion, GrailsVersion.getGrailsVersion(project));
		
	}
	
	
	@Override
	public void testOutputLimit() throws Exception {
		//Skip: output limit isn't implemented for long running grails process executor.
	}
	
	/**
	 * @return An 'empty' input stream
	 */
	private InputStream nullInput() {
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	protected void tearDown() throws Exception {
		GrailsProcessManager.consoleProvider = savedConsoleProvider;
		GrailsCoreActivator.getDefault().setKeepGrailsRunning(GrailsCoreActivator.DEFAULT_KEEP_RUNNING_PREFERENCE);
		super.tearDown();
	}

}
