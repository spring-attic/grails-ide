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
package org.grails.ide.eclipse.runtime.shared.longrunning;

/**
 * If this class is instantiated, it will start a thread monitoring the 'heartBeat'. Clients implementing
 * a process typically subclass this and call the heartBeat method on a regular basis when they are
 * 'alive'.
 * <p>
 * If the heartBeat isn't called for some time, then the HeartBeatMonitor thread will call System.exit
 * automatically.
 * 
 * @since 2.6
 * @author Kris De Volder
 */
public class SafeProcess {
	
	/**
	 * Time recorded each time the 'heartBeat' method is called.
	 */
	private long lastHeartBeat;
	
	/**
	 * A process that is 'alive' (being used on a fairly regular basis) should be calling
	 * this method once every so often.
	 */
	public synchronized void heartBeat() {
		lastHeartBeat = System.currentTimeMillis();
	}

	private boolean hasRecentHeartBeat() {
		return System.currentTimeMillis()-getLastHeartBeat() < MAX_IDLE_TIME;
	}

	private synchronized long getLastHeartBeat() {
		return lastHeartBeat;
	}

	/**
	 * Thread monitoring the process' heartBeat. If there's no heartbeat will call System.exit
	 */
	public class HeartBeatMonitor extends Thread {

		/**
		 * Extra time added to 'sleep' time to avoid waking thread 'just before death'.
		 */
		final long MARGIN_FOR_ERROR = 500; 
		
		public HeartBeatMonitor() {
			setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				sleep();
				if (!hasRecentHeartBeat()) {
					System.exit(-99);
				}
			}
		}

		/**
		 * Try to sleep as long as possible (i.e. until the earliest time this 
		 * process could be considered 'dead'.
		 */
		private void sleep() {
			long waitFor = MAX_IDLE_TIME - (System.currentTimeMillis() - getLastHeartBeat());
			if (waitFor > 0) {
				try {
					sleep(waitFor+MARGIN_FOR_ERROR);
				} catch (InterruptedException e) {
					//Ignore
				}
			}
		}

	}

	private static final int MINUTES = 60 * 1000;
	
	/**
	 * As a safety precaution to avoid runaway rogue processes, GrailsProcess will terminate if
	 * it hasn't been asked to execute any commands for this amount of time.
	 */
	public static final long MAX_IDLE_TIME = 20 * MINUTES;
	
}
