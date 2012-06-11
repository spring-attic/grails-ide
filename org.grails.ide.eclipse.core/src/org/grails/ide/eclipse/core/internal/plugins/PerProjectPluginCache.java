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
package org.grails.ide.eclipse.core.internal.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.grails.ide.eclipse.core.internal.classpath.DependencyData;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginParser;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginVersion;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.classpath.PluginDescriptorParser;
import org.grails.ide.eclipse.core.model.ContributedMethod;
import org.grails.ide.eclipse.core.model.ContributedProperty;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.BasePluginData;


/**
 * Caches information about plugin.xml for each grails plugin on a per-project
 * basis
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @author Kris De Volder
 * @created Jan 28, 2010
 */
public class PerProjectPluginCache implements IGrailsProjectInfo {

	private IProject project;

	/**
	 * Map associating a plugin.xml file/path name to a PluginData object.
	 */
	private Map<String, GrailsPluginVersion> dependencyPluginDataMap;

	private Map<String, ContributedProperty> allControllerProps;
	private Map<String, ContributedProperty> allDomainProps;
	private Map<String, Set<ContributedMethod>> allControllerMethods;
	private Map<String, Set<ContributedMethod>> allDomainMethods;
	
	
	// for testing, used to store extra dependency files
	// so that they remain in the cache after refreshes
	private List<IFile> extraPluginFiles = new ArrayList<IFile>();

	public PerProjectPluginCache() {
	}

	private void initializePluginData() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
    		allControllerProps = null;
    		allDomainProps = null;
    		allControllerMethods = null;
    		allDomainMethods = null;
    
    		dependencyPluginDataMap = new HashMap<String, GrailsPluginVersion>();
    
    		// re-parse the plugin descriptors
            DependencyData dependencyData = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class).getData();
            if (dependencyData != null) {
    	        Set<String> pluginDescriptorPaths = dependencyData.getPluginDescriptors();
                if (pluginDescriptorPaths != null) {
                    for (String path : pluginDescriptorPaths) {
                        GrailsPluginVersion data = parseData(path);
                        if (data != null) {
                            dependencyPluginDataMap.put(path, data);
                        }
                    }
                }
    
            }
            for (IFile file : extraPluginFiles) {
                PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
                GrailsPluginVersion data = parser.parse();
                dependencyPluginDataMap.put(file.getFullPath().toOSString(), data);
            }
        }
	}


	/**
	 * Removes the old cache and generates a new cache based on the latest
	 * dependency data that can be obtained for the given project
	 */
	public void refreshDependencyCache() {
		initializePluginData();
	}

	private GrailsPluginVersion parseData(String pluginDescriptor) {
		if (pluginDescriptor == null) {
			return null;
		}
		return new GrailsPluginParser(pluginDescriptor).parse();
	}

	public void dispose() {
		project = null;
        allControllerProps = null;
        allDomainProps = null;
        allControllerMethods = null;
        allDomainMethods = null;
        dependencyPluginDataMap.clear();
	}

	public IProject getProject() {
		return project;
	}

	public void projectChanged(GrailsElementKind[] changeKinds,
			IResourceDelta change) {
		boolean foundRelevantChange = false;
		for (GrailsElementKind changeKind : changeKinds) {
			if (changeKind == GrailsElementKind.PROJECT
					|| changeKind == GrailsElementKind.CLASSPATH) {
				foundRelevantChange = true;
				break;
			}
		}
		if (foundRelevantChange) {
			initializePluginData();
		}
	}

	public void setProject(IProject project) {
		this.project = project;
		initializePluginData();
	}

	public Map<String, ContributedProperty> getAllControllerProperties() {
		if (allControllerProps == null) {
			allControllerProps = new HashMap<String, ContributedProperty>();
			for (GrailsPluginVersion data : dependencyPluginDataMap.values()) {
				if (data.getControllerProperties() != null) {
					allControllerProps.putAll(data.getControllerProperties());
				}
			}
		}
		return allControllerProps;
	}

	public Map<String, ContributedProperty> getAllDomainProperties() {
		if (allDomainProps == null) {
			allDomainProps = new HashMap<String, ContributedProperty>();
			for (GrailsPluginVersion data : dependencyPluginDataMap.values()) {
				if (data.getDomainProperties() != null) {
					allDomainProps.putAll(data.getDomainProperties());
				}
			}
		}

		return allDomainProps;
	}

	public Map<String, Set<ContributedMethod>> getAllControllerMethods() {
		if (allControllerMethods == null) {
			allControllerMethods = new HashMap<String, Set<ContributedMethod>>();
			for (GrailsPluginVersion data : dependencyPluginDataMap.values()) {
				Map<String, Set<ContributedMethod>> newMethods = data
						.getControllerMethods();
				for (Entry<String, Set<ContributedMethod>> newMethod : newMethods
						.entrySet()) {
					Set<ContributedMethod> existing = allControllerMethods
							.get(newMethod.getKey());
					if (existing == null) {
						existing = new HashSet<ContributedMethod>();
						allControllerMethods.put(newMethod.getKey(), existing);
					}
					existing.addAll(newMethod.getValue());
				}
			}
		}
		return allControllerMethods;
	}

	public Map<String, Set<ContributedMethod>> getAllDomainMethods() {
		if (allDomainMethods == null) {
			allDomainMethods = new HashMap<String, Set<ContributedMethod>>();
			for (GrailsPluginVersion data : dependencyPluginDataMap.values()) {
				Map<String, Set<ContributedMethod>> newMethods = data
						.getDomainMethods();
				for (Entry<String, Set<ContributedMethod>> newMethod : newMethods
						.entrySet()) {
					Set<ContributedMethod> existing = allDomainMethods
							.get(newMethod.getKey());
					if (existing == null) {
						existing = new HashSet<ContributedMethod>();
						allDomainMethods.put(newMethod.getKey(), existing);
					}
					existing.addAll(newMethod.getValue());
				}
			}
		}
		return allDomainMethods;
	}

	public String getDependencyPluginDescriptor(BasePluginData data) {
		if (data == null) {
			return null;
		}
		for (Entry<String, GrailsPluginVersion> entry : dependencyPluginDataMap
				.entrySet()) {
			GrailsPluginVersion dependencyData = entry.getValue();
			if (dependencyData.getName() != null
					&& dependencyData.getName().equals(data.getName())) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Given a base plugin data will determine if that plugin data corresponds
	 * to an installed plugin, and if so, will return the plugin data containing
	 * contributed information.
	 */
	public GrailsPluginVersion getInstalled(BasePluginData pluginData) {
		if (pluginData == null) {
			return null;
		}
		Collection<GrailsPluginVersion> values = dependencyPluginDataMap.values();
		for (GrailsPluginVersion installedData : values) {
			if (installedData.getName().equals(pluginData.getName())) {
				return installedData;
			}
		}
		return null;
	}

	/**
	 * Returns a COPY of the cached list of dependencies. Note that this not
	 * recalculate dependencies. Dependencies must be refreshed separaterly in
	 * order to get the latest list of dependencies
	 * 
	 * @return non-null list of dependencies. May be empty.
	 */
	public Set<GrailsPluginVersion> getCachedDependencies() {
		return new LinkedHashSet<GrailsPluginVersion>(dependencyPluginDataMap.values());
	}

	/**
	 * Not API!!! for testing
	 */
	public void addExtraPluginFile(IFile file) {
	    extraPluginFiles.add(file);
	    initializePluginData();
	}
	
    /**
     * Not API!!! for testing
     */
	public void flushExtraPluginFiles() {
	    extraPluginFiles.clear();
	    initializePluginData();
	}

    /**
     * Caution do not remove any entries from this map.
     * @return the map from plugin file name to plugin data
     */
    public Map<String, GrailsPluginVersion> getPluginDataMap() {
        return dependencyPluginDataMap;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PerProjectPluginCache [project=");
        builder.append(project);
        builder.append(", dependencyPluginDataMap=");
        builder.append(dependencyPluginDataMap);
        builder.append(", allControllerProps=");
        builder.append(allControllerProps);
        builder.append(", allDomainProps=");
        builder.append(allDomainProps);
        builder.append(", allControllerMethods=");
        builder.append(allControllerMethods);
        builder.append(", allDomainMethods=");
        builder.append(allDomainMethods);
        builder.append("]");
        return builder.toString();
    }
}
