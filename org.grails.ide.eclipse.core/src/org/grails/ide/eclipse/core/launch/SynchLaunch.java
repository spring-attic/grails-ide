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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * Wrapper around a LaunchConfiguration that can be used to "synchExec" this configuration.
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class SynchLaunch {
	
	private static final boolean DEBUG = false;
	
	public SynchLaunch(ILaunchConfiguration launchConf, int timeOut, int outputLimit) {
		Assert.isLegal(outputLimit>0);
		this.launchConf = launchConf;
		this.timeOut = timeOut;
		this.outputLimit = outputLimit;
//		System.out.println("SynchLaunch Timeout is set to "+timeOut);
		Assert.isTrue(LaunchListenerManager.isSupported(launchConf), "Launch doesn't provide SynchLaunch support");
	}
	
	/**
	 * Execute Launch synchronously (i.e. block thread and wait for command to terminate)
	 * 
	 * @return The command result if the command terminated normally
	 * @throw CoreException if the command terminated with an Error of some kind.
	 */
	public ILaunchResult synchExec() throws CoreException {
		LaunchResult r = synchExecInternal();
		if (r.isOK())
			return r;
		else {
			throw r.getCoreException();
		}
	}
	
	public interface ILaunchResult {
		
		/**
		 * Exit code used when the process/command is terminated because it timed out.
		 */
		public static final int EXIT_TIMEOUT = -713007;

		/** 
		 * @return Text that was written to System.out
		 */
		String getOutput();
		
		/** 
		 * @return Text that was written to System.err
		 */
		String getErrorOutput();
		
		/**
		 * @return the status of the launch
		 */
		IStatus getStatus();
		
		boolean isOK();
	}

	///////////////////////////////////////////////////////////////////////////////
	// Implementation of public API
	///////////////////////////////////////////////////////////////////////////////

	/**
	 * Launched process is automatically terminated after this amount of time in milliseconds
	 * has elapsed without any new output coming from the process.
	 */
	private int timeOut;

	/**
	 * If more than this number of characters are logged into the output buffer, the oldest
	 * output will be discarded.
	 */
	private int outputLimit;

	public static abstract class LaunchResult implements ILaunchResult {

		private int exitValue;

		protected LaunchResult(int exitValue) {
			this.exitValue = exitValue;
		}

		protected final int getExitValue() {
			return exitValue;
		}

		public boolean isOK() {
			return exitValue == 0;
		}

		/**
		 * Override to provide better error messages!
		 */
		protected String getErrorMessage() {
			if (isOK())
				return "";
			else {
				return "Exit Value = " + getExitValue();
			}
		}

		public String getOutput() {
			return "";
		}

		public String getErrorOutput() {
			return "";
		}
		
		@Override
		public String toString() {
			return "GrailsCommandResult(" + getExitValue() + ", "
					+ getErrorMessage() + ")";
		}

		/**
		 * Produces something you can throw. You should only call this method if
		 * isOK returned false.
		 * <p>
		 * Override to provide a better implementation.
		 */
		public Exception getException() {
			return new Exception(getErrorMessage());
		}

		public IStatus getStatus() {
			if (isOK()) {
				return Status.OK_STATUS;
			}
			else {
				return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID,
						getErrorMessage(), getException());
			}
		}

		public CoreException getCoreException() {
			Exception e = getException();
			if (e instanceof CoreException)
				return (CoreException) e;
			else
				return new CoreException(getStatus());
		}
	}
	
	public static class ExceptionResult extends LaunchResult {

		private Throwable e;

		public ExceptionResult(Throwable e) {
			super(-99);
			this.e = e;
		}

		@Override
		public String getErrorMessage() {
			return e.getMessage();
		}

		@Override
		public Exception getException() {
			if (e instanceof Exception) {
				return (Exception) e;
			} else {
				return new Exception(e);
			}
		}
	}

	private ILaunchConfiguration launchConf;
	private LaunchListener launchListener;

	private LaunchResult synchExecInternal() {
		try {
			LaunchListenerManager.launchWithListener(launchConf, this, launchListener = new LaunchListener());
			if (launchListener.isActive()) {
				return launchListener.waitForResult();
			} else {
				return new ErrorMessage("LaunchListener did not become active. Launch was canceled?");
			}
		} catch (CoreException e) {
			return new ExceptionResult(e);
		}
	}
	
	private long timeLimit = Long.MAX_VALUE;
	
	private boolean isShowOutput = true; //Set this to false to avoid output being shown in the Eclipse UI Console.
	private boolean isDoBuild = false; //Set this to true to force a build before the launch

	private static class ErrorMessage extends LaunchResult {
		private String msg;

		ErrorMessage(String msg) {
			super(-99);
			this.msg = msg;
		}

		@Override
		public String getErrorMessage() {
			return msg;
		}
	}

	public static class ResultFromTerminatedLaunch extends LaunchResult {

		private String output;
		private String errorOutput;
		private String commandString = "<unknown>";

		public ResultFromTerminatedLaunch(String commandString, int exitValue, String output,
				String errorOutput) {
			super(exitValue);
			Assert.isNotNull(commandString);
			this.commandString = commandString;
			this.output = output;
			this.errorOutput = errorOutput;
		}

		@Override
		public String getOutput() {
			return output;
		}

		@Override
		public String getErrorOutput() {
			return errorOutput;
		}
		
		@Override
		public String getErrorMessage() {
			return "Command: "+commandString+"\n"+
				   "---- System.out ----\n"+
				   getOutput() +
				   "\n---- System.err ----\n" +
				   getErrorOutput();
		}
		
		@Override
		public IStatus getStatus() {
			String shortSummary;
			int code = getExitValue();
			if (code==0) {
				shortSummary = "Command terminated normally";
			} else if (code==ILaunchResult.EXIT_TIMEOUT) {
				shortSummary = "The command '"+commandString+"' was terminated because it didn't produce new output for some time.\n" +
						"\n" +
						"See details for the output produced so far.\n" +
						"\n" +
						"If you think the command simply needed more time, you can increase the " +
						"time limit in the Grails preferences page.\n" +
						"\n" +
						"See menu Windows >> Preferences >> Grails >> Launch";
			} else {
				shortSummary = "Command terminated with an error code (see details for output)";
			}
			MultiStatus status = new MultiStatus(GrailsCoreActivator.PLUGIN_ID, IStatus.ERROR, shortSummary, null);
			status.add(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "------System.out:-----------\n "+getOutput(), null));
			status.add(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "------System.err:-----------\n"+getErrorOutput(), null));
			return status;
		}
	}
	
	private class LaunchListener extends AbstractLaunchProcessListener {
		
		private StringBuffer output = new StringBuffer();
		private StringBuffer errorOutput = new StringBuffer();
		private LaunchResult result;

		@Override
		public void init(IProcess process) {
			super.init(process);
			process.getStreamsProxy().getOutputStreamMonitor()
					.addListener(new CaptureOutput(output));
			process.getStreamsProxy().getErrorStreamMonitor()
					.addListener(new CaptureOutput(errorOutput));
		}

		/**
		 * Kill the process if it is still running.
		 */
		public void killProcess() {
			if (!getProcess().isTerminated()) {
				try {
					getProcess().terminate();
					setResult(new ResultFromTerminatedLaunch(getProcess().getLabel(), ILaunchResult.EXIT_TIMEOUT, getOutput(), getErrorOutput()+"\nTerminating process: Timeout: no new output for "+timeOut+" milliseconds"));
				} catch (DebugException e) {
					//Ignore
				}
			}
		}

		@Override
		protected void handleTerminate(DebugEvent debugEvent) {
			try {
				setResult(new ResultFromTerminatedLaunch(getProcess().getLabel(), 
						getProcess().getExitValue(), 
						getOutput(), getErrorOutput()));
			} catch (DebugException e) {
				setResult(new ExceptionResult(e));
			}
		}

		private synchronized void setResult(LaunchResult result) {
			this.result = result;
			notify();
		}
		
		private synchronized LaunchResult waitForResult() {
			while (result == null) {
				try {
					wait(timeOut);
					if (System.currentTimeMillis()>=timeLimit) {
						if (result==null) {
							launchListener.killProcess();
						}
					}
				} catch (InterruptedException e) {
				}
			}
			if (DEBUG) {
				System.out.println(result);
			}
			return result;
		}

		private String getErrorOutput() {
			return getLimitedOutput(errorOutput, outputLimit);
		}

		private String getOutput() {
			return getLimitedOutput(output, outputLimit);
		}

	}
	
	private static String getLimitedOutput(StringBuffer output, int outputLimit) {
		if (output.length()>outputLimit) {
			return output.substring(output.length()-outputLimit);
		} else {
			return output.toString();
		}
	}

	private class CaptureOutput implements IStreamListener {

		private StringBuffer buffer;

		public CaptureOutput(StringBuffer buffer) {
			Assert.isNotNull(buffer);
			this.buffer = buffer;
		}

		public void streamAppended(String text, IStreamMonitor monitor) {
			refreshTimeLimit();
			this.buffer.append(text);
			if (outputLimit>0) { 
				if (buffer.length() > outputLimit + outputLimit/10) {
					//We use 10% over limit margin to avoid doing costly buffer delete for every character 
					buffer.delete(0, buffer.length()-outputLimit);
				}
			}
		}

		@Override
		public String toString() {
			return "CaptureOutput(" + buffer.toString() + ")";
		}

	}

	/**
	 * Resets the timeOut, timeLimit. This happens at the beginning
	 * of execution, as well as every time new output is received
	 * from the command.
	 */
	public void refreshTimeLimit() {
		timeLimit = System.currentTimeMillis()+timeOut;
	}

	public boolean isShowOutput() {
		return isShowOutput;
	}

	public boolean isDoBuild() {
		return isDoBuild;
	}

	/**
	 * This corresponds to the "register" parameter of the 
	 * {@link ILaunchConfiguration#launch(String, org.eclipse.core.runtime.IProgressMonitor, boolean, boolean)}
	 * method.
	 * <p>
	 * If set to true, then the output of the launch will be shown in the UI, otherwise it will not.
	 * The default value for this property is true. 
	 */
	public void setShowOutput(boolean isShowOutput) {
		this.isShowOutput = isShowOutput;
	}

	/**
	 * This corresponds to the "build" parameter of the 
	 * {@link ILaunchConfiguration#launch(String, org.eclipse.core.runtime.IProgressMonitor, boolean, boolean)}
	 * method.
	 * <p>
	 * If set to true, this will force a build to happen before launching. 
	 * The default value for this property is false.
	 */
	public void setDoBuild(boolean isDoBuild) {
		this.isDoBuild = isDoBuild;
	}
	
	@Override
	public String toString() {
		return this.launchConf.toString();
	}

}
