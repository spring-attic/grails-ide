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
package org.grails.ide.eclipse.core.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;

/**
 * Maintains a global list of 'ignored' grails project names. A number of listeners and other tasks
 * are triggered sometimes at unexpected times. E.g. when imporiting or creating a new project
 * the GrailsProjectVersionFixer is prone to kick in before the import is actually complete, 
 * at the first change event where the project appears partially created in the workspace.
 * <p>
 * This will typically result in spurious errors and dialogs popping up because problems are detected
 * in partially created projects. These problems are typically not 'real' and eventually get fixed
 * by the process that is creating these projects.
 * <p>
 * To handle situations like that this class manages a list of 'ignorable' projects. Version checkers
 * and other similar 'reactive' verifiers should skip checking any projects in the list.
 * <p>
 * At the other end, import wizards and the like should add project names they are in the 
 * process of creating to this list, and remove them when they are done.
 * 
 * @author Kris De Volder
 */
public abstract class IgnoredProjectsList {

	public static Set<String> ignoredProjects = new HashSet<String>();

	public static void addIgnoredProject(String projectName) {
		synchronized (ignoredProjects) {
			ignoredProjects.add(projectName);
		}
	}

	public static void removeIgnoredProject(String projectName) {
		synchronized (ignoredProjects) {
			ignoredProjects.remove(projectName);
		}
	}

	public static boolean isIgnorable(IProject project) {
		synchronized (ignoredProjects) {
			return ignoredProjects.contains(project.getName());
		}
	}

}
