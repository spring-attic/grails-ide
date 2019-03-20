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
package org.grails.ide.eclipse.groovy.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.grails.ide.eclipse.groovy.debug.core.GroovyDebugCoreActivator;
import org.osgi.framework.BundleContext;


/**
 * @author Andrew Eisenberg
 * @since 2.5.1
 */
public class GroovyDebugUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.grails.ide.eclipse.groovy.debug.core"; 

    private static GroovyDebugUIActivator activator;

    private ScopedPreferenceStore corePreferenceStore;

    public void start(BundleContext bundleContext) throws Exception {
        activator = this;
    }

    public void stop(BundleContext bundleContext) throws Exception {
        activator = null;
    }

    public static GroovyDebugUIActivator getDefault() {
        return activator;
    }

    public static void log(Throwable t) {
        Throwable top = t;
        if (t instanceof CoreException) {
            CoreException de = (CoreException) t;
            IStatus status = de.getStatus();
            if (status.getException() != null) {
                top = status.getException();
            }
        }
        log(new Status(IStatus.ERROR, PLUGIN_ID,
                "Internal error logged from Groovy UI Debug: ", top));  
    }
    
    public IPreferenceStore getCorePreferenceStore() {
        // Create the preference store lazily.
        if (corePreferenceStore == null) {
            corePreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, GroovyDebugCoreActivator.PLUGIN_ID);
        }
        return corePreferenceStore;
    }


    /**
     * Logs the specified status with this plug-in's log.
     * 
     * @param status status to log
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }
}
