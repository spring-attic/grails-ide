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
package org.grails.ide.eclipse.core.launch;

import java.io.IOException;
import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IStreamsProxy2;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.core.OutputStreamMonitor;
import org.grails.ide.eclipse.core.launch.TransformedStreamMonitor.StreamTransformer;


/**
 * Replaces RuntimeProcess, so that we can manipulate/transform the output streams from the process.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsRuntimeProcess extends RuntimeProcess {
	
	public static class StreamsProxy implements IStreamsProxy, IStreamsProxy2 {

		public StreamsProxy(IStreamMonitor out, IStreamMonitor err, IStreamsProxy in) {
			super();
			this.out = out;
			this.err = err;
			this.in = in;
		}

		private IStreamMonitor out;
		private IStreamMonitor err;
		private IStreamsProxy in; // Only cares about the 'write' method.

		public IStreamMonitor getErrorStreamMonitor() {
			return err;
		}

		public IStreamMonitor getOutputStreamMonitor() {
			return out;
		}

		public void write(String input) throws IOException {
			in.write(input);
		}

		public void closeInputStream() throws IOException {
			if (in instanceof IStreamsProxy2) {
				((IStreamsProxy2) in).closeInputStream();
			}
		}
		
	}

	private static final String NEWLINE = System.getProperty("line.separator");
	private IStreamsProxy mustCloseStreamProxy;

	public GrailsRuntimeProcess(ILaunch launch, Process process, String label, Map attributes) {
		super(launch, process, label, attributes);
	}

	@Override
	protected IStreamsProxy createStreamsProxy() {
		mustCloseStreamProxy = super.createStreamsProxy();
		//We must make sure that the streams proxy created from super is closed when process terminates.
		//This won't happen automatically because our StreamsProxy, which wraps it doesn't extend the 
		//org.eclipse.debug.internal.core.StreamsProxy class.
		return removeDuplicates(mustCloseStreamProxy);
	}
	
	@Override
	protected void terminated() {
		try {
			if (mustCloseStreamProxy instanceof org.eclipse.debug.internal.core.StreamsProxy) {
				((org.eclipse.debug.internal.core.StreamsProxy) mustCloseStreamProxy).close();
			}
		} finally {
			mustCloseStreamProxy = null;
			super.terminated();
		}
	}
	
	private IStreamsProxy removeDuplicates(IStreamsProxy streamsProxy) {
		return new StreamsProxy(removeDuplicates(streamsProxy.getOutputStreamMonitor()), 
				streamsProxy.getErrorStreamMonitor(), streamsProxy);
	}

	/**
	 * Removes 'duplicates' as follows: 
	 * <p>
	 * When the next line is just a copy of the previous line with some extra text added to the end...
	 * Then the next line is not printed, instead, the extra characters are appended to the previous line.
	 */
	private IStreamMonitor removeDuplicates(IStreamMonitor mon) {
		return new TransformedStreamMonitor((OutputStreamMonitor) mon, new StreamTransformer() {

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
			 * Text can be received from the process in bits and pieces. In large chunks or small chunks.
			 * The chunks don't necessarily break off at newline boundaries.
			 */
			@Override
			protected void receive(String text) {
				//TODO: A lot of String copying in here. Could be optimized?
				while (text.length()!=0) {
					if (!hasNewline(text)) {
						processLinePiece(text);
						text = "";
					} else { //There's a newline in the text somewhere.
						int newlinePos = text.indexOf(NEWLINE);
						String firstPiece = text.substring(0, newlinePos);
						String rest = text.substring(newlinePos+NEWLINE.length());
						processLinePiece(firstPiece);
						processNewline();
						text = rest;
					}
				}
			} 

			private void processNewline() {
				flush();
				
				lastLine = currentLine.toString();
				currentLine.setLength(0);
				pendingNewline = true;
				currentLineIsPending = true;
				
				if (lastLine.contains("http://") || lastLine.contains("https://")) { 
					//STS-2155: don't prevent console window from detecting the URL (which it will only do once a newline is received).
					flush();
				}
			}

			/**
			 * Called on to process a piece of text that has no newlines. Could be a whole line of
			 * text or just a portion of a line.
			 */
			private void processLinePiece(String text) {
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
						flush();
					}
				}
			}

			/**
			 * Flushes any unforwarded output.
			 */
			private void flush() {
				if (pendingNewline) {
					send(NEWLINE);
					pendingNewline = false;
				} 
				if (currentLineIsPending) {
					send(currentLine.toString());
					currentLineIsPending = false;
				}
			}

			private boolean hasNewline(String text) {
				return text.contains(NEWLINE);
			}
		});
	}

	
}
