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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 */
public class DependencyData {

	private Set<String> sources = null;
	private Set<String> dependencies = null;
	private String pluginsDirectory = null;
	private Set<String> pluginDescriptors = null;
	private String workDir = null;
	private String pluginClassesDirectory;

	public DependencyData(Set<String> sources, Set<String> dependencies,
			String workDir, String pluginsDirectory, Set<String> pluginDescriptors,
			String pluginClassesDir) {
		super();
		this.sources = notNull(sources);
		this.dependencies = notNull(dependencies);
		this.workDir = workDir;
		this.pluginsDirectory = pluginsDirectory;
		this.pluginDescriptors = notNull(pluginDescriptors);
		this.pluginClassesDirectory= pluginClassesDir;
	}

	private static Set<String> notNull(Set<String> aSetOrNull) {
		if (aSetOrNull == null) {
			return new LinkedHashSet<String>();
		} else {
			return aSetOrNull;
		}
	}

	public DependencyData(Set<File> pluginSourceFolders, Set<File> dependencies, File workDirFile, File pluginsDirectoryFile, Set<File> pluginXmlFiles, File pluginClassesDir) {
		this(
				file2string(pluginSourceFolders), 
				file2string(dependencies), 
				file2string(workDirFile), 
				file2string(pluginsDirectoryFile),
				file2string(pluginXmlFiles),
				file2string(pluginClassesDir)
		);
	}

	private static Set<String> file2string(Set<File> files) {
		Set<String> strings = new LinkedHashSet<String>();
		if (files!=null) {
			for (File file : files) {
				strings.add(file2string(file));
			}
		}
		return strings;
	}

	private static String file2string(File file) {
		if (file!=null) {
			try {
				return file.getCanonicalPath();
			} catch (IOException e) {
				// If canonical path fails, use absolute path as next best thing.
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	public Set<String> getSources() {
		return sources;
	}

	public Set<String> getDependencies() {
		return dependencies;
	}

	public String getPluginsDirectory() {
		return pluginsDirectory;
	}
	
	public String getWorkDir() {
		return workDir;
	}
	
	public String getPluginClassesDirectory() {
		return pluginClassesDirectory;
	}

	public Set<String> getPluginDescriptors() {
		return pluginDescriptors;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((dependencies == null) ? 0 : dependencies.hashCode());
		result = prime
				* result
				+ ((pluginDescriptors == null) ? 0 : pluginDescriptors
						.hashCode());
		result = prime
				* result
				+ ((pluginsDirectory == null) ? 0 : pluginsDirectory.hashCode());
		result = prime * result + ((sources == null) ? 0 : sources.hashCode());
		result = prime * result + ((workDir == null) ? 0 : workDir.hashCode());
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
		DependencyData other = (DependencyData) obj;
		if (dependencies == null) {
			if (other.dependencies != null)
				return false;
		} else if (!dependencies.equals(other.dependencies))
			return false;
		if (pluginDescriptors == null) {
			if (other.pluginDescriptors != null)
				return false;
		} else if (!pluginDescriptors.equals(other.pluginDescriptors))
			return false;
		if (pluginsDirectory == null) {
			if (other.pluginsDirectory != null)
				return false;
		} else if (!pluginsDirectory.equals(other.pluginsDirectory))
			return false;
		if (sources == null) {
			if (other.sources != null)
				return false;
		} else if (!sources.equals(other.sources))
			return false;
		if (workDir == null) {
			if (other.workDir != null)
				return false;
		} else if (!workDir.equals(other.workDir))
			return false;
		return true;
	}

	public File getPluginClassesDirectoryFile() {
		String path = getPluginClassesDirectory();
		if (path!=null) {
			return new File(path);
		}
		return null;
	}
	
	

}
