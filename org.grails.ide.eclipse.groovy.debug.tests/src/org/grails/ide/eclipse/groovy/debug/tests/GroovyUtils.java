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
package org.grails.ide.eclipse.groovy.debug.tests;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * 
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class GroovyUtils {
    static public final int GROOVY_LEVEL;
    static {
        int groovyLevel = 18;
        Bundle groovyBundle = Platform.getBundle("org.codehaus.groovy");
        if (groovyBundle != null) {
            groovyLevel = groovyBundle.getVersion().getMajor() * 10 + groovyBundle.getVersion().getMinor(); 
        }
        GROOVY_LEVEL = groovyLevel;
    }
    public static boolean isGroovy16() {
        return GROOVY_LEVEL == 16;
    }
    public static boolean isGroovy17() {
        return GROOVY_LEVEL == 17;
    }
    public static boolean isGroovy18() {
        return GROOVY_LEVEL == 18;
    }

}
