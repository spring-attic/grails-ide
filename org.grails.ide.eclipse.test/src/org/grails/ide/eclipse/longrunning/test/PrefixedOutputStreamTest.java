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
package org.grails.ide.eclipse.longrunning.test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.grails.ide.eclipse.runtime.shared.longrunning.PrefixedOutputStream;


/**
 * Unit tests for PrefixedOutputStream class. 
 * 
 * Doesn't really require Eclipse to run this, should be able to run as an ordinary Junit test. 
 */
public class PrefixedOutputStreamTest extends TestCase {
	
	private ByteArrayOutputStream captureOut;
	private PrefixedOutputStream out;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		captureOut = new ByteArrayOutputStream();
		
		
		//Note: the BufferedOutputStream is added in between to check if proper flushing is happening and
		// output isn't lost/stuck in intervening buffer.
		out = new PrefixedOutputStream("pre> ", new BufferedOutputStream(captureOut));
	}
	
	public void testEmpty() throws IOException {
		out.close();
		assertOutput("");
	}
	
	public void testNicelyTerminatedLines() throws Exception {
		unixPrintln("Hello");
		unixPrintln("World");
		out.close();
		assertOutput(
				"pre> Hello\n" +
				"pre> World\n");
	}

	public void testWindowsNicelyTerminatedLines() throws Exception {
		winPrintln("Hello");
		winPrintln("World");
		out.close();
		assertOutput(
				"pre> Hello\n\r" +
				"pre> World\n\r");
	}

	
	public void testNonTerminatedLine() throws Exception {
		unixPrintln("Hello");
		print("World");
		out.close();
		assertOutput(
				"pre> Hello\n" +
				"pre> World");
	}
	
	public void testWindowsNonTerminatedLine() throws Exception {
		winPrintln("Hello");
		print("World");
		out.close();
		assertOutput(
				"pre> Hello\n\r" +
				"pre> World");
	}
	
	public void testEmptyLines() throws Exception {
		unixPrintln("Hello");
		unixPrintln("");
		unixPrintln("");
		print("World");
		out.close();
		assertOutput(
				"pre> Hello\n" +
				"pre> \n" +
				"pre> \n" +
				"pre> World");
	}
	
	public void testWindowsEmptyLines() throws Exception {
		winPrintln("Hello");
		winPrintln("");
		winPrintln("");
		print("World");
		out.close();
		assertOutput(
				"pre> Hello\n\r" +
				"pre> \n\r" +
				"pre> \n\r" +
				"pre> World");
	}
	
	
	private void print(String string) throws IOException {
		out.write(string.getBytes());
	}

	/**
	 * Println that sends a '\n' only
	 */
	public void unixPrintln(String string) throws IOException {
		out.write(string.getBytes());
		out.write('\n');
	}
	
	/**
	 * Println that sends '\n' followed by a '\r'
	 */
	private void winPrintln(String string) throws IOException {
		out.write(string.getBytes());
		out.write('\n');
		out.write('\r');
	}
	

	private void assertOutput(String expected) {
		assertEquals(expected, captureOut.toString());
	}

}
