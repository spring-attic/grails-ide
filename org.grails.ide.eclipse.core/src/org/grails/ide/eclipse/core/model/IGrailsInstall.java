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
package org.grails.ide.eclipse.core.model;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.launching.IVMInstall;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;


/**
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public interface IGrailsInstall {
	
	String getName();
	
	String getHome();
	
	GrailsVersion getVersion();
	String getVersionString();
	
	File[] getDependencyClasspath();
	
	File[] getBootstrapClasspath();
	
	boolean isDefault();
	
	/**
	 * Perform a quick validity check on the install.
	 * @return OK_status if the install looks ok, and an error status with some error message indicating the nature of
	 * the problem.
	 */
	IStatus verify();
	
	/**
	 * @deprecated This method should not be used! It only works correctly if the work dir is
	 *    set from within STS, or if it is set to its default. It will return incorrect result
	 *    if a user uses some configuration file like settings.groovy to override the work dir's location.
	 *    <p>
	 *    The correct way to determine plugin home is by using Plugin
	 *    DependencyData from the {@link PerProjectDependencyDataCache}.
	 */
	String getPluginHome(IProject project);
	
	/**
	 * @deprecated This method should not be used! It only works correctly if the work dir is
	 *    set from within STS, or if it is set to its default. It will return incorrect result
	 *    if a user uses some configuration file like settings.groovy to override the work dir's location.
	 *    
	 * Also, it is probably not correct to consider workdir a property of a grails install since it is basically 
	 * something that can be set on the commandline (so technically may vary on a per-command execution basis, even
	 * for the same install.
	 *    
	 * @return Location of the ".grails" folder (typically in the user's home directory)
	 */
	String getGrailsWorkDir();

	/**
	 * @return Absolute File references to Spring loaded jar (if available, Grails 1.4 and up only). Otherwise
	 * returns null.
	 */
	File getSpringLoadedJar();

	/**
	 * Checks whether a given Java install is acceptable to run this Grails install with.
	 */
	void verifyJavaInstall(IVMInstall javaInstall) throws CoreException;

	/**
	 * A safe place where springloaded in grails can keep cache data.
	 */
	File getSpringLoadedCacheDir();
}
