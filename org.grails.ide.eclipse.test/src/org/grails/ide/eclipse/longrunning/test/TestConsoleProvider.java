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
import org.grails.ide.eclipse.longrunning.process.Pipe;

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
				line = grails.readLine();
			} while (line!=null && !line.contains(q.question));
			if (line!=null) {
				keyboard.println(q.answer);
				answered = true;
			}
			return answered;
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