/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.groovy.debug.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * @author Andrew Eisenberg
 * @since 2.5.1
 */
@SuppressWarnings("rawtypes")
public class DebugProviderAdapterFactory implements IAdapterFactory {

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        try {
            Class<?> clazz = Class.forName("org.eclipse.contribution.jdt.debug.IDebugProvider");
            if (adapterType == clazz) {
                return new GroovyDebugProvider();
            }
        } catch (Throwable e) {
            GroovyDebugCoreActivator.log(new Status(IStatus.WARNING, GroovyDebugCoreActivator.PLUGIN_ID, "Enhanced Groovy debug capability is disabled because JDT Weaving is not up to date"));
        }
        return null;
    }

    public Class[] getAdapterList() {
        try {
            Class<?> clazz = Class.forName("org.grails.ide.eclipse.groovy.debug.core.GroovyDebugProvider");
            return new Class[] { clazz };
        } catch (Throwable e) {
            GroovyDebugCoreActivator.log(new Status(IStatus.WARNING, GroovyDebugCoreActivator.PLUGIN_ID, "Enhanced Groovy debug capability is disabled because JDT Weaving is not up to date"));
        }
        return new Class[0];
    }

}
