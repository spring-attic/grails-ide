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

import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Provides contents based on the elements of a map or list. (Other kinds of containers of
 * content may be supported in the future).
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class GeneralPurposeContentProvider implements IStructuredContentProvider {

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Map) {
			return ((Map<?,?>) inputElement).entrySet().toArray();
		} else if (inputElement instanceof Collection<?>) {
			return ((Collection<?>)inputElement).toArray();
		}
		return new Object[0];
	}

}
