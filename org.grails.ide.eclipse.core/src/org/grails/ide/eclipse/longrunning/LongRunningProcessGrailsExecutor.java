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
		if (cmd.getCommand().contains("run-app")) {
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

			ByteArrayOutputStream bytesOut = new LimitedByteArrayOutputStream(GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit());
			ByteArrayOutputStream bytesErr = new LimitedByteArrayOutputStream(GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit());
			final Console console = buildConsole(cmd, bytesOut, bytesErr);
			ResultFromTerminatedLaunch result = null; 
			final String cmdInfo = cmd.toString();
			try {
				int code = process.executeCommand(cmd, console);
				result = new ResultFromTerminatedLaunch(cmdInfo, code, bytesOut.toString(), bytesErr.toString());
			} catch (TimeoutException e) {
				result = new ResultFromTerminatedLaunch(cmdInfo, ILaunchResult.EXIT_TIMEOUT, bytesOut.toString(), bytesErr.toString());
			} catch (final Exception e) {
				result = new ResultFromTerminatedLaunch(cmdInfo, -999, bytesOut.toString(), bytesErr.toString()) {
					public Exception getException() {
						return e;
					}
				};
			} finally {
				try {
					console.close();
				} catch (IOException e) {
				}
			}
			if (result.isOK()) {
				cmd.runPostOp();
				return result;
			} else {
				throw result.getCoreException();
			}
		}
	}
 
	protected Console buildConsole(GrailsCommand cmd, ByteArrayOutputStream bytesOut, ByteArrayOutputStream bytesErr) {
		if (cmd.isShowOutput()) {
			//Create a UI console and send output there.
			Console console = GrailsProcessManager.consoleProvider.getConsole(cmd.getCommand());
			OutputStream out = clean(new MultiplexingOutputStream(bytesOut, console.getOutputStream()));
			OutputStream err = new MultiplexingOutputStream(bytesErr, console.getErrorStream());
			return Console.make(console.getInputStream(), out, err);
		} else {
			//Create a dummy console that only sends output to 'bytes'
			return Console.make(clean(bytesOut), bytesErr);
		}
	}

	private OutputStream clean(OutputStream out) {
		if (GrailsCoreActivator.getDefault().getCleanOutput()) {
			return new Grails20OutputStreamCleaner(out);
		}
		return out;
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