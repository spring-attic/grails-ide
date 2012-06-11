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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

/**
 * Renamings computer where the original renaming is supposed to preserve a certain 
 * postfix that is added to the domain class name.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class PostfixedClassExtraRenamingsComputer extends ExtraRenamingsComputer {

	private String expectedPostfix;
	
	public PostfixedClassExtraRenamingsComputer(ITypeRenaming org, String... postfixes) {
		super(org);
		this.expectedPostfix = null;
		String orgName = getOrgFullyQualifiedName();
		for (String pf : postfixes) {
			if (orgName.endsWith(pf)) {
				this.expectedPostfix = pf;
				return;
			}
		}
		Assert.isLegal(false, "The original target of the renaming '"+orgName+"' doesn't follow expected naming pattern");
	}

	@Override
	protected String getBaseName() {
		ITypeRenaming ren = getOrginalRenaming();
		String orgName = ren.getTarget().getElementName();
		Assert.isLegal(orgName.endsWith(expectedPostfix));
		return removePostfix(orgName);
	}

	@Override
	protected String getNewBaseName() {
		ITypeRenaming ren = getOrginalRenaming();
		String orgName = ren.getNewName();
		if (orgName.endsWith(expectedPostfix)) {
			return removePostfix(orgName);
		}
		return null;
	}

	private String removePostfix(String orgName) {
		Assert.isLegal(orgName.endsWith(expectedPostfix));
		return orgName.substring(0, orgName.length()-expectedPostfix.length());
	}

	@Override
	public RefactoringStatus checkPreconditions() {
		RefactoringStatus status = super.checkPreconditions();
		ITypeRenaming ren = getOrginalRenaming();
		String orgName = ren.getTarget().getFullyQualifiedName();
		Assert.isLegal(orgName.endsWith(expectedPostfix));
		String newName = ren.getNewName();
		if (!newName.endsWith(expectedPostfix)) {
			status.addEntry(new RefactoringStatusEntry(IStatus.WARNING, 
					"Renaming '"+orgName+"' to '"+newName+", the new name no longer ends in '"+expectedPostfix+"'"));
		}
		return status;
	}
	
}
