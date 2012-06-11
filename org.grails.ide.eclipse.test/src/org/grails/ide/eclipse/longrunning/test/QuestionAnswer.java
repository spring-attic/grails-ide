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

/**
 * A "question" expected in the output from a Grails command and its corresponding answer.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class QuestionAnswer {
	
	String question;
	String answer;
	
	public QuestionAnswer(String q, String a) {
		this.question = q; this.answer = a;
	}
	
	@Override
	public String toString() {
		return question + " => " + answer;
	}
	
}