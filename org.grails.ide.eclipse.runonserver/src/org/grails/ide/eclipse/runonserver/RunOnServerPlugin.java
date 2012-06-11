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

import java.lang.reflect.Array;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 */
public class RunOnServerPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.grails.ide.eclipse.runonserver"; //$NON-NLS-1$

	// The shared instance
	private static RunOnServerPlugin plugin;

	/**
	 * The constructor
	 */
	public RunOnServerPlugin() {
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
	public static RunOnServerPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	///Custom code below
	// (All code above generate by Eclipse plugin creation wizard)
	
	
	// Location where we build war files for deployment, this area is cleared the first time it is
	// This is set to null until the area has been setup/cleared.
	private IPath stagingArea = null;
	
	/**
	 * Returns the location of a "staging" area that can be used to build war files and explode
	 * them. When this method is first called (for the current Workbench session) it ensures the
	 * staging area exists and clears it of any files left there by a previous session.
	 */
	public synchronized IPath getStagingArea() {
		if (stagingArea==null) { 
			IPath stateLocation = getStateLocation();
			IPath prepareArea = stateLocation.append("stage");
			FileUtils.deleteQuietly(prepareArea.toFile());
			if (prepareArea.toFile().mkdirs()) {
				stagingArea = prepareArea;
			} else {
				GrailsCoreActivator.log("Couldn't setup the Grails staging area at: "+prepareArea, null);
			}
		}
		return stagingArea;
	}
	
	// FIXADE This is not the right place for this method
	public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
	}
    public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
}
