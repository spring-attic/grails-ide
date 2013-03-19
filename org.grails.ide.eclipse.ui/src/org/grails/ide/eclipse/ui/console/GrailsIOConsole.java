/*******************************************************************************
 * Copyright (c) 2013 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.console;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.grails.ide.eclipse.longrunning.client.ExecutionEventSource;
import org.grails.ide.eclipse.longrunning.client.GrailsCommandExecution;
import org.grails.ide.eclipse.longrunning.client.ExecutionEventSource.ExecutionListener;

public class GrailsIOConsole extends IOConsole {

	private static final String CONSOLE_TYPE = GrailsIOConsole.class.getName();
	
	private final GrailsCommandExecution execution;
	private Display display;

	public GrailsIOConsole(final String title, GrailsCommandExecution execution) {
		super(title, CONSOLE_TYPE, null);
		display = Display.getCurrent();
		this.execution = execution;
		if (execution!=null) {
			execution.addExecutionListener(new ExecutionListener() {
				public void executionStateChanged(ExecutionEventSource target) {
					if (display==null) {
						display = Display.getDefault();
					}
					display.asyncExec(new Runnable() {
						public void run() {
							setName(title + " - TERMINATED");
						}
					});
				}
			});
		}
	}

	public GrailsCommandExecution getExecution() {
		return execution;
	}

	public boolean isTerminated() {
		return execution==null || execution.isTerminated();
	}

}
