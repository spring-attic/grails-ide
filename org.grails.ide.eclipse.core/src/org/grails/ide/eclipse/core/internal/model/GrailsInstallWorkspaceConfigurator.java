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
package org.grails.ide.eclipse.core.internal.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.configurator.ConfigurableExtension;
import org.springsource.ide.eclipse.commons.configurator.WorkspaceLocationConfiguratorParticipant;


/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsInstallWorkspaceConfigurator extends WorkspaceLocationConfiguratorParticipant {

	private class GrailsExtension extends ConfigurableExtension {

		private final File location;

		public GrailsExtension(String id, File location) {
			super(id);
			this.location = location;
			setLocation(location.getAbsolutePath());
			setLabel("Grails");
			try {
				setConfigured(getGrailsInstall() != null);
			} catch (IOException e) {
				// ignore
			}
		}

		@Override
		public String getBundleId() {
			return "org.codehaus.grails.bundle";
		}
		
		private IGrailsInstall getGrailsInstall() throws IOException {
			GrailsInstallManager installManager = GrailsCoreActivator.getDefault().getInstallManager();
			String path = location.getCanonicalPath();
			// Check existing installs if path is already configured
			for (IGrailsInstall existingInstall : installManager.getAllInstalls()) {
				if (new File(existingInstall.getHome()).getCanonicalPath().equals(path)) {
					return existingInstall;
				}
			}
			return null;
		}

		@Override
		public IStatus configure(IProgressMonitor monitor) {
			GrailsInstallManager installManager = GrailsCoreActivator.getDefault().getInstallManager();
			try {
				String path = location.getCanonicalPath();

				if (getGrailsInstall() != null) { 
					return new Status(IStatus.INFO, GrailsCoreActivator.PLUGIN_ID, NLS.bind("Grails runtime already configured at {0}", path));
				}
					
				int ix = location.getName().lastIndexOf('-');
				String name = generateName("Grails " + location.getName().substring(ix + 1));

				IGrailsInstall install = new DefaultGrailsInstall(path, name,
						installManager.getDefaultGrailsInstall() == null);

				// Set installs
				Set<IGrailsInstall> installs = new HashSet<IGrailsInstall>(installManager.getAllInstalls());
				installs.add(install);
				installManager.setGrailsInstalls(installs);
				
				setConfigured(true);
				return new Status(IStatus.OK, GrailsCoreActivator.PLUGIN_ID, NLS.bind("Grails runtime successfully configured at {0}", location));
			}
			catch (IOException e) {
				GrailsCoreActivator.log(e);
				return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Unexpected error during Grails runtime configuration", e);
			}
		}

		@Override
		public IStatus unConfigure(IProgressMonitor monitor) {
			GrailsInstallManager installManager = GrailsCoreActivator.getDefault().getInstallManager();
			try {
				String path = location.getCanonicalPath();

				Set<IGrailsInstall> installs = new HashSet<IGrailsInstall>(installManager.getAllInstalls());
				for (IGrailsInstall install : installManager.getAllInstalls()) {
					if (path.equals(install.getHome())) {
						installs.remove(install);
					}
				}

				installManager.setGrailsInstalls(installs);
			}
			catch (IOException e) {
				GrailsCoreActivator.log(e);
			}
			return new Status(IStatus.OK, GrailsCoreActivator.PLUGIN_ID, NLS.bind("Grails runtime successfully unconfigured at {0}", location));
		}
		
	}
	
	public String getPath() {
		return "grails";
	}

	public String getVersionRange() {
		return "[1.1.1, 3.0)";
	}

	private String generateName(String name) {
		if (!isDuplicateName(name)) {
			return name;
		}

		if (name.matches(".*\\(\\d*\\)")) {
			int start = name.lastIndexOf('(');
			int end = name.lastIndexOf(')');
			String stringInt = name.substring(start + 1, end);
			int numericValue = Integer.parseInt(stringInt);
			String newName = name.substring(0, start + 1) + (numericValue + 1) + ")"; //$NON-NLS-1$
			return generateName(newName);
		}
		else {
			return generateName(name + " (1)");
		}
	}

	private boolean isDuplicateName(String name) {
		for (IGrailsInstall vm : GrailsCoreActivator.getDefault().getInstallManager().getAllInstalls()) {
			if (vm.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected ConfigurableExtension doCreateExtension(File location,
			IProgressMonitor monitor) {
		return new GrailsExtension(location.getName(), location);
	}

}
