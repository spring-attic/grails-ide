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
package org.grails.ide.eclipse.refactoring.rename.type;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.refactoring.rename.ParticipantChangeManager;

/**
 * Abstract super class for plugable things that contribute extra changes triggered by Grails type
 * renames.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public abstract class ExtraChangeComputer {
	
	protected GrailsProject project;
	protected ITypeRenaming renaming;
	
	public boolean initialize(GrailsProject project, ITypeRenaming renaming) {
		this.project = project;
		this.renaming = renaming;
		return true;
	}

	/**
	 * Extra change computer is called by GrailsTypeRenameParticipant to give the extra change computer a chance to create
	 * additional changes to the workspace.
	 * @param status 
	 */
	protected abstract void createChanges(ParticipantChangeManager changes, RefactoringStatus status, IProgressMonitor pm);

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
			+"(" + renaming.getTarget().getElementName() + " => " + renaming.getNewName()+" )";
	}
}
