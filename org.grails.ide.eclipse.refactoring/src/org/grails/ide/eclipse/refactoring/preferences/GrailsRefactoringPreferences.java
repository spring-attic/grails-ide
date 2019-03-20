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
package org.grails.ide.eclipse.refactoring.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.osgi.service.prefs.BackingStoreException;

import org.grails.ide.eclipse.refactoring.GrailsRefactoringActivator;

/**
 * Helper methods to retrieve and set GrailsRefactoring related preferences.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsRefactoringPreferences {

	public static final boolean DEFAULT_SUPPRESS_NON_GRAILS_AWARE_WARNING = false;
	public static final String SUPPRESS_NON_GRAILS_AWARE_WARNING = "org.grails.ide.eclipse.refactoring.preferences.SUPPRESS_GRAILS_AWARE_WARNING";
	
	public static void setSuppressNonGrailsAwareWarning(boolean suppress) {
		IEclipsePreferences prefs = GrailsRefactoringActivator.getEclipsePreferences();
		prefs.putBoolean(SUPPRESS_NON_GRAILS_AWARE_WARNING, suppress);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			GrailsCoreActivator.log(e);
		}
	}

	public static boolean getSuppressGrailsAwareWArning() {
		IEclipsePreferences prefs = GrailsRefactoringActivator.getEclipsePreferences();
		return prefs.getBoolean(SUPPRESS_NON_GRAILS_AWARE_WARNING, DEFAULT_SUPPRESS_NON_GRAILS_AWARE_WARNING);
	}

	public static boolean getWarnNonGrailsAware() {
		return !getSuppressGrailsAwareWArning();
	}
	
	public static void setWarnNonGrailsAware(boolean warn) {
		setSuppressNonGrailsAwareWarning(!warn);
	}

}
