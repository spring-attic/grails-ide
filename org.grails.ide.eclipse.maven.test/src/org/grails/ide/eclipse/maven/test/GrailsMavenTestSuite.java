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
package org.grails.ide.eclipse.maven.test;

import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.springsource.ide.eclipse.commons.tests.util.ManagedTestSuite;

import junit.framework.Test;

/**
 * 
 * @author Andrew Eisenberg
 * @created Nov 30, 2012
 */
public class GrailsMavenTestSuite {
    public static Test suite() {
        GroovyCompilerVersionCheck.testMode(); // Disable modal dialog
        return new ManagedTestSuite(GrailsMavenTests.class.getName());
    }

}
