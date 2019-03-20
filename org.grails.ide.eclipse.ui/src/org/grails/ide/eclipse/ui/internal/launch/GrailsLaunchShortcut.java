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
package org.grails.ide.eclipse.ui.internal.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.commons.core.SpringCoreUtils;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsLaunchShortcut implements ILaunchShortcut, IExecutableExtension {

	private String script = null;

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
		this.script = (String) data;
	}

	public void launch(ISelection selection, String mode) {
		launch(searchForResource(selection), mode);
	}

	private IResource searchForResource(ISelection selection) {
		IResource resource = null;
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj instanceof IResource) {
				resource = (IResource) obj;
			}
			else if (obj instanceof IAdaptable) {
				resource = (IResource) ((IAdaptable) obj).getAdapter(IResource.class);
			}
		}
		if (SpringCoreUtils.hasNature(resource, GrailsNature.NATURE_ID)) {
			return resource;
		}
		return null;
	}

	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			launch(file, mode);
		}
	}

	@SuppressWarnings("deprecation")
	public void launch(IResource project, String mode) {
		try {
			ILaunchConfiguration launchConfiguration = findLaunchConfiguration(project);
			if (launchConfiguration != null) {
				if (script == null) {
					DebugUITools.saveBeforeLaunch();
					DebugUITools.openLaunchConfigurationDialog(getShell(), launchConfiguration,
							"org.eclipse.ui.externaltools.launchGroup", null);
				}
				else {
					DebugUITools.launch(launchConfiguration, mode);
				}
			}
		}
		catch (CoreException e) {
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Error running Grails command",
					"An error occured running Grails command", new Status(IStatus.ERROR, GrailsUiActivator.PLUGIN_ID,
							0, e.getMessage(), e));
		}
	}

	public ILaunchConfiguration findLaunchConfiguration(IResource resource) throws CoreException {
		if (resource != null) {
			boolean persist = true;
			IProject project = resource.getProject();
			if (project!=null) {
				String script = getScriptFor(resource);
				if (script!=null) {
					ILaunchConfiguration[] candidates = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(GrailsLaunchConfigurationDelegate.getLaunchConfigurationType());
					for (ILaunchConfiguration candidate : candidates) {
						IProject candProj = GrailsLaunchArgumentUtils.getProject(candidate);
						if (candProj!=null) {
							String candScript = GrailsLaunchConfigurationDelegate.getOrgScript(candidate);
							if (candScript!=null && candProj.equals(project) && candScript.equals(script)) {
								return candidate;
							}
						}
					}
				}
			}
			
			return GrailsLaunchConfigurationDelegate.getLaunchConfiguration(resource.getProject(), getScriptFor(resource), persist);
		}
		return null;
	}

	/**
	 * Get the text of the script (i.e. grails command) to execute. Default implementation returns the text
	 * set on initialisation of the shortcut. Subclasses may override this to customize the script, e.g. 
	 * by adding extra parameters based on the selected resource.
	 * 
	 * @param resource
	 * @return
	 */
	protected String getScriptFor(IResource resource) {
		return script;
	}

	protected Shell getShell() {
		return Display.getDefault().getActiveShell();
	}

//	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
//		try {
//			return new ILaunchConfiguration[] { findLaunchConfiguration(searchForResource(selection)) };
//		}
//		catch (CoreException e) {
//		}
//		return null;
//	}
//
//	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
//		IResource resource = getLaunchableResource(editor);
//		if (resource != null) {
//			try {
//				return new ILaunchConfiguration[] { findLaunchConfiguration(resource.getProject()) };
//			}
//			catch (CoreException e) {
//			}
//		}
//		return null;
//	}
//
//	public IResource getLaunchableResource(ISelection selection) {
//		return searchForResource(selection);
//	}
//
//	public IResource getLaunchableResource(IEditorPart editor) {
//		IEditorInput input = editor.getEditorInput();
//		if (input instanceof IFileEditorInput) {
//			IFile file = ((IFileEditorInput) input).getFile();
//			if (SpringCoreUtils.hasNature(file, GrailsNature.NATURE_ID)) {
//				return file;
//			}
//		}
//		return null;
//	}

}
