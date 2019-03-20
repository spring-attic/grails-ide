/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test;

import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * Checks whether the number of threads running is a reasonable number.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class ThreadLeakTest extends GrailsTest {

	/**
	 * This number is 20% more than what we typically see running in the Eclipse debugger, running STS.
	 */
	private static final int REASONABLE_NUMBER_OF_THREADS = (int)(45 * 1.2); 

	public void testThreadLeaks() {
		Thread[] threads = StsTestUtil.getAllThreads();
		if (threads.length > REASONABLE_NUMBER_OF_THREADS) {
			fail("There seem to be a lot of threads ("+threads.length+")!!!\n"+StsTestUtil.getStackDumps());
		}
	}
	
}
