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

import java.lang.reflect.Field;

import org.codehaus.groovy.grails.cli.support.GrailsStarter;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.runner.TestExecution;

/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
/**
 * This class is based on org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.
 * It is responsible for launching the Grails test-app command appropriate for the
 * command line arguments and ensuring that the GrailsJUnitBuildListener is added
 * as listener to the command run.
 */
public class GrailsRemoteTestRunner extends RemoteTestRunner {
	
	/**
	 * The class that should be attached to the Grails process and report back to Eclipse.
	 * NOTE: Because of class loader hackery in Grails, the reference here will not be
	 * the same one that Grails uses. Grails will load the same class, but with
	 * a different class loader.
	 */
	private static final Class<GrailsJUnitBuildListenerBoot> BUILD_LISTENER_CLASS = GrailsJUnitBuildListenerBoot.class;
	
	/**
	 * Change this to false, to suppress debug output
	 */
	private static final boolean DEBUG = true;
	
	public static void debug(String msg) {
		if (DEBUG) {
			System.err.println(msg);
		}
	}
	
	private GrailsRemoteTestRunner() {
	}

	/**
	 * Because of class loader stuff, in Grails, the BuildListener is in a different class loader.
	 * Therefore, it can't reach this object instance and it must create its own connection. Since
	 * we want to use the same socket, we must not actually open the connection here. We just pretend
	 * that we did, so the RemoteTestRunner will proceed to run the tests.
	 */
	@Override
	protected boolean connect() {
		return true;
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
			debug("----- GrailsRemoteTestRunner started with args ----");
			for (String arg : args) {
				debug(arg);
			}
			debug("---------------------------------------------------");
			
			GrailsRemoteTestRunner testRunServer= new GrailsRemoteTestRunner();
			testRunServer.init(args);
			testRunServer.run();
		} catch (Throwable e) {
			e.printStackTrace(); // don't allow System.exit(0) to swallow exceptions
		} finally {
			// fix for 14434
			System.exit(0);
		}
	}
	
//	@Override
//	public void runTests(String[] testClassNames, String testName, TestExecution execution) {
//		super.runTests(testClassNames, testName, execution);
//	}

	@Override
	public void runTests(String[] testClassNames, String testName,
			TestExecution execution) {
		debug("---- test run requested ---");
		debug("Test Classes = ");
		for (String cName : testClassNames) {
			System.err.println(cName);
		}
		debug("test name = '"+testName+"'");
		assertTrue("Only running a single test method, or a number of test classes is supported by this runner",
				testName==null || testClassNames.length==1);
		
		StringBuffer testAppCommand = new StringBuffer("test-app");
		if (testName==null) {
			for (String className : testClassNames) {
				testAppCommand.append(" "+removeSuffixes(className, "Test","Tests"));
			}
		}
		else {
			String className = testClassNames[0];
			testAppCommand.append(" "+removeSuffixes(className, "Test", "Tests")+"."+testName);
		}
		debug("Grails command to execute: ");
		debug(testAppCommand.toString());
		
		grailsScriptRunner(testAppCommand.toString());
	}

	/**
	 * Call Grails starter with appropriate arguments to execute a "test-app" command, add the {@link GrailsJUnitBuildListener}
	 * class to the classpath and set system properties to allow the build listener to initialise the "Server" that
	 * will send test run results back to Eclipse. 
	 */
	private void grailsScriptRunner(String script) {
		try {
			String grailsHome = System.getenv("GRAILS_HOME");
			debug("GRAILS_HOME="+grailsHome);
			String buildListenerClassPath = System.getenv("GRAILS_JUNIT_RUNTIME_CLASSPATH");
			debug("GRAILS_JUNIT_RUNTIME_CLASSPATH="+buildListenerClassPath);
			System.setProperty("grails.home", grailsHome);
			System.setProperty("grails.build.listeners", BUILD_LISTENER_CLASS.getName());
			System.setProperty(RemoteSTSConnection.PORT_PROP, ""+getField("fPort"));
			
			GrailsStarter.main(new String[] {
					"--classpath", buildListenerClassPath, 
					"--main", "org.codehaus.groovy.grails.cli.GrailsScriptRunner", 
					"--conf", grailsHome+"/conf/groovy-starter.conf",
					script
			});
		}
		catch (Exception e) {
			throw new Error(e);
		}
	}

	public Object getField(String fieldName) {
		try {
			GrailsRemoteTestRunner privateObject = this;
			Field privateField = RemoteTestRunner.class.getDeclaredField(fieldName);
			privateField.setAccessible(true);
			return privateField.get(privateObject);
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private String removeSuffixes(String string, String... suffixes) {
		for (String suffix : suffixes) {
			if (string.endsWith(suffix)) {
				return string.substring(0, string.length()-suffix.length());
			}
		}
		return string;
	}

	//
	// To avoid dependencies (and need for more classpath entries) we implement these ourselves.
	//
	
	public static void assertTrue(String string, boolean b) {
		if (!b) throw new Error(string);
	}
	
	public static void fail(String string) {
		throw new Error(string);
	}

}
