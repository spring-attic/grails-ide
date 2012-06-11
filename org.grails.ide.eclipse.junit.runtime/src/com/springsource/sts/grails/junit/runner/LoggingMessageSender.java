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
package com.springsource.sts.grails.junit.runner;

import org.eclipse.jdt.internal.junit.runner.MessageSender;

/**
 * A logging message sender logs all the messages a remote test run produces to
 * System.out.
 * This is just a "tool" to help reverse engineer the protocol we need to implement.
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class LoggingMessageSender implements MessageSender {

	private MessageSender wrappee;

	public LoggingMessageSender(MessageSender wrappee) {
		this.wrappee = wrappee;
	}

	public void sendMessage(String msg) {
		System.out.println("tester-message: '"+msg+"'");
		wrappee.sendMessage(msg);
	}

	public void flush() {
		wrappee.flush();
	}

}
