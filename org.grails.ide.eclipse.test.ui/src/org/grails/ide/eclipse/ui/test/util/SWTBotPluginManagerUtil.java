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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;
import org.springsource.ide.eclipse.commons.frameworks.test.util.TestKeyboard;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginOperation;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginState;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginManagerDialog.TreeElement;

import org.grails.ide.eclipse.ui.internal.dialogues.GrailsPluginManagerDialogue;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @created Jul 21, 2010
 */
public class SWTBotPluginManagerUtil {

	// May be too long, but for consistency, allow tests to run longer rather
	// than fail more frequently
	public static final long PLUGIN_LIST_POPULATE = 40000;
	public static final long OPEN_MANAGER_TIMEOUT = 5000;

	private SWTBotPluginManagerUtil() {
		// util
	}

	protected static String getGrailsPluginManagerContextMenuName() {
		return "Grails Plugin Manager...";
	}

	protected static String getGrailsPluginManagerShellTitle() {
		return "Grails Plugin Manager";
	}

	/**
	 * Given a tree item, this will retrieve the associated published version.
	 * 
	 * @param bot
	 *            from which the tree can be accessed
	 * @param item
	 *            tree item in the tree that should contain a published version
	 * @return published version associated with given tree item
	 */
	public static PluginVersion getPublishedVersion(
			final SWTWorkbenchBot bot, final SWTBotTreeItem item) {

		final List<PluginVersion> found = new ArrayList<PluginVersion>();
		bot.getDisplay().syncExec(new Runnable() {

			public void run() {
				TreeItem actualItem = item.widget;
				PluginVersion version = ((TreeElement) actualItem.getData())
						.getVersionModel();
				found.add(version);

			}
		});

		return found.get(0);
	}

	/**
	 * Given a tree item, retrieves the associated image. It may be null
	 * 
	 * @param bot
	 *            from which tree can be accessed
	 * @param item
	 *            whose image should be retrieved.
	 * @return found image, or null if no image for this item
	 */
	public static Image getIconForPluginElement(final SWTWorkbenchBot bot,
			final SWTBotTreeItem item) {
		final List<Image> found = new ArrayList<Image>();
		bot.getDisplay().syncExec(new Runnable() {

			public void run() {
				TreeItem actualItem = item.widget;
				found.add(actualItem.getImage());
			}
		});

		return found.get(0);
	}

	/**
	 * Returns the plugin state of the given tree item. Can be null if it has no
	 * state.
	 * 
	 * @param bot
	 *            from which tree can be accessed
	 * @param item
	 *            in the tree associated with a plugin version
	 * @return plugin state for the plugin version.
	 */
	public static PluginState getStateOfPlugin(final SWTWorkbenchBot bot,
			final SWTBotTreeItem item) {
		final List<PluginState> found = new ArrayList<PluginState>();
		bot.getDisplay().syncExec(new Runnable() {

			public void run() {
				TreeItem actualItem = item.widget;
				found.add(((TreeElement) actualItem.getData()).getPluginState());
			}
		});

		return found.get(0);
	}

	/**
	 * Returns the tree item corresponding to the child version matching the
	 * root item version.
	 * 
	 * @param bot
	 *            from which access to tree is possible
	 * @param rootItem
	 * @return child version that matches the version of the root element
	 */
	public static SWTBotTreeItem getCorrespondingChildVersion(
			SWTWorkbenchBot bot, SWTBotTreeItem rootItem) {
		PluginVersion rootVersion = getPublishedVersion(bot, rootItem);
		return getPluginTreeItem(bot, rootVersion.getName(),
				rootVersion.getVersion());
	}

	/**
	 * Gets the tree item associated with the given plugin. The version ID
	 * (number) is optional. If the version ID is null, this method will only
	 * return the ROOT element matching the plugin name.
	 * <p/>
	 * If a version ID is specified, this method will return the exact CHILD
	 * element matching the plugin name AND version ID.
	 * 
	 * @param bot
	 *            from which access to tree is possible
	 * @param pluginName
	 *            to find in tree
	 * @param versionID
	 *            optional. If specified, the CHILD version element is return.
	 *            If null, only the ROOT element is returned
	 * @return Child version element matching plugin name and version ID, or
	 *         ROOT element if no version ID is specified.
	 */
	public static SWTBotTreeItem getPluginTreeItem(SWTWorkbenchBot bot,
			final String pluginName, final String versionID) {

		SWTBotTreeItem[] items = bot.tree().getAllItems();
		SWTBotTreeItem foundItem = null;
		for (SWTBotTreeItem item : items) {
			PluginVersion rootVersion = getPublishedVersion(bot, item);
			if (rootVersion.getName().equals(pluginName)) {
				if (versionID != null) {
					item.expand();
					SWTBotTreeItem[] childElements = item.getItems();
					for (SWTBotTreeItem childElement : childElements) {
						PluginVersion childVersion = SWTBotPluginManagerUtil
								.getPublishedVersion(bot, childElement);
						if (childVersion.getVersion().equals(versionID)) {
							SWTBotUtils
									.print("Found expected child version plugin for: "
											+ childVersion.getName()
											+ " Actual version:"
											+ childVersion.getVersion()
											+ ". Expected version: "
											+ versionID);
							foundItem = childElement;
							break;
						}
					}
				} else {
					SWTBotUtils.print("Found expected root plugin. Actual: "
							+ rootVersion.getName() + ". Expected: "
							+ pluginName);
					foundItem = item;
				}
				break;
			}
		}
		return foundItem;
	}

	/**
	 * Gets Grails manager dialogue controller by using a keyboard shortcut.
	 * Note that if a dialogue is already opened, whether it was opened directly
	 * programmatically or through an actual UI path like a keyboard shortcut or
	 * context menu action, the opened dialogue will be returned instead until
	 * it is closed.
	 * 
	 * @param bot
	 *            that has access to the manager UI in the runtime
	 * @param viewName
	 *            containing the project selection
	 * @param projectName
	 *            name of a selected project for the manager
	 * @return opened manager.
	 */
	public static GrailsDialogueController getGrailsManagerDialogueViaShortcutKey(
			final SWTWorkbenchBot bot, String viewName, String projectName)
			throws Exception {
		return new GrailsDialogueShortcutKeyController(projectName, bot,
				viewName);
	}

	public static void pressInstallButton(SWTWorkbenchBot bot) {
		pressButton(getInstallButton(bot));
	}

	public static void pressOKButton(SWTWorkbenchBot bot) {
		pressButton(getOKButton(bot));
	}

	public static void pressUninstallButton(SWTWorkbenchBot bot) {
		pressButton(getUninstallButton(bot));
	}

	public static void pressUpdateButton(SWTWorkbenchBot bot) {
		pressButton(getUpdateButton(bot));
	}

	public static void pressButton(SWTBotButton button) {
		button.setFocus();
		if (button.isEnabled()) {
			button.click();
		}
	}

	public static SWTBotButton getOKButton(SWTWorkbenchBot bot) {
		return bot.button("OK");
	}

	public static SWTBotButton getInstallButton(SWTWorkbenchBot bot) {
		return bot.button(PluginOperation.INSTALL.getName());
	}

	public static SWTBotButton getUninstallButton(SWTWorkbenchBot bot) {
		return bot.button(PluginOperation.UNINSTALL.getName());
	}

	public static SWTBotButton getUpdateButton(SWTWorkbenchBot bot) {
		return bot.button(PluginOperation.UPDATE.getName());
	}

	public static List<String> switchToInstallOnlyView(SWTWorkbenchBot bot) {
		return null;
	}

	public static List<String> switchToUpdateOnlyView(SWTWorkbenchBot bot) {
		return null;
	}

	public static List<String> switchToPendingChangesOnlyView(
			SWTWorkbenchBot bot) {
		return null;
	}

	public static SWTBotTree waitUntilManagerTreePopulates(
			final SWTWorkbenchBot bot) {
		final SWTBotTree tree = bot.tree();
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {

				SWTBotTreeItem[] items = tree.getAllItems();

				if (items != null && items.length > 0) {
					return true;
				}
				return false;
			}

			public void init(SWTBot bot) {
				// nothing
			}

			public String getFailureMessage() {
				return "Manager tree did not populate";
			}

		}, PLUGIN_LIST_POPULATE);
		return tree;
	}

	static class GrailsDialogueShortcutKeyController extends
			GrailsDialogueController {

		public GrailsDialogueShortcutKeyController(String projectName,
				SWTWorkbenchBot bot, String viewName) {
			super(projectName, bot, viewName);
		}

		protected void openGrailsManager() throws Exception {
			SWTBotUtils.print("Opening Grails Plugin manager for: "
					+ getProject().getName());
			new TestKeyboard(bot).ALT_G_M();
			// wait for the manager to open.
			bot.sleep(OPEN_MANAGER_TIMEOUT);

			//May get a progress dialog while waiting for plugin list to resolve
			SWTBotShell warningShell = bot.activeShell();
			if (warningShell!=null && warningShell.isOpen() && warningShell.getText().equals("Progress Information")) {
				bot.waitUntil(Conditions.shellCloses(warningShell), PLUGIN_LIST_POPULATE);
			}
			
			// May get a warning dialogue
			warningShell = bot.activeShell();
			if (warningShell != null
					&& warningShell.isOpen()
					&& warningShell
							.getText()
							.equals(GrailsPluginManagerDialogue.INITIAL_REFRESH_DEPENDENCIES_DIALOGUE_TITLE)) {
				bot.button("Yes").click();
			}
			waitUntilManagerTreePopulates(bot);
		}
	}

	public abstract static class GrailsDialogueController {
		private static SWTBotShell activeManager;

		protected SWTWorkbenchBot bot;
		private String projectName;
		private String viewName;

		// Access through accessor method only
		private IProject project;

		public GrailsDialogueController(String projectName,
				SWTWorkbenchBot bot, String viewName) {
			this.projectName = projectName;
			this.bot = bot;
			this.viewName = viewName;
		}

		public SWTBotShell getOpenedGrailsManager() throws Exception {
			if (activeManager != null && activeManager.isOpen()) {
				return activeManager;
			}

			if (activeManager != null) {
				activeManager.close();
			}

			// Activate the main shell first.
			UIThreadRunnable.syncExec(new Result<Shell>() {
				public Shell run() {
					Shell mainShell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					mainShell.setActive();
					mainShell.setFocus();
					return mainShell;
				}
			});

			SWTBotTree explorerTree = SWTBotUtils.selectProject(bot,
					getProject().getName(), viewName);
			bot.waitUntil(SWTBotUtils.widgetMakeActive(explorerTree));
			assertTrue(explorerTree.isActive());
			openGrailsManager();

			try {
				activeManager = bot.shell(getGrailsPluginManagerShellTitle());
			} catch (WidgetNotFoundException e) {
				//Create an error message with some info about what's going on in the console (command output should be there)
				throw new Error("Couldn't find the PluginManager.\n" +
						"---- Console text at this time ----\n" +
						SWTBotUtils.getConsoleText(bot));
			}
			bot.waitUntil(SWTBotUtils.widgetMakeActive(activeManager));

			assertTrue(activeManager.isOpen());
			return activeManager;
		}

		public boolean isOpen() {
			return activeManager != null && activeManager.isOpen();
		}

		protected IProject getProject() {
			if (project == null) {
				project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
				SWTBotUtils
						.print("Obtained project selection for Grails Plugin Manager test: "
								+ project.getName());
			}
			return project;
		}

		abstract protected void openGrailsManager() throws Exception;
	}
}
