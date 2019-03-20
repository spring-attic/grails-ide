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

import org.grails.ide.eclipse.core.model.GrailsVersion;

import junit.framework.TestCase;


/**
 * Tests for the GrailsVersion class.
 * @author Kris De Volder
 */
public class GrailsVersionTest extends TestCase {
	
	public void testSameVersions() throws Exception {
		assertEquals(0, new GrailsVersion("1.3.5").compareTo(new GrailsVersion("1.3.5")));
		assertTrue(new GrailsVersion("1.3.5").equals(new GrailsVersion("1.3.5")));
	}
	
	public void testCompareVersions() throws Exception {
		assertEquals(-1, new GrailsVersion("1.2").compareTo(new GrailsVersion("1.3.5")));
		assertEquals(-1, new GrailsVersion("1.2").compareTo(new GrailsVersion("1.2.1")));
		assertEquals(+1, new GrailsVersion("1.2").compareTo(new GrailsVersion("1.1.1")));
		assertEquals(GrailsVersion.V_1_2, new GrailsVersion("1.2"));
		assertEquals(-1, GrailsVersion.V_2_0_.compareTo(GrailsVersion.V_2_0_0_M2));
		assertEquals(+1, GrailsVersion.V_2_0_0.compareTo(GrailsVersion.V_2_0_0_M2));
	}
	
	public void testUnkownVersions() throws Exception {
		assertEquals(GrailsVersion.UNKNOWN, new GrailsVersion(null));
		assertEquals(-1, sign(GrailsVersion.UNKNOWN.compareTo(new GrailsVersion("garbage-string"))));
	}
	
	/**
	 * @param compareTo
	 * @return
	 */
	private int sign(int num) {
		if (num < 0) 
			return -1;
		else if (num > 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public void testVersionString() throws Exception {
		assertEquals("1.3.5", new GrailsVersion("1.3.5").getVersionString());
	}
	
	public void testToString() throws Exception {
		assertEquals("1.3.4.BUILD-SNAPSHOT", new GrailsVersion("1.3.4.BUILD-SNAPSHOT").toString());
		assertEquals("1.3.4", new GrailsVersion("1.3.4").toString());
		assertEquals("<##unparseable##>(1.3.4.A.B)", new GrailsVersion("1.3.4.A.B").toString());
	}
	
	public void testIsRelease() throws Exception {
		assertTrue(new GrailsVersion("1.3.7").isRelease());
		assertTrue(new GrailsVersion("2.0.0").isRelease());
		assertFalse(new GrailsVersion("2.0.0.M2").isRelease());
	}
	
	public void testBuildSnapshot() throws Exception {
		assertEquals(+1, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.3.BUILD-SNAPSHOT"))));
		assertEquals(+1, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.3"))));
		assertEquals( 0, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.4.BUILD-SNAPSHOT"))));
		assertEquals(+1, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.4.RC1"))));
//		assertEquals(-1, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.4.1"))));
		assertEquals(-1, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.4"))));
		
		assertEquals(-1, sign(new GrailsVersion("1.3.3.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.4.BUILD-SNAPSHOT"))));
		assertEquals(-1, sign(new GrailsVersion("1.3.3"               ).compareTo(new GrailsVersion("1.3.4.BUILD-SNAPSHOT"))));
		assertEquals( 0, sign(new GrailsVersion("1.3.4.BUILD-SNAPSHOT").compareTo(new GrailsVersion("1.3.4.BUILD-SNAPSHOT"))));
		assertEquals(+1, sign(new GrailsVersion("1.3.4"               ).compareTo(new GrailsVersion("1.3.4.BUILD-SNAPSHOT"))));
//		assertEquals(+1, sign(new GrailsVersion("1.3.4.1"             ).compareTo(new GrailsVersion("1.3.4.BUILD-SNAPSHOT"))));
	}

}
