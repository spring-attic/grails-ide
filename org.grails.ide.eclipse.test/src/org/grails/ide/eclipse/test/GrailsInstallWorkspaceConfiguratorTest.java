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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.configurator.ConfigurableExtension;
import org.springsource.ide.eclipse.commons.internal.configurator.ConfiguratorImporter;


/**
 * @author Steffen Pingel
 */
public class GrailsInstallWorkspaceConfiguratorTest extends TestCase {

	public void testDetectExtensions() throws Exception {
		ConfiguratorImporter importer = new ConfiguratorImporter();
		List<ConfigurableExtension> extensions = importer
				.detectExtensions(new NullProgressMonitor());
		assertContains("grails", extensions);
	}

	public void testStartupJob() throws Exception {
		CountDownLatch latch = ConfiguratorImporter.getLazyStartupJobLatch();
		assertTrue("Configurator did not complete before timeout",
				latch.await(120, TimeUnit.SECONDS));
		GrailsInstallManager installMan = GrailsCoreActivator.getDefault()
				.getInstallManager();
		IGrailsInstall install = installMan.getDefaultGrailsInstall();
		assertNotNull("Expected auto-configured Grails installation", install);
	}

	private void assertContains(String id,
			List<ConfigurableExtension> extensions) {
		for (ConfigurableExtension extension : extensions) {
			if (extension.getId().startsWith(id)) {
				assertTrue("Expected auto configuration flag for extension "
						+ extension, extension.isAutoConfigurable());
				return;
			}
		}
		fail("Expected extension with id prefix '" + id + "' in "
				+ extensions.toString());
	}

}
