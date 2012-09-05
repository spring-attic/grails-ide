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
package org.grails.ide.eclipse.runtime;

import grails.build.GrailsBuildListener;
import grails.util.BuildSettings;
import grails.util.PluginBuildSettings;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.groovy.grails.io.support.Resource;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.runtime.shared.DependencyFileFormat;
//import org.codehaus.groovy.grails.io.support.Resource;


/**
 * This is a replacement of the now obsolete {@link GrailsBuildSettings}.
 * <p>
 * It does the same thing as GrailsBuildSettings, but differs in the way it gets initialized.
 * <p>
 * Unlike GrailsBuildSettings, which is responsible for creating its own instance of {@link BuildSettings}this class expects an instance of {@link BuildSettings} from an external source (e.g. by hooking into
 * the {@link GrailsBuildListener} interface.
 * @author Kris De Volder
 * @since 2.5.0
 */
public class GrailsBuildSettingsDependencyExtractor {

	private BuildSettings settings = null;
	
	private boolean initialized = false;

	private Set<File> jarFiles;

	private String pluginsDirectory;

	private SortedSet<File> sourceFolders;

	private Set<File> pluginXmlFiles;
	
	public GrailsBuildSettingsDependencyExtractor(BuildSettings settings) {
		this.settings = settings;
	}

	private File getWorkDir() {
		return settings.getGrailsWorkDir();
	}
	
	private File getPluginClassesDir() {
		return settings.getPluginClassesDir();
	}

	public synchronized Set<File> getDependencies() throws IOException {
		if (!initialized) {
			calculateDependencies();
		}
		return jarFiles;
	}

	public synchronized String getPluginsDirectory() throws IOException {
		if (!initialized) {
			calculateDependencies();
		}
		return pluginsDirectory;
	}

	public synchronized Set<File> getPluginSourceFolders() throws IOException {
		if (!initialized) {
			calculateDependencies();
		}
		return sourceFolders;
	}

	public synchronized Set<File> getPluginXmlFiles() throws IOException {
		if (!initialized) {
			calculateDependencies();
		}
		return pluginXmlFiles;
	}

	private void calculateDependencies() throws IOException {

		jarFiles = new LinkedHashSet<File>();
		pluginXmlFiles = new LinkedHashSet<File>();
		sourceFolders = new TreeSet<File>();
		pluginsDirectory = settings.getProjectPluginsDir().getCanonicalPath();

		Resource[] pluginSources = GrailsPluginUtils.getPluginSourceFiles(settings.getProjectPluginsDir()
				.getAbsolutePath());
		for (Resource sourceFolder : pluginSources) {
			if (sourceFolder.getFile().toString().contains("{}")) {
				DependencyExtractingBuildListener.debug("SKIPPING strange plugin source folder: "+sourceFolder);
			} else {
				DependencyExtractingBuildListener.debug("Adding plugin source folder: "+sourceFolder);
				sourceFolders.add(sourceFolder.getFile());
			}
		}

		Resource[] plugins = GrailsPluginUtils.getPluginJarFiles(settings.getProjectPluginsDir().getAbsolutePath());
		for (Resource plugin : plugins) {
			jarFiles.add(plugin.getFile());
		}
		plugins = GrailsPluginUtils.getPluginJarFiles(settings.getGlobalPluginsDir().getAbsolutePath());
		for (Resource plugin : plugins) {
			jarFiles.add(plugin.getFile());
		}

		// make sure that we use the Ivy dependency resolution strategy for Grails >= 1.2
		String version = settings.getGrailsVersion();
		if (!(version.startsWith("1.0") || version.startsWith("1.1"))) {
			jarFiles.addAll(settings.getBuildDependencies());
			jarFiles.addAll(settings.getTestDependencies());
			jarFiles.addAll(settings.getProvidedDependencies());

			PluginBuildSettings pluginSettings = new PluginBuildSettings(settings);
			for (Resource resource : pluginSettings.getPluginJarFiles()) {
				jarFiles.add(resource.getFile());
			}

			for (Resource resource : pluginSettings.getPluginSourceFiles()) {
				File srcFolder = resource.getFile();
				if (srcFolder.toString().contains("{}")) {
					DependencyExtractingBuildListener.debug("SKIPPING strange source folder: "+srcFolder);
				} else if (srcFolder.isDirectory()) {
					DependencyExtractingBuildListener.debug("Adding source folder: "+srcFolder);
					//It seems we get passed stuff like '.gitignore' which is not actually a srcFolder. So
					//we check here that what we are adding is at least a directory.
					sourceFolders.add(resource.getFile());
				}
			}
			for (Resource resource : pluginSettings.getPluginXmlMetadata()) {
				File file = resource.getFile();
				if (file.toString().contains("{}")) {
					//Not sure why these entries are there, but they cause trouble! So skip them.
					DependencyExtractingBuildListener.debug("SKIPPING strange plugin.xml file: "+resource.getFile());
				} else {
					DependencyExtractingBuildListener.debug("Adding plugin.xml file: "+resource.getFile());
					pluginXmlFiles.add(file);
				}
			}
		}
		else {

			if (settings.getGrailsHome() != null) {
				File folder = new File(settings.getGrailsHome(), "dist");
				if (folder.exists() && folder.canRead() && folder.listFiles() != null) {
					for (File file : folder.listFiles()) {
						if (file.getName().endsWith(".jar")) {
							jarFiles.add(file);
						}
					}
				}

				folder = new File(settings.getGrailsHome(), "lib");
				if (folder.exists() && folder.canRead() && folder.listFiles() != null) {
					for (File file : folder.listFiles()) {
						if (file.getName().endsWith(".jar")) {
							jarFiles.add(file);
						}
					}
				}
			}

			if (settings.getBaseDir() != null) {
				File folder = new File(settings.getBaseDir(), "lib");
				if (folder.exists() && folder.canRead() && folder.listFiles() != null) {
					for (File file : folder.listFiles()) {
						if (file.getName().endsWith(".jar")) {
							jarFiles.add(file);
						}
					}
				}
			}
		}

		initialized = true;
	}

	/**
	 * Write out the dependency data to a file that can later be read by STS grails tooling to setup a project's
	 * classpath, linked source folders etc.
	 * 
	 * @param dependencyDescriptorName The name of the file to write to.
	 * @throws IOException 
	 */
	public void writeDependencyFile(String dependencyDescriptorName) throws IOException {
		File dependencyDescriptor = new File(dependencyDescriptorName);
		DependencyFileFormat.write(dependencyDescriptor, getDependencyData());
	}

	private DependencyData getDependencyData() throws IOException {
		return new DependencyData(
				//sources, 
				getPluginSourceFolders(),
				//dependencies, 
				getDependencies(),
				//workDir, 
				getWorkDir(),
				//pluginsDirectory, 
				new File(getPluginsDirectory()),
				//pluginDescriptors
				getPluginXmlFiles(),
				//pluginClassesDir
				getPluginClassesDir()
		);
	}

}
