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
package org.grails.ide.eclipse.commands;

import java.io.IOException;
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
	
	public static synchronized GrailsExecutor getInstance(GrailsVersion version) {
		GrailsExecutor instance = instances.get(version);
		if (instance==null) {
			if (GrailsVersion.V_3_0_.compareTo(version)<=0) {
				instance = new Grails3Executor(version);
			} else {
				boolean keepRunning = GrailsCoreActivator.getDefault().getKeepRunning();
				if (keepRunning && LongRunningProcessGrailsExecutor.canHandleVersion(version)) {
					instance = LongRunningProcessGrailsExecutor.INSTANCE;
				} else {
					instance = new GrailsExecutor();
				}
				instances.put(version, instance);
			}
		}
		return instance;
	}

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
			ILaunchConfigurationWorkingCopy launchConf = createLaunchConfiguration(cmd, grailsHome);
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

	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
			GrailsCommand cmd, IGrailsInstall grailsHome) throws CoreException,
			IOException {
		return GrailsCommandLaunchConfigurationDelegate.getLaunchConfiguration(grailsHome, cmd.getProject(), cmd.getCommand(), cmd.getPath());
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
