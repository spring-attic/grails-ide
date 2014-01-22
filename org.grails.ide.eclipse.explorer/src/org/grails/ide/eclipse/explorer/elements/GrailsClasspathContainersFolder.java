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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;

import org.grails.ide.eclipse.explorer.types.GrailsContainerType;

/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsClasspathContainersFolder extends PlatformObject implements ILogicalFolder {
	
	private GrailsProjectStructureTypes type;
	private IProject parent;
	
	IClasspathEntry[] classpathContainer = null;
	
	public GrailsClasspathContainersFolder(IProject parent) {
		this.type = GrailsProjectStructureTypes.CLASSPATH_CONTAINERS;
		this.parent = parent;
	}

	public String getName() {
		return type != null ? type.getDisplayName() : null;
	}
	
	public Object getParent() {
		return parent;
	}
	
	public GrailsProjectStructureTypes getType() {
		return type;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == GrailsContainerType.class) {
			return getType();
		} else {
			if (adapter == IProject.class) {
				return parent;
			}
		}
		return super.getAdapter(adapter);
	}
	
	public List<Object> getChildren() {
		//TODO: cache?
		IJavaProject javaProject = JavaCore.create(parent);
		try {
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			List<Object> children = new ArrayList<Object>();
			for (IClasspathEntry e : classpath) {
				if (e.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
					children.add(new ClassPathContainer(javaProject, e));
				}
			}
			return children;
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		return null;
	}

	public IProject getProject() {
		return parent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		GrailsClasspathContainersFolder other = (GrailsClasspathContainersFolder) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

}
