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
import java.util.List;

import junit.framework.Test;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.types.DynamicFinderValidator;
import org.grails.ide.eclipse.editor.groovy.types.FinderValidatorFactory;

/**
 * A variant of {@link DynamicFinderProposalsTests} that completes on lower case expressions
 * @author Andrew Eisenberg
 * @created Nov 4, 2011
 */
public class CaseInsensitiveDynamicFinderProposalsTests  extends AbstractGrailsInferencingTests {
    public static Test suite() {
        return buildTestSuite(CaseInsensitiveDynamicFinderProposalsTests.class);
    }

    public CaseInsensitiveDynamicFinderProposalsTests(String name) {
        super(name);
    }
    
    public void testDynamicFinderMethodProposals1() throws Exception {
        assertProposedMethods("findbyfoo", 
                new String[] { "foo", "bar" }, 
                new String[] { "findByFooBetween", "findByFooNot", 
                "findByFooGreaterThan", "findByFooGreaterThanEquals", "findByFooIlike", "findByFooInList", 
                "findByFooIsNull", "findByFooIsNotNull", "findByFooLessThan", "findByFooLessThanEquals", 
                "findByFooLike", "findByFooNotEqual", "findByFooOr", "findByFooAnd", "findByFooBetween", "findByFooNot", 
                "findByFooGreaterThan", "findByFooGreaterThanEquals", "findByFooIlike", "findByFooInList", 
                "findByFooIsNull", "findByFooIsNotNull", "findByFooLessThan", "findByFooLessThanEquals", 
                "findByFooLike", "findByFooNotEqual" });
    }
    
    public void testDynamicFinderMethodProposals2() throws Exception {
        assertProposedMethods("findbyfo", 
                new String[] { "foo", "bar" }, 
                new String[] { "findByFoo", "findByFoo" });
    }

    public void testDynamicFinderMethodProposals2a() throws Exception {
        assertProposedMethods("findbyfo", 
                new String[] { "foo" }, 
                new String[] { "findByFoo", "findByFoo" });
    }
    
    public void testDynamicFinderMethodProposals3() throws Exception {
        assertProposedMethods("findby", 
                new String[] { "foo", "bar" }, 
                new String[] { "findByFoo", "findByBar", "findByFoo", "findByBar" });
    }
    
    public void testDynamicFinderMethodProposals4() throws Exception {
        assertProposedMethods("countby", 
                new String[] { "foo", "bar" }, 
                new String[] { "countByFoo", "countByBar", "countByFoo", "countByBar" });
    }
    
    public void testDynamicFinderMethodProposals5() throws Exception {
        assertProposedMethods("findallby", 
                new String[] { "foo", "bar" }, 
                new String[] { "findAllByFoo", "findAllByBar", "findAllByFoo", "findAllByBar" });
    }
    
    public void testDynamicFinderMethodProposals6() throws Exception {
        assertProposedMethods("listorderby", 
                new String[] { "foo", "bar" }, 
                new String[] { "listOrderByFoo", "listOrderByBar", "listOrderByFoo", "listOrderByBar" });
    }
    
    public void testDynamicFinderMethodProposals7a() throws Exception {
        assertProposedMethods("findbyfooandbaz", 
                new String[] { "foo", "bar", "bar" }, 
                new String[] {  });
    }
    
    public void testDynamicFinderMethodProposals7b() throws Exception {
        assertProposedMethods("findbybaz", 
                new String[] { "foo", "bar" }, 
                new String[] {  });
    }
    
    public void testDynamicFinderMethodProposals7c() throws Exception {
        assertProposedMethods("findbar", 
                new String[] { "foo", "bar" }, 
                new String[] {  });
    }
    
    public void testDynamicFinderMethodProposals8a() throws Exception {
        assertProposedMethods("findbybarorfoo", 
                new String[] { "foo", "bar", "baz", "bof" }, 
 new String[] { "findByBarOrFooBetween",
                "findByBarOrFooNot", "findByBarOrFooGreaterThan",
                "findByBarOrFooGreaterThanEquals", "findByBarOrFooIlike",
                "findByBarOrFooInList", "findByBarOrFooIsNull",
                "findByBarOrFooIsNotNull", "findByBarOrFooLessThan",
                "findByBarOrFooLessThanEquals", "findByBarOrFooLike",
                "findByBarOrFooNotEqual", "findByBarOrFooBetween",
                "findByBarOrFooNot", "findByBarOrFooGreaterThan",
                "findByBarOrFooGreaterThanEquals", "findByBarOrFooIlike",
                "findByBarOrFooInList", "findByBarOrFooIsNull",
                "findByBarOrFooIsNotNull", "findByBarOrFooLessThan",
                "findByBarOrFooLessThanEquals", "findByBarOrFooLike",
                "findByBarOrFooNotEqual", "findByBarOrFooOr" });
    }
    
    public void testDynamicFinderMethodProposals8b() throws Exception {
        assertProposedMethods("findbybaro", 
                new String[] { "foo", "bar", "baz" }, 
                new String[] { "findByBarOr" });
    }
    
    public void testDynamicFinderMethodProposals9() throws Exception {
        assertProposedMethods("findbyp1ando", 
                new String[] { "p1", "other", "other2" }, 
                new String[] { "findByP1AndOther", "findByP1AndOther2", "findByP1AndOther", "findByP1AndOther2" });
    }
    
    public void testDynamicFinderMethodProposals10() throws Exception {
        assertProposedMethods("findbyfoogreat", 
                new String[] { "foo", "bar" }, 
                new String[] { "findByFooGreaterThan", "findByFooGreaterThanEquals", "findByFooGreaterThan", "findByFooGreaterThanEquals" });
    }
    
    public void testDynamicFinderMethodProposals11a() throws Exception {
        assertProposedMethods("findbyfoofooa", 
                new String[] { "fooFoo", "barBar" }, 
                new String[] { "findByFooFooAnd" });
    }
    
    public void testDynamicFinderMethodProposals11b() throws Exception {
        assertProposedMethods("findbyfoofooan", 
                new String[] { "fooFoo", "barBar" }, 
                new String[] { "findByFooFooAnd" });
    }
    
    public void testDynamicFinderMethodProposals11c() throws Exception {
        assertProposedMethods("findbyfoofooandb", 
                new String[] { "fooFoo", "barBar" }, 
                new String[] { "findByFooFooAndBarBar", "findByFooFooAndBarBar" });
    }
    
    public void testDynamicFinderMethodProposals11d() throws Exception {
        assertProposedMethods("findbyfoofooandbarb", 
                new String[] { "fooFoo", "barBar" }, 
                new String[] { "findByFooFooAndBarBar", "findByFooFooAndBarBar" });
    }
    
    public void testDynamicFinderMethodWithSuper1() throws Exception {
        assertProposedMethods("findbyfooandb",
                new String[] { "foo", "bar" }, 
                new String[] { },
                new String[] { "findByFooAndBar", "findByFooAndBar" });
    }
    
    public void testDynamicFinderMethodWithSuper2() throws Exception {
        assertProposedMethods("findbyfooandb",
                new String[] { },
                new String[] { "foo", "bar" }, 
                new String[] { "findByFooAndBar", "findByFooAndBar" });
    }
    
    public void testDynamicFinderMethodWithSuper3() throws Exception {
        assertProposedMethods("findbyfooandb",
                new String[] { "bar" },
                new String[] { "foo" }, 
                new String[] { "findByFooAndBar", "findByFooAndBar" });
    }
    
    public void testDynamicFinderMethodWithSuper4() throws Exception {
        assertProposedMethods("findbyfooand",
                new String[] { "foo", "bar" },
                new String[] { "foo", "bar" }, 
                new String[] { "findByFooAndBar", "findByFooAndBar" });
    }
    
    private void assertProposedMethods(String finderName, String[] properties, String[] expectedMethodProposalNames) throws JavaModelException {
        assertProposedMethods(finderName, properties, null, expectedMethodProposalNames);
    }
    private void assertProposedMethods(String finderName, String[] properties, String[] superProperties, String[] expectedMethodProposalNames) throws JavaModelException {
        String superName = null;
        if (superProperties != null) {
            superName = "SearchSuper";
            createDomainClass("SearchSuper", createDomainTextWithSuper(null, null, true, superProperties));
        }
        String contents = createDomainTextWithSuper(finderName, superName, false, properties);
        // ensure this is executed in a static context
        GroovyCompilationUnit unit = createDomainClass("Search", contents);
        DynamicFinderValidator validator = new FinderValidatorFactory().createValidator(GrailsWorkspaceCore.get().create(project).getDomainClass(unit));
        List<AnnotatedNode> proposed = validator.findProposals(finderName);
        String[] actualMethodProposalNames = getProposalNames(proposed);
        Arrays.sort(expectedMethodProposalNames);
        Arrays.sort(actualMethodProposalNames);
        
        assertTrue("Actual and expected proposals not the same.\nExpected: " + 
                Arrays.toString(expectedMethodProposalNames) +
                "\nActual: " + Arrays.toString(actualMethodProposalNames), 
                testSameElements(expectedMethodProposalNames, actualMethodProposalNames));
    }

    /**
     * @param expectedMethodProposalNames
     * @param actualMethodProposalNames
     * @return
     */
    private boolean testSameElements(String[] expectedMethodProposalNames, String[] actualMethodProposalNames) {
        List<String> expectedSet = new ArrayList<String>();
        for (String expected : expectedMethodProposalNames) {
            expectedSet.add(expected);
        }
        
        for (String actual : actualMethodProposalNames) {
            if (!expectedSet.contains(actual)) {
                return false;
            }
            expectedSet.remove(actual);
        }
        return expectedSet.size() == 0;
    }
    
    
    private String[] getProposalNames(List<AnnotatedNode> proposed) {
        String[] actualMethodProposalNames = new String[proposed.size()];
        for (int i = 0; i < actualMethodProposalNames.length; i++) {
            AnnotatedNode annotatedNode = proposed.get(i);
            if (annotatedNode instanceof FieldNode) {
                actualMethodProposalNames[i] = ((FieldNode) annotatedNode).getName();
            } else if (annotatedNode instanceof MethodNode) {
                actualMethodProposalNames[i] = ((MethodNode) annotatedNode).getName();
            }
        }
        return actualMethodProposalNames;
    }

}
