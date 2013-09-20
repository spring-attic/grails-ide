/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.explorer.internal.actions;

import org.eclipse.ui.IActionFilter;

import org.grails.ide.eclipse.explorer.elements.GrailsDependencyPluginFolder;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public class ExplorerActionFilter implements IActionFilter {

	public boolean testAttribute(Object target, String name, String expected) {
		if (target instanceof GrailsDependencyPluginFolder
				&& "isWorkspacePlugin".equals(name)) {
			GrailsDependencyPluginFolder pluginFolder = (GrailsDependencyPluginFolder) target;
			return Boolean.valueOf(expected).equals(pluginFolder.isInPlacePlugin());
		}
		return false;
	}

}
