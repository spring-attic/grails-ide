/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.explorer.elements;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;


/**
 * Creates common navigator tree elements for Grails logical folders.
 * @author Nieraj Singh
 * @author Andy Clement
 */
public class GrailsFolderElementFactory {

	private static Map<IResource, WeakReference<ILogicalFolder>> map = new HashMap<IResource, WeakReference<ILogicalFolder>>();

	public ILogicalFolder getElement(Object parent, IFolder folder,
			GrailsProjectStructureTypes type) {

		if (parent == null || folder == null || type == null) {
			return null;
		}
		WeakReference<ILogicalFolder> weakRf = map.get(folder);
		ILogicalFolder element = (weakRf != null) ? weakRf.get() : null;

		switch (type) {
		case PLUGINS:
			element = new GrailsPluginFolder(parent, folder, type);
			break;
		case DEPENDENCY_PLUGIN:
			element = new GrailsDependencyPluginFolder(parent, folder, type);
			break;
		}

		if (element != null) {
			map.put(folder, new WeakReference<ILogicalFolder>(element));
		}

		return element;
	}

	public static ILogicalFolder getExisting(IFolder folder) {
		return map.get(folder).get();
	}

}
