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
package org.grails.ide.eclipse.longrunning.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ResultFromTerminatedLaunch;
import org.grails.ide.eclipse.core.util.LimitedByteArrayOutputStream;
import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.Grails20OutputStreamCleaner;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.springsource.ide.eclipse.commons.core.util.MultiplexingOutputStream;

/**
 * An instance of this class represents and tracks the execution of a Grails command
 * in the long running process execution infrastructure. It plays a similar role
 * to an IProcess in the DebugUI but it doesn't actually correspond to a process
 * in the same sense. This is because a single process can be used to execute
 * multiple commands.
 * 
 * @author Kris De Volder
 */
public class GrailsCommandExecution extends ExecutionEventSource {
	
	private GrailsClient process;
	private GrailsCommand cmd;
	private Console console;
	private ByteArrayOutputStream bytesOut;
	private ByteArrayOutputStream bytesErr;
	private boolean isTerminated = false;

	public GrailsCommandExecution(GrailsClient process, GrailsCommand cmd) {
		this.process = process;
		this.cmd = cmd;
		this.bytesOut = new LimitedByteArrayOutputStream(GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit());
		this.bytesErr = new LimitedByteArrayOutputStream(GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit());
		this.console = buildConsole(cmd, bytesOut, bytesErr);
		this.console.setExection(this);
	}
	
	protected Console buildConsole(GrailsCommand cmd, ByteArrayOutputStream bytesOut, ByteArrayOutputStream bytesErr) {
		if (cmd.isShowOutput()) {
			//Create a UI console and send output there.
			Console console = GrailsProcessManager.consoleProvider.getConsole(cmd.getCommand(), this);
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
	
	/**
	 * Starts the execution and blocks while waiting for command to terminate.
	 */
	public ResultFromTerminatedLaunch execute() throws CoreException {
		try {
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
		} finally {
			isTerminated = true;
			notifyExecutionListeners();
		}
	}
	
	public boolean canTerminate() {
		return !isTerminated;
	}
	
	/**
	 * Warning! Only use this method for forceful termination. It will kill the long running
	 * process as there is no other way to 'nicely ask' grails to stop working on its current
	 * command.
	 */
	public void destroy() {
		if (!isTerminated && process!=null) {
			process.destroy();
			process = null;
		}
	}

}
