package org.grails.ide.eclipse.maven;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class GrailsMavenActivator implements BundleActivator {
    
    public static final String PLUGIN_ID = "org.grails.ide.eclipse.maven";

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		GrailsMavenActivator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		GrailsMavenActivator.context = null;
	}

}
