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
package org.grails.ide.eclipse.explorer.elements;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.PlatformObject;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;

import org.grails.ide.eclipse.explorer.types.GrailsContainerType;

/**
 * Abstract implementation of a logical grails folder which assumes that the underlying
 * element being viewed is an IFolder.
 * 
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Christian Dupuis
 * @author Kris De Volder
 */
public abstract class AbstractGrailsFolder extends PlatformObject implements
		ILogicalFolder {
	private GrailsProjectStructureTypes type;
	private IFolder folder;
	private Object parent;

	protected AbstractGrailsFolder(Object parent, IFolder folder,
			GrailsProjectStructureTypes type) {
		this.type = type;
		this.folder = folder;
		this.parent = parent;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == GrailsContainerType.class) {
			return getType();
		} else {
			IFolder folder = getFolder();
			if (folder != null) {
				if (adapter == IProject.class) {
					return folder.getProject();
				} else if (folder.exists()) {
					return folder.getAdapter(adapter);
				}
			}
		}
		return super.getAdapter(adapter);
	}

	protected IFolder getFolder() {
		return folder;
	}

	public String getName() {
		return type != null ? type.getDisplayName() : null;
	}


	public GrailsProjectStructureTypes getType() {
		return type;
	}

	public Object getParent() {
		return parent;
	}
	
	/**
	 * Returns the project this element is nested under.
	 * @return 
	 */
	public IProject getProject() {
		Object p = getParent();
		if (p instanceof IProject) {
			return (IProject) p;
		}
		else if (p instanceof ILogicalFolder) {
			return ((ILogicalFolder) p).getProject();
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractGrailsFolder other = (AbstractGrailsFolder) obj;
		if (folder == null) {
			if (other.folder != null)
				return false;
		} else if (!folder.equals(other.folder))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

}
