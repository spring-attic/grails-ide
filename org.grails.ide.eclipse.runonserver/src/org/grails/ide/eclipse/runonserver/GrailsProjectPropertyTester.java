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
package org.grails.ide.eclipse.runonserver;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * A property tester that test some grails-project related properties.
 * <p>
 * TODO: KDV: (cleanup) This is more generally useable/useful than just for "RunOnServer". 
 * This class and its accompanying snippet of xml in plugin.xml should be moved to grails core plugin. 
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @since 2.5.1
 */
public class GrailsProjectPropertyTester extends PropertyTester {
	
	private static final boolean DEBUG = false;
	
	private static final String IS_GRAILS_APP = "isGrailsApp";
	private void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}

	public GrailsProjectPropertyTester() {
	}

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (property.equals(IS_GRAILS_APP)) {
			debug("isGrailsApp? "+receiver.getClass() +" = "+receiver);
			IProject project = toProject(receiver);
			debug("adapted to IProject = "+project);
			boolean result = project!=null && GrailsNature.isGrailsAppProject(project);
			debug("isGrailsApp? => "+result);
			return result;
		} 
		return false;
	}

	private IProject toProject(Object receiver) {
		if (receiver instanceof IProject)
			return (IProject) receiver;
		else if (receiver instanceof IAdaptable) {
			IAdaptable adapable = (IAdaptable) receiver;
			return (IProject) adapable.getAdapter(IProject.class);
		} else {
			return null;
		}
	}

}
