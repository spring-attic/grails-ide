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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;

import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.ConsoleProvider;
import org.grails.ide.eclipse.runtime.shared.longrunning.Pipe;

import junit.framework.Assert;


/**
 * Used to replace the 'console UI' for testing, so we can control what input gets sent to
 * a Grails command from the test.
 * <p>
 * The console is driven by a a list of "questions + answer" pairs. When the "question" is seen
 * in the input, we put the answer on the output.
 */
public class TestConsoleProvider extends ConsoleProvider {
	
	/**
	 * Thread that reads the output from a Grails command and answers questions it asked
	 * by putting an answer on the 'keyboard'.
	 */
	public class AnswerProvider extends Thread {
		
		private PrintStream keyboard;
		private BufferedReader grails;
		char[] readBuffer = new char[2048];
		StringBuilder currentLine = new StringBuilder();

		public AnswerProvider(PrintStream keyboard, InputStream grails) {
			this.keyboard = keyboard;
			this.grails = new BufferedReader(new InputStreamReader(grails));
		}

		@Override
		public void run() {
			try {
				boolean failedToAnswer = false;
				while (!qas.isEmpty() && !failedToAnswer) {
					QuestionAnswer q = qas.getFirst(); 
					if (answerQuestion(q)) {
						qas.removeFirst();
					}  else {
						failedToAnswer = true;
					}
				}
			} catch (IOException e) {
			}
		}

		private boolean answerQuestion(QuestionAnswer q) throws IOException {
			boolean answered = false;
			String line;
			do {
				line = readSome();
			} while (line!=null && !line.contains(q.question));
			if (line!=null) {
				keyboard.println(q.answer);
				answered = true;
			}
			return answered;
		}

		/**
		 * Read some input and return the text read so far on the currentline.
		 * The guarantees this function should provide for it to be 'correct' is
		 *  - should block until at least one character can be read or EOF
		 *  - should stop as soon as the first eol is returned.
		 *  - should return *before* an eol is returned if input is blocked.
		 *  
		 * The current implementation is very simplistic as it only reads at most one
		 * character at a time. This meets all the 'correctness' requirements, but is very inefficient
		 * since it will cause the question answerer to check whether the input text after
		 * each character it reads. A more efficient implementation could read multiple
		 * chars as long as no newline is seen and the input is not blocked.
		 */
		private String readSome() throws IOException {
			//TODO: we could try reading more than one character at a time, but it is harder to
			// implement that correctly.
			int c = grails.read();
			boolean eol = c==-1 || c=='\r' || c=='\n';
			if (c==-1 && "".equals(currentLine.toString())) {
				return null; //EOF
			}
			try {
				if (!eol) {
					currentLine.append((char)c);
				}
				return currentLine.toString();
			} finally {
				if (eol) {
					currentLine = new StringBuilder();
				}
			}
		}
	}

	boolean used = false;
	
	LinkedList<QuestionAnswer> qas = new LinkedList<QuestionAnswer>();

	public TestConsoleProvider(QuestionAnswer... qas) {
		this.qas = new LinkedList<QuestionAnswer>();
		for (QuestionAnswer it : qas) {
			this.qas.add(it);
		}
	}
	
	public void assertAllQuestionsAnswered() {
		if (qas.isEmpty()) return;
		StringBuffer unansweredQuestions = new StringBuffer("Unanswered questions:\n");
		for (QuestionAnswer q : qas) {
			unansweredQuestions.append(q);
			unansweredQuestions.append('\n');
		}
		Assert.fail(unansweredQuestions.toString());
	}

	@Override
	public Console getConsole(String title) {
		Pipe grailsOut; // pipe that receives output from the grails command
		Pipe grailsErr; // pipe that receives error output from the grails command
		Pipe keyboard;  // pipe that receives input from the keyboard
		try {
			grailsOut = new Pipe();
			grailsErr = grailsOut;
			keyboard = new Pipe();
		} catch (IOException e) {
			throw new Error(e);
		}
		
		LongRunningGrailsTest.assertFalse("This TestConsoleProvider can only make one test console", used);
		used = true;
		
		new AnswerProvider(keyboard.getOutputStream(), grailsOut.getInputStream()).start();
		
		return Console.make(
				keyboard.getInputStream(),
				grailsOut.getOutputStream(),
				grailsErr.getOutputStream()
		);
	}
}