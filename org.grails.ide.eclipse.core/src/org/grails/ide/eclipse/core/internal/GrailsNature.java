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
package org.grails.ide.eclipse.core.internal;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsNature implements IProjectNature {
	
	public static final String NATURE_ID = GrailsCoreActivator.PLUGIN_ID+".nature";
	public static final String OLD_NATURE_ID = GrailsCoreActivator.OLD_PLUGIN_ID + ".nature";
	
	private IProject project;
	
	public void configure() throws CoreException {
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return this.project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	
	/**
	 * Determine whether a given project 'looks like' a Grails project based 
	 * on its contents, without relying on eclipse metadata.
	 */
	public static boolean looksLikeGrailsProject(IProject project) {
		if (project!=null && project.isAccessible()) {
			return project.getFolder("grails-app").exists()
					&& project.getFile("application.properties").exists();
		}
		return false;
	}

	/**
	 * This expresses the same logic as looksLikeGrailsProject(IProject) but for
	 * a situation where we have java.io.File rather than an Eclipse project.
	 * (typically, this is because we are examining the project before it 
	 * has been imported so it doesn't yet exist in the workspace.
	 */
	public static boolean looksLikeGrailsProject(File project) {
		if (project!=null && project.isDirectory()) {
			return new File(project, "grails-app").isDirectory()
					&& new File(project, "application.properties").isFile();
		}
		return false;
	}

	public static boolean isGrailsProject(IProject project) {
		try {
			return project != null
					&& project.isAccessible()
					&& project.hasNature(GrailsNature.NATURE_ID);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
		return false;
	}
	
	public static boolean isGrailsPluginProject(IProject project) {
	    if (!isGrailsProject(project)) {
	        return false;
	    }
	    
	    String pluginName = createPluginName(project.getName());
	    return project.getFile(pluginName).exists();
	}

	public static boolean isGrailsAppProject(IProject project) {
		return isGrailsProject(project) && !isGrailsPluginProject(project);
	}
	
    /**
     * @param name
     * @return
     */
    public static String createPluginName(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid grails project name");
        }
        
        StringBuilder sb = new StringBuilder();
        char[] nameArr = name.toCharArray();
        
        sb.append(Character.toUpperCase(nameArr[0]));
        
        boolean prevWasDash = false;
        for (int i = 1; i < nameArr.length; i++) {
            if (nameArr[i] == '-') {
                prevWasDash = true;
            } else {
                if (prevWasDash) {
                    sb.append(Character.toUpperCase(nameArr[i]));
                } else {
                    sb.append(nameArr[i]);
                }
                prevWasDash = false;
            }
        }
        sb.append("GrailsPlugin.groovy");
        return sb.toString();
    }

    public static IPath createPathToPluginXml(IProject project) {
        return project.getLocation().append(createPluginName(project.getName()));
    }

    public static boolean hasOldGrailsNature(IProject project) {
        try {
            return project != null
                    && project.isAccessible()
                    && project.hasNature(GrailsNature.OLD_NATURE_ID);
        } catch (CoreException e) {
            GrailsCoreActivator.log(e);
        }
        return false;
    }

}
