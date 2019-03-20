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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.runtime.shared.DependencyData;


/**
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 */
public class GrailsPluginUtil {

    private static final String BUILD_CONFIG_LOCATION = "grails-app/conf/BuildConfig.groovy";

    private GrailsPluginUtil() {
		// Util class
	}

	/**
	 * Returns true iff this file system location is inside the workspace
	 * @param fileSystemLocation
	 * @return
	 */
	public static boolean isLocationInWorkspace(String fileSystemLocation) {
		return findContainingProject(fileSystemLocation) != null;
	}

	/**
	 * Finds the corresponding project for a file system location, or null
	 * if it is outside the workspace.
	 * @param fileSystemLocation
	 * @return containing project, or null
	 */
	public static IProject findContainingProject(String fileSystemLocation) {
	    if (fileSystemLocation == null) {
	        return null;
	    }

	    // see if we can convert the fileSystemLocation to an IResource
	    URI location = new File(fileSystemLocation).toURI();
        
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource[] maybes = root.findContainersForLocationURI(location);
	    if (maybes.length == 0) {
	        maybes = root.findFilesForLocationURI(location);
	    }
	    if (maybes.length > 0) {
	        return maybes[0].getProject();
	    } else {
	        return null;
	    }
	}
	
    /**
     * Creates a dependency from plugin to dependent.  The assumptions are that:
     * 
     * <ul>
     * <li>Both projects are grails projects</li>
     * <li>plugin is a Plugin project</li>
     * <li>an dependency currently does not exist between the two projects</li>
     * </ul>
     * 
     * Does <em>not</em> call refresh dependencies after completing.
     * 
     * @param dependent
     * @param plugin
     * @throws AssertionFailedException if either project is not a grails project, or if plugin is not a plugin project
     * @throws CoreException if the BuildConfig.groovy file cannot be read
     */
    public static IStatus addPluginDependency(IProject dependent, IProject plugin) throws CoreException {
        return addPluginDependency(dependent, plugin, false);
    }
    
    /**
     * This method is not meant for public use.  Only for testing.  use {@link #addPluginDependency(IProject, IProject)} instead.
     * If doMangle is true, then add an extra space in the added text so that it cannot be removed.
     */
    public static IStatus addPluginDependency(IProject dependent, IProject plugin, boolean doMangle) throws CoreException {
        Assert.isTrue(GrailsNature.isGrailsProject(dependent), dependent.getName() + " is not a grails project");
        Assert.isTrue(GrailsNature.isGrailsPluginProject(plugin), plugin.getName() + " is not a grails plugin project");
        
        IFile buildConfigFile = dependent.getFile(BUILD_CONFIG_LOCATION);
        
        // next create the text to add to BuildConfig.groovy
        
        String textToAdd = createDependencyText(dependent, plugin, doMangle);
        
        if (!dependencyExists(buildConfigFile, textToAdd)) {
    	    InputStream stream = createInputStream(dependent, textToAdd);
            buildConfigFile.appendContents(stream, true, true, null);
            return Status.OK_STATUS;
        } else {
            return new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Could not add a dependency from " + dependent.getName() + " to in place plugin " + plugin.getName() + ". Try manually editing the BuildConfig.groovy file.");
        }
    }

    /**
     * Removes a dependency from plugin to dependent.  The assumptions are that:
     * 
     * <ul>
     * <li>Both projects are grails projects</li>
     * <li>plugin is a Plugin project</li>
     * <li>an dependency currently does exist between the two projects</li>
     * </ul>
     * 
     * Does <em>not</em> call refresh dependencies after completing.
     * 
     * @param dependent
     * @param plugin
     * @param doMangle 
     * @throws AssertionFailedException if either project is not a grails project, or if plugin is not a plugin project
     * @throws CoreException if the BuildConfig.groovy file cannot be read
     */
    public static IStatus removePluginDependency(IProject dependent, IProject plugin) throws CoreException {
        return removePluginDependency(dependent, plugin, false);
    }
    
    /**
     * This method is not meant for public use.  Only for testing.  use {@link #removePluginDependency(IProject, IProject)} instead.
     * If doMangle is true, then add an extra space in the text to remove so that previously mangled plugin entries can be removed
     */
    public static IStatus removePluginDependency(IProject dependent, IProject plugin, boolean doMangle) throws CoreException {
        Assert.isTrue(GrailsNature.isGrailsProject(dependent), dependent.getName() + " is not a grails project");
        Assert.isTrue(GrailsNature.isGrailsPluginProject(plugin), plugin.getName() + " is not a grails plugin project");
        
        IFile buildConfigFile = dependent.getFile(BUILD_CONFIG_LOCATION);
        
        String textToRemove = createDependencyText(dependent, plugin, doMangle);
        
        if (dependencyExists(buildConfigFile, textToRemove)) {
            char[] contents = ((CompilationUnit) JavaCore.create(buildConfigFile)).getContents();
            String newText = String.valueOf(contents).replace(textToRemove, "");
            InputStream stream = createInputStream(dependent, newText);
            buildConfigFile.setContents(stream, true, true, null);
            return Status.OK_STATUS;
        } else {
            return new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Could not remove a dependency from " + dependent.getName() + " to in place plugin " + plugin.getName() + ". Try manually editing the BuildConfig.groovy file.");
        }
    }

    public static boolean dependencyExists(IProject dependent, IProject plugin) throws CoreException {
        IFile buildConfigFile = dependent.getFile(BUILD_CONFIG_LOCATION);
        String dependencyText = createDependencyText(dependent, plugin, false);
        return dependencyExists(buildConfigFile, dependencyText);
    }

    private static boolean dependencyExists(IFile buildConfigFile,
            String dependencyText) throws CoreException {
        char[] contents = ((CompilationUnit) JavaCore.create(buildConfigFile)).getContents();
        return CharOperation.indexOf(dependencyText.toCharArray(), contents, true) >= 0;
    }

    private static InputStream createInputStream(IProject dependent,
            String textToAdd) throws CoreException {
        String encoding = JavaCore.create(dependent).getOption(JavaCore.CORE_ENCODING, true);
        InputStream stream;
        try {
            stream = new ByteArrayInputStream(encoding == null ? textToAdd
                    .getBytes() : textToAdd.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            throw new CoreException(new Status(IStatus.ERROR,
                    GrailsCoreActivator.PLUGIN_ID, IStatus.ERROR,
                    "failed to add plugin dependency", e));
        }
        return stream;
    }

    /**
     * Creates the text that will be added to the BuildConfig.groovy file
     * @param dependent the target grails project
     * @param plugin the plugin project that will be depended on
     * @param doMangle if true, add an extra space in the returned text
     * @return A string containing the "grails.plugin.location" property for the dependent project
     */
    private static String createDependencyText(IProject dependent,
            IProject plugin, boolean doMangle) {
        // if plugin path and dependent path are both in same directory use relative path
        // otherwise, use full path.
        IPath pathToPluginProject = plugin.getLocation();
        IPath pathToDependentProject = dependent.getLocation();
        boolean isColocated = pathToDependentProject.segmentCount() == pathToPluginProject.segmentCount() &&
                              pathToDependentProject.removeLastSegments(1).isPrefixOf(pathToPluginProject);
        
        String textToAdd = "grails.plugin.location." + maybeQuote(plugin.getName()) + (doMangle ? " " : "") + " = \"" + (isColocated ? "../" + pathToPluginProject.lastSegment() : pathToPluginProject.toPortableString()) + "\"\n";
        return textToAdd;
    }

    private static String maybeQuote(String name) {
        return name.contains("-") ? "'" + name + "'" : name;
    }
    
    /**
     * Returns the location of the plugins directory inside the .grails folder. This information is extracted from
     * our dependency data file. So to ensure this info is up-to-date the file must be up-to-date. The file gets generated 
     * by refreshDependencies, or by any GrailsCommand executed with "enableRefreshDependencyFile".
     * <p>
     * Callers of this method are responsible for ensuring that the file is up-to-date.
     * 
     * @param project
     */
    public static String getPluginsDirectory(IProject project) {
		PerProjectDependencyDataCache info = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class);
		if (info!=null) {
			DependencyData data = info.getData();
			if (data!=null) {
				return data.getPluginsDirectory();
			}
		}
		return null;
    }

    /**
     * The Grails work Directory as (for example) set on commandLine by -Dgrails.work.dir. Typically this
     * is a folder in the user's .grails folder. 
     * <p>
     * This information is extracted from our dependency data file. So to ensure this info is up-to-date the 
     * file must be up-to-date. The file gets generated by refreshDependencies (or by any GrailsCommand executed 
     * with "enableRefreshDependencyFile" turned on.
     * <p>
     * Callers of this method are responsible for ensuring that the file is up-to-date.
     */
	public static String getGrailsWorkDir(IProject project) {
		PerProjectDependencyDataCache info = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class);
		if (info != null) {
			DependencyData data = info.getData();
			if (data != null) {
				return data.getWorkDir();
			}
		}
		return null;
	}

	public static boolean isInPlacePluginDescriptor(String descriptor) {
		if (descriptor!=null) {
			URI location = new File(descriptor).toURI();
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource[] maybes = root.findContainersForLocationURI(location);
			if (maybes.length>0) {
				//It is a location in the workspace (or several locations :-0 
				//So maybe it is an inplace plugin... except if..
				for (IResource iResource : maybes) {
					if (iResource.getFullPath().toString().contains(SourceFolderJob.PLUGINS_FOLDER_LINK)) {
						//Stuff inside this special hidden linked folder are externally installed by Grails
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

}
