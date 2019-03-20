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
package org.grails.ide.eclipse.ui.test;

import static org.junit.Assert.assertArrayEquals;
import static org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils.openPerspective;
import static org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils.waitForAllProcessesToTerminate;
import static org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils.waitResourceCreated;
import static org.springsource.ide.eclipse.commons.tests.util.StsTestUtil.getResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotList;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.SourceFolderJob;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsInplaceDialog;
import org.grails.ide.eclipse.ui.test.util.GrailsUITestCase;
import org.springsource.ide.eclipse.commons.core.Entry;
import org.springsource.ide.eclipse.commons.core.ICommandHistory;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotConditions;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @author Nieraj Singh
 */
public class TestInplaceDialog extends GrailsUITestCase {
	
	private static String[] testProjectNames = {
		"kris", "nieraj", "andrew"// , "andy"
	};

	
	@Override
	protected String getProjectViewCategory() {
		return "Java";
	}

	@Override
	protected String getProjectViewName() {
		return "Package Explorer";
	}

	@Override
	public void setupClass() throws Exception {
		super.setupClass();
		createProjectsInView(testProjectNames);
		IProject project = createGeneralProject("notGrails"); 
		assertFalse(GrailsNature.isGrailsProject(project));
	}
	
	@Override 
	public void setupInstance() throws Exception {
		waitForAllProcessesToTerminate(bot);
	}

	@Override
	public void tearDownClass() throws Exception {
		super.tearDownClass();
	}
	
	/**
	 * A first simple test to see if we are able to launch the tests.
	 */
	public void testScaffolding() throws Exception {
		assertEquals(2, 2);
	}

	/**
	 * What happens if we try to re-open already open Grails perspective?
	 * Should not stop the test from proceeding!
	 */
	public void testRobustOpenPersective() throws Exception {
		openPerspective(bot, "Grails"); // Actually, was open before (setupClass did it!)
		openPerspective(bot, "Grails"); // for good measure!
		assertTrue("The Grails perspective should really be open/active now!", 
				bot.perspectiveByLabel("Grails").isActive());
	}
	
	/**
	 * Command prompt opens even when no project selected?
	 * (This test *must* run first, because once a project is selected, the 
	 * menu action will remember it even if later deselected no matter what!)
	 */
	public void testInPlaceDialogOpensAndCloses() {
		SWTBotShell commandPromptShell = openActivePrompt(null);
		
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		assertIsOneOf(testProjectNames, projectList.getText()); // should auto select some available project
		assertEqualElements(testProjectNames, projectList.items());

		closeCommandPrompt(commandPromptShell);
	}
	
	/**
	 * The ALT-SHIFT-CTRL-G key binding should work outside the grails perpsective.
	 */
	public void testKeyBindingOutsideGrailsPerpective() {
		if (StsTestUtil.isOnBuildSite()) {
			//Skip this test, can't simulate ALT-SHIFT-CTRL-G keypresses on buildsite
			return;
		}
		try {
			openPerspective(bot, "Java");

			//Select a Grails project and the prompt should be able to open
			String projectName = "nieraj";
			SWTBotTree explorerTree = selectProject(projectName);
			assertEquals(projectName, getSelection(explorerTree));
			bot.waitUntil(SWTBotUtils.widgetMakeActive(explorerTree));
			assertTrue(explorerTree.isActive());
			keyboard.ALT_SHIFT_CTRL_G();
			SWTBotShell commandPromptShell = bot.shell("Grails Command Prompt");
			assertTrue(commandPromptShell.isOpen());
			closeCommandPrompt(commandPromptShell);
		}
		finally {
			//Make sure to reactivate the Grails perspective for other tests
			openPerspective(bot, "Grails");
		}
	}

	private void assertIsOneOf(String[] expecteds, String text) {
		boolean ok = false;
		String expectedMsg = "Expected one of ";
		for (String expected : expecteds) {
			expectedMsg += "'" + expected +"' "; 
			ok |= expected.equals(text);
		}
		assertTrue(expectedMsg +"but got '"+text+"'", ok);
	}
	
	/**
	 * Test messages in status line when we select different kinds of "bogus"
	 * projects.
	 */
	public void testStatusLineForBogusProjects() throws Exception {
		SWTBotShell promptShell = openActivePrompt("notGrails");
		checkStatusMessage(promptShell, "Selected project is not a Grails project");
		
		IProject project = createGeneralProject("closed");
		project.close(null);
		assertFalse(project.isOpen());
		promptShell = openActivePrompt(project.getName());
		checkStatusMessage(promptShell, "Selected project is not open");
		
		project.delete(true, null);
		assertFalse(project.exists());
		
		promptShell = openActivePrompt(null);
		SWTBotCombo projectList = getProjectList(promptShell);
		assertEquals(project.getName(), projectList.getText()); // Although deleted, should still be selected
		checkStatusMessage(promptShell, "Selected project does not exist");
		
		//Check whether status line changes when we select a good project
		projectList.setSelection("nieraj");
		assertEquals("nieraj", projectList.getText());
		checkStatusMessage(promptShell, "Type Grails command and press 'Enter'");
	}

	private void checkStatusMessage(SWTBotShell promptShell, String expectedMessage) {
		SWTBotLabel statusLabel = promptShell.bot().label(expectedMessage);
		assertEquals(expectedMessage, statusLabel.getText());
	}

	/**
	 * Create a File in the workspace. If the file already exists, it is
	 * overwritten.
	 * @param path to file to be created, starting from workspace root.
	 * @param contents to put into the file
	 * @throws CoreException
	 * @throws UnsupportedEncodingException
	 */
	public static void createFileResource(String pathToFile, String contents) throws CoreException,
			UnsupportedEncodingException {
		IResource resource = getResource(pathToFile);
		if (resource.exists()) {
			resource.delete(true, null);
		}
		IFile file = (IFile) resource;
		file.create(new ByteArrayInputStream(contents.getBytes("UTF-8")), true, new NullProgressMonitor());
	}
	
	public void testInPlaceDialogOpensAndClosesOnProject() {
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		assertEquals("kris",projectList.getText());
		assertEqualElements(testProjectNames, projectList.items());

		closeCommandPrompt(commandPromptShell);
	}

	private void closeCommandPrompt(SWTBotShell commandPromptShell) {
		TimeoutException e = null;
		for (int i=0; i<5; i++) {
			try {
				keyboard.ESCAPE_KEY();
				bot.waitUntil(Conditions.shellCloses(commandPromptShell));
				return;
			} catch (TimeoutException _e) {
				e = _e;
			}
		}
		//We can only get here by catching TimeoutException several times
		if (e!=null) {
			throw e ;
		}
	}

	public void testIsCodeAssistProjectSensitive() throws UnsupportedEncodingException, CoreException {
		createScript("kris/scripts/DoTheFoo.groovy");
		createScript("nieraj/scripts/DoTheBar.groovy");
		
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		SWTBotText commandText = commandPromptShell.bot().text();
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		
		assertEquals("kris",projectList.getText());
		List<String> items = getProposals("", commandPromptShell, commandText);
		assertTrue(items.contains("do-the-foo")); 
		assertFalse(items.contains("do-the-bar"));
		
		projectList.setSelection("nieraj");
		do {
			items = getProposals("", commandPromptShell, commandText);
			if (items.contains(" -- content assist not ready yet -- ")) {
				bot.sleep(500);
			}
			else break;
		} while (true);
		assertFalse(items.contains("do-the-foo"));
		assertTrue(items.contains("do-the-bar"));
		
		closeCommandPrompt(commandPromptShell);
	}

	public void testDuplicateCodeAssistWhenSwitchingProjectsQuickly() {
		SWTBotShell commandPromptShell = openActivePrompt(null);
		SWTBotText commandText = commandPromptShell.bot().text();
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		
		for (String project : testProjectNames) {
			projectList.setSelection(project);
		}
		List<String> items;
		do {
			items = getProposals("create-", commandPromptShell, commandText);
			if (items.contains(" -- content assist not ready yet -- ")) {
				bot.sleep(500);
			}
			else break;
		} while (true);
		checkNoDuplicates(items);
		
		closeCommandPrompt(commandPromptShell);
	}
	
	private void checkNoDuplicates(List<String> items) {
		System.out.println(">>> Items ");
		for (String item : items) {
			System.out.println("'"+item+"'");
		}
		System.out.println("<<< Items ");
		
		Set<String> alreadySeen = new HashSet<String>();
		for (String item : items) {
			assertFalse("Duplicate: "+item, alreadySeen.contains(item));
			alreadySeen.add(item);
		}
	}

	/**
	 * @param commandPromptShell
	 * @return
	 */
	private SWTBotCombo getProjectList(SWTBotShell commandPromptShell) {
		return commandPromptShell.bot().comboBox();
	}
	
	public void testCommandHistoryPopup() {
		//Create some test history
		ICommandHistory history = GrailsInplaceDialog.getCommandHistory();
		history.clear();
		Entry[] testEntries = new Entry[] {
				new Entry("test-app", "nieraj"),
				new Entry("run-app", "nieraj"),
				new Entry("test-app", "kris"),
				new Entry("run-app", "kris")
		};
		
		// Expected labels should be same order as history (last item added at bottom of list)
		// when popuplist appears *above* the widget (which is default unless not enough space)
		String[] expectedLabels = new String[testEntries.length];
		for (int i = 0; i < testEntries.length; i++) {
			Entry entry = testEntries[i];
			history.add(entry);
			// project name is only displayed if project is different from selection
			if (entry.getProject().equals("kris")) 
				expectedLabels[i] = entry.getCommand();
			else
				expectedLabels[i] = entry.getCommand() + " ("+entry.getProject()+")";
		}

		// Open command prompt and get its history to popup
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		keyboard.UP_KEY(); // popup!
		bot.sleep(500);
		SWTBotShell historyShell = SWTBotUtils.shell(commandPromptShell);
		final SWTBotList historyList = historyShell.bot().list();
		assertArrayEquals(expectedLabels, historyList.getItems());
		
		int expectedSelection = testEntries.length-1; // start with last item selected
		while (expectedSelection>0) { 
			assertEquals(1, historyList.selectionCount());
			waitForSelection(expectedLabels[expectedSelection], historyList);
			keyboard.UP_KEY();
			expectedSelection--;
		}
		assertEquals(0, expectedSelection);
		waitForSelection(expectedLabels[expectedSelection], historyList);
		
		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(historyShell));
		
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		assertEquals(testEntries[expectedSelection].getCommand(), commandText.getText());
		assertEquals(testEntries[expectedSelection].getProject(), projectList.getText());
	
		closeCommandPrompt(commandPromptShell);
	}

	

	/**
	 * Verify whether two arrays contain the same elements, possibly in
	 * a different order.
	 */
	private void assertEqualElements(Object[] expectedItems, Object[] actualItems) {
		LinkedList<Object> expected = new LinkedList<Object>(Arrays.asList(expectedItems));
		for (Object object : actualItems) {
			if (!expected.remove(object))
				fail("Unexpected element: "+object);
		}
		if (!expected.isEmpty()) {
			fail("Expected element not found: "+expected.getFirst());
		}
 	}

	public void testCommandWithASlash() throws Exception {
		//Create a "test plugin" .zip file in the workspace at "plugins/<TEST_PLUGIN>" 
		final String TEST_PLUGIN = "grails-tinyurl-0.1.zip";
		IProject pluginsProject = createGeneralProject("plugins");
		InputStream testPluginContents = getClass().getClassLoader().getResourceAsStream(TEST_PLUGIN); // test plugin name
		assertNotNull("Test zip file not found: "+TEST_PLUGIN, testPluginContents);
		IFile pluginFile = pluginsProject.getFile(TEST_PLUGIN);
		pluginFile.create(testPluginContents, false, null);
		assertTrue(pluginFile.exists());
		
		// Open grails prompt and try to execute command to install this plugin
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		commandText.setText("install-plugin ../plugins/"+TEST_PLUGIN);
		assertEquals("install-plugin ../plugins/"+TEST_PLUGIN, commandText.getText());

		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(commandPromptShell));
		
		try {
			waitResourceCreated(bot, "kris/"+SourceFolderJob.PLUGINS_FOLDER_LINK+"/tinyurl-0.1/grails-app/services/", TIMEOUT_CREATE_DOMAIN_CLASS);
		}
		catch (Throwable e) {
			throw new Error("---- Console Text ----\n"+SWTBotUtils.getConsoleText(bot)+"\n--------\n", e);
		}
	}
	
	public void testCommandWithAStar() throws Exception {
		// Open grails prompt and try to execute command to run tests in some package
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		commandText.setText("test-app -unit bogus.*");
		assertEquals("test-app -unit bogus.*", commandText.getText());

		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(commandPromptShell));
		
		try {
			SWTBotConditions.waitForConsoleOutput(bot, "view reports in", TIMEOUT_TEST_APP);
		}
		catch (Throwable e) {
			throw new Error("---- Console Text ----\n"+SWTBotUtils.getConsoleText(bot)+"\n--------\n", e);
		}
	}
	

	public void testCommandOutputStaysAfterCompletion() throws Exception {
		// Open grails prompt and try to execute command to install this a plugin
		SWTBotShell commandPromptShell = openActivePrompt("nieraj");
		
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		commandText.setText("install-plugin twitter 0.1");
		assertEquals("install-plugin twitter 0.1", commandText.getText());
		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(commandPromptShell));
		try {
			waitResourceCreated(bot, "/nieraj/"+SourceFolderJob.PLUGINS_FOLDER_LINK+"/twitter-0.1/src/java/", TIMEOUT_CREATE_DOMAIN_CLASS);
		}
		catch (TimeoutException e) {
			throw new Error("---console Text---\n"+SWTBotUtils.getConsoleText(bot)+"\n-------------", e);
		}
		SWTBotConditions.waitForConsoleOutput(bot, "Installed plugin twitter-0.1", TIMEOUT_CREATE_DOMAIN_CLASS);
		bot.sleep(1000); // Wait a little, to see if the text stays put after completion
		SWTBotConditions.waitForConsoleOutput(bot, "Installed plugin twitter-0.1"); // Text still there?
	}
	
	public void testCanSelectProject() {
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		assertEquals("kris",projectList.getText());
		assertEqualElements(testProjectNames, projectList.items());
		
		projectList.setSelection("nieraj");
		assertEquals("nieraj",projectList.getText());

		closeCommandPrompt(commandPromptShell);
	}
	
	public void testCodeAssist() {
		SWTBotShell commandPromptShell = openActivePrompt("kris");

		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());

		commandText.typeText("create-");
		keyboard.CTRL_SPACE();
		SWTBotShell codeAssistShell = SWTBotUtils.shell(commandPromptShell);
		
		checkCodeAssist("create-", 3, codeAssistShell);
		
		keyboard.typeText("d");
		bot.waitUntil(Conditions.shellCloses(codeAssistShell));
		
		assertEquals("create-domain-class ", commandText.getText());
		
		keyboard.typeText("my.foo.Bar");
		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(commandPromptShell));
		
		waitResourceCreated(bot, "kris/grails-app/domain/my/foo/Bar.groovy", TIMEOUT_CREATE_DOMAIN_CLASS);
		waitResourceCreated(bot, "kris/test/unit/my/foo/BarTests.groovy");
	}
	
	/**
	 * When we open propmp without a project selected, it should open anyway and use the
	 * project selection from last time. In this case also, the code assist should be
	 * initialized.
	 */
	public void testCodeAssistWithLatestProjectAutoSelected() {
		
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		closeCommandPrompt(commandPromptShell);
		
		commandPromptShell = openActivePrompt(null);
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());
		SWTBotCombo projectList = getProjectList(commandPromptShell);
		assertEquals("kris", projectList.getText());

		String prefix = "create-";
		int minimumResults = 3;
		
		commandText.typeText(prefix);
		keyboard.CTRL_SPACE();
		
		SWTBotShell codeAssistShell = SWTBotUtils.shell(commandPromptShell);
		checkCodeAssist(prefix, minimumResults, codeAssistShell);
		
		keyboard.typeText("d");
		bot.waitUntil(Conditions.shellCloses(codeAssistShell));
		
		assertEquals("create-domain-class ", commandText.getText());
		
		closeCommandPrompt(commandPromptShell);
	}

	/**
	 * @param prefix
	 * @param minimumResults
	 * @param codeAssistShell
	 */
	private void checkCodeAssist(String prefix, int minimumResults,
			SWTBotShell codeAssistShell) {
		SWTBotTable codeAssistTable = codeAssistShell.bot().table();
		
		Set<String> alreadySeen = new HashSet<String>();
		int rows = codeAssistTable.rowCount();
		assertTrue(rows >= minimumResults);
		for (int i=0; i<rows; i++) { 
			String item = codeAssistTable.cell(i, 0);
			assertFalse("Duplicate code assist: '"+item+"'", alreadySeen.contains(item));
			alreadySeen.add(item);
			assertTrue(item.startsWith(prefix));
		}
	}

	
	public void testPinnedPromptStaysWhenLosingFocus() {
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		SWTBotText commandText = commandPromptShell.bot().text();

		assertEquals("", commandText.getText());
		keyboard.typeText("blah");
		assertEquals("blah", commandText.getText());

		SWTBotMenu pinMenu  = null;
		pinMenu = contextMenu(commandPromptShell, "Pin");
		assertTrue(pinMenu.isVisible());
		assertFalse(pinMenu.isChecked());
		pinMenu.click();
		assertTrue(pinMenu.isChecked());
		
		selectProject("nieraj"); // This should deactivate the pinned shell without closing it.
		assertTrue(commandPromptShell.isVisible());
		assertFalse(commandPromptShell.isActive());

		commandPromptShell.activate(); // Reactivate shell
		assertTrue(commandPromptShell.isActive());
		commandText = commandPromptShell.bot().text();
		assertEquals("blah", commandText.getText());
		commandText.typeText("blah");
		assertEquals("blahblah", commandText.getText());

		closeCommandPrompt(commandPromptShell);

		commandPromptShell = openActivePrompt("kris");
		pinMenu = contextMenu(commandPromptShell, "Pin");
		assertTrue(pinMenu.isChecked()); // pin state should be saved from last prompt!
		pinMenu.click();
		assertFalse(pinMenu.isChecked());

		closeCommandPrompt(commandPromptShell);
	}

	/**
	 * Retrieve a context menu item from the command prompt context menu.
	 * <p>
	 * This code is very specific to the command prompt. It's menu is a bit
	 * non-standard, and the menu is only created on the fly. So me must
	 * make sure it gets opened in the UI before we try to access it via
	 * SWTBot API.
	 */
	private SWTBotMenu contextMenu(SWTBotShell commandPromptShell, String name) {
		SWTBotMenu pinMenu;
		SWTBotToolbarButton tb = commandPromptShell.bot().toolbarButton();
		assertTrue(tb.isVisible());
		tb.click(); // We must click this or context menu is not created? Because inpalce dialog initializes the menu lazily
		keyboard.ESCAPE_KEY(); // We can close this now?
		pinMenu = commandPromptShell.contextMenu("Pin");
		return pinMenu;
	}

	public void testCodeAssistArrowKeysEnter() {
		SWTBotShell commandPromptShell = openActivePrompt("kris");
		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());

		commandText.typeText("create-");
		keyboard.CTRL_SPACE();
		
		SWTBotShell codeAssistShell = SWTBotUtils.shell(commandPromptShell);
		SWTBotTable codeAssistTable = codeAssistShell.bot().table();
		
		int rows = codeAssistTable.rowCount();
		assertTrue(rows >= 3);
		int targetIndex = -1;
		for (int i=0; i<rows; i++) { 
			String item = codeAssistTable.cell(i, 0);
			assertTrue(item.startsWith("create-"));
			if (item.startsWith("create-domain-class"))
				targetIndex = i;
		}
		assertTrue(targetIndex>=0);
		
		for (int i=0; i<targetIndex; i++)
			keyboard.DOWN_KEY();
		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(codeAssistShell));
		
		assertEquals("create-domain-class ", commandText.getText());
		
		keyboard.typeText("my.foo.Foo");
		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(commandPromptShell));
		
		waitResourceCreated(bot, "kris/grails-app/domain/my/foo/Foo.groovy", TIMEOUT_CREATE_DOMAIN_CLASS);
		waitResourceCreated(bot, "kris/test/unit/my/foo/FooTests.groovy");
	}
	
	public void testCodeAssistSelectClick() {
		SWTBotShell commandPromptShell = openActivePrompt("kris");

		SWTBotText commandText = commandPromptShell.bot().text();
		assertEquals("", commandText.getText());

		commandText.typeText("create-");
		keyboard.CTRL_SPACE();
		bot.sleep(500);
		
		SWTBotShell codeAssistShell = SWTBotUtils.shell(commandPromptShell);
		final SWTBotTable codeAssistTable = codeAssistShell.bot().table();
		
		int rows = codeAssistTable.rowCount();
		assertTrue(rows >= 3);
		int targetIndex = -1;
		for (int i=0; i<rows; i++) { 
			String item = codeAssistTable.cell(i, 0);
			assertTrue(item.startsWith("create-"));
			if (item.startsWith("create-domain-class"))
				targetIndex = i;
		}
		assertTrue(targetIndex>=0);
		
		//Note: table.doubleClick() method doesn't work, table appears not to be listening
		// to double click events. Use utility method instead.
		SWTBotUtils.doubleClick(bot, codeAssistTable, targetIndex, 0);
		bot.waitUntil(Conditions.shellCloses(codeAssistShell));
		
		assertEquals("create-domain-class ", commandText.getText());
		
		keyboard.typeText("my.foo.Zor");
		keyboard.ENTER_KEY();
		bot.waitUntil(Conditions.shellCloses(commandPromptShell));
		
		waitResourceCreated(bot, "kris/grails-app/domain/my/foo/Zor.groovy", TIMEOUT_CREATE_DOMAIN_CLASS);
		waitResourceCreated(bot, "kris/test/unit/my/foo/ZorTests.groovy");
	}
	
	/**
	 * Create a dummy script
	 */
	private void createScript(String scriptPathName) throws CoreException,
			UnsupportedEncodingException {
		createFileResource(scriptPathName, "//It doesn't really matter");
		waitResourceCreated(bot, scriptPathName);
		StsTestUtil.waitForAutoBuild();
	}

	private List<String> getProposals(String prefix,
			SWTBotShell commandPromptShell, SWTBotText commandText) {
		assertEquals("", commandText.getText());
		commandText.setText(""); 
		commandText.typeText(prefix);
		commandText.pressShortcut(SWT.CTRL, ' ');
		SWTBotShell codeAssistShell = SWTBotUtils.shell(commandPromptShell);
		SWTBotTable codeAssistTable = codeAssistShell.bot().table();
		int rows = codeAssistTable.rowCount();
		List<String> items = new ArrayList<String>(rows);
		for (int i=0; i<rows; i++) { 
			String item = codeAssistTable.cell(i, 0);
			items.add(item);
		}
		
		codeAssistShell.close();
		
		return items;
	}	



}
