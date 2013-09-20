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
package org.grails.ide.eclipse.longrunning;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsExecutor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ResultFromTerminatedLaunch;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.util.LimitedByteArrayOutputStream;
import org.grails.ide.eclipse.longrunning.client.GrailsClient;
import org.grails.ide.eclipse.longrunning.client.GrailsCommandExecution;
import org.springsource.ide.eclipse.commons.core.util.MultiplexingOutputStream;

import org.grails.ide.eclipse.core.launch.Grails20OutputCleaner;


/**
 * Alternate implementation of GrailsExecutor that executes Grails commands using a "long running"
 * GrailsProcess.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class LongRunningProcessGrailsExecutor extends GrailsExecutor {
	
	/**
	 * Note that although this instance is public... the proper way to use this is via {@link GrailsExecutor}.getInstance
	 * which has some logic to determine which executor to use depending on the Grails version.
	 */
	public static final LongRunningProcessGrailsExecutor INSTANCE = new LongRunningProcessGrailsExecutor();
	
	private LongRunningProcessGrailsExecutor() {
		super();
	}
	
	@Override
	public ILaunchResult synchExec(GrailsCommand cmd) throws CoreException {
		if (useDefaultExecutor(cmd.getCommand())) {
			//For now... we know run-app isn't really working in long running process. So use the
			//older launch infrasctucture instead.
			return DEFAULT_INSTANCE.synchExec(cmd);
		}
		IGrailsInstall grailsHome = cmd.getGrailsInstall();

		if (grailsHome == null) {
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							GrailsCoreActivator.PLUGIN_ID,
							"The Grails installation directory has not been configured or is invalid.\n"
							+ "Check the Grails project or workspace preference page."));
		}
		synchronized(GrailsProcessManager.getInstance()) {
			GrailsClient process;
			try {
				 process = GrailsProcessManager.getInstance().getGrailsProcess(grailsHome, new File(cmd.getPath()));
			} catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Error creating grails process", e));
			}

			GrailsCommandExecution execution = new GrailsCommandExecution(process, cmd);
			return execution.execute();
		}
	}
 

	private boolean useDefaultExecutor(String command) {
		return command.contains("run-app"); // || command.contains("create-app") || command.contains("create-plugin");
	}

	@Override
	public void shutDown() {
		GrailsProcessManager.getInstance().shutDown();
		super.shutDown();
	}

	public static boolean canHandleVersion(GrailsVersion version) {
		//Long running process executor now only works for V_2_2 or above
 		return version.compareTo(GrailsVersion.V_2_2_)>=0;
	}
}
