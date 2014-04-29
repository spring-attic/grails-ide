/*******************************************************************************
 * Copyright (c) 2012, 2014 VMWare, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *     Pivotal Software Inc.
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.inplace;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.progress.UIJob;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
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
	 * Launch a grails command. The launch configuration created for the launch is not persisted.
	 * 
	 * @param javaProject Context for launching
	 * @param script      Text of grails command to launch
	 * @throws CoreException 
	 */
	public static void launch(IJavaProject javaProject, final String script) {
		if (script!=null && (script.contains("run-app") || script.contains("test-app") || script.contains("run-war") || script.contains("console"))) {
			launchNoTimeout(javaProject, script);
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
	private static void launchNoTimeout(final IJavaProject javaProject, final String script) {
		final String title = "Launching "+javaProject.getElementName() + ": " + script;
		new UIJob(title) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
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
