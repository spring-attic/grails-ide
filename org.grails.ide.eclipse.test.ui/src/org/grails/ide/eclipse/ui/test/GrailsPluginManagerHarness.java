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

import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginState;

import org.grails.ide.eclipse.ui.test.util.SWTBotPluginManagerUtil;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @created Jul 22, 2010
 */
public class GrailsPluginManagerHarness extends GrailsProjectHarness {

	protected SWTBotShell getPopulatedPluginManagerDialogue() throws Exception {
		SWTBotShell manager = SWTBotPluginManagerUtil
				.getGrailsManagerDialogueViaShortcutKey(bot,
						getProjectViewName(), getTestProject().getName())
				.getOpenedGrailsManager();

		assertNotNull(manager);
		assertTrue(manager.isActive());
		assertTrue(isManagerOpen());
		return manager;
	}

	protected boolean isManagerOpen() throws Exception {
		return SWTBotPluginManagerUtil.getGrailsManagerDialogueViaShortcutKey(
				bot, getProjectViewName(), getTestProject().getName()).isOpen();
	}

	protected PluginVersion getPublishedVersion(SWTBotTreeItem item) {
		PluginVersion version = SWTBotPluginManagerUtil.getPublishedVersion(
				bot, item);
		assertNotNull(version);
		return version;
	}

	public void setupInstance() throws Exception {
		super.setupInstance();
		assertTrue(getTestProject().exists());
		getPopulatedPluginManagerDialogue();
	}

	public void tearDownClass() throws Exception {
		if(isManagerOpen()) {
			getPopulatedPluginManagerDialogue().close();
		}
		super.tearDownClass();
	}

	/**
	 * <p>
	 * Press the "Install" button on the given root plugin, and performs the
	 * following verifications:
	 * </p>
	 * <p>
	 * Verifies that the corresponding child version is also marked, and that
	 * the root version and plugins state matches the marked child version, and
	 * that the root element state matches the expected state.
	 * </p>
	 * <p>
	 * Verifies that no other child versions are marked. NOTE: This means that
	 * this method WILL FAIL if other child versions are expected to be marked.
	 * Therefore use this method only to press install when the outcome is to
	 * have exactly ONE child version marked.
	 * </p>
	 * <p>
	 * If all verification passes, returns the selected root Item.
	 * </p>
	 * <p>
	 * Install operation can be used to UNDO an Uninstall selection, therefore
	 * the expected plugin state is the state expected when the install
	 * operation is performed. It will vary depending on whether install is used
	 * to select a plugin for install or used to undo an uninstall.
	 * </p>
	 * 
	 * @param pluginName
	 *            to install or undo
	 * @param expectedPluginState
	 *            state after install performed
	 * @return
	 */
	protected SWTBotTreeItem pressInstallOnRoot(String pluginName,
			PluginState expectedPluginState) {
		SWTBotTreeItem rootItem = selectRootTreeItem(pluginName);
		// Get the corresponding child version, and verify
		// it is the actual corresponding child
		String rootID = getVersionID(rootItem);
		SWTBotTreeItem expectedVersion = getTreeItem(pluginName, rootID);
		assertTrue(isInstallButtonEnabled());
		pressInstallButton();
		assertCorrectnessInstalledOrUninstallPlugin(rootItem, expectedVersion,
				expectedPluginState);
		return rootItem;
	}

	/**
	 * Press install on the specified plugin version and checks the state
	 * against the expected state. In addition performs the following
	 * validations:
	 * <p>
	 * Verifies that the root reflects the version that is marked for install,
	 * meaning that the root pluginstate and version ID should match the version
	 * that was selected
	 * </p>
	 * <p>
	 * Verifies that no other child version for that plugin is marked. NOTE:
	 * This means that this method WILL FAIL if other child versions are
	 * expected to be marked. Therefore use this method only to press install
	 * when the outcome is to have exactly ONE child version marked.
	 * </p>
	 * <p>
	 * If all verification passes, returns the selected child version
	 * </p>
	 * <p>
	 * Install operation can be used to UNDO an Uninstall selection, therefore
	 * the expected plugin state is the state expected when the install
	 * operation is performed. It will vary depending on whether install is used
	 * to select a plugin for install or used to undo an uninstall.
	 * </p>
	 * 
	 * @param pluginName
	 *            to install or to undo
	 * @param versionIDToSelect
	 *            version ID to select for install
	 * @param expectedPluginState
	 *            state after install performed
	 * @return
	 */
	protected SWTBotTreeItem pressInstallOnSelectedChildVersion(
			String pluginName, PluginState expectedPluginState,
			String versionIDToSelect) {
		SWTBotTreeItem childVersion = selectChildVersionElement(pluginName,
				versionIDToSelect);
		SWTBotTreeItem root = getTreeItem(pluginName, null);
		assertTrue(isInstallButtonEnabled());
		pressInstallButton();
		assertCorrectnessInstalledOrUninstallPlugin(root, childVersion,
				expectedPluginState);
		return childVersion;
	}

	protected void installPlugins(String[][] plugins) throws Exception {
		getPopulatedPluginManagerDialogue();  //Code below relies on Plug. Man to be there! Ensure it is open! 
		for (String[] plugin : plugins) {
			String pluginName = plugin[0];
			String version = plugin.length > 1 ? plugin[1] : null;
			SWTBotTreeItem item = selectChildVersionElement(pluginName, version);
			assertNotNull(item);
			pressInstallButton();
		}

		pressOKButton();
		bot.sleep(30000);
		SWTBotUtils.waitForAllBuildJobs(bot);
	}

	/**
	 * Press Uninstall on the specified root element. Validates against the
	 * expected state after the uninstall is pressed. NOTE: this Only works when
	 * expecting one Child version to be marked after the operation completes.
	 * It will detect and fail if any other child versions are marked after an
	 * uninstall operation is performed.
	 * <p>
	 * Verifies that the root and corresponding child versions are both marked
	 * correctly, and that the root version ID matches the corresponding child
	 * version ID
	 * </p>
	 * <p>
	 * If Verification passes, returns the selected root plugin element
	 * </p>
	 * <p>
	 * Note that the uninstall operation can also be used to UNDO a selection
	 * marked for install. Therefore the expected plugin state is the state
	 * expected once an undo operation is performed, and it may vary based on
	 * whether uninstall is performed to uninstall an installed plugin, or to
	 * undo a selection marked for install.
	 * </p>
	 * 
	 * @param pluginName
	 *            to uninstall or undo
	 * @param expectedState
	 *            state after the uninstall is performed
	 * @return
	 */
	protected SWTBotTreeItem pressUninstallOnRoot(String pluginName,
			PluginState expectedState) {
		SWTBotTreeItem rootItem = selectRootTreeItem(pluginName);
		assertTrue(isUninstallButtonEnabled());
		pressUninstallButton();

		// Verify that the version of the root has not changed and that it has
		// no plugin state
		assertCorrectnessInstalledOrUninstallPlugin(rootItem, null,
				expectedState);
		return rootItem;
	}

	/**
	 * Get the corresponding child version that matches the version of the root
	 * element
	 * 
	 * @param rootItem
	 * @return
	 */
	protected SWTBotTreeItem getCorrespondingChildVersion(
			SWTBotTreeItem rootItem) {
		return SWTBotPluginManagerUtil.getCorrespondingChildVersion(bot,
				rootItem);
	}

	protected void pressInstallButton() {
		SWTBotPluginManagerUtil.pressInstallButton(bot);
	}

	protected void pressUninstallButton() {
		SWTBotPluginManagerUtil.pressUninstallButton(bot);
	}

	protected void pressUpdateButton() {
		SWTBotPluginManagerUtil.pressUpdateButton(bot);
	}

	protected void pressOKButton() {
		SWTBotPluginManagerUtil.pressOKButton(bot);
	}

	protected boolean isInstallButtonEnabled() {
		return SWTBotPluginManagerUtil.getInstallButton(bot).isEnabled();
	}

	protected boolean isUninstallButtonEnabled() {
		return SWTBotPluginManagerUtil.getUninstallButton(bot).isEnabled();
	}

	protected boolean isUpdateButtonEnabled() {
		return SWTBotPluginManagerUtil.getUpdateButton(bot).isEnabled();
	}

	/**
	 * Gets a tree item WITHOUT selecting it. If a version ID is specified, it
	 * will get the child version element. If no version ID is specified, it
	 * will get the root element matching the plugin name.
	 * 
	 * @param pluginName
	 * @param versionID
	 * @return
	 */
	protected SWTBotTreeItem getTreeItem(String pluginName, String versionID) {
		return SWTBotPluginManagerUtil.getPluginTreeItem(bot, pluginName,
				versionID);
	}

	/**
	 * Select a particular child version for the given plugin. Asserts that the
	 * selection has been made before returning the selection.
	 * 
	 * @param pluginName
	 * @param versionID
	 *            if null, it will only select the root element matching the
	 *            plugin name
	 * @return
	 */
	protected SWTBotTreeItem selectChildVersionElement(String pluginName,
			String versionID) {
		SWTBotTreeItem foundItem = SWTBotPluginManagerUtil.getPluginTreeItem(
				bot, pluginName, versionID);
		foundItem.select();
		assertNotNull(foundItem);
		assertTrue(foundItem.isSelected());
		return foundItem;
	}

	/**
	 * Selects the root tree element matching the given pluginName. Asserts that
	 * the selection has been made before returning the selection.
	 * 
	 * @param pluginName
	 * @return
	 */
	protected SWTBotTreeItem selectRootTreeItem(String pluginName) {
		return selectChildVersionElement(pluginName, null);
	}

	/**
	 * Verifies that none of the other child versions of a given plugin are
	 * marked, except those that are listed as exempt.
	 * 
	 * @param rootItem
	 * @param exempt
	 * @return
	 */
	protected boolean verifyRemainingChildVersionUnmarked(
			SWTBotTreeItem rootItem, Set<PluginVersion> exempt) {
		SWTBotTreeItem[] children = rootItem.getItems();
		for (SWTBotTreeItem child : children) {
			PluginState childState = getPluginState(child);
			PluginVersion version = SWTBotPluginManagerUtil
					.getPublishedVersion(bot, child);
			if ((exempt == null || !exempt.contains(version))
					&& childState != null) {
				print("Verifying child version states. Expected null, but actual: "
						+ childState);
				return false;
			}
		}
		return true;
	}

	/**
	 * <p>
	 * Asserts whether the root plugin state and version ID match the expected
	 * child version Note that this ONLY works for installing NEW plugins and
	 * uninstalling existing plugins. It does NOT work for plugins that are
	 * installed but have another version marked for update, as the latter will
	 * have two child versions that are marked, the version that is installed
	 * and the version that the user wants to upgrade/downgrade to.
	 * </p>
	 * if no expected corresponding version is specified, it will check the root
	 * selection state against the corresponding child version it actually
	 * finds, but it will not check it against an expected child version.
	 * <p>
	 * if no expected Version ID is specified, it will verify that the root
	 * version and the corresponding version it finds have the same ID, but does
	 * not check it against an expected version ID.
	 * </p>
	 * 
	 * @param root
	 * @param expectedCorrespondingVersion
	 * @param expectedRootChildState
	 * @param expectedVersionID
	 */
	protected void assertCorrectnessInstalledOrUninstallPlugin(
			SWTBotTreeItem root, SWTBotTreeItem expectedCorrespondingVersion,
			PluginState expectedRootChildState) {
		// Verify there is a corresponding version child for the root
		// and it matches the expected corresponding version
		SWTBotTreeItem childTreeItem = getCorrespondingChildVersion(root);
		assertNotNull(childTreeItem);

		if (expectedCorrespondingVersion != null) {
			assertEquals(getPublishedVersion(childTreeItem),
					getPublishedVersion(expectedCorrespondingVersion));
			childTreeItem = expectedCorrespondingVersion;
		}

		// Verify that the expected version number of the root matches the
		// actual root version
		String actualRootVersion = getVersionID(root);
		String correspondingChildVersion = getVersionID(childTreeItem);
		assertTrue(actualRootVersion.equals(correspondingChildVersion));

		// Verify that the root and corresponding child both have the same
		// expected state
		assertTrue(validateRootAndChildVersionState(expectedRootChildState,
				root));

		// Verify that none of the other child versions are marked.
		assertTrue(verifyRemainingChildVersionUnmarked(root, childTreeItem));
	}

	/**
	 * Verifies that the root is marked with a "Update Available" state, that
	 * it's version id matches a child version element marked with "installed"
	 * and that no other child versions are marked.
	 * 
	 * @param pluginName
	 * @param expectedInstalledVersionID
	 */
	protected void assertCorrectnessOfInstalledPluginWithUpdate(
			String pluginName, String expectedInstalledVersionID) {
		// Verify that both the root and corresponding child version
		// match the expected version ID
		SWTBotTreeItem root = getTreeItem(pluginName, null);
		String versionID = getVersionID(root);
		assertTrue(versionID.equals(expectedInstalledVersionID));

		SWTBotTreeItem correspondingChild = getCorrespondingChildVersion(root);
		versionID = getVersionID(correspondingChild);
		assertTrue(versionID.equals(expectedInstalledVersionID));

		// Validate that the root and corresponding child have the correct state
		assertTrue(validateRootAndChildVersionState(
				PluginState.UPDATE_AVAILABLE, root));

		Set<PluginVersion> markedVersions = new HashSet<PluginVersion>();
		markedVersions.add(getPublishedVersion(correspondingChild));

		// Verify that none of the other child versions are marked.
		assertTrue(verifyRemainingChildVersionUnmarked(root, markedVersions));

	}

	/**
	 * Verify that when a newer/older version of a currently installed plugin is
	 * selected for install, and that the root of the plugin reflects the
	 * selected version marked for update. It also verifies that the installed
	 * version is still marked as installed.
	 * <p>
	 * This method checks that there are TWO marked versions, the version
	 * currently installed and the version that has been marked for
	 * updating/downgrading, and verifies that both are different.
	 * </p>
	 * 
	 * @param pluginName
	 * @param installedVersionID
	 * @param expectedUpdatedVersionID
	 */
	protected void assertPluginHasBeenUpdated(String pluginName,
			String installedVersionID, String expectedUpdatedVersionID) {
		// The root should be marked with the updated version ID
		SWTBotTreeItem root = getTreeItem(pluginName, null);
		String versionID = getVersionID(root);
		assertTrue(versionID.equals(expectedUpdatedVersionID));

		SWTBotTreeItem updatedChild = getCorrespondingChildVersion(root);
		versionID = getVersionID(updatedChild);
		assertTrue(versionID.equals(expectedUpdatedVersionID));

		SWTBotTreeItem installedChild = getTreeItem(pluginName,
				installedVersionID);
		// Verify the expected version is not the same as the installed version
		assertTrue(!installedVersionID.equals(expectedUpdatedVersionID));
		assertNotSame(installedChild, updatedChild);

		// Verify that the root and its corresponding updated child are marked
		// as select install
		// (i.e. meaning they are marked to be updated)
		assertTrue(validateRootAndChildVersionState(PluginState.SELECT_INSTALL,
				root));

		// Verify that the installed version is marked as installed. THis should
		// not change even when selecting another version to upgrade to.
		assertTrue(getPluginState(installedChild) == PluginState.INSTALLED);

		// Verify that no other children except the two mentioned above (the
		// installed version
		// and the updated version) are marked
		Set<PluginVersion> markedVersions = new HashSet<PluginVersion>();
		markedVersions.add(getPublishedVersion(updatedChild));
		markedVersions.add(getPublishedVersion(installedChild));

		// Verify that none of the other child versions are marked.
		assertTrue(verifyRemainingChildVersionUnmarked(root, markedVersions));
	}

	/**
	 * This verifies that no other child versions are marked except for those
	 * listed as exempt.
	 * 
	 * @param rootItem
	 * @param exemptVersionItem
	 * @return
	 */
	protected boolean verifyRemainingChildVersionUnmarked(
			SWTBotTreeItem rootItem, SWTBotTreeItem exemptVersionItem) {

		Set<PluginVersion> set = new HashSet<PluginVersion>();
		if (exemptVersionItem != null) {
			set.add(getPublishedVersion(exemptVersionItem));
		}
		return verifyRemainingChildVersionUnmarked(rootItem, set);

	}

	/**
	 * Returns the version ID for any given tree item, whether root or child.
	 * 
	 * @param treeItem
	 * @return
	 */
	protected String getVersionID(SWTBotTreeItem treeItem) {
		return SWTBotPluginManagerUtil.getPublishedVersion(bot, treeItem)
				.getVersion();
	}

	/**
	 * The root element always reflects the current version selection, therefore
	 * the root element state must match the child version state, EXCEPT for
	 * root elements marked as update available. The latter case is also handled
	 * by this method, where the root version is checked to be in
	 * "update available" state and the corresponding child in "installed"
	 * state.
	 * 
	 * @param expectedState
	 * @param rootItem
	 * @return true if the match, false otherwise
	 */
	protected boolean validateRootAndChildVersionState(
			PluginState expectedState, SWTBotTreeItem rootItem) {
		SWTBotTreeItem childItem = SWTBotPluginManagerUtil
				.getCorrespondingChildVersion(bot, rootItem);
		PluginState rootState = getPluginState(rootItem);
		PluginState childState = getPluginState(childItem);
		boolean isValid = (rootState == expectedState);
		print("Validating  root state. Expected state: " + expectedState
				+ " Actual State: " + rootState);
		if (expectedState == null) {
			isValid &= childState == null;
		} else {
			switch (expectedState) {
			case SELECT_INSTALL:
			case SELECT_UNINSTALL:
			case INSTALLED:
				isValid &= (childState == expectedState);
				print("Validating child state. Expected state: "
						+ expectedState + " Actual State: " + childState);
				break;
			case UPDATE_AVAILABLE:
				isValid &= (childState == PluginState.INSTALLED);
				print("Validating child state. Expected state: "
						+ expectedState + " Actual State: " + childState);

				break;
			}
		}
		return isValid;
	}

	protected PluginState getPluginState(SWTBotTreeItem item) {
		return SWTBotPluginManagerUtil.getStateOfPlugin(bot, item);
	}

	protected void assertSelectionButtonEnablement(boolean installEnablement,
			boolean uninstallEnablement, boolean updateEnablement) {
		assertEquals(installEnablement, isInstallButtonEnabled());
		assertEquals(uninstallEnablement, isUninstallButtonEnabled());
		assertEquals(updateEnablement, isUpdateButtonEnabled());
	}
}
