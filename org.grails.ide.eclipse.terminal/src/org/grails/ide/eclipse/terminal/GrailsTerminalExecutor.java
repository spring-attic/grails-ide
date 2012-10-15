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
package org.grails.ide.eclipse.terminal;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.internal.terminal.connector.TerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorImpl;
import org.eclipse.tm.internal.terminal.view.TerminalView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsExecutor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ResultFromTerminatedLaunch;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.grails.ide.eclipse.longrunning.client.GrailsClient;


/**
 * Alternate implementation of LongRunnginGrailsExecutor that executes Grails commands using a "long running"
 * GrailsProcess and sends the output of the command to terminal view capable of rendering ansi codes.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
@SuppressWarnings("restriction")
public class GrailsTerminalExecutor extends GrailsExecutor {
	
	
	private static Object call(Class<?> cls, String name, Class<?>[] types, Object obj, Object... args) {
		try {
			Method m = cls.getDeclaredMethod(name, types);
			m.setAccessible(true);
			return m.invoke(obj, args);
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	/**
	 * Programatically open a new terminal for a given terminal connector (i.e. instead
	 * popping up a settings dialog to fill in connection detauls, the connector
	 * should already have all its details pre-configured by the caller.
	 */
	public static void newTerminal(TerminalView tv, ITerminalConnector c) {
		//tv.setupControls();
		call(TerminalView.class, "setupControls", new Class[0], tv);
		tv.setCommandInputField(true);
		if(c!=null) {
			//tv.setConnector(c);
			call(TerminalView.class, "setConnector", new Class<?>[] {ITerminalConnector.class}, tv, c);
			tv.onTerminalConnect();
		}
	}
	
	public static class GrailsCommandConnectorImpl extends TerminalConnectorImpl {
		
		/**
		 * THe thing we like/need to implement is a TerminalConnectorImpl, but the thing that 
		 * we actually need to provide to the terminal view for opening is a ITerminalConnector.
		 * <p>
		 * This method creates a ITerminalConnector based on our impl.
		 */
		public static ITerminalConnector create(final GrailsCommand cmd, final ArrayBlockingQueue<ILaunchResult> result) {
			return new TerminalConnector(new TerminalConnector.Factory() {
				@Override
				public TerminalConnectorImpl makeConnector() throws Exception {
					return new GrailsCommandConnectorImpl(cmd, result);
				}
			}, null, "grails "+cmd.getCommand(), true);
		}

		/**
		 * The command to execute and display on the terminal emulator.
		 */
		private GrailsCommand cmd;
		private ArrayBlockingQueue<ILaunchResult> requestor;
		private PipedOutputStream terminalInputOut;

		public GrailsCommandConnectorImpl(GrailsCommand cmd, ArrayBlockingQueue<ILaunchResult> requestor) {
			this.cmd = cmd;
			this.requestor = requestor;
		}
		
		@Override
		public void connect( final ITerminalControl control) {
			super.connect(control);
			control.setState(TerminalState.CONNECTING);
			final GrailsProcessManager pm = GrailsProcessManager.getInstance();
			final IGrailsInstall install = cmd.getGrailsInstall();
			final File workingDir = cmd.getWorkingDir();
			if (install!=null) {
				new Job(""+cmd) {
					@Override
					protected IStatus run(IProgressMonitor m) {
						m.beginTask("Execute "+cmd, 1);
						try {
							GrailsClient process = pm.getGrailsProcess(install, workingDir);
							terminalInputOut = new PipedOutputStream();
							Console console = buildConsole(control, terminalInputOut);
							control.setState(TerminalState.CONNECTED);
							int code = process.executeCommand(cmd, console);
							//TODO: capture output (how?)
							requestor.put(new ResultFromTerminatedLaunch(cmd.getCommand(), code, "output not catpured", ""));
							return Status.OK_STATUS;
						} catch (final Exception e) {
							try {
								requestor.put(new ResultFromTerminatedLaunch(cmd.getCommand(), -999, "output not catpured", "") {
									public Exception getException() {
										return e;
									}
								});
							} catch (InterruptedException e1) {
								GrailsCoreActivator.log(e1);
							}
							return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, null, e); 
						} finally {
							control.setState(TerminalState.CLOSED);
							m.done();
						}
					}
				}.schedule();
			}
		}
		
		private Console buildConsole(ITerminalControl control, PipedOutputStream input) {
			try {
				OutputStream out = control.getRemoteToTerminalOutputStream();
				InputStream in = new PipedInputStream(input); //TODO: This is already being IO piped inside the client
				                                              // we should be able to make this work with the inputstream
				                                              // directly using an output stream.
				//TODO: hookup input stream.
				Console console = Console.make(in, out, out);
				return console;
			} catch (Exception e) {
				throw new Error(e);
			}
		}

		@Override
		public OutputStream getTerminalToRemoteStream() {
			return terminalInputOut;
		}

		@Override
		public String getSettingsSummary() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public GrailsTerminalExecutor() {
		super();
	}
	
	@Override
	public ILaunchResult synchExec(GrailsCommand cmd) throws CoreException {
		//TODO: This code is a mess in terms of execption handling. Cleanup. 
		// Idea: make a 
		
		if (cmd.getCommand().contains("run-app")) {
			//For now... we know run-app isn't really working in long running process. So use the
			//older launch infrasctucture instead.
			return DEFAULT_INSTANCE.synchExec(cmd);
		}
		ArrayBlockingQueue<ILaunchResult> resultQ = new ArrayBlockingQueue<ILaunchResult>(1);
		final ITerminalConnector terminalConnector = GrailsCommandConnectorImpl.create(cmd, resultQ);
		IGrailsInstall grailsHome = cmd.getGrailsInstall();

		if (grailsHome == null) {
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							GrailsCoreActivator.PLUGIN_ID,
							"The Grails installation directory has not been configured or is invalid.\n"
							+ "Check the Grails project or workspace preference page."));
		}
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				TerminalView tv;
				try {
					tv = (TerminalView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.tm.terminal.view.TerminalView");
					newTerminal(tv, terminalConnector);
				} catch (PartInitException e) {
					GrailsCoreActivator.log(e);
				}			
			}
		});
		ILaunchResult result = null;
		try {
			result = resultQ.take();
		} catch (InterruptedException e) {
		}
		return result;
	}
 
//	protected Console buildConsole(GrailsCommand cmd, ByteArrayOutputStream bytesOut, ByteArrayOutputStream bytesErr) {
//		if (cmd.isShowOutput()) {
//			//Create a UI console and send output there.
//			Console console = GrailsTerminalExecutor.consoleProvider.getConsole(cmd.getCommand());
//			OutputStream out = clean(new MultiplexingOutputStream(bytesOut, console.getOutputStream()));
//			OutputStream err = new MultiplexingOutputStream(bytesErr, console.getErrorStream());
//			return Console.make(console.getInputStream(), out, err);
//		} else {
//			//Create a dummy console that only sends output to 'bytes'
//			return Console.make(clean(bytesOut), bytesErr);
//		}
//	}

//	private OutputStream clean(OutputStream out) {
//		if (GrailsCoreActivator.getDefault().getCleanOutput()) {
//			return new Grails20OutputStreamCleaner(out);
//		}
//		return out;
//	}

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