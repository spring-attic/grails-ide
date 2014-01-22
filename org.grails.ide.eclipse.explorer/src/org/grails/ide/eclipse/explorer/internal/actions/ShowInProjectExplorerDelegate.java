/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.explorer.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.actions.AbstractActionDelegate;

import org.grails.ide.eclipse.explorer.elements.GrailsDependencyPluginFolder;

/**
 * Navigates to a local plugin in the workspace. May be replaced in the future
 * with a "Navigate -> Show in" contribution.
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public class ShowInProjectExplorerDelegate extends AbstractActionDelegate {

	private IProject targetPluginProject;

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel == null) {
			return;
		}
		if (sel instanceof IStructuredSelection) {
			//Don't think there should be cases other than structured selection, since this is an action
			// on in the explorer tree context menu.
			IStructuredSelection selection = (IStructuredSelection)sel;

			Object selObj = selection.getFirstElement();
			if (selObj instanceof GrailsDependencyPluginFolder) {
				GrailsDependencyPluginFolder pluginFolder = (GrailsDependencyPluginFolder) selObj;
				if (!pluginFolder.isInPlacePlugin()) {
					return;
				}
				// Fix for in-place plugins that are not packaged and have no
				// plugin.xml
				targetPluginProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(pluginFolder.getPluginName());
				if (!targetPluginProject.exists()) {
					targetPluginProject = null;
					GrailsCoreActivator.logWarning("Failed to navigate to: "
							+ pluginFolder.getPluginName(), null);
				}
			}
		}
	}

	public void run(IAction action) {

		if (targetPluginProject == null) {
			return;
		}

		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage workbenchPage = window.getActivePage();
			if (workbenchPage != null) {
				try {
					IViewPart viewPart = workbenchPage
							.showView("org.eclipse.ui.navigator.ProjectExplorer");
					if (viewPart instanceof ISetSelectionTarget) {
						ISetSelectionTarget selectionTarget = (ISetSelectionTarget) viewPart;
						ISelection selection = new StructuredSelection(
								targetPluginProject);
						selectionTarget.selectReveal(selection);
					}
				} catch (PartInitException pie) {
					GrailsCoreActivator.log(pie);
				}
			}
		}
		// clear it
		targetPluginProject = null;
	}
}
