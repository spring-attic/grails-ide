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
package org.grails.ide.eclipse.core.workspace.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.workspace.GrailsProject;


/**
 * Utility methods to work with Grails projects. Generally, these methods should be considered 'internal'.
 * The proper way to work with them is via an instance of {@link GrailsProject}.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsProjectUtil {

	public static List<IProject> getDependentGrailsProjects(IJavaProject javaProject) throws JavaModelException {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		ArrayList<IProject> result = new ArrayList<IProject>();
		for (IClasspathEntry entry : entries) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				IProject relatedProject = root.getProject(entry.getPath().lastSegment());
				if (GrailsNature.isGrailsPluginProject(relatedProject)) {
					result.add(relatedProject);
				}
			}
		}
		return result;
	}

}
