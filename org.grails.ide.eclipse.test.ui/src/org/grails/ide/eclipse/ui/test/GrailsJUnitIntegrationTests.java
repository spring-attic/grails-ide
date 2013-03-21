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
package org.grails.ide.eclipse.ui.test;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.springsource.ide.eclipse.commons.frameworks.test.util.AbstractedWaitCondition;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotConditions;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;


/**
 * Tests for Eclipse / Junit integration features in the Grails tooling.
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @created 2010-08-20
 */
public class GrailsJUnitIntegrationTests extends GrailsProjectHarness {

	private abstract class ViewOKTest extends AbstractedWaitCondition {

		private final SWTBotView view;
		private String msg = "no message";

		private ViewOKTest(SWTBot bot, SWTBotView junitView) {
			super(bot);
			this.view = junitView;
			junitView.bot().textWithLabel("Runs: ").setText("***"); //Hack!
			     // This hack is to make sure we aren't accidentally passing the test
			     // because the "output" from previous test run is still being displayed in
			     // the JUnit view.
		}

		protected abstract void assertViewOK();
		
		protected void assertNodeStartingWith(String string) {
			StringBuffer badNodes = new StringBuffer();
			SWTBotTreeItem[] items = view.bot().tree().getAllItems();
			for (SWTBotTreeItem item : items) {
				if (item.getText().startsWith(string))
					return; //OK!
				else
					badNodes.append(item.getText()+"\n");
			}
			fail("Looking for '"+string+"' but found only \n"+badNodes.toString());
		}

		protected void assertTextWithLabel(String label, String expected) {
			assertEquals("JUnit View >> Text with label '"+label+"'", expected, view.bot().textWithLabel(label).getText());
		}


		public boolean test() throws Exception {
			try {
				assertViewOK();
				return true;
			} catch (Throwable e) {
				msg = e.getMessage();
				return false;
			}
		}

		public String getFailureMessage() {
			return msg;
		}
	}

	private static final long TIMEOUT_TEST_APP = 60000;
	private static IProject project;

	@Override
	protected String generateTestProjectName() {
		return "gTunes";
	}
	
	@Override
	public void setupClass() throws Exception {
		super.setupClass();
		
		StsTestUtil.setAutoBuilding(false);
		project = getTestProject();

		createFile(project, "grails-app/domain/gTunes/domain/Song.groovy",
				"package gTunes.domain\n" + 
				"\n" + 
				"class Song {\n" + 
				"\n" + 
				"    static constraints = {\n" + 
				"		title(blank:false)\n" + 
				"		artist(blank:false)\n" + 
				"    }\n" + 
				"	\n" + 
				"	String title\n" + 
				"	String artist\n" + 
				"	\n" + 
				"}\n");
		
		createFile(project, "test/unit/gTunes/domain/SongTests.groovy",
				"package gTunes.domain\n" + 
				"\n" + 
				"import grails.test.*\n" + 
				"\n" + 
				"class SongTests extends GrailsUnitTestCase {\n" + 
				"    protected void setUp() {\n" + 
				"        super.setUp()\n" + 
				"    }\n" + 
				"\n" + 
				"    protected void tearDown() {\n" + 
				"        super.tearDown()\n" + 
				"    }\n" + 
				"\n" + 
				"    void testSomething() {\n" + 
				"    }\n" + 
				"}\n");
		
		createFile(project, "test/integration/gTunes/SongITests.groovy",
				"package gTunes\n" + 
				"\n" + 
				"import gTunes.domain.Song;\n" + 
				"import grails.test.*\n" + 
				"\n" + 
				"class SongITests extends GrailsUnitTestCase {\n" + 
				"    protected void setUp() {\n" + 
				"        super.setUp()\n" + 
				"    }\n" + 
				"\n" + 
				"    protected void tearDown() {\n" + 
				"        super.tearDown()\n" + 
				"    }\n" + 
				"\n" + 
				"    void testSomething() {\n" + 
				"		Song song = new Song()\n" + 
				"		song.title = \"foo\"\n" + 
				"		song.artist = \"Kung\"\n" + 
				"		if (song.validate()) {\n" + 
				"			//OK\n" + 
				"		}\n" + 
				"		else {\n" + 
				"			fail \"Validation should be ok!\"\n" + 
				"		}\n" + 
				"    }\n" + 
				"	\n" + 
				"}\n");
		
		StsTestUtil.setAutoBuilding(true);
		StsTestUtil.waitForAutoBuild();
	}

	/**
	 * Create a file in the given project
	 * @throws CoreException 
	 */
	private void createFile(IProject project, String pathName, String contents) throws CoreException {
		IFile file = project.getFile(pathName);
		createParents(file);
		file.create(new ByteArrayInputStream(contents.getBytes()), true, null);
	}
	
	/**
	 * Typically called before creating a File to ensure that all the parents in the path leading
	 * to the file are created if necessary
	 * @throws CoreException 
	 */
	private void createParents(IResource fileOrFolder) throws CoreException {
		IContainer parent = fileOrFolder.getParent();
		if (parent!=null && !parent.exists()) {
			IFolder folder = (IFolder) parent;
			createParents(folder);
			folder.create(true, true, null);
		}
	}
	
	/**
	 * This test verifies whether the {@link SWTBotUtils}.isOnBuildSite works properly.
	 * This test will/should fail when it is being run normally. It should pass on the
	 * buildServer. 
	 */
	public void testIsOnBuildSite() {
		if (!StsTestUtil.isOnBuildSite()) { 
			String workspaceLoc = ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toString();
			fail("SWTBotUtils.isOnBuildSite returned false for workspaceLoc = "+
					ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toString());
		}
	}

	/**
	 * Test that does nothing, we run this test just to ensure that all the setup / scaffolding code works.
	 */
	public void testScaffolding() {
		assertTrue(getTestProject().getFile("test/unit/gTunes/domain/SongTests.groovy").exists());
	}
	
	public void testTestAppOnProject() throws Exception {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem projectNode = explorerTree
				.getTreeItem(project.getName());
		final SWTBotView junitView = runTestApp(projectNode);

		try {
			AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
				@Override
				protected void assertViewOK() {
					assertTextWithLabel("Runs: ", "2/2");
					assertTextWithLabel("Errors: ", "0");
					assertTextWithLabel("Failures: ", "0");
					assertNodeStartingWith("gTunes.SongITests");
					assertNodeStartingWith("gTunes.domain.SongTests");
				}
			};
			viewOK.waitForTest(TIMEOUT_TEST_APP);
		} catch (Exception e) {
			System.out.println(">>>Console text");
			System.out.println(SWTBotUtils.getConsoleText(bot));
			System.out.println("<<<Console text");
			throw e;
		}
	}
	
	protected SWTBotView runTestApp(SWTBotTreeItem node) {
		SWTBotMenu runAsMenu = node.contextMenu("Run As");
		SWTBotMenu testAppMenu = SWTBotUtils.subMenuContaining(bot, runAsMenu, "Grails Command (test-app)");
		testAppMenu.click();
		SWTBotView junitView = getView("JUnit");
		return junitView;
	}
	
	public void testTestAppOnIntegrationTestFolder() {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem node = explorerTree.expandNode(project.getName(), "test/integration");
		final SWTBotView junitView = runTestApp(node);
		
		AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
			protected void assertViewOK() {
				assertTextWithLabel("Runs: ","1/1");
				assertTextWithLabel("Errors: ","0");
				assertTextWithLabel("Failures: ","0");
				assertNodeStartingWith("gTunes.SongITests");
			}
		};
		viewOK.waitForTest(TIMEOUT_TEST_APP);
	}
	
	public void testTestAppOnUnitTestFolder() {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem node = explorerTree.expandNode(project.getName(), "test/unit");
		final SWTBotView junitView = runTestApp(node);
		AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
			@Override
			protected void assertViewOK() {
				assertTextWithLabel("Runs: ","1/1");
				assertTextWithLabel("Errors: ","0");
				assertTextWithLabel("Failures: ","0");
				assertNodeStartingWith("gTunes.domain.SongTests");
			}
		};
		viewOK.waitForTest(TIMEOUT_TEST_APP);
	}
	
	public void testTestAppOnTestFile() {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem node = explorerTree.expandNode(project.getName(), "test/integration", "gTunes", "SongITests.groovy");
		SWTBotView junitView = runTestApp(node);
		
		AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
			protected void assertViewOK() {
				assertTextWithLabel("Runs: ","1/1");
				assertTextWithLabel("Errors: ","0");
				assertTextWithLabel("Failures: ","0");
				assertNodeStartingWith("gTunes.SongITests");
			}
		};
		viewOK.waitForTest(TIMEOUT_TEST_APP);
		
		SWTBotConditions.waitForConsoleOutput(bot, "Tests PASSED - view reports in");
		assertFalse("Unit test phase should not have run!", SWTBotUtils.getConsoleText(bot).contains("Starting unit test phase"));
	}
	
	public void testTestAppOnPackage() {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem node = explorerTree.expandNode(project.getName(), "domain", "gTunes.domain");
		SWTBotView junitView = runTestApp(node);
		
		AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
			protected void assertViewOK() {
				assertTextWithLabel("Runs: ","1/1");
				assertTextWithLabel("Errors: ","0");
				assertTextWithLabel("Failures: ","0");
				assertNodeStartingWith("gTunes.domain.SongTests");
			}
		};
		viewOK.waitForTest(TIMEOUT_TEST_APP);
		
		SWTBotConditions.waitForConsoleOutput(bot, "Tests PASSED - view reports in");
	}
	
	public void testTestAppOnPackageInUnitTests() {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem node = explorerTree.expandNode(project.getName(), "test/unit", "gTunes.domain");
		SWTBotView junitView = runTestApp(node);
		
		AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
			protected void assertViewOK() {
				assertTextWithLabel("Runs: ","1/1");
				assertTextWithLabel("Errors: ","0");
				assertTextWithLabel("Failures: ","0");
				assertNodeStartingWith("gTunes.domain.SongTests");
			}
		};
		viewOK.waitForTest(TIMEOUT_TEST_APP);
		
		SWTBotConditions.waitForConsoleOutput(bot, "Tests PASSED - view reports in");
		assertFalse("Integration test phase should not have run!", SWTBotUtils.getConsoleText(bot).contains("Starting integration test phase"));
	}
	
	public void testTestAppOnSourceFile() {
		SWTBotTree explorerTree = selectProject(project.getName());
		SWTBotTreeItem node = explorerTree.expandNode(project.getName(), "domain", "gTunes.domain", "Song.groovy");
		SWTBotView junitView = runTestApp(node);
		
		AbstractedWaitCondition viewOK = new ViewOKTest(bot, junitView) {
			protected void assertViewOK() {
				assertTextWithLabel("Runs: ","1/1");
				assertTextWithLabel("Errors: ","0");
				assertTextWithLabel("Failures: ","0");
				assertNodeStartingWith("gTunes.domain.SongTests");
			}
		};
		viewOK.waitForTest(TIMEOUT_TEST_APP);
	}
}
