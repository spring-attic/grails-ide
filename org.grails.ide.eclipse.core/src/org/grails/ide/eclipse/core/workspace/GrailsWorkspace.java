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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * Grails-centric view on the workspace.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsWorkspace {
	
	private IWorkspace workspace;
	
	private GrailsWorkspace(IWorkspace workspace) {
		this.workspace = workspace;
	}
	
	public static GrailsWorkspace get() {
		return new GrailsWorkspace(ResourcesPlugin.getWorkspace());
	}

	public synchronized GrailsProject create(IProject p) {
		return new GrailsProject(this, p);
	}
	public GrailsProject create(IJavaProject javaProject) {
		return create(javaProject.getProject());
	}

	public List<GrailsProject> getProjects() {
		List<GrailsProject> result = new ArrayList<GrailsProject>();
		for (IProject p : workspace.getRoot().getProjects()) {
			if (GrailsNature.isGrailsProject(p)) {
				result.add(create(p));
			}
		}
		return result;
	}


}
