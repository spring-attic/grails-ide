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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

import org.grails.ide.eclipse.longrunning.process.GrailsProcess;


/**
 * Provides a readline method on some input stream. This is our own custom implementation of readline because it
 * needs to meet some specific requirements that aren't provided by BufferedReader's implementation. Specifically,
 * we need to be able to guarantee that readline won't get stuck indefinitely if no input is forthcoming.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class LineReader {
	
	private final boolean DEBUG_PROTOCOL; 
	private BufferedReader in;

	public LineReader(InputStream in) {
		this.DEBUG_PROTOCOL = false;
		this.in = new BufferedReader(new InputStreamReader(in));
	}
	
	/**
	 * @param inputStream
	 * @param debugProtocol
	 */
	public LineReader(InputStream in, boolean debugProtocol) {
		this.DEBUG_PROTOCOL = debugProtocol;
		this.in =  new BufferedReader(new InputStreamReader(in));
	}

	public String readLine(long timeOut) throws IOException, TimeoutException {
		String line = readLineHelp(timeOut);
		while (line!=null && line.startsWith("%debug")) {
			line = readLineHelp(timeOut);
		}
		return line;
	}
	
	public void close() throws IOException {
		if (in!=null) {
			try {
				in.close();
			} finally {
				in = null;
			}
		}
	}

	/**
	 * To avoid getting stuck when the process only sends a partial line without line terminator...
	 * readline may return early with only partial line data. In that case, it is important that next
	 * time it reads... it should treat the input that comes as preceded with the same line header
	 * as the partial line that was aborted early.
	 * <p>
	 * Thus, this field will contain a line header like {@link GrailsProcess}.CONSOLE_OUT or
	 * {@link GrailsProcess}.CONSOLE_ERR if the last line read was a partial line. It will
	 * contain null if the last line read was a complete, terminated line.
	 */
	private String partialLineHeader = null; 
	
	private String readLineHelp(long timeOut) throws IOException, TimeoutException {
		// We have to implement our own readLine, otherwise it isn't possible to guarantee
		// that the readLine will not hang indefinitely when no input is forthcoming from
		// a stuck Grails process.
		long maxTime = System.currentTimeMillis() + timeOut;
		boolean eol = false;
		boolean readSomething = false;
		StringBuffer line = partialLineHeader==null? new StringBuffer() : new StringBuffer(partialLineHeader);
		partialLineHeader = null;
		int waiting = 0;
		while (!eol) {
			if (System.currentTimeMillis() > maxTime) 
				throw new TimeoutException("Timeout: Grails process did not produce output for over "+timeOut+" milliseconds");
			if (ready()) {
				waiting = 0;
				maxTime = System.currentTimeMillis() + timeOut;
				int charOrEof = read();
				if (charOrEof==-1) {
					//Reached end of file
					eol = true;
					if (!readSomething) {
						return null;
					}
				} else {
					char c = (char) charOrEof;
					if (c=='\n') {
						eol = true;
					} else 	if (c!='\r') {
						readSomething = true;
						line.append(c);
					}
				}
			} else {
				try {
					waiting++;
					if (waiting>1 && line.length()>GrailsProcess.PROTOCOL_HEADER_LEN) {
						String header = line.substring(0, GrailsProcess.PROTOCOL_HEADER_LEN);
						if (header.equals(GrailsProcess.CONSOLE_OUT) || header.equals(GrailsProcess.CONSOLE_ERR)) {
							//If we are blocked for more than one polling interval, and we have partial line data meant for the console...
							//then pretend we had a newline. This is to avoid getting blocked when we are sent a question without a line terminator.
							partialLineHeader = header; //next time we read... we'll need to pretend we see this
											//header since we won't see the header again when reading the remainder of a partial line.
							eol = true;
						}
					}
					Thread.sleep(GrailsClient.POLLING_INTERVAL); 
				} catch (InterruptedException e) {
					//Ignore
				}
			}
		}
		if (DEBUG_PROTOCOL) {
			GrailsClient.debug_protocol("recv<<< "+line);
		}
		return line.toString();
	}
	
	private int read() throws IOException {
		if (in==null) {
			return -1; // pretend we have an eof ready
		} else {
			return in.read();
		}
	}

	private boolean ready() throws IOException {
		if (in==null) {
			// pretend we have an EOF ready
			return true;
		} else {
			return in.ready();
		}
	}

}
