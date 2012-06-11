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
package org.grails.ide.eclipse.core.launch;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * This class provides a mechanism to listen for the termination of a Process spawned by a Launch. To be able
 * to function, this infrastructure requires the cooperation of the LaunchConfigurationDelegate to check whether
 * a listener should be added to launch sometime between the time when the Launch is created, and when it
 * is actually launched.
 * @author Kris De Volder
 */
public class LaunchListenerManager {

	/**
	 * This utility requires the cooperation of a LaunchConfigurationDelegate to register the listener at
	 * the appropriate time during the launch procedure. This set contains the Ids of the launch configuration
	 * types of LaunchConfiguration delegates that have promised they provide this support.
	 */
	private static Set<String> supportedLaunchTypes = new HashSet<String>();

	/**
	 * This map is used by launchWithListener to temporarily associate a Listener with a launch configuration.
	 * This is not a nice way to do that, but there seems to be no other way to add an object instance as a 
	 * property of the LaunchConfiguration because it only supports simple attribute types that can be stored
	 * into XML files.
	 */
	private static Map<ILaunchConfiguration, AbstractLaunchProcessListener> listenerMap = new IdentityHashMap<ILaunchConfiguration, AbstractLaunchProcessListener>();

	private static void removeLaunchListener(ILaunchConfiguration conf, AbstractLaunchProcessListener listener) {
		synchronized (listenerMap) {
			Assert.isTrue(listenerMap.get(conf)==listener);
			listenerMap.remove(conf);
		}
	}

	/**
	 * LaunchConfigurationDelegates that are registered as supporting the LaunchListenerManager infrastructure
	 * must call this method during the launch sequence to retrieve any listener associated with the
	 * launch configuration and call the Listener's init method.
	 */
	public static AbstractLaunchProcessListener getLaunchListener(ILaunchConfiguration configuration) {
		synchronized (listenerMap) {
			return listenerMap.get(configuration);
		}
	}

	
	/**
	 * Launch the given launch configuration ensuring that the listener gets associated with
	 * this launch configuration once it gets launched. It is more or less assumed that
	 * launch configurations will only by launched once (or at least not concurrently with themselves). 
	 * Should this assumption get violated this will be detected and an AssertionFailedException
	 * will be raised.
	 */
	public static AbstractLaunchProcessListener launchWithListener(ILaunchConfiguration launchConf, SynchLaunch synchLaunch, AbstractLaunchProcessListener launchListener) throws CoreException {
		synchronized (listenerMap) {
			Assert.isTrue(isSupported(launchConf.getType()));
			Assert.isTrue(listenerMap.get(launchConf)==null, "Concurrent reuse of a LaunchConfiguration");
								// Concurrent reuse is not allowed, since this will make it impossible to
								// reliably associate a listener with a given Launch of the configuration.
								// This should not be a problem, since we are only creating "run once" configurations to
								// execute Grails commands.
			listenerMap.put(launchConf, launchListener);
		}
		try {
			launchConf.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor(), synchLaunch.isDoBuild(), synchLaunch.isShowOutput());
		}
		finally {
			removeLaunchListener(launchConf, launchListener);
		}
		return launchListener;
	}
	
	public static boolean isSupported(ILaunchConfigurationType type) {
		return supportedLaunchTypes.contains(type.getIdentifier());
	}
	
	public static boolean isSupported(ILaunchConfiguration launchConf) {
		try {
			return isSupported(launchConf.getType());
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			return false;
		}
	}

	/**
	 * A LaunchConfigurationDelegate should call this method to declare that it supports
	 * the LaunchListener infrastructure.
	 * <p>
	 * By doing so, the LaunchConfigurationDelegate promises to call the getLaunchListener
	 * methods to check for registered listener and initialize it at the appropriate
	 * time (i.e. just before actually launching the Launch).
	 */
	public static void promiseSupportForType(String launchConfTypetypeID) {
		supportedLaunchTypes.add(launchConfTypetypeID);
	}

	/**
	 * Method provided only for testing purposes. 
	 */
	public static boolean isMemoryLeaked() {
		return !listenerMap.isEmpty();
	}

}
