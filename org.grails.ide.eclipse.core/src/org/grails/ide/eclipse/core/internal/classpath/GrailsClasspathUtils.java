/*******************************************************************************
 * Copyright (c) 2012-2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.frameworks.core.legacyconversion.IConversionConstants;


/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsClasspathUtils {

	private GrailsClasspathUtils() {
		// utils class
	}

	/**
	 * Creates a new path variable with the Eclipse workspace using {@link GrailsCoreActivator#PATH_VARIABLE_NAME} as
	 * name.
	 */
	@SuppressWarnings("deprecation")
	public static void createPathVariableIfRequired() throws CoreException {
		IPathVariableManager variableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
		if (variableManager.getValue(GrailsCoreActivator.PATH_VARIABLE_NAME) == null) {
			String userHomeProperty = System.getProperty("user.home");
            IPath userHome = new Path(userHomeProperty).append(".grails");
			try {
                variableManager.setValue(GrailsCoreActivator.PATH_VARIABLE_NAME, userHome);
            } catch (CoreException e) {
                GrailsCoreActivator.log("Error looking for Grails home.  Make sure that your 'user.home' system property is properly set.\n" +
                		"Current 'user.home' is: " + userHomeProperty, e);
            }
		}
	}

	/**
	 * Returns the {@link GrailsClasspathContainer} for the given <code>javaProject</code>.
	 * <p>
	 * This method returns <code>null</code> if no appropriate class path container could be found on the given project.
	 */
	public static GrailsClasspathContainer getClasspathContainer(IJavaProject javaProject) {
		try {
			if (hasClasspathContainer(javaProject)) {
				IClasspathContainer container = JavaCore.getClasspathContainer(
						GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH, javaProject);
				if (container instanceof GrailsClasspathContainer) {
					return (GrailsClasspathContainer) container;
				}
			}
		}
		catch (JavaModelException e) {
		}
		return null;
	}

	public static File getDependencyDescriptor(IProject project) {
		final String fileName = getDependencyDescriptorName(project);
		if (fileName!=null) {
			File file = new File(fileName);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public static String getDependencyDescriptorName(IProject project) {
		final GrailsCoreActivator grailsCore = GrailsCoreActivator.getDefault();
		//grailsCore can be null, sometimes, if we are being called during shutdown
		if (grailsCore!=null) {
			IPath path = grailsCore.getStateLocation().append(project.getName() + "-dependencies.txt");
			File file = path.toFile();
			try {
				return file.getCanonicalPath();
			}
			catch (IOException e) {
			}
		}
		return null;
	}

	public static String getDependencySourcesDescriptorName(IProject project) {
		final GrailsCoreActivator grailsCore = GrailsCoreActivator.getDefault();
		//grailsCore can be null, sometimes, if we are being called during shutdown
		if (grailsCore!=null) {
			IPath path = grailsCore.getStateLocation().append(project.getName() + "-sources.xml");
			File file = path.toFile();
			try {
				return file.getCanonicalPath();
			}
			catch (IOException e) {
			}
		}
		return null;
	}
	
	/**
	 * Returns <code>true</code> if the given project has the bundle dependency classpath container installed.
	 */
	public static boolean hasClasspathContainer(IJavaProject javaProject) {
		boolean hasContainer = false;
		try {
			for (IClasspathEntry entry : javaProject.getRawClasspath()) {
				if (entry.getPath().equals(GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH)) {
					hasContainer = true;
					break;
				}
			}
		}
		catch (JavaModelException e) {
		}
		return hasContainer;
	}
	/**
	 * Returns <code>true</code> if the given project has the OLD (pre-3.0.0) bundle dependency classpath container installed.
	 */
	public static boolean hasOldClasspathContainer(IJavaProject javaProject) {
	    boolean hasContainer = false;
	    try {
	        for (IClasspathEntry entry : javaProject.getRawClasspath()) {
	            if (entry.getPath().equals(IConversionConstants.GRAILS_OLD_CONTAINER)) {
	                hasContainer = true;
	                break;
	            }
	        }
	    }
	    catch (JavaModelException e) {
	    }
	    return hasContainer;
	}

}
