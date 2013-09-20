/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.GrailsCommandLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.launch.SynchLaunch;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;

/**
 * An instance of this class provides some way of executing Grails commands.
 * <p>
 * Default implementation provided here is using the "old" mechanism that creates special
 * launch configuration for the grails command and launches it. A single command is executed
 * per launch.
 * <p>
 * An alternative implementation is found in {@link LongRunningProcessGrailsExecutor}
 * 
 * @author Kris De Volder
 * @since 2.5.3
 */
public class GrailsExecutor {
	
	protected static GrailsExecutor DEFAULT_INSTANCE = new GrailsExecutor();
	
	private static Map<GrailsVersion, GrailsExecutor> instances = new HashMap<GrailsVersion, GrailsExecutor>();
	
// The code that is commented out below, related to listener is used by run on server tests which are currently disabled.
// When those tests are re-enabled we may need this code back, and it will then need some work, since it
// assumes only a single executor is exists... but we now have a map of executors per grails version.
	
//	/**
//	 * At present this only used for testing purposes, to monitor whether the expected commands get executed, and if
//	 * they produce the expected results.
//	 */
//	public static GrailsExecutorListener listener = null;
//	
//	public static synchronized void setListener(GrailsExecutorListener newListener) {
//		shutDownIfNeeded(); // Force new executor creation to ensure listener will be installed in it.
//		listener = newListener; 
//	}
	
	public static synchronized GrailsExecutor getInstance(GrailsVersion version) {
		GrailsExecutor instance = instances.get(version);
		if (instance==null) {
			boolean keepRunning = GrailsCoreActivator.getDefault().getKeepRunning();
			if (keepRunning && LongRunningProcessGrailsExecutor.canHandleVersion(version)) {
				instance = LongRunningProcessGrailsExecutor.INSTANCE;
			} else {
				instance = new GrailsExecutor();
			}
			instances.put(version, instance);
		}
//		if (listener!=null) {
//			//copy listener to local, final variable used by the instance. Don't want to have access
//			//listener variable, which is mutable, outside of this synchronized method.
//			final GrailsExecutorListener wrappedListener = listener;
//			final GrailsExecutor wrapped = instance;
//			instance = new GrailsExecutor() {
//				public ILaunchResult synchExec(GrailsCommand cmd) throws CoreException {
//					try {
//						ILaunchResult result = wrapped.synchExec(cmd);
//						wrappedListener.commandExecuted(cmd, result);
//						return result;
//					} catch (CoreException e) {
//						wrappedListener.commandExecuted(cmd, e);
//						throw e;
//					} catch (RuntimeException e) {
//						wrappedListener.commandExecuted(cmd, e);
//						throw e;
//					}
//				}
//				public void shutDown() {
//					wrapped.shutDown();
//				}
//			};
//		}
		return instance;
	}

//	private static boolean canHandleVersion(GrailsExecutor instance, GrailsVersion version) {
//		if (instance instanceof LongRunningProcessGrailsExecutor) {
//			return LongRunningProcessGrailsExecutor.canHandleVersion(version);
//		}
//		return true; // default executor handles all versions.
//	}

	public ILaunchResult synchExec(GrailsCommand cmd) throws CoreException {
		try {
			IGrailsInstall grailsHome = cmd.getGrailsInstall();

			if (grailsHome == null) {
				throw new CoreException(
						new Status(
								IStatus.ERROR,
								GrailsCoreActivator.PLUGIN_ID,
								"The Grails installation directory has not been configured or is invalid.\n"
								+ "Check the Grails project or workspace preference page."));
			}
			ILaunchConfigurationWorkingCopy launchConf = GrailsCommandLaunchConfigurationDelegate.getLaunchConfiguration(grailsHome, cmd.getProject(), cmd.getCommand(), cmd.getPath());
			String buildListener = cmd.getBuildListener();
			if (buildListener!=null) {
				GrailsLaunchArgumentUtils.setGrailsBuildListener(launchConf, buildListener);
			}
			Map<String, String> systemProperties = cmd.getSystemProperties();
			if (systemProperties!=null) {
				GrailsLaunchArgumentUtils.setSystemProperties(launchConf, systemProperties);
			}
			SynchLaunch synchLaunch = new SynchLaunch(launchConf, cmd.getGrailsCommandTimeOut(), GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit());
			synchLaunch.setShowOutput(cmd.isShowOutput());
			ILaunchResult launchResult = synchLaunch.synchExec();
			if (launchResult.getStatus().isOK()) {
				cmd.runPostOp();
			}
			return launchResult;
		} catch (CoreException e) {
			throw e;
		} catch (Throwable e) {
			throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Problem executing grails command", e));
		}
	}

	/**
	 * A GrailsExecutor may have some "stuff" it needs to cleanup when it is deactivated. This method performs the
	 * cleanup. Default implementation does nothing. 
	 */
	public void shutDown() {
	}

	public static synchronized void shutDownIfNeeded() {
		Iterator<GrailsVersion> versions = instances.keySet().iterator();
		while (versions.hasNext()) {
			GrailsVersion version = versions.next();
			GrailsExecutor instance = instances.get(version);
			instance.shutDown();
			versions.remove();
		}
	}

}
