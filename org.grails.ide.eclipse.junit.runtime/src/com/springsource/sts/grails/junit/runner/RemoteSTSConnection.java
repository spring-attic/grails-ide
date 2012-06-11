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

import java.lang.reflect.Method;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

/**
 * An instance of this class is created to communicate with STS Eclipse and send
 * back test results. 
 * <p>
 * It assumes that a system property has been set to determine the port number for its socket.
 * <p>
 * See the file "sample_messages.log" for a sample log file created by using the {@link SpyingRemoteTestRunner}to capture messages from a typical test run. Very useful to understand the protocol used by the
 * RemoteTestRunner.
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class RemoteSTSConnection extends RemoteTestRunner {

	public static final String PORT_PROP = RemoteSTSConnection.class.getName()+".PORT";
	
	public RemoteSTSConnection() {
		init(new String[] {
				"-port", System.getProperty(PORT_PROP), 
				"-className", "Dummy" // satisfy a check in superclass, but not using this part of superclass.
		});
		if (!connect()) throw new Error("Couldn't open RemoteSTSConnection connection");
	}

	@Override
	public void sendMessage(String msg) {
		super.sendMessage(msg);
	}

	public void initDefaultLoader() {
		//Stubbed, we don't use the test loader from the super class, we use Grails to
		// create the test suites.
	}

	public void close() {
		try {
			Method shutdown = RemoteTestRunner.class.getDeclaredMethod("shutDown");
			shutdown.setAccessible(true);
			shutdown.invoke(this);
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	@Override
	public void notifyTestRunStarted(int testCount) {
		super.notifyTestRunStarted(testCount);
	}
	
	public void notifyTestRunEnded(long elapsedTime) {
		sendMessage(MessageIds.TEST_RUN_END + elapsedTime);
		flush();
	}
	
	@Override
	public void visitTreeEntry(ITestIdentifier id, boolean b, int i) {
		super.visitTreeEntry(id, b, i);
	}

	public void notifyTestStart() {
	}

}
