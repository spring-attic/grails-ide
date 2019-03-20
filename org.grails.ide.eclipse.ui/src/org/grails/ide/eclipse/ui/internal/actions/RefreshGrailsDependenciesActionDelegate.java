/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainerUpdateJob;
import org.grails.ide.eclipse.ui.GrailsUiActivator;
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
    private Shell shell;
	
	public void run(IAction action) {
		debug(">>> running action 'refresh dependencies'");
		
		List<IProject> mavenProjects = getMavenProjects(selected);
		if (mavenProjects.size() > 0) {
		    String errorLabel = createLabel(mavenProjects);
		    MessageDialog.openInformation(shell, "Maven project", errorLabel);
		    return;
		}
		
		for (IProject project : selected) {
			debug("project: "+project);
			GrailsClasspathContainerUpdateJob
					.scheduleClasspathContainerUpdateJob(project, true);
		}
		debug("<<< running action 'refresh dependencies'");
	}

	/**
     * @param mavenProjects
     * @return
     */
    private String createLabel(List<IProject> mavenProjects) {
        if (mavenProjects.size() == 1) {
            return "The project '" + mavenProjects.get(0).getName() + "' is a maven project.  Must run Maven -> Update project... instead of Refresh dependencies.";
        } else {
            StringBuilder sb = new StringBuilder();
            for (Iterator<IProject> iterator = mavenProjects.iterator(); iterator
                    .hasNext();) {
                IProject project = iterator.next();
                if (!iterator.hasNext()) {
                    sb.append("and ");
                }
                sb.append(project.getName());
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
                
            }
            return "The projects " + sb + " are a maven projects.  Must run Maven -> Update project... instead of Refresh dependencies.";
        }
    }

    /**
     * @param projects
     * @return
     */
    private List<IProject> getMavenProjects(List<IProject> projects) {
        List<IProject> mavenProjects = new ArrayList<IProject>(projects.size());
        for (IProject project : projects) {
            if (GrailsUiActivator.isM2EProject(project)) {
                mavenProjects.add(project);
            }
        }
        return mavenProjects;
    }

    public void selectionChanged(IAction action, ISelection selection) {
		debug("selectionChanged: "+this);
		debug("      selection = "+selection);
		selected = SelectionUtils.getProjects(selection, new ProjectFilter() {
			@Override
			public boolean isAcceptable(IProject project) {
				return GrailsNature.isGrailsProject(project) && !GrailsUiActivator.isM2EProject(project);
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
	    shell = null;
	}

	public void init(IWorkbenchWindow window) {
	    shell = window.getShell();
	}

}
