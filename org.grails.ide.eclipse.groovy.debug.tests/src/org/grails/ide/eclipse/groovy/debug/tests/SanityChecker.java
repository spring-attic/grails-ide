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

import junit.framework.TestCase;

import org.eclipse.contribution.jdt.IsWovenTester;

/**
 * 
 * 
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class SanityChecker extends TestCase {
    
    public SanityChecker(String name) {
        super(name);
    }
    
    public void testJDTWeavingIsEnabled() throws Exception {
        assertJDTWeaving();
    }

    public static void assertJDTWeaving() {
        assertTrue("JDT Weaving is not active!!!  All Debug tests will fail", IsWovenTester.isWeavingActive());
    }
}
