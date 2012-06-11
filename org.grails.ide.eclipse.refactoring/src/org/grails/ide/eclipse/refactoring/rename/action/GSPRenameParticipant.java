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

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;

import org.grails.ide.eclipse.refactoring.util.RefactoringUtils;

/**
 * @author Kris De Volder
 * @since 2.8
 */
public class GSPRenameParticipant extends GrailsActionOrViewRenameParticipant {

	private static final String GSP_EXT = ".gsp";

	private IFile gspFile; // The gspFile that's being renamed

	public GSPRenameParticipant() {}

	@Override
	protected boolean initialize(Object element) {
		String newActionName = getArguments().getNewName();
		if (newActionName.endsWith(GSP_EXT)) {
			newActionName = newActionName.substring(0, newActionName.length()-GSP_EXT.length());
			setNewActionName(newActionName);
			if (element instanceof IFile) {
				gspFile = (IFile) element;
				String name = gspFile.getName();
				if (name.endsWith(GSP_EXT)) {
					IProject project = gspFile.getProject();
					if (project!=null) {
						if (GrailsNature.isGrailsProject(project)) {
							setProject(project);
							return parsePath();
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return "Rename Grails View Participant";
	}

	/**
	 * @return true if path follows expected pattern and the participant should be triggered.
	 */
	public boolean parsePath() {
		//Assuming the following pattern for the path:
		// "/<project>/grails-app/views/<controller>/<action>.gsp"
		IPath path = gspFile.getFullPath();
		if (path.segmentCount()==5 
				&& path.segment(1).equals("grails-app")
				&& path.segment(2).equals("views")) {
			setTargetControllerName(path.segment(3));
			String actionName = path.segment(4);
			if (actionName.endsWith(GSP_EXT)) {
				setOldActionName(actionName.substring(0, actionName.length()-GSP_EXT.length()));
				return true;
			}
		}
		return false;
	}


	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		final RefactoringStatus status = new RefactoringStatus();
		rewriteActionReferences(status);
		renameAction(context, status);
		return status;
	}

	public void renameAction(CheckConditionsContext context, final RefactoringStatus status) {
		try {
			Collection<? extends IMember> actions = getActions(getTargetController(), getOldActionName());
			IMember targetAction = null;
			for (IMember action : actions) {
				if (targetAction==null) {
					targetAction = action;
				} else {
					status.addError("More than one controller action with name '"+getOldActionName()
							+"' in '"+getTargetControllerClassName()+"'");
				}
			}
			if (targetAction!=null) {
				renameAction(targetAction, status, context);
			}
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
			status.addError(e.getMessage());
		}
	}

	private void renameAction(IMember target, RefactoringStatus status, CheckConditionsContext context) {
		try {
			RenameProcessor processor = null;
			if (target instanceof IMethod) {
				processor = renameMethodProcessor((IMethod) target, status, context);
			} else if (target instanceof IField) {
				processor = renameFieldProcessor((IField) target, status, context);
			} else {
				status.addError("Action is neither a field nor a method, this doesn't make sense: "+target.getElementName(), RefactoringUtils.statusContext(target));
			}
			if (processor!=null) {
				status.merge(processor.checkInitialConditions(new NullProgressMonitor()));
				status.merge(processor.checkFinalConditions(new NullProgressMonitor(), context));
				changes.add(processor.createChange(new NullProgressMonitor()));
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
			status.addFatalError("Unexpected error see error log for details");
		}
	}

	private RenameProcessor renameFieldProcessor(IField target, RefactoringStatus status, CheckConditionsContext context) {
		RenameFieldProcessor processor = new RenameFieldProcessor(target);
		processor.setNewElementName(getNewActionName());
		return processor;
	}

	private RenameProcessor renameMethodProcessor(IMethod target, RefactoringStatus status, CheckConditionsContext context) throws JavaModelException {
		RenameMethodProcessor processor = null;
		if (MethodChecks.isVirtual(target)) {
			processor = new RenameVirtualMethodProcessor(target);
		} else {
			status.addWarning("Action is a non-virtual method: '"+target.getElementName()+"'", RefactoringUtils.statusContext(target));
			processor = new RenameNonVirtualMethodProcessor(target);
		}
		processor.setNewElementName(getNewActionName());
		processor.setUpdateReferences(true); //TODO: True?
		return processor;
	}

}
