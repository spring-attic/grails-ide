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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.ReplaceEdit;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.refactoring.rename.ParticipantChangeManager;
import org.grails.ide.eclipse.search.action.ControllerActionSearch;

/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public abstract class GrailsActionOrViewRenameParticipant extends RenameParticipant {

	public interface IActionRequestor {
		void add(IMember action);
	}

	protected ParticipantChangeManager changes = new ParticipantChangeManager(this);

	private IProject project;
	private GrailsProject grailsProject;
	
	private String oldActionName;
	private String newActionName; 
	private String targetControllerClassName;
	private String targetControllerName;

	/**
	 * It is assumed that this method precomputes all the changes and adds them to the {@link ParticipantChangeManager}
	 */
	@Override
	public abstract RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context);
	
	@Override
	public Change createPreChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		changes.copyExistingChangesTo(this);
		return changes.getNewTextChanges();
	}
	
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return changes.getOtherChanges();
	}
	
	/**
	 * Rewrites references to 'actions' but only making those changes that the ordinary JDT rename method or field refactoring
	 * would not automatically perform by itself.
	 */
	protected void rewriteActionReferences(RefactoringStatus status) {
		try {
			ControllerActionSearch search = new ControllerActionSearch(getGrailsProject(), targetControllerClassName, oldActionName);
			search.perform(new ISearchRequestor() {
				public void reportMatch(Match match) {
					Object el = match.getElement();
					TextChange txtChange = changes.getTextChangeFor(el);
					if (txtChange!=null) {
						if (match.getLength()==oldActionName.length()) { 
							//only perform edit if assumption holds 
							final ReplaceEdit edit = new ReplaceEdit(match.getOffset(), oldActionName.length(), newActionName);
							txtChange.addEdit(edit);
						}
					}
				}

			});
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
			status.addFatalError(e.getMessage());
		}
		
	}
	
	public String getNewActionName() {
		return newActionName;
	}

	public void setNewActionName(String newActionName) {
		this.newActionName = newActionName;
	}

	public String getOldActionName() {
		return oldActionName;
	}

	public void setOldActionName(String oldActionName) {
		this.oldActionName = oldActionName;
	}
	
	public void setTargetControllerClassName(String targetControllerClassName) {
		this.targetControllerClassName = targetControllerClassName;
		this.targetControllerName = GrailsNameUtils.getLogicalPropertyName(targetControllerClassName, "Controller");
	}

	public String getTargetControllerName() {
		return targetControllerName;
	}

	public void setTargetControllerName(String targetControllerName) {
		this.targetControllerName = targetControllerName;
		this.targetControllerClassName = GrailsNameUtils.getClassName(targetControllerName, "Controller");
	}
	
	public ControllerClass getTargetController() {
		return getGrailsProject().getControllerClass(getTargetControllerClassName());
	}

	public String getTargetControllerClassName() {
		return targetControllerClassName;
	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

	protected GrailsProject getGrailsProject() {
		if (this.grailsProject==null) {
			this.grailsProject = GrailsWorkspaceCore.get().create(project);
		}
		return this.grailsProject;
	}
	
	public static Collection<? extends IMember> getActions(IType controllerClass, String actionName) throws JavaModelException {
		Assert.isNotNull(controllerClass);
		ArrayList<IMember> actions = new ArrayList<IMember>();
		IField field = controllerClass.getField(actionName);
		if (field.exists()) {
			actions.add(field);
		}
		
		collectMethodActions(controllerClass, actionName, actions);
		return actions;
	}
	
	public static Collection<? extends IMember> getActions(ControllerClass controllerClass, String actionName) throws JavaModelException {
		IType type = controllerClass.getType();
		if (type!=null) {
			return getActions(controllerClass.getType(), actionName);
		} else {
			return new ArrayList<IMember>();
		}
	}

	private static void collectMethodActions(IType controllerClass, String actionName, ArrayList<IMember> actions) throws JavaModelException {
		for (IMethod m : controllerClass.getMethods()) {
			if (m.getElementName().equals(actionName)) {
				actions.add(m);
			}
		}
	}

}
