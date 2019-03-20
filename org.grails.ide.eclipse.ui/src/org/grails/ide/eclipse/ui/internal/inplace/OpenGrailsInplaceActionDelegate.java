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
package org.grails.ide.eclipse.ui.internal.inplace;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.springsource.ide.eclipse.commons.ui.SpringUIUtils;


/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @since 2.2.0
 */
@SuppressWarnings("restriction")
public class OpenGrailsInplaceActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	protected IProject selected = null;

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		Shell parent = JavaPlugin.getActiveWorkbenchShell();
		GrailsInplaceDialog dialog = GrailsInplaceDialog.getInstance(parent);
		dialog.setSelectedProject(selected);
		dialog.open();
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		selected = null;
		
		if (selection instanceof IStructuredSelection && !(selection instanceof ITextSelection)) {
			Iterator<?> iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				if (obj instanceof IJavaProject) {
					obj = ((IJavaProject) obj).getProject();
				}
				else if (obj instanceof IAdaptable) {
					obj = ((IAdaptable) obj).getAdapter(IResource.class);
				}
				if (obj instanceof IResource) {
					IResource project = (IResource) obj;
					selected = project.getProject();
				}
			}
		}
		else {
			if (SpringUIUtils.getActiveEditor() != null) {
				if (SpringUIUtils.getActiveEditor().getEditorInput() instanceof IFileEditorInput) {
					selected = (((IFileEditorInput) SpringUIUtils.getActiveEditor().getEditorInput()).getFile()
							.getProject());
				}
			}
		}
		
		action.setEnabled(true);

		// Have selected something in the editor - therefore
		// want to close the inplace view if haven't already done so
		if (selection != null && !(selection instanceof TreeSelection)) {
			GrailsInplaceDialog.closeIfNotPinned();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
