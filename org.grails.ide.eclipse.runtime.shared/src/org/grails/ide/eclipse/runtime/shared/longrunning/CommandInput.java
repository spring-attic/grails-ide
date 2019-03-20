/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.runtime.shared.longrunning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;


/**
 * An instance of this class provides access to an input stream created by reading from the
 * commands stream and extracting the input sent right after a command.
 * 
 * @author kdvolder
 */
public class CommandInput extends Thread {

	private BufferedReader fromClient;
	private Pipe toCommand;
	
	public CommandInput(BufferedReader input) throws IOException {
		this.fromClient = input;
		this.toCommand = new Pipe();
		setDaemon(true);
		start();
	}
	
	@Override
	public void run() {
		super.run();
		try {
			String line = fromClient.readLine();
			while (!GrailsProcessConstants.CONSOLE_EOF.equals(line)) {
				if (line.startsWith(GrailsProcessConstants.CONSOLE_INPUT)) {
					toCommand.println(line.substring(GrailsProcessConstants.PROTOCOL_HEADER_LEN));
				} else {
					throw new ProtocolException("Unexpected: "+line);
				}
				line = fromClient.readLine();
			}
		} catch (IOException e) {
			//TODO: not sure where this will print, probably lost!
			e.printStackTrace();
		} finally {
			toCommand.closeOutputStream();
		}
	}

	/**
	 * Essentially this is like Thread.join, but it handles {@link InterruptedException} to
	 * keep rertrying.
	 * <p>
	 * Calling this method may block indefinitely, if reading of the input blocks indefinitely.
	 */
	public void terminate() {
		boolean retry = true;
		while (retry) {
			try {
				this.join();
				retry = false;
			} catch (InterruptedException e) {
				retry = true;
			}
		}
	}

	public InputStream getInputStream() {
		return toCommand.getInputStream();
	}

}
