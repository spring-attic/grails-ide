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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.ui.GrailsUiActivator;
import org.grails.ide.eclipse.ui.internal.utils.OpenNewResourcesCommandListener;
import org.grails.ide.eclipse.ui.internal.utils.RefreshDependenciesCommandListener;

/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @since 2.2.1
 */
public abstract class GrailsLaunchUtils {

	/**
	 * Launch a grails command.
	 * 
	 * @param javaProject Context for launching
	 * @param script      Text of grails command to launch
	 * @param persist     Controls whether created launch configuration is persisted in Eclipse launch history.
	 */
	public static void launch(IJavaProject javaProject, String script, boolean persist) {

		IGrailsInstall grailsHome = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(
				javaProject.getProject());
		if (grailsHome == null) {
			MessageDialog
					.openInformation(
							Display.getDefault().getActiveShell(),
							"Grails Installation",
							"The Grails installation directory has not been configured or is invalid.\\n\\nCheck the Grails project or workspace preference page.");
		}

		// Register the command listener
		GrailsCoreActivator.getDefault().addGrailsCommandResourceListener(
				new OpenNewResourcesCommandListener(javaProject.getProject()));

		if (script != null && script.contains("install-plugin") || script.contains("s2-create-acl-domains")) {
			GrailsCoreActivator.getDefault().addGrailsCommandResourceListener(
					new RefreshDependenciesCommandListener(javaProject.getProject()));
		}
		if (script!=null)

		try {
			ILaunchConfiguration launchConf = GrailsLaunchConfigurationDelegate.getLaunchConfiguration(javaProject.getProject(),
					script, persist);
			DebugUITools.launch(launchConf, ILaunchManager.RUN_MODE);
		}
		catch (CoreException e) {
			GrailsCoreActivator.log(e);
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Error running Grails command",
					"An error occured running Grails command", new Status(IStatus.ERROR, GrailsUiActivator.PLUGIN_ID,
							0, e.getMessage(), e));
		}
	}

	/**
	 * Launch a grails command. The launch configuration created for the launch is not persisted.
	 * 
	 * @param javaProject Context for launching
	 * @param script      Text of grails command to launch
	 * @throws CoreException 
	 */
	public static void launch(IJavaProject javaProject, final String script) {
		if (script!=null && (script.contains("run-app") || script.contains("run-war"))) {
			launchRunApp(javaProject, script);
			return;
		}
		//launch(javaProject, script, false);
		final IProject project = javaProject.getProject();
		final GrailsCommand cmd = GrailsCommandFactory.fromString(project, script);
		final String title = "grails "+script;
		
		// Register the command listeners
		final GrailsCoreActivator grailsCore = GrailsCoreActivator.getDefault();
		grailsCore.addGrailsCommandResourceListener(
				new OpenNewResourcesCommandListener(javaProject.getProject()));

		if (script != null && script.contains("install-plugin") || script.contains("s2-create-acl-domains")) {
			grailsCore.addGrailsCommandResourceListener(
					new RefreshDependenciesCommandListener(javaProject.getProject()));
		}
		
		new Job(title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(title, 3);
				try {
					try {
						monitor.worked(1);
						grailsCore.notifyCommandStart(project);
						try {
							cmd.synchExec();
							monitor.worked(1);
						} finally {
							project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
							grailsCore.notifyCommandFinish(project);
						}
					} catch (CoreException e) {
						return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Problem executing: "+script, e);
					}
					return Status.OK_STATUS;
				} finally {
					monitor.done();
				}
			}
		}.schedule();
	}

	/**
	 * Special handling for run-app and run-war, see https://issuetracker.springsource.com/browse/STS-3155
	 * https://issuetracker.springsource.com/browse/STS-3299
	 * 
	 * @throws CoreException 
	 */
	private static void launchRunApp(final IJavaProject javaProject, final String script) {
		final String title = "Launching "+javaProject.getElementName() + ": " + script;
		new Job(title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(title, 3);
				try {
					try {
						ILaunchConfiguration launchConf = GrailsLaunchConfigurationDelegate.getLaunchConfiguration(javaProject.getProject(), script, false);
						monitor.worked(1);
						DebugUITools.launch(launchConf, ILaunchManager.RUN_MODE);
					} catch (CoreException e) {
						return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Problem executing: "+script, e);
					}
					return Status.OK_STATUS;
				} finally {
					monitor.done();
				}
			}
		}.schedule();
	}

}
