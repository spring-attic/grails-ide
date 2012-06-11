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
package org.grails.ide.eclipse.test.inferencing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import junit.framework.TestCase;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;

import org.grails.ide.eclipse.editor.groovy.types.DynamicFinderValidator;
import org.grails.ide.eclipse.test.GrailsTestsActivator;


/**
 * @author Andrew Eisenberg
 * @created May 28, 2010
 */
public class DynamicFinderTests extends TestCase {
    private class MockDynamicFinderValidator extends DynamicFinderValidator {
        public MockDynamicFinderValidator() {
            super(GrailsTestsActivator.isGrails200OrLater(), new HashSet<String>(Arrays.asList("foo", "baz", "bax")));
        }
        
        public MockDynamicFinderValidator(Set<String> domainProperties) {
            super(GrailsTestsActivator.isGrails200OrLater(), domainProperties);
        }

        // does not check for duplicate components
        protected List<String> getFinderComponents(String finderName) {
            Matcher matcher = validFinderPattern.matcher(finderName);
            if (! matcher.matches()) {
                return null;
            } else {
                List<String> matches = new ArrayList<String>(matcher.groupCount());
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    matches.add(matcher.group(i));
                }
                return matches;
            }
        }

    }
    
    public void testPattern1() throws Exception {
        tryMatch("findByFooOrNuthinAndBax", null);
    }
    public void testPattern2() throws Exception {
        tryMatch("findByFoo", createList("findBy", "Foo", null, null, null, null));
    }
    public void testPattern3() throws Exception {
        tryMatch("findByFooAnd", null);
    }
    public void testPattern4() throws Exception {
        tryMatch("findByAnd", null);
    }
    public void testPattern5() throws Exception {
        tryMatch("findByFooLikeAndBax", createList("findBy", "Foo", "Like", "And", "Bax", null));
    }
    
    public void testPattern6() throws Exception {
        tryMatch("findByFooBetween", createList("findBy", "Foo", "Between", null, null, null));
    }
    
    public void testPattern6a() throws Exception {
        tryMatch("findByFooLessThanEquals", createList("findBy", "Foo", "LessThanEquals", null, null, null));
    }
    
    public void testPattern7() throws Exception {
        tryMatch("findByFooAndBazBetween", createList("findBy", "Foo", null, "And", "Baz", "Between"));
    }
    
    public void testPattern8() throws Exception {
        tryMatch("findByFooAndBetween", null);
    }
    public void testPattern9() throws Exception {
        tryMatch("findByFooLikeAndBaxBetween", createList("findBy", "Foo", "Like", "And", "Bax", "Between"));
    }
    
    public void testPattern10() throws Exception {
        tryMatch("findByFooLikeOrBaxBetween", createList("findBy", "Foo", "Like", "Or", "Bax", "Between"));
    }
    
    public void testPattern11() throws Exception {
        tryMatch("findByFooLikeOrBaxBetweenOrBaz", createList("findBy", "Foo", "Like", "Or", "Bax", "Between", "Or", "Baz"));
    }
    
    public void testPattern12() throws Exception {
        tryMatch("countByFooOrBaxBetween", createList("countBy", "Foo", null, "Or", "Bax", "Between"));
    }
    
    public void testPattern13() throws Exception {
        tryMatch("countByFooBetweenOrBax", createList("countBy", "Foo", "Between", "Or", "Bax", null));
    }
    
    // This is an illegal pattern since same component is used twice, but we are not checking for that here.
    public void testPattern14() throws Exception {
        tryMatch("countByFooBetweenOrFoo", createList("countBy", "Foo", "Between", "Or", "Foo", null));
    }
    
    // This is an illegal pattern since same component is used twice, but we are not checking for that here.
    public void testPattern15() throws Exception {
        tryMatch("countByFooOrFoo", createList("countBy", "Foo", null, "Or", "Foo", null));
    }
    
    // Grails 2.0+ style finders
    public void testPattern16() throws Exception {
        tryMatch("countByFooBetweenOrBaxOrBax", createList("countBy", "Foo", "Between", "Or", "Bax", null, "Or", "Bax", null));
    }

    // Grails 2.0+ style finders
    // invalid since operators are differenet
    public void testPattern17() throws Exception {
        tryMatch("countByFooBetweenOrBaxBetweenOrBax", createList("countBy", "Foo", "Between", "Or", "Bax", "Between", "Or", "Bax", null));
    }
    
    // Grails 2.0+ style finders
    // invalid since operators are differenet
    public void testPattern18() throws Exception {
        tryMatch("countByFooBetweenOrBaxOrBaxBetween", createList("countBy", "Foo", "Between", "Or", "Bax", null, "Or", "Bax", "Between"));
    }
    
    // Grails 2.0+ style finders
    public void testPattern19() throws Exception {
        tryMatch("countByFooBetweenOrFooOrBaxOrFooOrFooBetween", createList("countBy", "Foo", "Between", "Or", "Foo", null, "Or", "Bax", null, "Or", "Foo", null, "Or", "Foo", "Between"));
    }
    
    public void testValidate1() throws Exception {
        validate("", props(), false);
    }

    public void testValidate2() throws Exception {
        validate("findBy", props(), false);
    }
    
    public void testValidate3() throws Exception {
        validate("findBy", props("foo", "bar"), false);
    }
    
    public void testValidate4() throws Exception {
        validate("findByFoo", props("foo", "bar"), true);
    }
    
    public void testValidate5() throws Exception {
        validate("findByFooAnd", props("foo", "bar"), false);
    }
    
    public void testValidate6() throws Exception {
        validate("findByFooOrBar", props("foo", "bar"), true);
    }
    
    public void testValidate7() throws Exception {
        validate("findByFooAndBarOr", props("foo", "bar"), false);
    }
    
    public void testValidate8() throws Exception {
        validate("findByFooAndBarOrFoo", props("foo", "bar"), false);
    }
    
    public void testValidate9() throws Exception {
        validate("findByFAndBarOrFoo", props("foo", "bar"), false);
    }
    
    public void testValidate10() throws Exception {
        validate("countByFooAndBar", props("foo", "bar"), true);
    }
    
    public void testValidate11() throws Exception {
        validate("findAllByFooAndBar", props("foo", "bar"), true);
    }
    
    public void testValidate12() throws Exception {
        validate("listOrderByFooAndBar", props("foo", "bar"), true);
    }
    
    public void testValidate13() throws Exception {
        validate("listOrderByFooNotEqual", props("foo"), true);
    }
    
    public void testValidate14() throws Exception {
        validate("listOrderByFooAndBarNotEqual", props("foo", "bar"), true);
    }

    public void testValidate15() throws Exception {
        validate("listOrderByFooAndFooNotEqual", props("foo", "bar"), false);
    }
    
    public void testValidate16() throws Exception {
        validate("listOrderBy", props("foo", "bar"), false);
    }
    
    public void testValidate17() throws Exception {
        validate("listOrderByFooLikeOrBar", props("foo", "bar"), true);
    }
    
    public void testValidate18() throws Exception {
        validate("listOrderByFooLikeOrBarLike", props("foo", "bar"), true);
    }
    
    public void testValidate19() throws Exception {
        validate("listOrderByFooLikeBarLike", props("foo", "bar"), false);
    }
    
    public void testValidate20() throws Exception {
        validate("listOrderByFooLikeOrBarLik", props("foo", "bar"), false);
    }
    
    public void testValidate21() throws Exception {
        validate("listOrderByFooLikeOrFooLike", props("foo", "bar"), false);
    }
    
    public void testValidate22() throws Exception {
        validate("listOrderByFooLikeOrFoo", props("foo", "bar"), false);
    }
    
    public void testValidate23() throws Exception {
        validate("listOrderByFooOrFoo", props("foo", "bar"), false);
    }
    
    // Grails 2.0+ style finders
    public void testValidate24() throws Exception {
        validate("countByFooBetweenOrBaxOrBaz", props("foo", "baz", "bax"), true);
    }

    // Grails 2.0+ style finders
    public void testValidate25() throws Exception {
        validate("countByFooBetweenOrBaxBetweenOrBax", props("foo", "baz", "bax"), false);
    }
    
    // Grails 2.0+ style finders
    // invalid since operators are differenet
    public void testValidate26() throws Exception {
        validate("countByFooBetweenOrBaxAndBaz", props("foo", "baz", "bax"), false);
    }
    
    // Grails 2.0+ style finders
    public void testValidate27() throws Exception {
        validate("countByFooBetweenOrBarOrBazOrBopOrBaxBetween", props("foo", "bar", "baz", "bax", "bop"), true);
    }
    // Grails 2.0+ style finders
    public void testValidate27a() throws Exception {
        validate("countByFooOrBarBetweenOrBazOrBopOrBaxBetween", props("foo", "bar", "baz", "bax", "bop"), true);
    }
    // Grails 2.0+ style finders
    public void testValidate27b() throws Exception {
        validate("countByFooOrBarOrBazBetweenOrBopOrBaxBetween", props("foo", "bar", "baz", "bax", "bop"), true);
    }
    // Grails 2.0+ style finders
    public void testValidate27c() throws Exception {
        validate("countByFooOrBarOrBazOrBopBetweenOrBaxBetween", props("foo", "bar", "baz", "bax", "bop"), true);
    }

    // Grails 2.0+ style finders
    // failure since we only suport max of 5 components
    public void testValidate28() throws Exception {
        validate("countByFooBetweenOrBarOrBazOrBopOrBaxBetweenTooMany", props("foo", "bar", "baz", "bax", "bop", "other"), false);
    }

    
    
    public void testCompletions1() throws Exception {
        // duplicates are because both method and field variants are called
        assertCompletions("findBy", "findByFoo", "findByFoo", "findByBax", "findByBax", "findByBaz", "findByBaz");
    }

    public void testCompletions2() throws Exception {
        assertCompletions("findByF", "findByFoo", "findByFoo");
    }
    
    public void testCompletions3() throws Exception {
        assertCompletions("findByFoo", "findByFooBetween", "findByFooNotEqual", 
                "findByFooGreaterThan", "findByFooGreaterThanEquals", "findByFooIlike", 
                "findByFooInList", "findByFooIsNull", "findByFooIsNotNull", 
                "findByFooLessThan", "findByFooLessThanEquals", "findByFooLike", 
                "findByFooNot", "findByFooAnd", "findByFooOr", "findByFooBetween", 
                "findByFooNotEqual", "findByFooGreaterThan", "findByFooGreaterThanEquals", 
                "findByFooIlike", "findByFooInList", "findByFooIsNull", "findByFooIsNotNull", 
                "findByFooLessThan", "findByFooLessThanEquals", "findByFooLike", 
                "findByFooNot");
    }
    
    public void testCompletions4() throws Exception {
        assertCompletions("findByFooO", "findByFooOr");
    }
    public void testCompletions5() throws Exception {
        assertCompletions("findByFooA", "findByFooAnd");
    }
    public void testCompletions6() throws Exception {
        assertCompletions("findByFooAn", "findByFooAnd");
    }
    public void testCompletions7() throws Exception {
        assertCompletions("findByFooIl", "findByFooIlike", "findByFooIlike");
    }
    public void testCompletions8() throws Exception {
        assertCompletions("findByFooIlike", "findByFooIlikeAnd", "findByFooIlikeOr");
    }
    public void testCompletions9() throws Exception {
        assertCompletions("findByFooAnd", "findByFooAndBax", "findByFooAndBaz", "findByFooAndBax", "findByFooAndBaz");
    }
    public void testCompletions9a() throws Exception {
        assertCompletions("findByFooAndBa", "findByFooAndBax", "findByFooAndBaz", "findByFooAndBax", "findByFooAndBaz");
    }
    public void testCompletions10() throws Exception {
        assertCompletions("findByFooIlikeOr", "findByFooIlikeOrBax", "findByFooIlikeOrBaz", "findByFooIlikeOrBax", "findByFooIlikeOrBaz");
    }
    public void testCompletions10a() throws Exception {
        assertCompletions("findByFooIlikeOrB", "findByFooIlikeOrBax", "findByFooIlikeOrBaz", "findByFooIlikeOrBax", "findByFooIlikeOrBaz");
    }
    public void testCompletions11() throws Exception {
        assertCompletions("findByFooOrBaz", "findByFooOrBazBetween", "findByFooOrBazNotEqual", 
                "findByFooOrBazGreaterThan", "findByFooOrBazGreaterThanEquals", 
                "findByFooOrBazIlike", "findByFooOrBazInList", "findByFooOrBazIsNull", 
                "findByFooOrBazIsNotNull", "findByFooOrBazLessThan", 
                "findByFooOrBazLessThanEquals", "findByFooOrBazLike", "findByFooOrBazNot",
                "findByFooOrBazBetween", "findByFooOrBazNotEqual", "findByFooOrBazGreaterThan",
                "findByFooOrBazGreaterThanEquals", "findByFooOrBazIlike", "findByFooOrBazInList",
                "findByFooOrBazIsNull", "findByFooOrBazIsNotNull", "findByFooOrBazLessThan", 
                "findByFooOrBazLessThanEquals", "findByFooOrBazLike", "findByFooOrBazNot",
                "findByFooOrBazOr");
    }
    public void testCompletions12() throws Exception {
        assertCompletions("findByFooIlikeOrBaz", "findByFooIlikeOrBazBetween", 
                "findByFooIlikeOrBazNotEqual", "findByFooIlikeOrBazGreaterThan", 
                "findByFooIlikeOrBazGreaterThanEquals", "findByFooIlikeOrBazIlike", 
                "findByFooIlikeOrBazInList", "findByFooIlikeOrBazIsNull", 
                "findByFooIlikeOrBazIsNotNull", "findByFooIlikeOrBazLessThan", 
                "findByFooIlikeOrBazLessThanEquals", "findByFooIlikeOrBazLike", 
                "findByFooIlikeOrBazNot", "findByFooIlikeOrBazBetween", 
                "findByFooIlikeOrBazNotEqual", "findByFooIlikeOrBazGreaterThan", 
                "findByFooIlikeOrBazGreaterThanEquals", "findByFooIlikeOrBazIlike", 
                "findByFooIlikeOrBazInList", "findByFooIlikeOrBazIsNull", 
                "findByFooIlikeOrBazIsNotNull", "findByFooIlikeOrBazLessThan", 
                "findByFooIlikeOrBazLessThanEquals", "findByFooIlikeOrBazLike", 
                "findByFooIlikeOrBazNot", "findByFooIlikeOrBazOr");
    }
    public void testCompletions13() throws Exception {
        assertCompletions("findByFooOrBazB", "findByFooOrBazBetween", "findByFooOrBazBetween");
    }
    public void testCompletions14() throws Exception {
        assertCompletions("findByFooIlikeOrBazB", "findByFooIlikeOrBazBetween", "findByFooIlikeOrBazBetween");
    }
    
    public void testCompletions15() throws Exception {
        assertCompletions("findByFooIlikeOrBazOr", "findByFooIlikeOrBazOrBax", "findByFooIlikeOrBazOrBax");
    }
    
    public void testCompletions16() throws Exception {
        assertCompletions("findByFooIlikeOrBazAnd");
    }
    
    public void testCompletions16a() throws Exception {
        assertCompletions("findByFooIlikeOrBazOrBaxOr");
    }
    
    public void testCompletions17() throws Exception {
        assertCompletions("findByFooIlikeOrBazOrBax", "findByFooIlikeOrBazOrBaxBetween", 
                "findByFooIlikeOrBazOrBaxNotEqual", "findByFooIlikeOrBazOrBaxGreaterThan", 
                "findByFooIlikeOrBazOrBaxGreaterThanEquals", "findByFooIlikeOrBazOrBaxIlike", 
                "findByFooIlikeOrBazOrBaxInList", "findByFooIlikeOrBazOrBaxIsNull", 
                "findByFooIlikeOrBazOrBaxIsNotNull", "findByFooIlikeOrBazOrBaxLessThan", 
                "findByFooIlikeOrBazOrBaxLessThanEquals", "findByFooIlikeOrBazOrBaxLike", 
                "findByFooIlikeOrBazOrBaxNot", "findByFooIlikeOrBazOrBaxBetween", 
                "findByFooIlikeOrBazOrBaxNotEqual", "findByFooIlikeOrBazOrBaxGreaterThan", 
                "findByFooIlikeOrBazOrBaxGreaterThanEquals", "findByFooIlikeOrBazOrBaxIlike", 
                "findByFooIlikeOrBazOrBaxInList", "findByFooIlikeOrBazOrBaxIsNull", 
                "findByFooIlikeOrBazOrBaxIsNotNull", "findByFooIlikeOrBazOrBaxLessThan", 
                "findByFooIlikeOrBazOrBaxLessThanEquals", "findByFooIlikeOrBazOrBaxLike", 
                "findByFooIlikeOrBazOrBaxNot");
    }
    
    public void testCompletions18() throws Exception {
        assertCompletions("findByFooIlikeOrBazOrBaxN", "findByFooIlikeOrBazOrBaxNot", 
                "findByFooIlikeOrBazOrBaxNotEqual", "findByFooIlikeOrBazOrBaxNot",
                "findByFooIlikeOrBazOrBaxNotEqual");
    }
    
    private List<String> createList(String ... components) {
        List<String> l = new ArrayList<String>(15);
        for (String component : components) {
            l.add(component);
        }
        while (l.size() < 15) {
            l.add(null);
        }
        return l;
    }
    
    private void assertCompletions(String finderName, String...expectedCompletions) {
        List<AnnotatedNode> proposals = new MockDynamicFinderValidator().findProposals(finderName);
        List<String> actual = new LinkedList<String>();
        for (AnnotatedNode proposed : proposals) {
            if (proposed instanceof FieldNode) {
                actual.add(((FieldNode) proposed).getName());
            } else if (proposed instanceof MethodNode) {
                actual.add(((MethodNode) proposed).getName());
            }
        }
        
        Set<String> notFound = new HashSet<String>();
        for (String expected : expectedCompletions) {
            if (!actual.remove(expected)) {
                notFound.add(expected);
            }
        }
        
        if (notFound.size() > 0 || actual.size() > 0) {
            fail("Mismatch of completions:\n" + "Should have found but didn't: " + notFound + "\nShouldn't have found but did: " + actual);
        }
    }
    
    private HashSet<String> props(String...properties) {
        return new HashSet<String>(Arrays.asList(properties));
    }
    
    private void validate(String input, Set<String> properties, boolean expected) {
        System.out.println("------------------");
        System.out.println("Validate: " + input);
        System.out.println("Against: " + properties);
        System.out.println("Valid: " + expected);
        boolean isValid = new MockDynamicFinderValidator(properties).isValidFinderName(input);
        assertEquals("Input should be valid: " + expected + "\n but wasn't:\n" + input, expected, isValid);
    }
    
    private void tryMatch(String input, List<String> expected) {
        System.out.println("------------------");
        System.out.println("ToMatch: " + input);
        
        List<String> components = new MockDynamicFinderValidator().getFinderComponents(input);
        if (expected == null) {
            assertNull("Should have been null: " + input,  components);
        } else {
            System.out.println(components);
            assertTrue("Expected: " + expected + "\nBut found: " + components + "\nIn: " + input, 
                    Arrays.equals(expected.toArray(), components.toArray()));
        }
    }

}
