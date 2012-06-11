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
package org.grails.ide.eclipse.refactoring.rename.action;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;

/**
 * Rename participant triggered when renaming a method or field in a Grails Controller class.
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ControllerActionRenameParticipant extends GrailsActionOrViewRenameParticipant {

	private IMember targetAction;

	public ControllerActionRenameParticipant() {
	}

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IField || element instanceof IMethod) {
			targetAction = (IMember)element;
			IType type = targetAction.getDeclaringType();
			if (type!=null) {
				IGrailsElement maybeController = GrailsWorkspaceCore.get().create(type);
				if (maybeController!=null && maybeController.getKind()==GrailsElementKind.CONTROLLER_CLASS) {
					ControllerClass controller = (ControllerClass) maybeController;
					setProject(type.getJavaProject().getProject());
					setOldActionName(targetAction.getElementName());
					setNewActionName(getArguments().getNewName());
					setTargetControllerClassName(type.getElementName());
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		rewriteActionReferences(status);
		renameGSP(status);
		return status;
	}

	/**
	 * Rename the GSP file that corresponds to the view associated with the renamed action.
	 */
	private void renameGSP(RefactoringStatus status) {
		IFolder viewsFolder = getProject().getFolder(new Path("grails-app/views"));
		IFolder viewsFolderForController = viewsFolder.getFolder(getTargetControllerName());
		if (viewsFolderForController!=null && viewsFolderForController.exists()) {
			IFile gspFile = viewsFolderForController.getFile(getOldActionName()+".gsp");
			if (gspFile.exists()) {
				changes.add(new RenameResourceChange(gspFile.getFullPath(), getNewActionName()+".gsp"));
			}
		}
	}

	@Override
	public String getName() {
		return "Rename Grails Controller Action Participant";
	}
	
}
