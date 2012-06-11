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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
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
public class GrailsViewRenameTargetAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] types = {
		IRenameTarget.class
	};
 
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		try {
			if (adapterType==IRenameTarget.class) {
				if (adaptableObject instanceof IFile) {
					IFile file = (IFile) adaptableObject;
					file.getName();
				}
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		return null;
	}

	public Class[] getAdapterList() {
		return types;
	}

}
