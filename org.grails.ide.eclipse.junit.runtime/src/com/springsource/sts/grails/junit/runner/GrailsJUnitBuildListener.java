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

import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.ALL_TESTS_ENDED;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_CASE_END;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_CASE_START;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_END;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_FAILURE;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_PHASE_START;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_START;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_SUITE_END;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_SUITE_PREPARED;
import static com.springsource.sts.grails.junit.runner.GrailsBuildListenerEvents.TEST_SUITE_START;
import grails.build.GrailsBuildListener;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.junit.runner.FirstRunExecutionListener;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;
import org.eclipse.jdt.internal.junit4.runner.JUnit4Identifier;
import org.junit.runner.Description;
import org.junit.runners.Suite;


/**
 * This hooks into the Grails "build listener" interface to be able to receive notifications when
 * tests are run etc.
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GrailsJUnitBuildListener implements GrailsBuildListener {

	//////// Debugging support /////////////////////////////////////////////////////
	private static final boolean DEBUG = true;
	private static PrintStream debugOut; // capture this before Grails redirects it!
	private static void debug(String string) {
		if (!DEBUG) return;
		if (debugOut==null) {
			debugOut = System.out;
		}
		debugOut.println(string);
	}
	////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * An instance of the JDT'
	 */
	private RemoteSTSConnection connection;

	/**
	 * Used to collect suite references until the tests actually start running.
	 * We have to collect the suites, because we need to send a message with the 
	 * number of total tests that will be run. But we cannot determine this number 
	 * until we have all the suites.
	 */
	private List<ITestReference> suites = new ArrayList<ITestReference>(3);
	
	private Description allTests = Description.createSuiteDescription("allTests");
	
	///////////////////////////////////////////////////////////////////////////////////
	/// For tracking the current test execution context as we receive events
	private Description currentSuite = null;
	private Description currentCase = null;
	private Description currentTest = null;
	private JUnit4Identifier currentTestId;

	private long allTestStart;
	
	private void enterSuite(String name) {
		ArrayList<Description> children = allTests.getChildren();
		for (Description child : children) {
			if (child.getDisplayName().equals(name)) {
				currentSuite = child;
				return;
			}
		}
	}
	private void exitSuite(String name) {
		currentSuite = null;
	}
	
	private void enterCase(String name) {
		ArrayList<Description> children = currentSuite.getChildren();
		for (Description child : children) {
			if (child.getDisplayName().equals(name)) {
				currentCase = child;
				return;
			}
		}
	}
	
	private void exitCase(String name) {
		currentCase = null;
	}
	
	private void enterTest(String name) {
		ArrayList<Description> children = currentCase.getChildren();
		for (Description child : children) {
			if (child.getMethodName().equals(name)) {
				currentTest = child;
				currentTestId = new JUnit4Identifier(currentTest);
				return;
			}
		}
	}
	
	private void exitTest(String name) {
		currentTest = null;
		currentTestId = null;
	}
	///////////////////////////////////////////////////////////////////////////////////

	private void addSuite(Suite suite, String phaseName) {
		GrailsJUnit4TestSuiteReference suiteRef = new GrailsJUnit4TestSuiteReference(suite, phaseName);
		suites.add(suiteRef);
		allTests.addChild(suiteRef.getDescription());
	}
	
	private void createConnection() {
		debug("GRAILS_JUNIT_RUNTIME_CLASSPATH=" + System.getenv("GRAILS_JUNIT_RUNTIME_CLASSPATH"));
		this.connection = new RemoteSTSConnection();
		if (DEBUG) {
			connection.setMessageSender(new LoggingMessageSender(connection));
		}
	}
	
	public interface EventListener {
		void receive(Object... args);
	}
	
	Map<String, EventListener> eventListeners = new HashMap<String, EventListener>();
	private FirstRunExecutionListener testListener;
	
	public GrailsJUnitBuildListener() {
		createConnection();
		createTestListener();
		
		//First a number of the following events will be received, one for each test phase
		//These events arrive before any of the tests are executed.
		register(TEST_SUITE_PREPARED, new EventListener() {
			public void receive(Object... args) {
				Suite suite = (Suite) args[0];
				String phaseName = (String) args[1];
				if (suite!=null) {
					debug(TEST_SUITE_PREPARED+" : "+suite);
					addSuite(suite, phaseName);
				}
			}
		});
		
		//Second a number of these will be received (one for each suite, which corresponds to a test phase)
		//We are only interested in the first one of these events
		register(TEST_PHASE_START, new EventListener() {
			public void receive(Object... args) {
				eventListeners.remove(TEST_PHASE_START);
				int testCount = 0;
				allTestStart = System.currentTimeMillis();
				for (ITestReference suiteRef : suites) {
					testCount += suiteRef.countTestCases();
				}
				connection.notifyTestRunStarted(testCount);
				for (ITestReference suiteRef : suites) {
					suiteRef.sendTree(connection);
				}
				createTestListener();
			}
		});
				
		register(TEST_SUITE_START, new EventListener() {
			public void receive(Object... args) {
				enterSuite((String)args[0]);
			}
		});
		
		register(TEST_CASE_START, new EventListener() {
			public void receive(Object... args) {
				enterCase((String)args[0]);
			}
		});
		
		register(TEST_START, new EventListener() {
			public void receive(Object... args) {
				enterTest((String)args[0]);
				testListener.notifyTestStarted(currentTestId);
			}
		});
		
		register(TEST_FAILURE, new EventListener() {
			public void receive(Object... args) {
				//String name = (String) args[0];
				Throwable failure = (Throwable) args[1];
				boolean isError = (Boolean) args[2];
				StringWriter trace = new StringWriter();
				if (failure!=null) {
					failure.printStackTrace(new PrintWriter(trace));
				}
				testListener.notifyTestFailed(new TestReferenceFailure(
						currentTestId, 
						isError?MessageIds.TEST_ERROR:MessageIds.TEST_FAILED, 
						trace.toString())
				);
			}
		});
		
		register(TEST_END, new EventListener() {
			public void receive(Object... args) {
				testListener.notifyTestEnded(currentTestId);
				exitTest((String)args[0]);
			}
		});
		
		register(TEST_CASE_END, new EventListener() {
			public void receive(Object... args) {
				exitCase((String)args[0]);
			}
		});
		
		register(TEST_SUITE_END, new EventListener() {
			public void receive(Object... args) {
				exitSuite((String)args[0]);
			}
		});
		
		//Finally The following event is received upon completion of all the tests.
		register(ALL_TESTS_ENDED, new EventListener() {
			public void receive(Object... args) {
				connection.notifyTestRunEnded(System.currentTimeMillis() - allTestStart);
				connection.close();
			}
		});
	}

	private void createTestListener() {
		testListener = connection.firstRunExecutionListener();
	}
	
	private void register(String name, EventListener eventListener) {
		EventListener existing = eventListeners.get(name);
		if (existing!=null) throw new Error("Multipe listeners for "+name);
		eventListeners.put(name, eventListener);
	}

	public void receiveGrailsBuildEvent(String name, Object... args) {
		debug("event received: "+name);
		EventListener listener = eventListeners.get(name);
		if (listener!=null)
			listener.receive(args);
	}

}
