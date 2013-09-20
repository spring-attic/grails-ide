/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.explorer.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginInstaller;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.actions.AbstractActionDelegate;

import org.grails.ide.eclipse.explorer.GrailsExplorerPlugin;
import org.grails.ide.eclipse.explorer.elements.GrailsDependencyPluginFolder;

/**
 * @author Kris De Volder
 */
public class UninstallPluginAction extends AbstractActionDelegate {

	private static final String DO_NOT_PROMPT_SWITCH = UninstallPluginAction.class.getName()+".dialogswitch";

	/**
	 * Single project from which plugins should be uninstalled.
	 */
	private IProject targetProject;
	
	/**
	 * Plugins that should be uninstalled from targetProject
	 */
	private List<PluginVersion> pluginsToUninstall = null;
	
	public void run(IAction action) {
		if (pluginsToUninstall==null || pluginsToUninstall.isEmpty()) 
			return;
		
		StringBuffer msg = new StringBuffer("Uninstall plugins: \n");
		for (PluginVersion p : pluginsToUninstall) {
			msg.append("   "+p.getName()+"\n");
		}
		MessageDialogWithToggle dialogue = MessageDialogWithToggle
		.openOkCancelConfirm(
				getShell(),
				"Confirm Uninstall Projects",
				msg.toString(),
				"Do not show this dialogue again.", 
				false,
				GrailsExplorerPlugin.getDefault().getPreferenceStore(),
				DO_NOT_PROMPT_SWITCH);
		if (dialogue.getReturnCode() != IDialogConstants.YES_ID) {
			Job job = new Job("Uninstall plugins") {
				@Override
				protected IStatus run(IProgressMonitor mon) {
					return GrailsPluginInstaller.performPluginChanges(pluginsToUninstall, null, targetProject, mon);
				}
			};
			job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
			job.setPriority(Job.BUILD);
			job.schedule();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			Object[] selectedItems = selection.toArray();
			IProject project = null;
			List<PluginVersion> selectedPlugins = new ArrayList<PluginVersion>();
			for (Object obj : selectedItems) {
				if (obj instanceof GrailsDependencyPluginFolder) {
					GrailsDependencyPluginFolder pluginItem = (GrailsDependencyPluginFolder) obj;
					selectedPlugins.add(pluginItem.getPluginModel());
					IProject addProj = pluginItem.getProject();
					if (project==null || project.equals(addProj)) {
						project = addProj;
					} else {
						//All selected plugins should be in the same project
						// (maybe in future we could support cross project selections, if users ask for this).
						action.setEnabled(false);
						return; 
					}
				}
			}
			
			if (selectedPlugins.size()>0) {
				targetProject = project;
				pluginsToUninstall = selectedPlugins;
				action.setEnabled(true);
				return;
			}
		}
		action.setEnabled(false);
	}

}
