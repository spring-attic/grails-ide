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
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * Represents a link to a plugin dependency in a Grails project. It may point to
 * either a local workspace plugin or an installed Grails plugin.
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Andrew Eisenberg
 * @author Kris De Volder
 */
public class GrailsDependencyPluginFolder extends GrailsPluginFolder {

	private PluginVersion pluginVersion;

	public GrailsDependencyPluginFolder(Object parent, IFolder folder,
			GrailsProjectStructureTypes type) {
		super(parent, folder, type);

	}

	/**
	 * Note that for dependency plugins the name includes the version number
	 * <p>
	 * To get the actual plugin name use the getPluginName() method
	 * </p>
	 */
	public String getName() {
		String derivedName = getPluginName();
		String version = getVersion();
		if (version != null) {
			derivedName += " " + version;
		}
		return derivedName;
	}

	/**
	 * 
	 * @return the plugin name with no version number
	 */
	public String getPluginName() {
		PluginVersion versionModel = getPluginModel();
		String dataName = null;

		if (versionModel != null && (dataName = versionModel.getName()) != null) {
			return dataName;
		} else {
			return getFolder().getName();
		}
	}

	public PluginVersion getPluginModel() {
		return pluginVersion;
	}

	/**
	 * 
	 * @return true if the corresponding dependency plugin exists.
	 */
	public boolean exists() {
		return getPluginModel() != null;
	}

	/**
	 * 
	 * @return Version ID of plugin if it is an installed plugin. Null if it is a
	 *         workspace plugin.
	 */
	protected String getVersion() {

		PluginVersion pluginVersion = getPluginModel();
		String version = null;
		// If it is not a workspace plugin, then get the plugin version from the
		// plugin data. Dont compute
		// the version if it is a workspace plugin
		return (pluginVersion != null && (version = pluginVersion.getVersion()) != null) ? version
				: null;
	}

	public void setPluginVersion(PluginVersion pluginVersion) {
		this.pluginVersion = pluginVersion;
	}

	public boolean isInPlacePlugin() {
		return pluginVersion != null ? pluginVersion.getParent().isInPlace()
				: false;
	}

	public List<Object> getChildren() {
        DependencyData data = GrailsCore.get().connect(getFolder().getProject(), PerProjectDependencyDataCache.class).getData();
        Set<String> source = data != null ? data.getSources() : null;

        // Don't return children if it is a workspace plugin
        if (isInPlacePlugin()) {
            return null;
        }

        if (source != null) {
            List<String> subElementSources = new ArrayList<String>();
            for (String sourceDescriptor : source) {
                String name = getPluginName();
                if (sourceDescriptor != null && sourceDescriptor.contains(name)) {
                    subElementSources.add(sourceDescriptor);
                }
            }

            if (!subElementSources.isEmpty()) {
                List<Object> children = new ArrayList<Object>();

                for (String subSource : subElementSources) {
                    IPath path = new Path(subSource);
                    IProject project = getFolder().getProject();

                    IPackageFragmentRoot[] roots = GrailsResourceUtil
                            .getGrailsDependencyPackageFragmentRoots(project,
                                    path);

                    if (roots != null) {
                        for (IPackageFragmentRoot root : roots) {
                            children.add(root);
                        }
                    }
                }
                return children;
            }
        }

		return null;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"("+getName()+")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((pluginVersion == null) ? 0 : pluginVersion.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GrailsDependencyPluginFolder other = (GrailsDependencyPluginFolder) obj;
		if (pluginVersion == null) {
			if (other.pluginVersion != null)
				return false;
		} else if (!pluginVersion.equals(other.pluginVersion))
			return false;
		return true;
	}
	
}
