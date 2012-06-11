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
package org.grails.ide.eclipse.groovy.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.grails.ide.eclipse.groovy.debug.core.preferences.ExtendedDebugPreferenceConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


/**
 * @author Andrew Eisenberg
 * @since 2.5.1
 */
public class GroovyDebugCoreActivator extends Plugin implements BundleActivator {

    public static final String PLUGIN_ID = "org.grails.ide.eclipse.groovy.debug.core";

    private static GroovyDebugCoreActivator activator;

    private IEclipsePreferences instanceScope;

    public void start(BundleContext bundleContext) throws Exception {
        activator = this;
    }

    public void stop(BundleContext bundleContext) throws Exception {
        activator = null;
    }

    public static GroovyDebugCoreActivator getDefault() {
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
                "Internal error logged from Groovy Core Debug: ", top));
    }

    /**
     * Logs the specified status with this plug-in's log.
     * 
     * @param status status to log
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

    public IEclipsePreferences getPreferences() {
        if (instanceScope == null) {
            instanceScope = InstanceScope.INSTANCE
                    .getNode(GroovyDebugCoreActivator.PLUGIN_ID);
        }
        return instanceScope;
    }

    public static boolean isStepFilteringEnabled() {
        return GroovyDebugCoreActivator
                .getDefault()
                .getPreferences()
                .getBoolean(
                        ExtendedDebugPreferenceConstants.ENABLE_EXTRA_STEP_FILTERING,
                        ExtendedDebugPreferenceConstants.DEFAULT_ENABLE_EXTRA_STEP_FILTERING);
    }

    public static boolean isStepFilteringEnabledOnAll() {
        return GroovyDebugCoreActivator
                .getDefault()
                .getPreferences()
                .getBoolean(
                        ExtendedDebugPreferenceConstants.ENABLE_EXTRA_STEP_FILTERING_ON_ALL,
                        ExtendedDebugPreferenceConstants.DEFAULT_ENABLE_EXTRA_STEP_FILTERING_ON_ALL);
    }
    
    public static boolean isDisplayViewEnabled() {
        return GroovyDebugCoreActivator
                .getDefault()
                .getPreferences()
                .getBoolean(
                        ExtendedDebugPreferenceConstants.ENABLE_GROOVY_DISPLAY_VIEW,
                        ExtendedDebugPreferenceConstants.DEFAULT_ENABLE_GROOVY_DISPLAY_VIEW);
    }

}
