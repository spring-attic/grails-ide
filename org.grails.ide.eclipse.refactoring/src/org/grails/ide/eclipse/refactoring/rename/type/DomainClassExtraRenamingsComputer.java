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


/**
 * Computes additional renamings for a domain class.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class DomainClassExtraRenamingsComputer extends ExtraRenamingsComputer {
	
	public DomainClassExtraRenamingsComputer(ITypeRenaming org) {
		super(org);
	}

	protected String getBaseName() {
		return simpleName(getOrgFullyQualifiedName());
	}
	
	protected String getNewBaseName() {
		return getOrginalRenaming().getNewName();
	}

}
