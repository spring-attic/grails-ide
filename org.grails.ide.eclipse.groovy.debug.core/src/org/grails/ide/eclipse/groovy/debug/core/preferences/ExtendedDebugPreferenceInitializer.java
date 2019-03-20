/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.groovy.debug.core.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.grails.ide.eclipse.groovy.debug.core.GroovyDebugCoreActivator;
import org.osgi.service.prefs.BackingStoreException;


/**
 * @author Andrew Eisenberg
 * @since 2.5.1
 */
public class ExtendedDebugPreferenceInitializer extends
        AbstractPreferenceInitializer {

    public void initializeDefaultPreferences() {
        IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(GroovyDebugCoreActivator.PLUGIN_ID);;
        prefs.putBoolean(
                ExtendedDebugPreferenceConstants.ENABLE_EXTRA_STEP_FILTERING,
                ExtendedDebugPreferenceConstants.DEFAULT_ENABLE_EXTRA_STEP_FILTERING);
        prefs.putBoolean(
                ExtendedDebugPreferenceConstants.ENABLE_EXTRA_STEP_FILTERING_ON_ALL,
                ExtendedDebugPreferenceConstants.DEFAULT_ENABLE_EXTRA_STEP_FILTERING_ON_ALL);
        prefs.putBoolean(
                ExtendedDebugPreferenceConstants.ENABLE_GROOVY_DISPLAY_VIEW,
                ExtendedDebugPreferenceConstants.DEFAULT_ENABLE_GROOVY_DISPLAY_VIEW);
        prefs.putBoolean(
                ExtendedDebugPreferenceConstants.INITIAL_CONFIGURING_OF_DEBUG_SETTINGS_PERFORMED,
                ExtendedDebugPreferenceConstants.DEFAULT_INITIAL_CONFIGURING_OF_DEBUG_SETTINGS_PERFORMED);
        
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            GroovyDebugCoreActivator.log(e);
        }
    }
}
