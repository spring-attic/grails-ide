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

import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that wraps another output stream. Any output written to the {@link PrefixedOutputStream}
 * will be written through to the wrapped output stream, but prefixing every new line of text with a given
 * String.
 * 
 * @since 2.6
 * @author Kris De Volder
 */
public class PrefixedOutputStream extends OutputStream {

	boolean seenNewline = true;
	byte[] prefix;
	
	private OutputStream out;
	
	public PrefixedOutputStream(String prefix, OutputStream out) {
		this.prefix = prefix.getBytes();
		this.out = out;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (seenNewline) {
			if (b=='\r') {
				out.write(b);
			} else {
				out.write(prefix);
				out.write(b);
				seenNewline = b=='\n';
			}
		} else {
			seenNewline = b=='\n';
			out.write(b);
		} 
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}
	
	@Override
	public void close() throws IOException {
		out.close();
	}

}
