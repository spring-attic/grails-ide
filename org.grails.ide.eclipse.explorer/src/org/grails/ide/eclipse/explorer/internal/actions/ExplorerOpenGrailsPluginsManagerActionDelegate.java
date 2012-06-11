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
package org.grails.ide.eclipse.explorer.internal.actions;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.grails.ide.eclipse.explorer.elements.GrailsPluginFolder;
import org.grails.ide.eclipse.ui.internal.actions.OpenGrailsPluginsManagerActionDelegate;

/**
 * The explorer action for opening the grails plugin manager from the context menu of the plugins node.
 * <p>
 * Differs slightly from the OpenGrailsPluginsManagerActionDelegate which implements the same action in
 * the "Grails Tools" menu. Reason: must deal with the selection consisting of the plugin node, which
 * is not a real resource but a virtual node in the explorer tree only. (Could not modify the
 * action in grails.ui plugin because that plugin cannot depend on the explorer plugin or will have
 * circular plugin dependencies).
 * @author Kris De Volder
 */
public class ExplorerOpenGrailsPluginsManagerActionDelegate extends OpenGrailsPluginsManagerActionDelegate {

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			LinkedHashSet<IProject> projects = new LinkedHashSet<IProject>();
			IStructuredSelection selection  = (IStructuredSelection) sel;
			for (Object obj : selection.toArray()) {
				if (obj instanceof GrailsPluginFolder) {
					GrailsPluginFolder pluginNode = (GrailsPluginFolder) obj;
					IProject project = pluginNode.getProject();
					projects.add(project);
				}
			}
			if (!projects.isEmpty()) {
				selectedProjects = new ArrayList<IProject>(projects);
				action.setEnabled(true);
				return;
			}
		}
		action.setEnabled(false);
	}
	
}
