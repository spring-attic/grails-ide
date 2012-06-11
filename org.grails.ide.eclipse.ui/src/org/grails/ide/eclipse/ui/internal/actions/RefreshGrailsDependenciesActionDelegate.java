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
package org.grails.ide.eclipse.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainerUpdateJob;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.ProjectFilter;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.SelectionUtils;


/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @author Nieraj Singh
 * @since 2.2.0
 */
public class RefreshGrailsDependenciesActionDelegate implements IObjectActionDelegate, IWorkbenchWindowActionDelegate {

	private static final boolean DEBUG = false;
	private static void debug(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}
	
	private List<IProject> selected = null;
	
	public void run(IAction action) {
		debug(">>> running action 'refresh dependencies'");
		for (IProject project : selected) {
			debug("project: "+project);
			GrailsClasspathContainerUpdateJob
					.scheduleClasspathContainerUpdateJob(project, true);
		}
		debug("<<< running action 'refresh dependencies'");
	}

	public void selectionChanged(IAction action, ISelection selection) {
		debug("selectionChanged: "+this);
		debug("      selection = "+selection);
		selected = SelectionUtils.getProjects(selection, new ProjectFilter() {
			@Override
			public boolean isAcceptable(IProject project) {
				return GrailsNature.isGrailsProject(project);
			}
		});
		boolean enabled = !selected.isEmpty();
		action.setEnabled(enabled);
		debug("        enabled = "+enabled);
		debug("       selected = "+selected);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void dispose() {
		// Nothing
	}

	public void init(IWorkbenchWindow window) {
		// Nothing
	}

}
