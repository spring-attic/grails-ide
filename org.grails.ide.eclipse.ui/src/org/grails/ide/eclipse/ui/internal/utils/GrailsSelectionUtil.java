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
package org.grails.ide.eclipse.ui.internal.utils;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.SelectionUtils;



/**
 * In several places (typically action delegates and the like) we end up writing very similar "selectionChanged" methods. Often
 * replicating bugs in the selection handling logic across them. Ideally we should centralize "selection processing" in here,
 * so that we won't have to fix these bugs multiple times.
 * <p>
 * Note: see also com.springsource.sts.frameworks.ui.internal.utils.SelectionUtils which contains similar selection
 * utility methods. Only selection utility methods that are specific to Grails should be placed in this class. 
 * Selection utilities that are more generally useful should be placed in the SelectionUtil class in the frameworks bundle
 * instead.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class GrailsSelectionUtil {

	/**
	 * Call this to attempt to get a domain class out of a selection somehow.
	 */
	public static IType getDomainClass(IStructuredSelection selection) {
		try {
			IType type = SelectionUtils.getType(selection);
			if (type!=null) {
				IPackageFragmentRoot pkgFragmentRoot = SelectionUtils.getPackageFragmentRoot(type);
				if (pkgFragmentRoot!=null) {
					GrailsProjectStructureTypes containerType = GrailsResourceUtil.getGrailsContainerType(pkgFragmentRoot);
					if (containerType==GrailsProjectStructureTypes.DOMAIN) {
						// The selected IJavaElement is inside a Grails "domain" folder, so presumably its a domain class
						return type;
					} else if (containerType==GrailsProjectStructureTypes.CONTROLLERS) {
						// Selected element is a controller, we can return the corresponding domain class for that controller.
						String typeName = type.getFullyQualifiedName();
						if (typeName.endsWith("Controller")) {
							typeName = typeName.substring(0,typeName.length()-"Controller".length());
							return type.getJavaProject().findType(typeName);
						}
					}
				}
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		return null;
	}

}
