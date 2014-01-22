/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;

/**
 * Utility to find free ports. Can find multiple ports making sure they are all distinct (as long as the
 * same PortFinder instance is used.
 * 
 * @author Kris De Volder
 */
public class PortFinder {

	/**
	 * If you only need the one port you can use this. No need to instantiate the class
	 */
	public static int findFreePort() throws IOException {
		ServerSocket socket = new ServerSocket(0);
		try {
			return socket.getLocalPort();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
	
	private HashSet<Integer> used = new HashSet<Integer>();
	
	/**
	 * Finds a port that is currently free and is guaranteed to be different from any of the
	 * port numbers previously returned by this PortFinder instance.
	 */
	public synchronized int findUniqueFreePort() throws IOException {
		int port;
		do {
			port = findFreePort();
		} while (used.contains(port));
		used.add(port);
		return port;
	}
	
}
