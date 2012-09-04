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
package org.grails.ide.eclipse.commands;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.grails.ide.eclipse.core.internal.IgnoredProjectsList;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.osgi.framework.Version;


/**
 * Class that performs a basic version compatibility check between Grails/Groovy each time refresh dependencies is called.
 * @author Kris De Volder
 * @since 2.6.1
 */
public class GroovyCompilerVersionCheck {

	/**
	 * The actual dialog related code belongs in the UI plugin so this an interface here.
	 * 
	 * @author Kris De Volder
	 * @since 2.6.1
	 */
	public static interface IGroovyCompilerVersionCheckDialog {
		void openMessage(IProject project, GrailsVersion grailsVersion, VersionRange requiredGroovyVersion, Version actualGroovyVersion);
	}

	private static final VersionRange VERSION_RANGE_V_1_7 = new VersionRange("[1.7,1.8)");
	private static final VersionRange VERSION_RANGE_1_8 = new VersionRange("[1.8,2.0)");
	private static final VersionRange VERSION_RANGE_2_0 = new VersionRange("2.0");
	
	private static boolean testMode = false;
	private static IGroovyCompilerVersionCheckDialog dialogProvider;
	
	private static Set<GrailsVersion> seen = new HashSet<GrailsVersion>();
	
	/**
	 * This method is only to be used in "test" mode and sets the Dialog provider to something that doesn't
	 * actually pop-up dialog. This provider should always have priority over any other provider provisioned
	 * by the UI plugin.
	 * 
	 * @param ignore: if set to true, compiler version discrepancies will be ignored. If set to
	 *  false, they will result in runtime exceptions instead.
	 */
	public static synchronized void testMode(final boolean ignore) {
		testMode = true;
		dialogProvider = new IGroovyCompilerVersionCheckDialog() {
			/**
			 * In test mode, we will make this throw an exception instead of opening a dialog. If the test environment is setup with the
			 * correct Groovy compiler version this method should never get called. 
			 */
			public void openMessage(IProject project, GrailsVersion grailsVersion, VersionRange requiredGroovyVersion, Version actualGroovyVersion) {
				Error error = new Error("Using Grails "+grailsVersion+" detected wrong Groovy compiler version: required = "+requiredGroovyVersion+ " actual = "+actualGroovyVersion);
				if (ignore) {
				//	GrailsCoreActivator.log(error);
				} else {
					throw error;
				}
			}
		};
	}
	
	public static void testMode() {
//		boolean ignore = GrailsVersion.MOST_RECENT.equals(GrailsVersion.V_2_0_4_revisit) //Revisit decision to turn this check of on next release (i.e. when both previous and current release use Groovy 1.8
//				|| GrailsVersion.MOST_RECENT.equals(GrailsVersion.BUILDSNAPHOT_2_0_2);
		boolean ignore = true;
		testMode(ignore);
	}
	
	/**
	 * This method should be called by the UI plugin providing the actual dialog that shows the warning messages to the user.
	 */
	public static synchronized void setDialogProvider(IGroovyCompilerVersionCheckDialog theProvider) {
		//In test mode we are counting on the testMode method to override the provider so we ignore any other attempts to set it.
		if (!testMode) {
			Assert.isLegal(dialogProvider==null); //provider can only be set once
			dialogProvider = theProvider;
		}
	}

	/**
	 * This method is called every time refresh dependencies is called.
	 */
	public static void check(IJavaProject javaProject) {
		if (IgnoredProjectsList.isIgnorable(javaProject.getProject())) {
			return;
		} else {
			IProject project = javaProject.getProject(); 
			GrailsVersion grailsVersion = GrailsVersion.getEclipseGrailsVersion(project);
			if (seen.contains(grailsVersion)) { 
				//if already seen we can skip the rest since no message will be shown anyway.
			} else {
				VersionRange requiredGroovyVersion = getRequiredGroovyVersion(project);
				if (requiredGroovyVersion!=null) {
					Version actualGroovyVersion = new Version(CompilerUtils.getGroovyVersion());
					if (!requiredGroovyVersion.isIncluded(actualGroovyVersion)) {
						messageDialog(project, grailsVersion, requiredGroovyVersion, actualGroovyVersion);
					}
				}
			}
		}
	}
	
	private static synchronized void messageDialog(IProject project, GrailsVersion grailsVersion, VersionRange requiredGroovyVersion, Version actualGroovyVersion) {
		if (dialogProvider!=null) {
			if (!alreadyShown(grailsVersion)) {
				dialogProvider.openMessage(project, grailsVersion, requiredGroovyVersion, actualGroovyVersion);
			}
		}
	}

	/**
	 * @return true the first time it is called for any grails version and false the next 
	 * time it is called for a version it was called with before.
	 */
	private static boolean alreadyShown(GrailsVersion grailsVersion) {
		if (!seen.contains(grailsVersion)) {
			seen.add(grailsVersion);
			return false;
		}
		return true;
	}

	private static VersionRange getRequiredGroovyVersion(IProject project) {
		GrailsVersion gv = GrailsVersion.getEclipseGrailsVersion(project);
		if (gv != null) {
			if (GrailsVersion.V_2_2_.compareTo(gv)<=0) {
				return VERSION_RANGE_2_0;
			} else if (GrailsVersion.V_2_0_.compareTo(gv) <= 0) {
				//Grails 2.0 or higher requires a 1.8 Greclipse compiler
				return VERSION_RANGE_1_8;
			} else {
				//Pre Grails 2.0
				return VERSION_RANGE_V_1_7;
			}
		}
		return null;
	}
	

}
