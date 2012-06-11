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

import org.eclipse.jdt.core.IType;

/**
 * @author Kris De Volder
 * @since 2.7
 */
@SuppressWarnings("restriction")
public class TypeRenaming implements ITypeRenaming {

	private IType type;
	private String newName;

	public TypeRenaming(IType type, String newName) {
		this.type = type;
		this.newName = newName;
	}

	@Override
	public String toString() {
		return "TypeRenaming(" + type.getFullyQualifiedName('.') +" => "+newName+")";
	}
	
	public String getNewName() {
		return newName;
	}

	public IType getTarget() {
		return type;
	}
}
