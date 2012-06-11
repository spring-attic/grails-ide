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
package org.grails.ide.eclipse.ui.internal.actions;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginInstaller;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.actions.AbstractActionDelegate;

import org.grails.ide.eclipse.ui.internal.dialogues.GrailsPluginManagerDialogue;

/**
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 */
public class OpenGrailsPluginsManagerActionDelegate extends
		AbstractActionDelegate {

	public OpenGrailsPluginsManagerActionDelegate() {
		debug("new instance : "+this);
	}
	
	public void run(IAction action) {
		Shell shell = getShell();
		List<IProject> projects = getSelectedProjects();

		// Don't open dialogue if no projects are selected
		if (shell == null || projects == null || projects.isEmpty()) {
			debug("Triggered but not executing "+this);
			debug("shell = "+ shell);
			debug("projects = "+projects);
			return;
		}
		final GrailsPluginManagerDialogue dialogue = new GrailsPluginManagerDialogue(
				shell, projects);

		if (dialogue != null && dialogue.open() == Window.OK) {

			final Collection<PluginVersion> selectedUninstallPlugins = dialogue
					.getSelectedtoUninstall();
			final Collection<PluginVersion> selectedInstallPlugins = dialogue
					.getSelectedToInstall();

			// Nothing to execute if no plugins were selected
			if (selectedUninstallPlugins.isEmpty()
					&& selectedInstallPlugins.isEmpty()) {
				return;
			}

			WorkspaceJob job = new WorkspaceJob("Install and uninstall plugins") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor)
						throws CoreException {
					IProject project = dialogue.getSelectedProject();
					if (project != null) {
						return GrailsPluginInstaller.performPluginChanges(selectedUninstallPlugins,
								selectedInstallPlugins, project, monitor);
					} else {
					    return Status.OK_STATUS;
					}
				}
			};

			IWorkbench workbench = getWorkbench();
			if (workbench != null) {
				workbench.getProgressService().showInDialog(shell, job);
			}
			job.setPriority(Job.BUILD);
			job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory()
					.buildRule());
			job.schedule();
		}

	}

	protected boolean shouldAddToProjectList(IProject project) {
		return GrailsNature.isGrailsProject(project);
	}

}
