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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.springsource.ide.eclipse.commons.core.SpringCoreUtils;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * This class is responsible for generating a complete list of Grails plugins
 * for a given project, including in-place plugins, as well as marking published
 * plugins as installed if they are installed in the given project.
 * <p>
 * A cached list of dependencies for the project is also kept, and it is
 * re-generated on every parsing operation. The cached list contains dependency
 * data converted into a plugin model representation, and includes in-place
 * plugin models.
 * </p>
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 */
public class GrailsPluginsListManager {

	public static final String NO_VERSION_ID = "";

	private IProject project;

	public static final String PLUGIN_NODE = "plugin";
	public static final String RELEASE_NODE = "release";
	public static final String TITLE_NODE = "title";
	public static final String AUTHOR_NODE = "author";
	public static final String DESCRIPTION_NODE = "description";
	public static final String DOCUMENTATION_NODE = "documentation";

	public static final String NAME_ATT = "name";
	public static final String LATEST_RELEASE_ATT = "latest-release";
	public static final String VERSION_ATT = "version";

	private static final String[] PREINSTALLED_PLUGINS = new String[] {
			"hibernate", "tomcat", "webflow" };

	public GrailsPluginsListManager(IProject project) {
		this.project = project;
	}

	public PerProjectPluginCache getDependencyCache() {
		PerProjectPluginCache cache = GrailsCore.get().connect(project,
				PerProjectPluginCache.class);
		return cache;
	}

	public Collection<GrailsPlugin> getDependenciesAsPluginModels() {
		return generateDependenciesAsPluginModels(null);
	}

	/**
	 * These are plugins that are preinstalled when a Grails project is created.
	 * These are dependent on the Grails version. Examples are hibernate and
	 * tomcat. Regardless of whether newer versions of these plugins exist,
	 * these plugins are always considered to be installed to the "latest"
	 * version. The are not marked as having update available, although a user
	 * can manually update them if necessary
	 * 
	 * @return list of preinstalled plugins in the project. Never null, although
	 *         may be empty
	 */
	public static Collection<String> getPreInstalledPlugins() {
		return new HashSet<String>(Arrays.asList(PREINSTALLED_PLUGINS));
	}

	/**
	 * Get the manager instance for the given project, if the project is a
	 * Grails project. Otherwise return null;
	 * 
	 * @param project
	 *            must be Grails project
	 * @return manager if project is a Grails project, or null
	 */
	public static GrailsPluginsListManager getGrailsPluginsListManager(
			IProject project) {
		if (GrailsNature.isGrailsProject(project)) {
			return new GrailsPluginsListManager(project);
		}
		return null;
	}

	/**
	 * Given a map of all plugins, merge plugin dependencies for the given
	 * project into the map, making sure that, among other things, installed
	 * versions and in-place plugins are added into the plugin map, as the
	 * plugin map should contain all published, in-place and installed plugin
	 * data.
	 * 
	 * @param pluginMap
	 *            if null, it will generate new plugin models for dependencies
	 *            instead of using existing models in the map
	 * @return list of dependency plugins converted to plugin models. Never
	 *         null, but may be empty
	 */
	protected List<GrailsPlugin> generateDependenciesAsPluginModels(
			Map<String, GrailsPlugin> pluginMap) {
		List<GrailsPlugin> cachedDependencies = new ArrayList<GrailsPlugin>();
		PerProjectPluginCache cache = getDependencyCache();
		if (cache != null) {
			for (GrailsPluginVersion dependency : cache.getCachedDependencies()) {

				// First obtain an existing plugin model
				GrailsPlugin dependencyModel = pluginMap != null ? pluginMap
						.get(dependency.getName()) : null;

				// If the plugin model doesn't exist it is most likely an
				// in-place plugin
				if (dependencyModel != null) {
					// The installed version MUST exist, unless it is an
					// in-place plugin
					PluginVersion installedVersion = dependencyModel
							.getVersion(dependency.getVersion());
					if (installedVersion != null) {
						installedVersion.setInstalled(true);
					}
				} else {
					dependencyModel = createPlugin(dependency);
			        dependencyModel.getLatestReleasedVersion().setInstalled(true);
			        
			        // Handle the in-place plugin addition
			        String descriptor = cache
			                .getDependencyPluginDescriptor(dependency);
			        if (GrailsPluginUtil.isInPlacePluginDescriptor(descriptor)) {
			            dependencyModel.setIsInPlace(true);
			        }
			        
					if (pluginMap != null) {
			            pluginMap.put(dependencyModel.getName(),
			                    dependencyModel);
			        }

				}
				cachedDependencies.add(dependencyModel);
			}
		}
		return cachedDependencies;
	}

    /**
     * @param pluginMap
     * @param cache
     * @param dependency
     * @return
     */
    private GrailsPlugin createPlugin(
            GrailsPluginVersion dependency) {
        GrailsPlugin dependencyModel = new GrailsPlugin(dependency.getName());
        // create a version element to represent the single
        // version of an in-place plugin
        PluginVersion version = new PluginVersion(dependency);

        // Add the dependency as a version, usually inplace only
        // have one version: the inplace plugin itself
        addVersion(version, dependencyModel);

        // The single version is also the latest "released" version
        dependencyModel.setLatestReleasedVersion(version);

        return dependencyModel;
    }

	/**
	 * Parse the list of all available Grails plugins, including all published
	 * plugins as well as in-place and installed plugins for the given project.
	 * <p>
	 * If 'aggressive' is true, it will force the execution of 'list-plugins'. 
	 * Otherwise it will try to use existing plugin list files first.  
	 * 
	 * @return parsed list of plugins, or null if parsed failed
	 */
	public Collection<GrailsPlugin> generateList(boolean aggressive) {

		File[] pluginsFiles = getPluginsListFiles(); 
		if (aggressive && pluginsFiles!=null) {
			for (File file : pluginsFiles) {
				file.delete();
			}
			pluginsFiles = null;
		}

		// The files may not be there therefore re-generate them by
		// running "list-plugins" command
		if (pluginsFiles == null || pluginsFiles.length == 0) {
			//If aggressive, then pluginFiles will be null so this code will always run.
			try {
				GrailsCommand cmd = GrailsCommandFactory.listPlugins(project);
				cmd.enableRefreshDependencyFile(); // must make sure we have dependency data, it tells us where the pluginsFolder is!
				cmd.synchExec();
				GrailsCore.get().connect(project, PerProjectDependencyDataCache.class).refreshData();
				pluginsFiles = getPluginsListFiles();
			} catch (CoreException e) {
				GrailsCoreActivator.log(e);
			}
		}
		
		if (pluginsFiles != null && pluginsFiles.length > 0) {
			Map<String, GrailsPlugin> publishedPluginsMap = new HashMap<String, GrailsPlugin>();
			for (File pluginsFile : pluginsFiles) {
				if (pluginsFile.exists() && pluginsFile.canRead()) {
					parseLocation(pluginsFile, publishedPluginsMap);
				}
			}

		     // now include in place plugins from the workspace
			addInPlacePluginsFromWorkspace(publishedPluginsMap);
			
			// Now merge any dependency information from the project, including
			// in-place plugins
			generateDependenciesAsPluginModels(publishedPluginsMap);
			return publishedPluginsMap.values();
		} else {
			GrailsCoreActivator.log("Unable to find or generate plugins list in: "+GrailsPluginUtil.getGrailsWorkDir(project), null);
		}

		return null;
	}

	/**
     * @param publishedPluginsMap
     */
    private void addInPlacePluginsFromWorkspace(
            Map<String, GrailsPlugin> publishedPluginsMap) {
        IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : allProjects) {
            if (GrailsNature.isGrailsPluginProject(project) && !project.equals(this.project)) {
                IPath pluginPath = GrailsNature.createPathToPluginXml(project);
                GroovyPluginParser parser = new GroovyPluginParser(pluginPath);
                GrailsPluginVersion data = parser.parse();
                
                // only add if data doesn't already exists (ie- do not overwrite
                // existing published plugin
                if (data != null && !publishedPluginsMap.containsKey(data.getName())) {
                    GrailsPlugin plugin = createPlugin(data);
                    plugin.setIsInPlace(true);
                    publishedPluginsMap.put(plugin.getName(), plugin);
                }
            }
        }
    }

    /**
	 * Get all the files the xml files that contain plugin lists. This operation may fail and
	 * return null a number of reasons:
	 *   - the plugins directory could not be determined because the project's dependency data has not yet been generated
	 *   - the plugins files have not yet been generated by grails.
	 *   
	 * @param pluginsFolder
	 * @return
	 */
	private File[] getPluginsListFiles() {
		File[] pluginsFiles;
		pluginsFiles = null;
		String pluginsDirectory = GrailsPluginUtil.getGrailsWorkDir(project);
		if (pluginsDirectory!=null) {
			File pluginsFolder = new File(pluginsDirectory);
			if (pluginsFolder.exists() && pluginsFolder.isDirectory()
					&& pluginsFolder.canRead()) {
				pluginsFiles = pluginsFolder.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {

						return name.startsWith("plugins-list-")
								&& name.endsWith(".xml");
					}
				});
			}
		}
		return pluginsFiles;
	}
	
	/**
	 * Given a plugin descriptor location, parse the contents and create plugin
	 * models for each plugin that is parsed.
	 * 
	 * @param file
	 * @param parsedPublishedPluginData
	 */
	protected void parseLocation(File file,
			Map<String, GrailsPlugin> parsedPublishedPluginData) {

		if (file == null || !file.exists() || parsedPublishedPluginData == null) {
			return;
		}
		try {
			DocumentBuilder docBuilder = SpringCoreUtils.getDocumentBuilder();
			Document doc = docBuilder.parse(file);
			NodeList binaryNodes = doc.getElementsByTagName(PLUGIN_NODE);
			for (int i = 0; i < binaryNodes.getLength(); i++) {
				Node pluginNode = binaryNodes.item(i);

				// The parent node is the latest version, therefore create a
				// model for it
				// first. The information may be incomplete, so keep the
				// reference until
				// the children of this node are parsed in which case the latest
				// version
				// will be added in the correct order in the list of verison as
				// well
				// as any additional information that is missing will also be
				// added
				PluginVersion latestVersion = createLatestReleasedVersionFromParentNode(pluginNode);

				// If no version can be generated for this node skip it as
				// something
				// may have gone wrong.
				if (latestVersion == null) {
					continue;
				}

				// Create the plugin model that contains all the versions,
				// including the latest
				// version
				String pluginName = latestVersion.getName();

				GrailsPlugin parentPlugin = parsedPublishedPluginData
						.get(pluginName);
				if (parentPlugin == null) {
					parentPlugin = new GrailsPlugin(pluginName);
				}

				NodeList childNodes = pluginNode.getChildNodes();
				for (int j = 0; j < childNodes.getLength(); j++) {
					Node releaseNode = childNodes.item(j);
					if (RELEASE_NODE.equals(releaseNode.getNodeName())) {
						addVersion(releaseNode, parentPlugin, latestVersion);
					}
				}

				parentPlugin.setLatestReleasedVersion(latestVersion);
				parsedPublishedPluginData.put(parentPlugin.getName(),
						parentPlugin);
			}

		} catch (SAXException e) {
			GrailsCoreActivator.log(e);

		} catch (IOException e) {
			GrailsCoreActivator.log(e);

		}
	}

	/**
	 * Creates a version model from the parent node, but does not add it to the
	 * list of plugins for the container plugin model yet.
	 * 
	 * @param pluginNode
	 * @return
	 */
	protected PluginVersion createLatestReleasedVersionFromParentNode(
			Node pluginNode) {
		if (pluginNode == null || !PLUGIN_NODE.equals(pluginNode.getNodeName())) {
			return null;
		}
		Node nameNode = pluginNode.getAttributes().getNamedItem(NAME_ATT);
		Node latestReleaseNode = pluginNode.getAttributes().getNamedItem(
				LATEST_RELEASE_ATT);

		if (nameNode == null) {
			return null;
		}

		String name = nameNode.getTextContent();

		// plugin data must contain a plugin name
		if (name == null) {
			return null;
		}

		PluginVersion latestVersion = new PluginVersion();
		latestVersion.setName(name);

		String versionID = null;
		if (latestReleaseNode != null) {
			versionID = latestReleaseNode.getTextContent();
		}

		setVersionID(latestVersion, versionID);

		return latestVersion;
	}

	/**
	 * Add a version extracted from given node, and return it if succesfully
	 * added, or return null if the version was not added (either invalid node,
	 * or version already exists). It also updates an existing version if the
	 * plugin model already contains the version.
	 * <p>
	 * Note that the latest version must be created first as it is the parent
	 * node in the XML structure. The latest version is therefore added to the
	 * list of versions ONLY when the corresponding child version is
	 * encountered, and additional information about the latest version is
	 * parsed.
	 * </p>
	 * 
	 * @param releaseNode
	 *            must not be null
	 * @param parentPlugin
	 *            must not be null
	 * @param latestReleasedVersion
	 *            must not be null. It is the parent node which always lists the
	 *            latest version
	 * @return added version, or null if nothing is added
	 */
	protected PluginVersion addVersion(Node releaseNode,
			GrailsPlugin parentPlugin, PluginVersion latestReleasedVersion) {
		if (releaseNode == null || parentPlugin == null
				|| !RELEASE_NODE.equals(releaseNode.getNodeName())
				|| latestReleasedVersion == null) {
			return null;
		}

		String versionID = null;
		Node att = releaseNode.getAttributes().getNamedItem(VERSION_ATT);
		if (att != null) {
			versionID = att.getTextContent();
		}

		// check if an update is occuring
		PluginVersion versionToAdd = parentPlugin.getVersion(versionID);
		boolean isLatestVersionBeingAdded = latestReleasedVersion.getVersion()
				.equals(versionID);

		if (versionToAdd == null) {
			// Check if the latest version has been encountered in the list of
			// children. If so
			// use the latest version instead of creating a new version.

			if (isLatestVersionBeingAdded) {
				versionToAdd = latestReleasedVersion;
			} else {
				versionToAdd = new PluginVersion();
				setVersionID(versionToAdd, versionID);
			}

			addVersion(versionToAdd, parentPlugin);
		}

		NodeList list = releaseNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			String name = child.getNodeName();
			String value = child.getTextContent();
			if (TITLE_NODE.equals(name)) {
				versionToAdd.setTitle(value);
			} else if (AUTHOR_NODE.equals(name)) {
				versionToAdd.setAuthor(value);
			} else if (DESCRIPTION_NODE.equals(name)) {
				versionToAdd.setDescription(reformatDescription(value));
			} else if (DOCUMENTATION_NODE.equals(name)) {
				versionToAdd.setDocumentation(value);
			}
		}

		return versionToAdd;
	}

	protected void setVersionID(PluginVersion version, String versionID) {
		version.setVersion(versionID != null ? versionID : NO_VERSION_ID);
	}

	/**
	 * Adds the given version at the end of the list of plugins for the parent
	 * plugin, and returns true if successfully added. It also creates
	 * relationships between the version and the parent. Returns false if
	 * version was not added (either version is null, parent is null, or version
	 * already exists in the list)
	 * 
	 * @param version
	 * @param parent
	 * @return true if successfully added. False otherwise
	 */
	protected boolean addVersion(PluginVersion version, GrailsPlugin parent) {
		if (version == null || parent == null
				|| parent.getVersions().contains(version)) {
			return false;
		}

		version.setName(parent.getName());
		return parent.addVersion(version);
	}

	public static boolean equals(String versionID1, String versionID2) {
		if (versionID1 != null) {
			return versionID1.equals(versionID2);
		} else if (versionID2 != null) {
			return false;
		} else {
			return true;
		}
	}

	protected String reformatDescription(String description) {

		if (description == null) {
			return null;
		}

		description = description.trim();
		StringBuffer descriptionBuffer = new StringBuffer(description);
		for (; descriptionBuffer.length() > 0;) {
			char charVal = description.charAt(0);
			if (charVal == '\\' || Character.isWhitespace(charVal)) {
				descriptionBuffer.deleteCharAt(0);
			} else {
				break;
			}
		}
		description = descriptionBuffer.toString();
		return description;
	}

}
