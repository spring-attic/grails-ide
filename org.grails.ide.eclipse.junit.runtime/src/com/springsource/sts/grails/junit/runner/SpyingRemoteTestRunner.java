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

import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
/**
 * This class's only purpose is to help in reverse engineering the 'protocol' used by the 
 * JDT JUnit Remote Test runner. We instantiate this class instead of the regular remote
 * test runner to log the messages that are sent by it.
 * <p>
 * See GrailsJUn
 */
public class SpyingRemoteTestRunner extends RemoteTestRunner {
	
	/**
	 * Change this to false, to suppress debug output
	 */
	private static final boolean DEBUG = true;
	
	public static void debug(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}
	
	private SpyingRemoteTestRunner() {
	}

	/**
	 * The main entry point.
	 *
	 * @param args Parameters:
	 * <pre>-classnames: the name of the test suite class
	 * -testfilename: the name of a file containing classnames of test suites
	 * -test: the test method name (format classname testname)
	 * -host: the host to connect to default local host
	 * -port: the port to connect to, mandatory argument
	 * -keepalive: keep the process alive after a test run
     * </pre>
     */
	public static void main(String[] args) {
		try {
			debug("----- SpyingRemoteTestRunner started with args ----");
			for (String arg : args) {
				debug(arg);
			}
			debug("---------------------------------------------------");
			
			SpyingRemoteTestRunner testRunServer= new SpyingRemoteTestRunner();
			testRunServer.setMessageSender(new LoggingMessageSender(testRunServer));
			testRunServer.init(args);
			testRunServer.run();
		} catch (Throwable e) {
			e.printStackTrace(); // don't allow System.exit(0) to swallow exceptions
		} finally {
			// fix for 14434
			System.exit(0);
		}
	}
	
}
