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

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Because normal printstream only flush when newlines are sent, we must make sure
 * that our version also flushes when partial lines are sent. This is because some
 * grails commands may send questions to system out that aren't terminated by newline.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class FlushingPrintStream extends PrintStream {

	/**
	 * @param out
	 */
	public FlushingPrintStream(OutputStream out) {
		super(out, true);
	}
	
	@Override
	public void print(String s) {
		super.print(s);
		flush();
	}

}
