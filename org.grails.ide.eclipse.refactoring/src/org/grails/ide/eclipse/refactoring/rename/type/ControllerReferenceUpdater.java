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
package org.grails.ide.eclipse.refactoring.rename.type;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.ReplaceEdit;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.refactoring.rename.ParticipantChangeManager;
import org.grails.ide.eclipse.search.SearchUtil;
import org.grails.ide.eclipse.search.controller.ControllerTypeSearch;

/**
 * Computes changes to replace controller references (other than direct references to the controller type). 
 * In particular it finds references like:
 * 
 *   redirect(controller: "<name>", ...)  inside controller classes
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ControllerReferenceUpdater extends ExtraChangeComputer {
	
	private static final String CONTROLLER = "Controller";
	private ControllerClass controllerClass;
	private String oldName;
	private String newName;
	
	@Override
	public boolean initialize(GrailsProject project, ITypeRenaming renaming) {
		if (super.initialize(project, renaming)) {
			if (renaming.getTarget().getElementName().endsWith(CONTROLLER)) {
				controllerClass = project.getControllerClass(renaming.getTarget());
				oldName = controllerClass.getLogicalName();
				if (renaming.getNewName().endsWith(CONTROLLER)) {
					newName = GrailsNameUtils.getLogicalPropertyName(renaming.getNewName(), CONTROLLER);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void createChanges(final ParticipantChangeManager changes, RefactoringStatus status, IProgressMonitor pm) {
		try {
			IType targetType = controllerClass.getType();
			ControllerTypeSearch search = new ControllerTypeSearch(SearchUtil.createReferencesQuery(targetType));
			search.perform(new ISearchRequestor() {
				public void reportMatch(Match match) {
					TextChange textChange = changes.getTextChangeFor(match.getElement());
					if (textChange!=null) {
						if (match.getLength()==oldName.length()) {
							textChange.addEdit(new ReplaceEdit(match.getOffset(), oldName.length(), newName));
						}
					}
				}
			});
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
			status.addError(e.getMessage());
		}
	}
	
}
