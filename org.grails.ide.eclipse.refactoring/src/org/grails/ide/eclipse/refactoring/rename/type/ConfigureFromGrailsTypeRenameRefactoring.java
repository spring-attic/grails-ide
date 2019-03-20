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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.grails.ide.eclipse.refactoring.rename.type.GrailsTypeRenameParticipant.IGrailsTypeRenameConfigurer;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class ConfigureFromGrailsTypeRenameRefactoring implements IGrailsTypeRenameConfigurer {

	private GrailsTypeRenameRefactoring refactoring;

	public ConfigureFromGrailsTypeRenameRefactoring(GrailsTypeRenameRefactoring refactoring) {
		this.refactoring = refactoring;
	}
	
	public Collection<ITypeRenaming> chooseAdditionalRenamings(ITypeRenaming orgType, Collection<ITypeRenaming> values, RefactoringStatus status) {
		return Arrays.asList(refactoring.getChosenAdditionalRenamings());
	}

	public boolean updateServiceReferences() {
		return refactoring.getUpdateServiceRefs();
	}

	public boolean updateGSPs() {
		return refactoring.getUpdateGSPs();
	}

}
