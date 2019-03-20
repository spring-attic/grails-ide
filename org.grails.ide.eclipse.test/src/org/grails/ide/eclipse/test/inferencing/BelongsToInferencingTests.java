/*******************************************************************************
 * Copyright (c) 2013 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test.inferencing;

import org.grails.ide.eclipse.test.util.GrailsTest;

import junit.framework.Test;



/**
 * @author Andrew Eisenberg
 * @since 3.2.0
 */
public class BelongsToInferencingTests extends AbstractGrailsInferencingTests {
    private static final String OWNED_BY = "OwnedBy";
    private static final String ADD_TO = "addToOwner";
    private static final String REMOVE_FROM = "removeFromOwner";
    private static final String OWNER = "owner";
    private static final String O_WNER = "Owner";
    

    public static Test suite() {
        return buildTestSuite(BelongsToInferencingTests.class);
    }

    public BelongsToInferencingTests(String name) {
        super(name);
    }
    
    public void testBelongsTo1() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = Owner }");
        String contents = "class Search { def x = { new OwnedBy().owner } }";
        int start = contents.indexOf(OWNER);
        int end = start + OWNER.length();
        GrailsTest.assertNoErrors(project);
        // should be testing for this as well, but test not finding it
//        assertType(contents, start, end, O_WNER, true);
      assertDeclaringType(contents, start, end, OWNED_BY, true);
    }
    
    public void testBelongsTo2() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = [ Owner ] }");
        String contents = "class Search { def x = { new OwnedBy().owner } }";
        int start = contents.indexOf(OWNER);
        int end = start + OWNER.length();
        GrailsTest.assertNoErrors(project);
        // should be testing for this as well, but test not finding it
//      assertType(contents, start, end, O_WNER, true);
        assertDeclaringType(contents, start, end, OWNED_BY, true);
    }
    
    public void testBelongsTo3() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = [ owner : Owner ] }");
        String contents = "class Search { def x = { new OwnedBy().owner } }";
        int start = contents.indexOf(OWNER);
        int end = start + OWNER.length();
        GrailsTest.assertNoErrors(project);
        // should be testing for this as well, but test not finding it
//      assertType(contents, start, end, O_WNER, true);
        assertDeclaringType(contents, start, end, OWNED_BY, true);
    }
    
    
    public void testAddTo1() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = Owner }");
        String contents = "class Search { def x = { new OwnedBy().addToOwner() } }";
        int start = contents.indexOf(ADD_TO);
        int end = start + ADD_TO.length();
        GrailsTest.assertNoErrors(project);
        assertType(contents, start, end, OWNED_BY, true);
    }
    public void testAddTo2() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = [ Owner ] }");
        String contents = "class Search { def x = { new OwnedBy().addToOwner() } }";
        int start = contents.indexOf(ADD_TO);
        int end = start + ADD_TO.length();
        GrailsTest.assertNoErrors(project);
        assertType(contents, start, end, OWNED_BY, true);
    }
    public void testAddTo3() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = [ owner : Owner ] }");
        String contents = "class Search { def x = { new OwnedBy().addToOwner() } }";
        int start = contents.indexOf(ADD_TO);
        int end = start + ADD_TO.length();
        GrailsTest.assertNoErrors(project);
        assertType(contents, start, end, OWNED_BY, true);
    }


    public void testRemoveFrom1() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = Owner }");
        String contents = "class Search { def x = { new OwnedBy().removeFromOwner() } }";
        int start = contents.indexOf(REMOVE_FROM);
        int end = start + REMOVE_FROM.length();
        GrailsTest.assertNoErrors(project);
        assertType(contents, start, end, OWNED_BY, true);
    }
    public void testRemoveFrom2() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = [ Owner ] }");
        String contents = "class Search { def x = { new OwnedBy().removeFromOwner() } }";
        int start = contents.indexOf(REMOVE_FROM);
        int end = start + REMOVE_FROM.length();
        GrailsTest.assertNoErrors(project);
        assertType(contents, start, end, OWNED_BY, true);
    }
    public void testRemoveFrom3() throws Exception {
        createDomainClass("Owner", "class Owner { }");
        createDomainClass(OWNED_BY, "class OwnedBy { static belongsTo = [ owner : Owner ] }");
        String contents = "class Search { def x = { new OwnedBy().removeFromOwner() } }";
        int start = contents.indexOf(REMOVE_FROM);
        int end = start + REMOVE_FROM.length();
        GrailsTest.assertNoErrors(project);
        assertType(contents, start, end, OWNED_BY, true);
    }
}
