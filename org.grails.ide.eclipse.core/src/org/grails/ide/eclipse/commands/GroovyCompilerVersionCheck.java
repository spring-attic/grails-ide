/*******************************************************************************
 * Copyright (c) 2012-2013 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands;

import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.grails.ide.eclipse.core.model.GrailsVersion;

/**
 * Class that performs a basic version compatibility check between Grails/Groovy each time refresh dependencies is called.
 * @author Kris De Volder
 * @since 2.6.1
 */
public class GroovyCompilerVersionCheck {
	
	//Make this setable by system prop. That's a back door just in case it misbehaves users
	//can turn it off via 'secret' system prop. There shouldn't be a need to do this. But... 
	private static boolean ignore = Boolean.getBoolean("org.grails.ide.groovy.compiler.version.check");
	
	/**
	 * This method is only to be used in "test" mode and it supresses the compiler version
	 * checks from happening. (I.e. we will set the required groovy compiler version to 'unspecified'
	 * instead of an actual version.
	 */
	public static void testMode() {
		ignore = true;
	}
	
	/**
	 * This method is called every time refresh dependencies is called.
	 */
	public static void check(IJavaProject javaProject) {
		IProject project = javaProject.getProject();
		//since GGTS 3.3.M1 this check no nonger uses popup dialogs. Instead it creates appropriate
		// Greclipse metadata to specify the desired Groovy compiler version. 
		//This will cause Greclipse to show errors in the problems view.
		if (ignore) {
			CompilerUtils.setCompilerLevel(project, SpecifiedVersion.UNSPECIFIED);
		} else {
			SpecifiedVersion requiredVersion = getRequiredGroovyVersion(project);
			CompilerUtils.setCompilerLevel(project, requiredVersion);
		}
	}
	
	private static SpecifiedVersion getRequiredGroovyVersion(IProject project) {
		GrailsVersion gv = GrailsVersion.getEclipseGrailsVersion(project);
		return getRequiredGroovyVersion(gv);
	}

	public static SpecifiedVersion getRequiredGroovyVersion(GrailsVersion gv) {
		if (gv != null) {
			if (GrailsVersion.V_2_3_.compareTo(gv) <= 0) {
				return SpecifiedVersion._21;
			} else if (GrailsVersion.V_2_2_.compareTo(gv)<=0) {
				return SpecifiedVersion._20;
			} else if (GrailsVersion.V_2_0_.compareTo(gv) <= 0) {
				return SpecifiedVersion._18;
			} else {
				//Pre Grails 2.0
				return SpecifiedVersion._17;
			}
		}
		return SpecifiedVersion.UNSPECIFIED;
	}
	

}
