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

/**
 * @author Kris De Volder
 */
public class GrailsBuildListenerEvents {

	public static final String TEST_SUITE_PREPARED = "TestSuitePrepared"; // Added event type for STS!!!!
	public static final String ALL_TESTS_ENDED = "TestProduceReports";
	public static final String TEST_PHASE_START = "TestPhaseStart";
	public static final String TEST_SUITE_START = "TestSuiteStart";
	public static final String TEST_SUITE_END = "TestSuiteEnd";
	public static final String TEST_CASE_START = "TestCaseStart";
	public static final String TEST_CASE_END = "TestCaseEnd";
	public static final String TEST_START = "TestStart";
	public static final String TEST_END = "TestEnd";
	public static final String TEST_FAILURE = "TestFailure";
	
}
