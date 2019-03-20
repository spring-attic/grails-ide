/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.runtime.shared;

/**
 * Grails commands are executed by launching an external process. To interact with this process we have to add some
 * classes to that external process's classpath. These classes may have dependencies on grails jars that are only
 * present in the external Grails distribution, but not shipped with STS itself. 
 * <p>
 * It will cause "class not found" exceptions if any class running inside of STS tries to directly or indirectly
 * classload one of the classes in an external Grails jar.
 * <p>
 * This class contains some constants that should be shared between the external grails process and the code
 * executing inside of STS. As such, this class may be classloaded both inside and outside of STS and as such,
 * extreme care should be taken with what classes this class depends on, directly or indirectly. To keep this clear,
 * the only thing in this class should be obviously safe things such as literal String constants.
 * 
 * @author Kris De Volder
 * @since 2.6.1
 */
public class SharedLaunchConstants {

	public static final String DependencyExtractingBuildListener_CLASS = "org.grails.ide.eclipse.runtime.DependencyExtractingBuildListener";
	   // Note: it is tempting to use DependencyExtractingBuildListener.class.getName() but that would pull in unwanted dependencies!
	   // and cause "class not found exceptions".

	/**
	 * Name of the system property used to pass 'filename' to the external Grails process.
	 * 
	 * IMPORTANT: Try not to change the String value of this constant moving forward because Grails Maven support has a 'copy-pasted' version
	 * of this constant. So if it is changed here, it will break Grails Maven support.
	 */
	public static final String DEPENDENCY_FILE_NAME_PROP = "org.grails.ide.eclipse.dependencies.filename";
	
}
