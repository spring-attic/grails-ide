/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.launch;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IProcess;

/**
 * An object implementing this interface can be passed to GrailsLaunchConfigurationDelegate.launchWithListener
 * in order to be able to intercept debug events for the associated process created by the launch.
 * <p>
 * The default implementation is set up for the most common scenerio, where are only running in "Normal" mode,
 * expecting a single process to be started as te result of the launch and are only interested in Termination
 * events.
 * @author Kris De Volder
 */
public abstract class AbstractLaunchProcessListener implements IDebugEventSetListener {

	private IProcess process = null;
	
	/**
	 * Get the associated process. This is null before the Listener is properly 
	 * initialized.
	 */
	protected IProcess getProcess() {
		return process;
	}
	
	/**
	 * This method is called during the launch sequence, it receives
	 * a reference to the associated process, before this process is
	 * started.
	 */
	public void init(IProcess process) {
		Assert.isTrue(this.process==null);
		this.process = process;
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	/**
	 * Assuming that we are registered as Listener (which should be done by "init") 
	 * this method will be called by the DebugUI plugin to notify us of DebugEvents.
	 * <p>
	 * The Default implementation is only interested in Termination events on the 
	 * process associated with this listener and ignore all others.
	 */
	public void handleDebugEvents(DebugEvent[] events) {
		if (events != null) {
			int size = events.length;
			for (int i = 0; i < size; i++) {
				if (process != null && process.equals(events[i].getSource())
						&& events[i].getKind() == DebugEvent.TERMINATE) {
					handleTerminate(events[i]);
					DebugPlugin.getDefault().removeDebugEventListener(this);
					process = null; //So that isActive() == false
				}
			}
		}
	}

	/**
	 * This method is called when the process is Terminated. You can examine the
	 * return code by calling getProcess().getExitValue().
	 */
	protected abstract void handleTerminate(DebugEvent debugEvent);

	public boolean isActive() {
		return this.process!=null;
	}

}
