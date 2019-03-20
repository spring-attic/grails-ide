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
package org.grails.ide.eclipse.refactoring;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class GrailsRefactoringActivator implements BundleActivator {

	private static final String PLUGIN_ID = "org.grails.ide.eclipse.refactoring";
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		GrailsRefactoringActivator.context = bundleContext;
		//This call is suspected of causing https://issuetracker.springsource.com/browse/STS-1853
		//by sometimes triggering a too early classload chain leading to intializing the ResourcesPlugin.
		//GrailsTypeRenameParticipant.startup(); 
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		GrailsRefactoringActivator.context = null;
	}

	public static IEclipsePreferences getEclipsePreferences() {
		return new InstanceScope().getNode(PLUGIN_ID);
	}
	
}
