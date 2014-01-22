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
package org.grails.ide.eclipse.explorer;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import org.grails.ide.eclipse.explorer.preferences.GrailsExplorerPreferences;

/**
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Kris De Volder
 */
public class GrailsExplorerPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.grails.ide.eclipse.explorer";

	// The shared instance
	private static GrailsExplorerPlugin plugin;

	private GrailsExplorerPreferences prefs;
	
	/**
	 * The constructor
	 */
	public GrailsExplorerPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static GrailsExplorerPlugin getDefault() {
		return plugin;
	}
	
	public GrailsExplorerPreferences getPreferences() {
		return GrailsExplorerPreferences.getInstance();
	}

}
