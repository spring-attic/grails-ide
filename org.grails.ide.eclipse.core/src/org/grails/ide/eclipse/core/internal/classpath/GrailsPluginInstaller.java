/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal.classpath;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.workspace.GrailsWorkspace;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * A helper class, to help installing and uninstalling plugins.
 * <p>
 * Note: old version of this lives in {@link FastGrailsPluginInstaller} but it isn't used. This version uses
 * grails commands only to install and uninstall plugins. (So we are not directly responsible for reverting application.properties
 * 
 * @author Kris De Volder
 * @author Martin Lippert
 * @author Andrew Eisenberg
 */
public class GrailsPluginInstaller {

	private static class Canceled extends Exception {
		private static final long serialVersionUID = 1L;
	}

	private IProject project;
	private Collection<PluginVersion> uninstallPlugins;
	private Collection<PluginVersion> installPlugins;
	private IProgressMonitor monitor;
	private IStatus status;
	
	public static IStatus performPluginChanges(
			Collection<PluginVersion> selectedUninstallPlugins,
			Collection<PluginVersion> selectedInstallPlugins, IProject project,
			IProgressMonitor monitor) {
		return new GrailsPluginInstaller(selectedUninstallPlugins, selectedInstallPlugins, project, monitor).performChanges();
	}

	private GrailsPluginInstaller(
			Collection<PluginVersion> selectedUninstallPlugins,
			Collection<PluginVersion> selectedInstallPlugins, 
			IProject project,
			IProgressMonitor monitor) {
		if (selectedInstallPlugins==null) {
			selectedInstallPlugins = new ArrayList<PluginVersion>();
		}
		if (selectedUninstallPlugins==null) {
			selectedUninstallPlugins = new ArrayList<PluginVersion>();
		}
		this.uninstallPlugins = selectedUninstallPlugins;
		this.installPlugins = selectedInstallPlugins;
		this.project = project;
		this.monitor = monitor;
		this.status = Status.OK_STATUS;
	}

	private IStatus performChanges() {
	    if (monitor == null) {
	        monitor = new NullProgressMonitor();
	    }

		if (!GrailsClasspathUtils.hasClasspathContainer(JavaCore.create(project))) {
			return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Plugin Management requires that Grails Dependency Management is enabled for project '"+project.getName()+"'");
		}
		
		int totalWork = uninstallPlugins.size() + installPlugins.size() + 1/*refresh deps*/;
		monitor.beginTask("Install and uninstall grails plugins", totalWork);
		
		try {
			uninstallPlugins();
			installPlugins();
		} catch (Canceled e) {
			return Status.CANCEL_STATUS;
		} finally {
			try {
				refreshDependencies();
			} catch (CoreException e) {
				addErrorStatus(e.getStatus());
			} finally {
				monitor.done();
			}
		}
		return status;
	}

	private void refreshDependencies() throws CoreException {
		GrailsCommandUtils.transitiveRefreshDependencies(GrailsWorkspace.get().create(project), false);
	}

	private void installPlugins() throws Canceled {
		for (PluginVersion plugin : installPlugins) {
			cancelCheck();
			try {
				monitor.setTaskName("Installing plugin "+plugin.getName()+" "+plugin.getVersion());
				installPlugin(plugin);
			}
			finally {
				monitor.worked(1);
			}
		}
	}

	private void installPlugin(PluginVersion plugin) {
		if (plugin.getParent().isInPlace()) {
			installInPlacePlugin(plugin);
		} else {
			try {
				GrailsCommandFactory.installPlugin(project, plugin).synchExec();
			} catch (CoreException e) {
				addErrorStatus(e.getStatus());
			}
		}
	}

	private void cancelCheck() throws Canceled {
		if (monitor.isCanceled()) {
			throw new Canceled();
		}
	}

	private void uninstallPlugins() throws Canceled {
		for (PluginVersion plugin : uninstallPlugins) {
			cancelCheck();
			monitor.setTaskName("Uninstalling plugin "+plugin.getName());
			try {
				uninstallPlugin(plugin);
			} finally {
				monitor.worked(1);
			}
		}
	}

	private void uninstallPlugin(PluginVersion plugin) {
		if (plugin.getParent().isInPlace()) {
			uninstallInPlacePlugin(plugin);
		} else {
			try {
				GrailsCommandFactory.uninstallPlugin(project, plugin).synchExec();
			} catch (CoreException e) {
				addErrorStatus(e.getStatus());
			}
		}
	}

	/**
	 * Makes modification to BuildConfig.groovy to uninstall an inplace plugin. Note that this relies
	 * on the refresh dependencies to run at the end of the install process to pick up on the
	 * changes in classpath container, project explorer plugin nodes etc.
	 * 
	 * @param toUninstall
	 */
	private void uninstallInPlacePlugin(PluginVersion toUninstall) {
		// change BuildConfig.groovy script
        IProject uninstallProject = ResourcesPlugin.getWorkspace().getRoot().getProject(toUninstall.getName());
        if (uninstallProject.isAccessible()) {
            IStatus result;
			try {
				result = GrailsPluginUtil.removePluginDependency(project, uninstallProject);
	            if (!result.isOK()) {
	                addErrorStatus(result);
	            }
			} catch (CoreException e) {
				addErrorStatus(e.getStatus());
			}
        } else {
            // add warning status
            String reason;
            if (uninstallProject.exists()) {
                reason = " is closed. ";
            } else {
                reason = " does not exist. ";
            }
            addErrorStatus(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, 
                    "Project " + uninstallProject.getName() + reason + "Could not uninstall dependency.  Try manually editing the BuildConfig.groovy file."));
        }
	}

	private void addErrorStatus(IStatus error) {
		if (status.isOK()) {
			status = new MultiStatus(GrailsCoreActivator.PLUGIN_ID, IStatus.ERROR, "Problems occurred (un)installing plugins. See details for more information",null);
		}
		MultiStatus multi = (MultiStatus) status;
		multi.merge(error);
	}

	/**
	 * Makes modification to BuildConfig.groovy to install an inplace plugin. Note that this relies
	 * on the refresh dependencies to run at the end of the install process to pick up on the
	 * changes in classpath container, project explorer plugin nodes etc.
	 * 
	 * @param toUninstall
	 */
	private void installInPlacePlugin(PluginVersion toInstall) {
		IProject installProject = ResourcesPlugin.getWorkspace().getRoot().getProject(toInstall.getName());
		if (installProject.isAccessible()) {
			try {
				IStatus result = GrailsPluginUtil.addPluginDependency(project, installProject);
				if (!result.isOK()) {
					addErrorStatus(result);
				}
			} catch (CoreException e) {
				addErrorStatus(e.getStatus());
			}
		} else {
			// add project inaccessible related error message
			String reason;
			if (installProject.exists()) {
				reason = " is closed. ";
			} else {
				reason = " does not exist. ";
			}
			addErrorStatus(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, 
					"Project " + installProject.getName() + reason + "Could not install dependency.  Try manually editing the BuildConfig.groovy file."));
		}
	}
	
}
