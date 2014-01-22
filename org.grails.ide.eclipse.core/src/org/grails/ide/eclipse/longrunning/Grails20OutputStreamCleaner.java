/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.longrunning;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.grails.ide.eclipse.core.launch.GrailsRuntimeProcess;

/**
 * Cleans Grails 2.0 output by removing duplicated messages.
 * 
 * @author Kris De Volder
 */
public class Grails20OutputStreamCleaner extends OutputStream {
	
	//TODO: This code copied from org.grails.ide.eclipse.core.launch.Grails20OutputCleaner
	//  somehow it should be possible to avoid duplicating all this code and just.
	//  abstract out the cleaning logic into a reusable entity instead.
	
	/**
	 * Remembers the last line of text received. When it is a prefix of the currentLine the prefix will
	 * be supressed in the output.
	 */
	String lastLine = "";
	
	/**
	 * Builds up the currentLine as we receive output from the original Stream.
	 */
	StringBuilder currentLine = new StringBuilder();
	
	/**
	 * When a newline is sent, we hold on to it until we have made a decision what to do with the
	 * next line (maybe it will not be on a newline if it just 'extends' the previous line).
	 * <p>
	 * This flag is true when we are holding on to a newline that has not been sent downstream
	 * yet.
	 */
	boolean pendingNewline = false;
	
	/**
	 * This will be true as long as we have not yet been able to decide whether the current line of
	 * input matches the last line and needs to be transformed. Once a decision is made,
	 * the output will be transformed as required and flushed; and this flag set to false.
	 */
	boolean currentLineIsPending = true;
	
	/**
	 * The place where the cleaned output will be sent to.
	 */
	private OutputStream downStream;

	/**
	 * Create a Grails20OutputStreamCleaner. This is an output stream that cleans the
	 * output it receives and sends on a filtered/cleaned version of the output to the
	 * wrapped 'downStream' Stream.
	 * 
	 * @param downStream
	 */
	public Grails20OutputStreamCleaner(OutputStream downStream) {
		this.downStream = downStream;
	}

	/**
	 * Text can be received from the process in bits and pieces. In large chunks or small chunks.
	 * The chunks don't necessarily break off at newline boundaries.
	 * @throws IOException 
	 */
	protected void receive(String text) throws IOException {
		//TODO: A lot of String copying in here. Could be optimized?
		while (text.length()!=0) {
			if (!hasNewline(text)) {
				processLinePiece(text);
				text = "";
			} else { //There's a newline in the text somewhere.
				int newlinePos = text.indexOf(GrailsRuntimeProcess.NEWLINE);
				String firstPiece = text.substring(0, newlinePos);
				String rest = text.substring(newlinePos+GrailsRuntimeProcess.NEWLINE.length());
				processLinePiece(firstPiece);
				processNewline();
				text = rest;
			}
		}
	}

	private void processNewline() throws IOException {
		flushLine();
		
		lastLine = currentLine.toString();
		currentLine.setLength(0);
		pendingNewline = true;
		currentLineIsPending = true;
		
		if (lastLine.contains("http://") || lastLine.contains("https://")) { 
			//STS-2155: don't prevent console window from detecting the URL (which it will only do once a newline is received).
			flushLine();
		}
	}

	/**
	 * Called on to process a piece of text that has no newlines. Could be a whole line of
	 * text or just a portion of a line.
	 * @throws IOException 
	 */
	private void processLinePiece(String text) throws IOException {
		// The tricky bit in here is that we must flush output as early as possible.
		//otherwise the output won't appear in the console window. 
		// In particular, we can't wait for a newline to decide when the current line is complete. 
		// It is common for Grails to print a question without a newline at the end, and
		// this text should appear immediately on the screen rather than remain buffered up in this
		// transformer (leaving the user wondering why the process appears stuck).
		currentLine.append(text);
		if (!currentLineIsPending) {
			send(text);
		} else { //currentLineIsPending
			String currentLineStr = currentLine.toString();
			if (currentLineStr.startsWith(lastLine)) {
				//We've got a match... eat the repeated bit and send the rest.
				currentLineIsPending = false;
				send(currentLineStr.substring(lastLine.length()));
				pendingNewline = false;
			} else if (lastLine.startsWith(currentLineStr)) {
				//Could be a match... or not... 
				//Nothing to do, just keep it pending
			} else {
				//It's not a match... flush it
				flushLine();
			}
		}
	}

	/**
	 * Flushes any unforwarded output.
	 * @throws IOException 
	 */
	private void flushLine() throws IOException {
		//Note: can not call this method flush, because that's a public method in the
		// output stream api. Be careful to call the right flush method!
		if (pendingNewline) {
			send(GrailsRuntimeProcess.NEWLINE);
			pendingNewline = false;
		} 
		if (currentLineIsPending) {
			send(currentLine.toString());
			currentLineIsPending = false;
		}
	}

	private void send(String string) throws IOException {
		downStream.write(string.getBytes());
	}

	private boolean hasNewline(String text) {
		return text.contains(GrailsRuntimeProcess.NEWLINE);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////// I am an output stream so I implement all the 'write' methods
	
	@Override
	public void write(int b) throws IOException {
		String input = ""+((char)b);
		receive(input);
	}
	
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		receive(new String(b, off, len));
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		receive(new String(b));
	}
	
	@Override
	public void flush() throws IOException {
		//Important: this does not and should not flush the currentline Otherwise
		//it messes up our transfomration by forcing output before we have decided
		//whether it should be transformed.
		downStream.flush();
	}
	
	@Override
	public void close() throws IOException {
		flushLine();
		downStream.close();
	}
	
	
}
