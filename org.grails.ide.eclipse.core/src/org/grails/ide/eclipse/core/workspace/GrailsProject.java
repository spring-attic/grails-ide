/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.workspace;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.workspace.internal.GrailsProjectUtil;
import org.grails.ide.eclipse.runtime.shared.DependencyData;


/**
 * Grails-centric view on a IProject.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */ 
public class GrailsProject {
	
	/**
	 * Identifies this project, should never be null
	 */
	private IProject project;
	
	/**
	 * Java-centric view on our project, created as needed.
	 */
	private IJavaProject javaProject;
	
	/**
	 * The workspace that owns this project. Never null.
	 */
	private GrailsWorkspace ws;
	
	/**
	 * Package private, use {@link GrailsWorkspace}.create()
	 * @param ws 
	 * @param project
	 */
	GrailsProject(GrailsWorkspace ws, IProject project) {
		this.ws = ws;
		this.project = project;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((project == null) ? 0 : project.hashCode());
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
		GrailsProject other = (GrailsProject) obj;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "G"+project.toString();
	}
	
	public Set<GrailsProject> getDependentProjects() throws JavaModelException {
		Set<GrailsProject> dependentProjects = new HashSet<GrailsProject>();
		for (IProject p : GrailsProjectUtil.getDependentGrailsProjects(getJavaProject())) {
			dependentProjects.add(ws.create(p));
		} 
		return dependentProjects;
	}

	public IJavaProject getJavaProject() {
		if (javaProject==null) {
			javaProject = JavaCore.create(project);
		}
		return javaProject;
	}

	/**
	 * Inverse of 'getDependentProjects'. I.e. returns the Grails projects that depend on the
	 * receiver.
	 * @throws JavaModelException 
	 */
	public Set<GrailsProject> getProjectsDependingOn() throws JavaModelException {
		HashSet<GrailsProject> result = new HashSet<GrailsProject>();
		for (GrailsProject p : ws.getProjects()) {
			if (p.getDependentProjects().contains(this)) {
				result.add(p);
			}
		}
		return result;
	}

	public boolean isPlugin() {
		return GrailsNature.isGrailsPluginProject(project);
	}
	
	public GrailsClassPath getClassPath() throws JavaModelException {
		return new GrailsClassPath(this);
	}
	
	public void setClassPath(GrailsClassPath rawClasspath, IProgressMonitor mon) throws JavaModelException {
		getJavaProject().setRawClasspath(rawClasspath.toArray(), mon);
	}


	public int getServerPort() {
		PerProjectDependencyDataCache data = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class);
		if (data!=null) {
			return data.getData().getServerPort();
		}
		return DependencyData.UNKNOWN_PORT;
	}
	
}
