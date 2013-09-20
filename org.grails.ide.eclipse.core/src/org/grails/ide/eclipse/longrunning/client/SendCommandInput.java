/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.longrunning.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeoutException;

import org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants;


/**
 * Client side counterpart of CommandInput on the remote process.
 * <p>
 * This thread is responsible for reading input from the console and sending that input to the process
 * in the format the process is expecting.
 * <p>
 * Tricky part is that console may not have any input, and thus the reading from console may
 * block indefinitely. That is annoying since it doesn't leave us with a reliable way to
 * terminate the thread doing the reading.
 * 
 * @author Kris De Volder
 */
public class SendCommandInput extends Thread {

	private LineReader fromConsole;
	private PrintWriter toProcess;

	boolean eof = false;
	private GrailsClient client;
	
	public SendCommandInput(GrailsClient client, InputStream fromConsole, PrintWriter toProcess) {
		this.client = client;
		this.fromConsole = new LineReader(fromConsole);
		this.toProcess = toProcess;
		start();
	}
	
	@Override
	public void run() {
		while (!eof) {
			try {
				String line = fromConsole.readLine(GrailsClient.POLLING_INTERVAL);
				if (line == null) {
					eof = true;
				} else {
					client.println(toProcess, GrailsProcessConstants.CONSOLE_INPUT+line);
					toProcess.flush();
				}
			} catch (TimeoutException e) {
				//We'll keep trying unless someone makes us terminate by calling terminate
			} catch (IOException e) {
				eof = true;
			}
		}
		client.println(toProcess, GrailsProcessConstants.CONSOLE_EOF);
	}

	public void terminate() {
		eof = true;
		try {
			fromConsole.close();
		} catch (IOException e1) {
		}
		boolean retry = true;
		while (retry) {
			try {
				join();
				retry = false;
			} catch (InterruptedException e) {
				retry = true;
			}
		}
	}

}
