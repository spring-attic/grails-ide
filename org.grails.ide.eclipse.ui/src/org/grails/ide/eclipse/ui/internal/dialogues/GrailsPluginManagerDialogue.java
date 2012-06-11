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
package org.grails.ide.eclipse.ui.internal.dialogues;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginsListManager;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.Plugin;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginManagerDialog;


/**
 * The manager allows plugins to be selected for install, uninstall or update
 * <p>
 * The actual operations on the selected plugins are executed only after the
 * dialogue closes.
 * </p>
 * <p>
 * The install and uninstall operations can be used to undo each other. So if a
 * plugin is marked as selected for install, pressing the uninstall operation
 * undos the selection, restoring the plugin state to its original state.
 * </p>
 * <p>
 * IMPORTANT: to understand the behaviour of this manager, it is important to
 * distinguish between the underlying plugin model and the tree elements in a
 * viewer that represent the plugin model. They are separate, although a tree
 * element always holds a reference to its corresponding plugin model entity.
 * </p>
 * <p>
 * When the manager first opens it will populate the list of plugins based on
 * the local list of available plugins as well as will mark those plugins on
 * that list that are installed in a given project context. This list can be
 * refreshed by requesting Grails for an updated list, or it can be reset to the
 * state the list was in when the manager first opened (i.e. the state based on
 * the local list of plugins).
 * </p>
 * Plugins and their versions are represented in a tree viewer as tree elements
 * that hold a reference to a plugin version model entity. In addition, these
 * tree elements also hold selection and install state, indicating whether the
 * plugin is currently installed, or the selection status while the manager is
 * still open (i.e. whether a plugin has been selected for upgrade, uninstall,
 * etc..
 * <p>
 * A tree element is therefore the UI representation of a plugin version model
 * entity, with additional information specific to the operations that can be
 * performed with this manager.
 * </p>
 * <p>
 * The tree viewer itself displays two types of elements, a root element and a
 * version element. BOTH of these represent a plugin version model entity,
 * except that the root element indicates which version is either available for
 * install OR which version is currently installed. The version element simply
 * indicates a particular version model entity for a plugin. The list of child
 * version elements is IMMUTABLE, but the root element version is NOT. The
 * reason is that the root element always reflects which version of a plugin the
 * user has selected for a particular operation, and therefore may change as the
 * user changes selections, and performs different operations.
 * </p>
 * 
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Kris De Volder
 */
public class GrailsPluginManagerDialogue extends PluginManagerDialog {

	public GrailsPluginManagerDialogue(Shell parentShell,
			List<IProject> projects) {
		super(parentShell, projects);
	}

	@Override
	protected Collection<IProject> updateProjects() {
		return GrailsResourceUtil.getAllGrailsProjects();
	}

	@Override
	protected Collection<? extends Plugin> updatePlugins(boolean aggressive, IProgressMonitor monitor) {
		GrailsPluginsListManager manager = GrailsPluginsListManager.getGrailsPluginsListManager(getSelectedProject());
		return manager != null ? manager.generateList(aggressive) : null;
	}

	@Override
	protected boolean isPreinstalled(
			org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion version) {
		return GrailsPluginsListManager.getPreInstalledPlugins().contains(
				version.getName());
	}

	public String getMessage() {
		return "Browse published and installed Grails plugins, and schedule plugins to be installed, uninstalled, or updated. Changes take effect after the manager closes.";
	}

	public String getTitle() {
		return "Grails Plugin Manager";
	}

}
