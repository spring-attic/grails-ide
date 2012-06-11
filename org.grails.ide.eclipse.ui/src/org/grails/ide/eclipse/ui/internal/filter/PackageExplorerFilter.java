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
package org.grails.ide.eclipse.ui.internal.filter;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;


/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @since 2.2.0
 */
public class PackageExplorerFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IPackageFragmentRoot) {
			try {
				IClasspathEntry entry = ((IPackageFragmentRoot) element).getRawClasspathEntry();
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					for (IClasspathAttribute attr : entry.getExtraAttributes()) {
						if (attr.getName().equals(GrailsClasspathContainer.PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME)) {
							return false;
						}
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

}
