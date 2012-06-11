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
package org.grails.ide.eclipse.ui.internal.inplace;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.springsource.ide.eclipse.commons.core.CommandHistoryProvider;
import org.springsource.ide.eclipse.commons.core.Entry;
import org.springsource.ide.eclipse.commons.core.ICommandHistory;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * Action to access GrailsCommandHistory from the Toolbar. Provides a "one-click" action to fire up
 * the last command. as well as a pull-down menu with the most recent items. The history is filtered 
 * to only show entries involving currently existing, open grails projects.
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.5.0
 */
public class GrailsRunToolbarAction extends OpenGrailsInplaceActionDelegate implements IWorkbenchWindowPulldownDelegate2 {

	/**
	 * The maximum number of items that will be shown in the history menu.
	 * (Note that this number is independent from the number of entries
	 * actually stored in the history. This only limits the number of items
	 * shown in the UI.)
	 */
	public int MAX_HISTORY_ITEMS = 10;
	
	public class OpenInplaceDialogOnHistoryEntryAction extends Action {

		private Entry entry;

		public OpenInplaceDialogOnHistoryEntryAction(Entry entry) {
			this.entry = entry;
			this.setText(entry.getMenuLabel());
			this.setToolTipText("execute command '"+entry.getCommand()+"' in project '"+entry.getProject());
		}

		@Override
		public void run() {
			Shell parent = JavaPlugin.getActiveWorkbenchShell();
			GrailsInplaceDialog dialog = GrailsInplaceDialog.getInstance(parent);
			dialog.setupFromHistory(entry);
			dialog.open();
		}
		
		@Override
		public String toString() {
			return "OpenInplaceDialogOnHistoryEntry("+entry.getCommand()+", "+entry.getProject()+")";
		}
		
	}

	private ICommandHistory history = CommandHistoryProvider.getCommandHistory(GrailsUiActivator.PLUGIN_ID, GrailsNature.NATURE_ID);
	
	public Menu getMenu(Menu parent) {
		Menu menu = new Menu(parent);
		fillMenu(menu);
		return menu;
	}

	protected void fillMenu(Menu menu) {	
		int limit = MAX_HISTORY_ITEMS;
		for (Entry entry : history.validEntries()) {
			if (limit-- <= 0) return;
			OpenInplaceDialogOnHistoryEntryAction action= new OpenInplaceDialogOnHistoryEntryAction(entry);
			ActionContributionItem item = new ActionContributionItem(action);
			item.fill(menu, -1);
		}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		if (!history.getRecentValid(1).isEmpty()) {
			//At least one valid entry! We don't care about the selection.
			action.setEnabled(true);
		}
	}

	public Menu getMenu(Control parent) {
		Menu menu = new Menu(parent);
		fillMenu(menu);
		return menu; // This menu will be empty for now
	}

	@Override
	public void run(IAction action) {
		if (!history.getRecentValid(1).isEmpty()) {
			Shell parent = JavaPlugin.getActiveWorkbenchShell();
			GrailsInplaceDialog dialog = GrailsInplaceDialog.getInstance(parent);
			dialog.setupFromHistory(history.getLast());
			dialog.open();
		}
		else {
			super.run(action);
		}
	}

}
