/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal.classpath;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class GroovyPluginParser implements IPluginParser {

	private IPath pluginPath;
	private IProject project;

	/**
	 * The path should either point to a Grails plugin groovy file or be a
	 * directory path that exists and that may contain the Grails plugin groovy
	 * file.
	 * 
	 * @param pluginPath
	 */
	public GroovyPluginParser(IPath pluginPath) {
		this.pluginPath = pluginPath;
	}

	public GrailsPluginVersion parse() {

		URL url = getScriptLocation();
		if (url != null) {
			try {
				GrailsPluginVersion data = new GrailsPluginVersion();
				new GrailsConfigSlurper().parse(url, data);
				IProject project = getInPlaceProject();
				if (project != null) {
					data.setName(project.getName());
				}
				return data;
			} catch (Throwable e) {
				// If anything fails with Groovy, return null;
				GrailsCoreActivator.log(e);
			}
		}
		return null;
	}

	protected URL getScriptLocation() {
		if (pluginPath == null) {
			return null;
		}

		// if the plugin path points to a groovy file, assume
		// it is the script path
		IPath scriptPath = null;

		if (isGrailsPluginFile(pluginPath)) {
			scriptPath = pluginPath;
		} else if (Platform.getLocation().isPrefixOf(pluginPath)) {
			// If the caller passed a proper path, this should be the project
			// path
			IProject project = getInPlaceProject();
			if (project != null && project.isAccessible()) {
				try {
					IResource[] members = project.members();
					if (members != null) {
						for (IResource resource : members) {
							if (resource.exists()
									&& resource.getType() == IResource.FILE) {
								IPath possiblePath = resource.getLocation();
								if (isGrailsPluginFile(possiblePath)) {
									scriptPath = possiblePath;
								}
							}
						}
					}
				} catch (CoreException e) {
					GrailsCoreActivator.log(e);
				}
			}
		}

		if (scriptPath != null) {
			try {
				URL scriptURL = scriptPath.toFile().toURI().toURL();
				return FileLocator.toFileURL(scriptURL);
			} catch (MalformedURLException e) {
				GrailsCoreActivator.log(e);
			} catch (IOException e) {
				GrailsCoreActivator.log(e);
			}
		}

		return null;

	}

	protected boolean isGrailsPluginFile(IPath path) {
		File file = path.toFile();
		String fileExtension = path.getFileExtension();
		if (file.exists() && file.isFile() && fileExtension != null
				&& fileExtension.equals("groovy")
				&& path.lastSegment().contains("GrailsPlugin")) {
			return true;
		}
		return false;
	}

	protected IProject getInPlaceProject() {
		if (project == null) {
			IPath path = pluginPath;
			if (path == null) {
				return null;
			}

			File file = path.toFile();
			if (file.isFile()) {
				path = path.removeLastSegments(1);
			}
			String possibleProjectName = path.lastSegment();
			IProject possibleProject = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(possibleProjectName);
			if (possibleProject != null && possibleProject.isAccessible()) {
				this.project = possibleProject;
			}
		}
		return project;
	}

	public static class GrailsConfigSlurper {
		public void parse(URL url, GrailsPluginVersion data) {
			GroovyClassLoader classLoader = new GroovyClassLoader();
			try {
				Class<?> clazz = classLoader
						.parseClass(new File(url.getFile()));

				if (clazz != null) {

					Object configObj = clazz.newInstance();

					if (configObj instanceof GroovyObject) {
						GroovyObject groovyObject = (GroovyObject) configObj;

						data.setAuthor(getProperty(groovyObject, "author"));
						data.setDescription(getProperty(groovyObject,
								"description"));
						data.setTitle(getProperty(groovyObject, "title"));
						data.setVersion(getProperty(groovyObject, "version"));
						data.setRuntimeVersion(getProperty(groovyObject,
								"grailsVersion"));
						// do not show the documentation link since it will be broken
//						data.setDocumentation(getProperty(groovyObject,
//						        "documentation"));
					}
				}
			} catch (Throwable e) {
				GrailsCoreActivator.log(e);
			}
		}

		protected String getProperty(GroovyObject groovyObject,
				String propertyName) {
			try {
				Object propObj = groovyObject.getProperty(propertyName);
				if (propObj instanceof String) {
					return (String) propObj;
				}
			} catch (Throwable e) {
				GrailsCoreActivator.log(e);
			}
			return null;
		}
	}

}
