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
package org.grails.ide.eclipse.refactoring.rename.ui;

import org.codehaus.groovy.eclipse.refactoring.actions.IRenameTarget;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;

/**
 * @author Kris De Volder
 * @since 2.7
 */
@SuppressWarnings("rawtypes") 
public class GrailsTypeRenameTargetAdapterFactory extends GrailsRenameTargetAdapterFactory {

	private static final Class<?>[] types = {
		IRenameTarget.class
	};
 
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		try {
			if (adapterType==IRenameTarget.class) {
				if (adaptableObject instanceof ICompilationUnit) {
					ICompilationUnit cu = (ICompilationUnit) adaptableObject;
					GrailsProject project = GrailsWorkspaceCore.get().getGrailsProjectFor(cu);
					if (project!=null) {
						IGrailsElement element = project.getGrailsElement(cu);
						if (isSpecialRenameKind(element.getKind())) {
							return new GrailsTypeRenameTarget(element);
						}
					}
				} else if (adaptableObject instanceof IType) {
					IType type = (IType) adaptableObject;
					return getAdapter(type.getCompilationUnit(), adapterType);
				}
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		return null;
	}

	private boolean isSpecialRenameKind(GrailsElementKind kind) {
		return kind == GrailsElementKind.CONTROLLER_CLASS
			|| kind == GrailsElementKind.DOMAIN_CLASS
			|| kind == GrailsElementKind.SERVICE_CLASS
			|| kind == GrailsElementKind.TAGLIB_CLASS
			|| kind == GrailsElementKind.INTEGRATION_TEST
			|| kind == GrailsElementKind.UNIT_TEST;
	}

	public Class[] getAdapterList() {
		return types;
	}

}
