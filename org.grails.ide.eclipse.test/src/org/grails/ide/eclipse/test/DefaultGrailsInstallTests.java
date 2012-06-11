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
package org.grails.ide.eclipse.test;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class DefaultGrailsInstallTests extends GrailsTest {
	
	public void testVerifyMissing() {
		IGrailsInstall install = new DefaultGrailsInstall("/bogus", "Bogus 1.0", false);
		IStatus status = install.verify();
		assertError(status, "does not exist");
	}
	
	public void testVerifyInvalid() throws IOException {
		File dir = StsTestUtil.createTempDirectory();
		IGrailsInstall install = new DefaultGrailsInstall(dir.toString(), "Bogus 1.1", false);
		IStatus status = install.verify();
		assertError(status, "does not appear to be a valid Grails install");
	}
	
	public void testVerifyOK() throws Exception {
		GrailsVersion version = GrailsVersion.MOST_RECENT;
		ensureDefaultGrailsVersion(version);
		IGrailsInstall install = version.getInstall();
		final IStatus status = install.verify();
		assertTrue("Expect ok status but got: "+status, status.isOK());
	}

	private void assertError(IStatus status, String expect) {
		assertTrue("Expecting an error status but got: "+status, IStatus.ERROR<=status.getSeverity());
		assertTrue("Expecting '"+expect+"'"+" but got "+status, status.getMessage().contains(expect));
	}

}
