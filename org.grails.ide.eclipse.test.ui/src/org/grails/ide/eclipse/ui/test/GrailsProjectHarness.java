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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;

import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.ui.test.util.GrailsUITestCase;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @created Jul 21, 2010
 */
public class GrailsProjectHarness extends GrailsUITestCase {

	private static String generatedProjectName;

	protected String generateTestProjectName() {
		return this.getClass().getSimpleName(); // Use test class name for less
												// chances of different tests
												// influencing each other.
	}

	@Override
	public void setupClass() throws Exception {
		super.setupClass();
		GrailsTest.ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		createProjectsInView(new String[] { generatedProjectName = generateTestProjectName() });
		SWTBotUtils.openPerspective(bot, "Grails");
		initTreeExpansion();
	}

	protected void validateProjectSetup() throws Exception {
		assertTrue(getExplorerViewTree().hasItems());

		SWTBotTreeItem projectNode = getExpandedProjectTreeNode();
		assertNotNull(projectNode);

		assertTrue(projectNode.isVisible());
		assertTrue(projectNode.isExpanded());
		assertEquals(getTestProject().getName(), projectNode.getText());
	}

	protected void initTreeExpansion() throws Exception {
		SWTBotTree tree = getExplorerViewTree();
		IProject project = getTestProject();
		tree.select(project.getName());
		tree.expandNode(project.getName(), false);

		validateProjectSetup();
	}

	/**
	 * Gets the tree item corresponding to the selected and expanded project in
	 * the explorer view.
	 * 
	 * @return
	 */
	protected SWTBotTreeItem getExpandedProjectTreeNode() {
		return getExplorerViewTree().getTreeItem(getTestProject().getName());
	}

	protected IProject getTestProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(generatedProjectName);
	}

}
