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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

import org.grails.ide.eclipse.explorer.elements.GrailsDependencyPluginFolder;

/**
 * @author Nieraj Singh
 */
public class ExplorerActionFilterAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IActionFilter.class
				&& adaptableObject instanceof GrailsDependencyPluginFolder) {
			return new ExplorerActionFilter();
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IActionFilter.class };
	}

}
