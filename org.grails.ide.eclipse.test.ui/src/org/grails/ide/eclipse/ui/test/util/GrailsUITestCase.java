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
package org.grails.ide.eclipse.ui.test.util;

import static org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils.openPerspective;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ResourceExists;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;
import org.springsource.ide.eclipse.commons.frameworks.test.util.UITestCase;


/**
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @author Nieraj Singh
 */
public abstract class GrailsUITestCase extends UITestCase {

	// ////////////////////////////
	// Executing Grails commands may take longer, so we use a separate timeout
	// for SWTBot
	// wait conditions for those.

	public static final long TIMEOUT_CREATE_DOMAIN_CLASS = 30000;
	public static final long TIMEOUT_CREATE_APP = 60000;
	public static final long TIMEOUT_TEST_APP = 60000;
	protected static String dotGrailsFolder;

	// /////////////////////////////////

	@Override
	public void setupClass() throws Exception {
		super.setupClass();
		initGrailsWorkingDir();
		openPerspective(bot, "Grails");
	}

	protected void initGrailsWorkingDir() {
		IGrailsInstall install = GrailsCoreActivator.getDefault()
				.getInstallManager().getDefaultGrailsInstall();
		
		String tempDir = System.getProperty("java.io.tmpdir");
		dotGrailsFolder = tempDir+"/.grails"+System.currentTimeMillis();
		DefaultGrailsInstall.setDefaultGrailsWorkDir(dotGrailsFolder);
	}

	protected SWTBotShell activateGrailsProjectWizardShell() {
		return activateFileNewWizardShell("Grails Project",
				"New Grails Project");
	}

	protected void createGrailsProject(String name) throws Exception {

		System.out.println("Create grails project:" + name);
		activateGrailsProjectWizardShell();
		SWTBotText projectName = bot.textWithLabel("Project name:");
		projectName.setFocus();
		assertTrue(projectName.isActive());
		projectName.setText(name);
		bot.button("Finish").click();

		// May take a while so long timeout
		// value needed
		try {
			bot.waitUntil(new ResourceExists(name), TIMEOUT_CREATE_APP);
		} catch (TimeoutException e) {
			fail(e.getMessage() + "\n--- Console Text ---\n"
					+ SWTBotUtils.getConsoleText(bot) + "\n--------\n");
		}
		System.out.println("Resources created... Waiting for build.");
		waitForProjectBuild(name);
		System.out.println("Build finished!");
	}

	/**
	 * Waits until project is completed building and compilation errors are
	 * removed.
	 */
	protected void waitForProjectBuild(String projectName) throws Exception {
		// If there is a build running, wait until all builds complete before
		// checking icons to prevent icons being decorated prematurely.
		print("Waiting for build jobs for " + projectName + " to complete.");
		SWTBotUtils.waitForAllBuildJobs(bot);
		print("All build jobs for " + projectName + " completed.");
	}

	protected void createProjectsInView(String[] projectNames) throws Exception {
		SWTBotView projectView = openView();
		assertTrue(projectView.isActive());
		// Create some test grails projects
		for (String name : projectNames) {
			createGrailsProject(name);
		}
	}

	/**
	 * Select projectName in the package explorer and then press key combination
	 * to open the Grails Command Prompt.
	 * 
	 * @param projectName
	 * @param keyboard
	 * @return Reference to SWTBot that represents the Grails Command Prompt
	 *         window.
	 */
	protected SWTBotShell openActivePrompt(String projectName) {
		SWTBotTree explorerTree = selectProject(projectName);
		assertEquals(projectName == null ? "" : projectName,
				getSelection(explorerTree));
		assertTrue(explorerTree.isActive());

//		SWTBotUtils.screenshot("openActivePrompt_afterSelectProject_"
//				+ projectName);
		mainShell.activate();
		assertTrue(mainShell.isActive());
		keyboard.ALT_SHIFT_CTRL_G();
		SWTBotShell commandPromptShell = bot.shell("Grails Command Prompt",
				mainShell.widget);
		assertTrue(commandPromptShell.isOpen());
		assertTrue(commandPromptShell.isActive());
//		SWTBotUtils.screenshot("openActivePrompt_afterOpenGrailsPrompt_"
//				+ projectName);
		return commandPromptShell;
	}
}
