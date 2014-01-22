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
package org.grails.ide.eclipse.core;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * Properties testers for Grails related properties, extending IProject.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class ProjectPropertyTester extends PropertyTester {

	public ProjectPropertyTester() {
	}

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IProject project = asProject(receiver);
		if (project!=null) {
			//Other property that may be added here is "isGrailsPlugin". 
			if (property.equals("isGrailsApp")) {
				if (expectedValue==null) {
					expectedValue = true;
				}
				return expectedValue.equals(GrailsNature.isGrailsAppProject(project));
			}
		}
		return false;
	}

	private IProject asProject(Object receiver) {
		if (receiver instanceof IProject) {
			return (IProject)receiver;
		} else if (receiver instanceof IAdaptable) {
			return (IProject) ((IAdaptable)receiver).getAdapter(IProject.class);
		} else {
			return null;
		}
	}

}
