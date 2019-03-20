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
package org.grails.ide.eclipse.explorer.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPlugin;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginsListManager;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * Grails logical folder that represents all plugin dependencies for a given
 * Grails project. The plugin dependencies may be local to the workspace or
 * installed via Grails commands.
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Christian Dupuis
 */
public class GrailsPluginFolder extends AbstractGrailsFolder {

	protected GrailsPluginFolder(Object parent, IFolder folder,
			GrailsProjectStructureTypes type) {
		super(parent, folder, type);
	}

	public List<Object> getChildren() {

		IFolder folder = getFolder();
		if (folder == null) {
			return null;
		}

		IProject project = folder.getProject();
		if (GrailsNature.isGrailsProject(project)) {

			GrailsPluginsListManager manager = GrailsPluginsListManager
					.getGrailsPluginsListManager(project);
			if (manager != null) {

				Collection<GrailsPlugin> dependencies = manager
						.getDependenciesAsPluginModels();

				if (dependencies != null && !dependencies.isEmpty()) {
					List<Object> children = new ArrayList<Object>();
					// Note that plugin descriptors reside in OTHER projects
					// and therefore are referenced in the current project
					// via folder links.
					for (GrailsPlugin dependency : dependencies) {

						PluginVersion version = dependency.getInstalled();

						// If no installed version can be resolved, continue
						if (version == null) {
							continue;
						}

						PerProjectPluginCache cache = manager
								.getDependencyCache();
						if (cache == null) {
							continue;
						}

						String descriptor = cache
								.getDependencyPluginDescriptor(version);

						if (descriptor == null) {
							continue;
						}

						// For now, until a better solution can be obtained
						// create a file to the external location of the
						// descriptor
						// NOTE that this file may exists in the file system,
						// but
						// is not a file of the project. The reason this "dummy"
						// workspace
						// file is created is to not trigger a resource change
						// event or
						// modify the project resource in any way, since the
						// Explorer is just
						// a view and shouldn't have to create new resources.
						// This can
						// change in the future

						File fileFolder = new File(descriptor);

						File physicalFolder = fileFolder.getParentFile();

						// Most likely does not exist, just a workspace wrapper
						// around
						// a non-workspace folder.
						IFolder linkFolder = project
								.getFolder(physicalFolder != null ? new Path(
										physicalFolder.getAbsolutePath())
										: new Path(fileFolder.getAbsolutePath()));

						ILogicalFolder treeElement = new GrailsFolderElementFactory()
								.getElement(
										this,
										linkFolder,
										GrailsProjectStructureTypes.DEPENDENCY_PLUGIN);
						if (treeElement instanceof GrailsDependencyPluginFolder) {
							GrailsDependencyPluginFolder subElement = (GrailsDependencyPluginFolder) treeElement;
							subElement.setPluginVersion(version);
							children.add(subElement);
						}
					}
					return children;
				}
			}
		}
		return null;
	}

	protected PerProjectPluginCache getDependencyCache() {
		return GrailsCore.get().connect(getFolder().getProject(),
				PerProjectPluginCache.class);
	}
	
}
