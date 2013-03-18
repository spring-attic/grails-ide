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
package org.grails.ide.eclipse.longrunning;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An 'abstract' representation of some 'console like' UI component providing input and
 * output streams to read/write to/from it.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public abstract class Console {
	
	public abstract InputStream getInputStream();
	public abstract OutputStream getOutputStream();
	public abstract OutputStream getErrorStream();

	/**
	 * Create a console that provides both input and output.
	 */
	public static Console make(final InputStream in, final OutputStream out, final OutputStream err) {
		return new Console() {
			@Override
			public OutputStream getOutputStream() {
				return out;
			}
			
			@Override
			public InputStream getInputStream() {
				return in;
			}
			
			@Override
			public OutputStream getErrorStream() {
				return err;
			}
		};
	}
	
	/**
	 * Create a console that only has an output and error Stream.
	 */
	public static Console make(final OutputStream out, final OutputStream err) {
		return make(DummyInputStream.the, out, err);
	}
	
	/**
	 * Closes all Streams associated with the console.
	 */
	public void close() throws IOException {
		IOException e = null;
		try {
			getOutputStream().close();
		} catch (IOException _e) {
			e =_e;
		}
		InputStream in = getInputStream();
		in.close();
		if (e!=null) {
			throw e;
		}
	}
}
