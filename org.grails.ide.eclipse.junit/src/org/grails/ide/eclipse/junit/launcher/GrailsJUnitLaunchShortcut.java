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
package org.grails.ide.eclipse.junit.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;



/**
 * An adaptation of JUnitLaunchShortcut to run Grails "test-app" instead of regular 
 * Java Junit tests.
 * @author Kris De Volder
 */
public class GrailsJUnitLaunchShortcut extends JUnitLaunchShortcut {
	
	/**
	 * Hack... change this flag to true to switch from running with Grails test-app to
	 * simply running the JDT JUnit runner and log all the messages it sends back
	 * to System.err for reverse engineering purposes. This flag should never 
	 * be set to true in committed code!
	 */
	public static final boolean SPYING_MODE = false;

	/**
	 * Default constructor.
	 */
	public GrailsJUnitLaunchShortcut() {
	}
	
	@Override
	protected String getLaunchConfigurationTypeId() {
		return GrailsJUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION;
	}
	
	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
			IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);
		//KDV: This is in the wrong place, what if we created a test launch not via this shortcut but via the
		//  regular Launch UI? Then the configuration classpath and other stuff will not be right!
		if (!SPYING_MODE) {
			//This changes the class path and other stuff needed for running grails 
			//command line 
			GrailsLaunchArgumentUtils.prepareLaunchConfigurationWithProject(wc);
		}
		return wc;
	}

}
