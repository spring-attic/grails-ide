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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * A helper class, with some local state to help installing and uninstalling plugins and reverting operation
 * if errors occur during the (un)installation process.
 * @author Kris De Volder
 * @author Martin Lippert
 * @author Andrew Eisenberg
 */
public class FastGrailsPluginInstaller {
	
	//IMPORTANT:
	//  This implements a 'fast' version of installing / uninstalling plugins. But unfortunately this fast version
	//  isn't always reliable. 
	
	// See 
	//  - https://issuetracker.springsource.com/browse/STS-1506
	//  - https://issuetracker.springsource.com/browse/STS-1502
	
	//I'm keeping it around for now, for reference, but ultimately I don't think we want
	//  to use this solution unless we have really good tests for it. Otherwise we should 'play it safe' and
	//  rely on the (un)install-plugin commands provided by Grails to install plugins.
	
	private static final String APPLICATION_PROPERTIES = "application.properties";
	private IProject project;
	private Collection<PluginVersion> uninstallPlugins;
	private Collection<PluginVersion> installPlugins;
	
	private Properties savedProps = null;

	public static IStatus performPluginChanges(
			Collection<PluginVersion> selectedUninstallPlugins,
			Collection<PluginVersion> selectedInstallPlugins, IProject project,
			IProgressMonitor monitor) {
		return new FastGrailsPluginInstaller(selectedUninstallPlugins, selectedInstallPlugins, project).performChanges(monitor);
	}

	private FastGrailsPluginInstaller(
			Collection<PluginVersion> selectedUninstallPlugins,
			Collection<PluginVersion> selectedInstallPlugins, IProject project) {
		if (selectedInstallPlugins==null) {
			selectedInstallPlugins = new ArrayList<PluginVersion>();
		}
		if (selectedUninstallPlugins==null) {
			selectedUninstallPlugins = new ArrayList<PluginVersion>();
		}
		this.uninstallPlugins = selectedUninstallPlugins;
		this.installPlugins = selectedInstallPlugins;
		this.project = project;
	}

	private IStatus performChanges(IProgressMonitor monitor) {
	    if (monitor == null) {
	        monitor = new NullProgressMonitor();
	    }

		if (!GrailsClasspathUtils.hasClasspathContainer(JavaCore.create(project))) {
			return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Plugin Management requires that Grails Dependency Management is enabled for project '"+project.getName()+"'");
		}
		monitor.beginTask("Install and uninstall grails plugins", 3);
		try {
			monitor.subTask("Updating application.properties and BuildConfig.groovy");
			IStatus result = internalPerformPluginChanges(project, uninstallPlugins, installPlugins);
			if (result.getSeverity() > IStatus.WARNING) {
				return result;
			}
			monitor.worked(1);

			try {
				monitor.subTask("Installing / Uninstalling plugins");
				GrailsCommandUtils.refreshDependencies(JavaCore.create(project), true);
				monitor.worked(1);
				
				monitor.subTask("Refreshing project dependencies");
				GrailsCommandUtils.refreshDependencies(JavaCore.create(project), true);
			} catch (CoreException e) {
				result = new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Exception thrown when trying to update 'application.properties'", e);
				GrailsCoreActivator.log(result);
				boolean revert = askRevert();
				if (revert) {
					monitor.subTask("An error occurred ==> Reverting operation");
					revert();
				}
			}
			monitor.worked(1);
			return result;
		} finally {
			monitor.done();
		}
	}

	private void revert() {
		Assert.isTrue(savedProps!=null, "Can't revert 'application.properties': no saved state");
		OutputStream out = null;
		try {
			IFile file = project.getFile(APPLICATION_PROPERTIES);
			out = new BufferedOutputStream(new FileOutputStream(file.getLocation().toFile()));
			savedProps.store(out, createComment());
			GrailsCommandUtils.refreshDependencies(JavaCore.create(project), true);
		} catch (Exception e) {
			//Swallow, already in error recovery mode, don't spam any more error messages.
			e.printStackTrace();
		} finally {
			if (out!=null) {
				try {
					out.close();
				} catch (IOException e) {
					//Swallow, already in error recovery mode, don't spam any more error messages.
				}
			}
		}
	}

	/** 
	 * When an error occurs during refresh dependecies (which install / uninstalls plugins) we ask the user if they want to revert 
	 * the application properties file to its original state or leave the project in its probably broken state.
	 */
	private boolean askRevert() {
		return true; // Can't do this here, not in UI thread and also this plugin is a 'core' plugin.
//		return MessageDialog.openQuestion(null, "An error has occurred updating 'application.properties' for '"+ project.getName()+"'. See error log for details." , 
//				"Do you want to revert 'application.properties' to its original state? (Recomended)");
	}

	private static String createComment() {
		return "Grails Metadata file";
	}

	/**
	 * Makes modifications to application.properties to reflect installed / uninstalled plugins. Also stores a copy of the original properties
	 * in 'savedProps' to be able to revert any changes in case of an error.
	 */
	private IStatus internalPerformPluginChanges(IProject project, Collection<PluginVersion> selectedUninstallPlugins, Collection<PluginVersion> selectedInstallPlugins) {
		InputStream in = null;
		OutputStream out = null;
		List<IStatus> statuses = new ArrayList<IStatus>(2);
		try {
			Properties props = new Properties();
			IFile file = project.getFile(APPLICATION_PROPERTIES);
			props.load(in=file.getContents());
			savedProps = (Properties) props.clone();

			for (PluginVersion toUninstall : selectedUninstallPlugins) {
			    if (toUninstall.getParent().isInPlace()) {
			        // change BuildConfig.groovy script
			        IProject uninstallProject = ResourcesPlugin.getWorkspace().getRoot().getProject(toUninstall.getName());
			        if (uninstallProject.isAccessible()) {
			            IStatus result = GrailsPluginUtil.removePluginDependency(project, uninstallProject);
			            if (!result.isOK()) {
			                statuses.add(result);
			            }
			        } else {
			            // add warning status
			            String reason;
			            if (uninstallProject.exists()) {
			                reason = " is closed. ";
			            } else {
			                reason = " does not exist. ";
			            }
			            statuses.add(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, 
			                    "Project " + uninstallProject.getName() + reason + "Could not uninstall dependency.  Try manually editing the BuildConfig.groovy file."));
			        }
			    } else {
			        // change application.properties
			        props.remove("plugins." + toUninstall.getName());
			    }
			}

			for (PluginVersion toInstall : selectedInstallPlugins) {
                if (toInstall.getParent().isInPlace()) {
                    // change BuildConfig.groovy script
                    IProject installProject = ResourcesPlugin.getWorkspace().getRoot().getProject(toInstall.getName());
                    if (installProject.isAccessible()) {
                        IStatus result = GrailsPluginUtil.addPluginDependency(project, installProject);
                        if (!result.isOK()) {
                            statuses.add(result);
                        }
                    } else {
                        // add warning status
                        String reason;
                        if (installProject.exists()) {
                            reason = " is closed. ";
                        } else {
                            reason = " does not exist. ";
                        }
                        statuses.add(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, 
                                "Project " + installProject.getName() + reason + "Could not install dependency.  Try manually editing the BuildConfig.groovy file."));
                    }
                } else {
                    // change application.properties
                    props.put("plugins." + toInstall.getName(), toInstall.getVersion());
                }
			}
			out = new FileOutputStream(project.getFile(APPLICATION_PROPERTIES).getLocation().toFile());
			props.store(out, createComment());
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (Exception e) {
			Status status = new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Exception thrown when trying to update 'application.properties'", e);
			return status;
		}
		finally {
			try {
				if (in!=null) {
					in.close();
				}
				if (out!=null) {
					out.close();
				}
			} catch (IOException e) {
			}
		}
		if (statuses.size() == 0) {
		    return Status.OK_STATUS;
		} else {
		    return new MultiStatus(GrailsCoreActivator.PLUGIN_ID, -1, statuses.toArray(new IStatus[0]), "Warnings occurred during plugin changes operation", null);
		}
	}

}
