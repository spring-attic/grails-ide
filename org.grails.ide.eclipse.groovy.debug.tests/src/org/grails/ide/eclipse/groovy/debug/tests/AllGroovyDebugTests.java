/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.groovy.debug.tests;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Andrew Eisenberg
 * @since 2.5.2
 */
public class AllGroovyDebugTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("Run of all Groovy Debug tests");
        suite.addTestSuite(SanityChecker.class);
        suite.addTestSuite(GroovyDebugTests.class);
        suite.addTestSuite(GroovyConditionalBreakpointsTests.class);
        return suite;
    }
}
