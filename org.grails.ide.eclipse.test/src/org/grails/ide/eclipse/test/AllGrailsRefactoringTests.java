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
package org.grails.ide.eclipse.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.grails.ide.eclipse.refactoring.test.GrailsActionRenameTest;
import org.grails.ide.eclipse.refactoring.test.GrailsTypeRenameTest;
import org.grails.ide.eclipse.refactoring.test.GrailsViewRenameTest;
import org.grails.ide.eclipse.search.test.ControllerActionQueryParticipantTest;
import org.grails.ide.eclipse.search.test.ControllerTypeQueryParticipantTests;
import org.grails.ide.eclipse.search.test.GSPControllerActionSearchTest;
import org.grails.ide.eclipse.search.test.GSPControllerTypeSearchTest;
import org.grails.ide.eclipse.search.test.URLMappingSearchTest;

public class AllGrailsRefactoringTests {
	
	public static Test suite() {
        TestSuite suite = new TestSuite(AllGrailsRefactoringTests.class.getName());
        //We'll include search tests inhere for now as they are closely related to the refactoring functionality.
        suite.addTestSuite(URLMappingSearchTest.class);
        suite.addTestSuite(GSPControllerTypeSearchTest.class);
        suite.addTestSuite(GSPControllerActionSearchTest.class);
        suite.addTestSuite(ControllerActionQueryParticipantTest.class);
        suite.addTestSuite(ControllerTypeQueryParticipantTests.class);
        
        //Actual refactoring tests.
        suite.addTestSuite(GrailsTypeRenameTest.class);
        suite.addTestSuite(GrailsViewRenameTest.class);
        suite.addTestSuite(GrailsActionRenameTest.class);
        
        return suite;
	}

}
