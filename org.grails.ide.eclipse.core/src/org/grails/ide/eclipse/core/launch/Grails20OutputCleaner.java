package org.grails.ide.eclipse.core.launch;

import org.grails.ide.eclipse.core.launch.TransformedStreamMonitor.StreamTransformer;

public class Grails20OutputCleaner extends StreamTransformer {
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
				int newlinePos = text.indexOf(GrailsRuntimeProcess.NEWLINE);
				String firstPiece = text.substring(0, newlinePos);
				String rest = text.substring(newlinePos+GrailsRuntimeProcess.NEWLINE.length());
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
			send(GrailsRuntimeProcess.NEWLINE);
			pendingNewline = false;
		} 
		if (currentLineIsPending) {
			send(currentLine.toString());
			currentLineIsPending = false;
		}
	}

	private boolean hasNewline(String text) {
		return text.contains(GrailsRuntimeProcess.NEWLINE);
	}
}