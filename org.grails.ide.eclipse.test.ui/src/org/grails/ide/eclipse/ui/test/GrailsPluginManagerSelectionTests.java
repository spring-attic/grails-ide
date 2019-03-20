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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginState;


/**
 * Tests performed
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @created Jul 22, 2010
 */
public class GrailsPluginManagerSelectionTests extends
		GrailsPluginManagerHarness {

	protected static final String[] PREINSTALLED_GRAILS_PROJECT_PLUGINS = new String[] {
			"hibernate", "tomcat" };

	public void testPreinstalledPluginsGrailsProject() throws Exception {

		for (String pluginName : PREINSTALLED_GRAILS_PROJECT_PLUGINS) {
			SWTBotTreeItem preinstalled = getTreeItem(pluginName, null);
			assertNotNull(preinstalled);
			String rootVersionID = getVersionID(preinstalled);

			// Get the corresponding child version, and verify
			// it is the actual corresponding child
			SWTBotTreeItem expectedVersion = getTreeItem(pluginName,
					rootVersionID);

			// Preinstalled should ALWAYS be marked as installed without
			// updates, even though newer versions may exist, as
			// preinstalled plugins should match the Grails version being used
			assertCorrectnessInstalledOrUninstallPlugin(preinstalled,
					expectedVersion, PluginState.INSTALLED);

		}
	}

	public void testCorrespondingVersion() throws Exception {
		// test if the corresponding version logic returns the
		// actual corresponding version that matches the root version
		SWTBotTreeItem rootItem = getTreeItem("hibernate", null);
		String rootVersion = getVersionID(rootItem);
		SWTBotTreeItem expectedChildVersion = getTreeItem("hibernate",
				rootVersion);
		SWTBotTreeItem actualChildVersion = getCorrespondingChildVersion(rootItem);
		assertEquals(getPublishedVersion(expectedChildVersion),
				getPublishedVersion(actualChildVersion));
		assertTrue(getPublishedVersion(expectedChildVersion).getVersion()
				.equals(getPublishedVersion(actualChildVersion).getVersion()));
	}

	public void testBasicRootSelection() throws Exception {
		SWTBotTreeItem expectedSelection = getTreeItem("code-coverage", null);
		SWTBotTreeItem selectedRoot = selectRootTreeItem("code-coverage");
		assertEquals(getPublishedVersion(expectedSelection),
				getPublishedVersion(selectedRoot));
		assertTrue(getPublishedVersion(expectedSelection).getVersion().equals(
				getPublishedVersion(selectedRoot).getVersion()));
	}

	public void testBasicVersionSelection() throws Exception {
		SWTBotTreeItem expectedSelection = getTreeItem("hibernate", "1.2.1");
		SWTBotTreeItem selectedRoot = selectChildVersionElement("hibernate",
				"1.2.1");
		assertEquals(getPublishedVersion(expectedSelection),
				getPublishedVersion(selectedRoot));
		assertTrue(getPublishedVersion(expectedSelection).getVersion().equals(
				getPublishedVersion(selectedRoot).getVersion()));
	}

	/**
	 * Tests an install operation on a root element, and also tests the undoing
	 * of the install operation via the "uninstall" button
	 * 
	 * @throws Exception
	 */
	public void testSingleInstallRootSelection() throws Exception {
		String pluginName = "twitter";
		selectRootTreeItem(pluginName);

		// Check correct button states
		assertSelectionButtonEnablement(true, false, false);

		assertNotNull(pressInstallOnRoot(pluginName, PluginState.SELECT_INSTALL));

		// Now that it has been selected, the install button should be disabled
		assertSelectionButtonEnablement(false, true, false);

		// Undo the install operation
		assertNotNull(pressUninstallOnRoot(pluginName, null));

		// Uninstall button should be disabled, install button enabled
		assertSelectionButtonEnablement(true, false, false);
	}

	/**
	 * Tests an install operation on a child version element, and also tests the
	 * undoing of the install operation via the "uninstall" button
	 * 
	 * @throws Exception
	 */
	public void testSingleChildVersionInstall() throws Exception {
		// Undo selection first
		String pluginName = "twitter";
		String latestID = getVersionID(getTreeItem(pluginName, null));
		SWTBotTreeItem child = selectChildVersionElement(pluginName, "0.1");
		assertEquals(null, getPluginState(child));

		assertSelectionButtonEnablement(true, false, false);

		SWTBotTreeItem childVersion = pressInstallOnSelectedChildVersion(
				pluginName, PluginState.SELECT_INSTALL, "0.1");
		assertNotNull(childVersion);

		assertSelectionButtonEnablement(false, true, false);

		// test the undo

		pressUninstallButton();
		SWTBotTreeItem root = getTreeItem(pluginName, null);
		childVersion = getTreeItem(pluginName, latestID);

		// Verify that the root and corresponding child are no longer marked
		assertCorrectnessInstalledOrUninstallPlugin(root, childVersion, null);
		assertSelectionButtonEnablement(true, false, false);
	}

	/**
	 * Tests an uninstall operation on a root element, and also tests the
	 * undoing of the uninstall operation via the "install" button
	 * 
	 * @throws Exception
	 */
	public void testSingleUninstallRootSelection() throws Exception {

		String pluginName = "hibernate";
		selectRootTreeItem(pluginName);

		// Check correct button states
		assertSelectionButtonEnablement(false, true, false);

		assertNotNull(pressUninstallOnRoot(pluginName,
				PluginState.SELECT_UNINSTALL));

		assertSelectionButtonEnablement(true, false, false);

		// Undo the install operation
		assertNotNull(pressInstallOnRoot(pluginName, PluginState.INSTALLED));

		assertSelectionButtonEnablement(false, true, false);
	}

	public void testSingleChildVersionUninstall() throws Exception {

		// Be sure it is in a installed state for testing.
		String pluginName = "hibernate";

		SWTBotTreeItem rootItem = getTreeItem(pluginName, null);
		assertNotNull(rootItem);

		String installedVersion = getVersionID(rootItem);

		// Select the corresponding installed version
		SWTBotTreeItem installedVersionElement = selectChildVersionElement(
				pluginName, installedVersion);

		assertNotNull(installedVersionElement);

		// Check correct button states
		assertSelectionButtonEnablement(false, true, false);

		pressUninstallButton();
		assertCorrectnessInstalledOrUninstallPlugin(rootItem,
				installedVersionElement, PluginState.SELECT_UNINSTALL);

		assertSelectionButtonEnablement(true, false, false);

		pressInstallOnSelectedChildVersion(pluginName, PluginState.INSTALLED,
				installedVersion);

		assertSelectionButtonEnablement(false, true, false);
	}

	public void testSeveralInstallVersionSelections() throws Exception {
		// Select and older version for install
		String pluginName = "amazon-s3";
		SWTBotTreeItem root = getTreeItem(pluginName, null);
		String latestID = getVersionID(root);
		selectRootTreeItem(pluginName);

		// Only install button should be enabled
		assertSelectionButtonEnablement(true, false, false);

		SWTBotTreeItem childVersion = pressInstallOnSelectedChildVersion(
				pluginName, PluginState.SELECT_INSTALL, "0.7.1");
		assertNotNull(childVersion);

		// Select a new version for install
		childVersion = pressInstallOnSelectedChildVersion(pluginName,
				PluginState.SELECT_INSTALL, "0.8.2");
		assertNotNull(childVersion);

		// Select an even older version
		childVersion = pressInstallOnSelectedChildVersion(pluginName,
				PluginState.SELECT_INSTALL, "0.6");
		assertNotNull(childVersion);

		// Verify that the root and last selected child are marked for install
		assertCorrectnessInstalledOrUninstallPlugin(root, childVersion,
				PluginState.SELECT_INSTALL);

		// Now select the correct version to undo.
		childVersion = selectChildVersionElement(pluginName, "0.6");
		assertSelectionButtonEnablement(false, true, false);

		pressUninstallButton();

		// Verify everything is restored, and nothing is marked
		childVersion = getTreeItem(pluginName, latestID);
		assertCorrectnessInstalledOrUninstallPlugin(root, childVersion, null);

	}

	/**
	 * Note that this test sometimes fails because it is trying to get an older
	 * version of hibernate that may no longer be available in the version of
	 * Grails that is used at STS runtime. If so, change the value to an older
	 * version of hibernate that does exist in the Grails install used by the
	 * STS runtime that is executing this test.
	 * 
	 * @throws Exception
	 */
	public void testSeveralUpdateVersionSelections() throws Exception {
		// Select and older version for install
		String pluginName = "hibernate";
		SWTBotTreeItem root = getTreeItem(pluginName, null);
		String latestID = getVersionID(root);
		selectRootTreeItem(pluginName);

		// Only uninstall button should be enabled
		assertSelectionButtonEnablement(false, true, false);

		// Select an older version.
		SWTBotTreeItem childVersion = selectChildVersionElement(pluginName,
				getOlderExistingHibernateVersion());

		// only update should be enabled
		assertSelectionButtonEnablement(false, false, true);

		pressUpdateButton();

		// Verify child is marked for install
		assertTrue(getPluginState(childVersion) == PluginState.SELECT_INSTALL);

		// Select a new version for install
		SWTBotTreeItem otherchildVersion = selectChildVersionElement(
				pluginName, getNewerExistingHibernateVersion());

		pressUpdateButton();

		assertTrue(getPluginState(childVersion) == null);
		assertTrue(getPluginState(otherchildVersion) == PluginState.SELECT_INSTALL);

		pressUninstallButton();

		// Verify everything is restored
		childVersion = getTreeItem(pluginName, latestID);
		assertCorrectnessInstalledOrUninstallPlugin(root, childVersion,
				PluginState.INSTALLED);
	}

	/**
	 * This may have to be changed in the future. This returns an older version
	 * of hibernate plugin that exists in the particular version of Grails that
	 * this test is using. If the hardcoded value no longer exists, change it to
	 * an old version that does exist.
	 * 
	 * @return version number of an old hibernate plugin
	 */
	protected String getOlderExistingHibernateVersion() {
		return "1.2.0";
	}

	/**
	 * Return a hibernate version that is relatively recent, BUT not the most
	 * recent version
	 * 
	 * @return
	 */
	protected String getNewerExistingHibernateVersion() {
		return "1.3.0";
	}

	public void testSTS1091() throws Exception {
		// Tests fix for STS_1091 defect
		String pluginName = "hibernate";

		// Select the plugin and verify it has the correct original state (i.e.
		// it should
		// be marked as Installed)
		SWTBotTreeItem rootItem = selectRootTreeItem(pluginName);

		assertNotNull(rootItem);

		assertTrue(validateRootAndChildVersionState(PluginState.INSTALLED,
				rootItem));

		String installedVersionID = getVersionID(rootItem);
		SWTBotTreeItem installedVersion = getTreeItem(pluginName,
				installedVersionID);

		assertNotNull(installedVersion);

		// Select an old unmarked version for uninstall. SHould do nothing
		// as it is not a valid selection
		String oldVersion = "1.2.1";

		// Verify the selected old version is indeed an old version
		// before proceeding with the test
		assertTrue(oldVersion.compareTo(installedVersionID) < 0);

		// Now press install on the old version to mark it for install
		SWTBotTreeItem selectedOldVersion = selectChildVersionElement(
				pluginName, oldVersion);

		assertNotNull(selectedOldVersion);

		// Only the update button should be enabled
		assertSelectionButtonEnablement(false, false, true);

		pressUpdateButton();

		// Only uninstall is enabled to allow users to undo
		assertSelectionButtonEnablement(false, true, false);

		// at this stage there are TWO marked versions, the actual installed
		// version
		// and the older version marked for install (i.e. marked for
		// "downgrade").
		assertTrue(getPluginState(selectedOldVersion) == PluginState.SELECT_INSTALL);
		assertTrue(getPluginState(installedVersion) == PluginState.INSTALLED);
		// Verify the root is marked for select install as well
		assertTrue(validateRootAndChildVersionState(PluginState.SELECT_INSTALL,
				rootItem));

		// Select the actual installed version and mark it for uninstall;
		selectChildVersionElement(pluginName, installedVersionID);

		// Only the uninstall button should be enabled
		assertSelectionButtonEnablement(false, true, false);

		pressUninstallButton();

		// Only the install button should be enabled for undo
		assertSelectionButtonEnablement(true, false, false);

		// Verify that old version that was marked for "downgrade" is no longer
		// selected AND
		// that the installed version and the root are now marked for Uninstall.
		assertTrue(getPluginState(selectedOldVersion) == null);
		assertTrue(validateRootAndChildVersionState(
				PluginState.SELECT_UNINSTALL, rootItem));
		Set<PluginVersion> markedVersion = new HashSet<PluginVersion>();
		markedVersion.add(getPublishedVersion(installedVersion));

		// Verify no other child versions are marked
		assertTrue(verifyRemainingChildVersionUnmarked(rootItem, markedVersion));

		// Select old version again
		selectedOldVersion = selectChildVersionElement(pluginName, oldVersion);

		// Verify that the update button is reenabled
		assertSelectionButtonEnablement(false, false, true);

		pressUpdateButton();

		assertTrue(getPluginState(selectedOldVersion) == PluginState.SELECT_INSTALL);
		assertTrue(getPluginState(installedVersion) == PluginState.INSTALLED);
		// Verify the root is marked for select install as well
		assertTrue(validateRootAndChildVersionState(PluginState.SELECT_INSTALL,
				rootItem));

	}
}
