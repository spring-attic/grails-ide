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
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsPage;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorImpl;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.grails.ide.eclipse.longrunning.client.GrailsClient;

/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GrailsTerminalConnector extends TerminalConnectorImpl {
	
	public final GrailsTerminalSettings settings = new GrailsTerminalSettings();

	@Override
	public OutputStream getTerminalToRemoteStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void connect(final ITerminalControl control) {
		super.connect(control);
		control.setState(TerminalState.CONNECTING);
		final GrailsProcessManager pm = GrailsProcessManager.getInstance();
		final IGrailsInstall install = settings.getGrailsInstall();
		final GrailsCommand cmd = settings.getGrailsCommand();
		final File workingDir = settings.getWorkingDir();
		if (install!=null) {
			final Console console = buildConsole(control);
			new Job(""+cmd) {
				@Override
				protected IStatus run(IProgressMonitor m) {
					m.beginTask("Execute "+cmd, 1);
					try {
						GrailsClient process = pm.getGrailsProcess(install, workingDir);
						control.setState(TerminalState.CONNECTED);
						int code = process.executeCommand(settings.getGrailsCommand(), console);
						// TODO Auto-generated method stub
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, null, e); 
					} finally {
						control.setState(TerminalState.CLOSED);
						m.done();
					}
				}
			}.schedule();
		}
	}
	
	private Console buildConsole(ITerminalControl control) {
		OutputStream out = control.getRemoteToTerminalOutputStream();
//		OutputStream in = getTerminalToRemoteStream();
		//TODO: hookup input stream.
		Console console = Console.make(out, out);
		return console;
	}

	@Override
	public String getSettingsSummary() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ISettingsPage makeSettingsPage() {
		return new GrailsTerminalSettingsPage(settings);
	}

}
