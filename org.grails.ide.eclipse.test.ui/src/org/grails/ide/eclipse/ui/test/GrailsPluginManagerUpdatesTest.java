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

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginState;


/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @created Jul 30, 2010
 */
public class GrailsPluginManagerUpdatesTest extends GrailsPluginManagerHarness {

	public void setupClass() throws Exception {
		super.setupClass();
		setupUpdatePlugins();
	}

	private static final String CODE_COVERAGE_OLD_VERSION = "1.1.5";
	private static final String TWITTER_OLD_VERSION = "0.1";
	private static final String CODE_COVERAGE = "code-coverage";
	private static final String TWITTER = "twitter";

	protected void setupUpdatePlugins() throws Exception {
		String[][] plugins = new String[][] {
				new String[] { CODE_COVERAGE, CODE_COVERAGE_OLD_VERSION },
				new String[] { TWITTER, TWITTER_OLD_VERSION, } };
		installPlugins(plugins);
		
		getPopulatedPluginManagerDialogue();

		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);
		assertCorrectnessOfInstalledPluginWithUpdate(TWITTER,
				TWITTER_OLD_VERSION);
	}

	public void testUpdateRoot() throws Exception {
		// Verify the root plugin is in the correct state before proceeding with
		// test. Also verify that the root version matches the old version
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		// Verify that the old version is marked as installed
		SWTBotTreeItem installedVersion = getTreeItem(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		String installedVersionID = getVersionID(installedVersion);

		SWTBotTreeItem root = selectRootTreeItem(CODE_COVERAGE);

		// Verify that only the Uninstall and update buttons are enabled.
		assertSelectionButtonEnablement(false, true, true);

		pressUpdateButton();

		// Verify that the root has been updated,
		// that the updated version is marked
		// and that installed verison is still marked as installed
		String updatedVersionID = getVersionID(root);

		assertTrue(updatedVersionID.compareTo(installedVersionID) > 0);
		assertPluginHasBeenUpdated(CODE_COVERAGE, installedVersionID,
				updatedVersionID);

		// THe only button enabled should be "Uninstall" to undo the update
		assertSelectionButtonEnablement(false, true, false);

		// Undo
		pressUninstallButton();

		// THis should undo and plugin should be marked with original
		// "update available" state
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		assertSelectionButtonEnablement(false, true, true);

	}

	public void testUninstallUpdateRoot() throws Exception {

		// Verify the root plugin is in the correct state before proceeding with
		// test. Also verify that the root version matches the old version
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		SWTBotTreeItem installedVersion = getTreeItem(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		String installedVersionID = getVersionID(installedVersion);

		// Select the root
		SWTBotTreeItem root = selectRootTreeItem(CODE_COVERAGE);

		// Verify that only the Uninstall and update buttons are enabled.
		assertSelectionButtonEnablement(false, true, true);

		// root should be marked as selected for uninstall along with the child
		// version
		assertNotNull(pressUninstallOnRoot(CODE_COVERAGE,
				PluginState.SELECT_UNINSTALL));

		// install should be enabled to undo the uninstall, and upate should
		// be enabled to still allow the update option
		assertSelectionButtonEnablement(true, false, true);

		// undo. The root should now be marked with the original
		// state (update available)
		pressInstallButton();

		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		// repeat the process to return to uninstall state
		assertNotNull(pressUninstallOnRoot(CODE_COVERAGE,
				PluginState.SELECT_UNINSTALL));

		pressUpdateButton();

		// verify that the plugin goes from an uninstall to an select-install
		// state, meaning that the user changed the state from "uninstalling" to
		// automatic update, which marks the root and a NEWER child version with
		// the "select-install" icon.
		String updateVersionID = getVersionID(root);

		// The updated version in the root should now be higher than the
		// installed version
		assertTrue(updateVersionID.compareTo(installedVersionID) > 0);
		assertPluginHasBeenUpdated(CODE_COVERAGE, installedVersionID,
				updateVersionID);

		// THe only button enabled should be "Uninstall" to undo the update
		assertSelectionButtonEnablement(false, true, false);

		pressUninstallButton();

		// THis should undo and plugin should be marked with original
		// "update available" state
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		assertSelectionButtonEnablement(false, true, true);

	}

	public void testUpdateVersion() throws Exception {

		// Verify the root plugin is in the correct state before proceeding with
		// test. Also verify that the root version matches the old version
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		SWTBotTreeItem installedVersion = getTreeItem(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		String installedVersionID = getVersionID(installedVersion);

		// Select the root
		selectRootTreeItem(CODE_COVERAGE);

		// Verify that only the Uninstall and update buttons are enabled.
		assertSelectionButtonEnablement(false, true, true);

		// Select an old unmarked version for uninstall. SHould do nothing
		// as it is not a valid selection
		String oldVersion = "0.9";
		String olderVersion = "0.8";

		// Verify the selected old version is indeed an old version
		// before proceeding with the test
		assertTrue(oldVersion.compareTo(installedVersionID) < 0);

		SWTBotTreeItem oldVersionElement = selectChildVersionElement(
				CODE_COVERAGE, oldVersion);

		// verify that only the update button is enabled
		assertSelectionButtonEnablement(false, false, true);

		pressUpdateButton();

		assertPluginHasBeenUpdated(CODE_COVERAGE, installedVersionID,
				oldVersion);

		// verify that only the uninstall button is enabled
		assertSelectionButtonEnablement(false, true, false);

		// Select an even older version
		SWTBotTreeItem olderVersionElement = selectChildVersionElement(
				CODE_COVERAGE, olderVersion);

		// verify that only the update button is enabled
		assertSelectionButtonEnablement(false, false, true);

		pressUpdateButton();

		assertPluginHasBeenUpdated(CODE_COVERAGE, installedVersionID,
				olderVersion);

		pressUninstallButton();

		// Should undo everything, and restore the plugin to "Update available"
		// state
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

		// Buttons should be restored too, with "update" the only one enabled
		assertSelectionButtonEnablement(false, false, true);

		// Select and upate another version again
		selectChildVersionElement(CODE_COVERAGE, oldVersion);

		pressUpdateButton();

		assertPluginHasBeenUpdated(CODE_COVERAGE, installedVersionID,
				oldVersion);

		// Select the installed version

		selectChildVersionElement(CODE_COVERAGE, installedVersionID);

		// Only the uninstall button is enabled
		assertSelectionButtonEnablement(false, true, false);

		// Press uninstall and verify that the installed version and root are
		// now the only nodes marked
		// the old version that was marked should no longer be marked
		pressUninstallButton();
		assertTrue(getPluginState(olderVersionElement) == null);
		assertTrue(getPluginState(oldVersionElement) == null);

		// now only the "install" button is enabled to undo the uninstall
		assertSelectionButtonEnablement(true, false, false);

		pressInstallButton();

		// Update-available state should be restored.
		assertCorrectnessOfInstalledPluginWithUpdate(CODE_COVERAGE,
				CODE_COVERAGE_OLD_VERSION);

	}

}
