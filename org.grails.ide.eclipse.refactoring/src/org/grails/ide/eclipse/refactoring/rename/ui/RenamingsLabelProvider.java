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
package org.grails.ide.eclipse.refactoring.rename.ui;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import org.grails.ide.eclipse.refactoring.rename.type.ITypeRenaming;
import org.grails.ide.eclipse.refactoring.rename.type.TypeRenaming;

/**
 * @author Kris De Volder
 * @since 2.6
 */
public class RenamingsLabelProvider extends BaseLabelProvider implements ILabelProvider {
	
	private JavaElementLabelProvider javaLabelProvider = new JavaElementLabelProvider();

	public Image getImage(Object element) {
		if (element instanceof TypeRenaming) {
			return javaLabelProvider.getImage(((TypeRenaming) element).getTarget());
		}
		return null;
	}

	public String getText(Object element) {
		if (element instanceof ITypeRenaming) {
			TypeRenaming ren = (TypeRenaming) element;
			String valueText = ren.getNewName();
			return javaLabelProvider.getText(ren.getTarget()) + " => "+valueText;
		}
		return ""+element;
	}
	
	public void dispose() {
		javaLabelProvider.dispose();
		super.dispose();
	}

}
