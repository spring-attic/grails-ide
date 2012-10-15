package org.grails.ide.eclipse.terminal;

import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class GrailsTerminalActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		GrailsTerminalActivator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		GrailsTerminalActivator.context = null;
	}
	
	

}
