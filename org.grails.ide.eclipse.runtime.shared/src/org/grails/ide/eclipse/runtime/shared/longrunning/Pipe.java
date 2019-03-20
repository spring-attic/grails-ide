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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

/**
 * A Pipe is a PipedInputStream connected to a PipedOutputStream. Typically, one thread will write to
 * the outpustream, while another thread is reading from the input stream.
 * 
 * @author Kris De Volder
 */
public class Pipe {
	
	private final InputStream in;
	private PrintStream out;
	
	public Pipe() throws IOException {
		PipedOutputStream myout = new PipedOutputStream();
		in = new PipedInputStream(myout);
		out = new PrintStream(myout);
	}

	/**
	 * Write something to the output stream. If the output stream is already closed or this does nothing.
	 * @throws IOException
	 */
	public synchronized void println(String line) throws IOException {
		if (out!=null) {
			out.println(line);
			out.flush();
		}
	}
 
	/**
	 * Close the output stream, the input stream remains open, since not all output written to
	 * the output stream may have been read from the input stream just yet (it gets buffered
	 * up).
	 */
	public void closeOutputStream() {
		if (out!=null) {
			try {
				out.close();
			} finally {
				out = null;
			}
		}
	}
	
	public InputStream getInputStream() {
		return in;
	}

	public PrintStream getOutputStream() {
		return out;
	}

}
