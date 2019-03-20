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
package org.grails.ide.eclipse.test.inferencing;

import junit.framework.Test;

import org.eclipse.jdt.groovy.search.TypeLookupResult.TypeConfidence;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;

import org.grails.ide.eclipse.test.GrailsTestsActivator;



/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Dec 14, 2009
 */
public class GrailsInferencingTests extends AbstractGrailsInferencingTests {
    public static Test suite() {
        return buildTestSuite(GrailsInferencingTests.class);
    }

    public GrailsInferencingTests(String name) {
        super(name);
    }
    
    public void testControllerClass1() throws Exception {
        assertTypeInControllerClass("request", "javax.servlet.http.HttpServletRequest");
    }
    public void testControllerClass2() throws Exception {
        assertTypeInControllerClass("response", "javax.servlet.http.HttpServletResponse");
    }
    
    public void testDomainClass1() throws Exception {
        assertTypeInDomainClass("executeQuery", "java.util.List<Search>");
        String expectedDeclaring;
        if (GrailsTestsActivator.isGrails200OrLater()) {
            expectedDeclaring = "org.grails.datastore.gorm.GormStaticApi<Search>";
        } else {
            expectedDeclaring = "grails.orm.HibernateCriteriaBuilder";
        }
        assertDeclarationTypeInDomainClass("executeQuery", expectedDeclaring);
    }
    
    public void testDomainDeclaringClass2() throws Exception {
        assertDeclarationTypeInDomainClass("withCriteria", "grails.orm.HibernateCriteriaBuilder");
    }
    
    public void testDomainDeclaringClass3() throws Exception {
        assertTypeInDomainClass("createCriteria", "grails.orm.HibernateCriteriaBuilder");
        assertDeclarationTypeInDomainClass("createCriteria", "grails.orm.HibernateCriteriaBuilder");
    }
    
    public void testTagLib1() throws Exception {
        assertTypeInTagLib("servletContext", "javax.servlet.ServletContext");
    }
    
    public void testDomainClass2() throws Exception {
        String contents = "Search.get()";
        int start = contents.indexOf("get");
        int end = start + "get".length();
        assertTypeInDomainClass(contents, start, end, "Search");
    }

    // ensure that list methods are generified
    public void testDomainClass3() throws Exception {
        String contents = "Search.list().get(0)";
        int start = contents.indexOf("get");
        int end = start + "get".length();
        assertTypeInDomainClass(contents, start, end, "Search");
    }
    
    // config classes...should never have a type confidence of UNKNOWN
    public void testConfigClass1() throws Exception {
        assertTypeInConfigClass("blah", "Search", TypeConfidence.LOOSELY_INFERRED, GrailsElementKind.CONFIG);
    }
    public void testConfigClass2() throws Exception {
        String contents = "def blah = \"\"\nblah";
        int start = contents.lastIndexOf("blah");
        int end = start + "blah".length();
        assertTypeInConfigClass(contents, VariableScope.STRING_CLASS_NODE.getName(), TypeConfidence.INFERRED, GrailsElementKind.CONFIG, start, end);
    }
    public void testBuildConfigClass1() throws Exception {
        assertTypeInConfigClass("blah", "Search", TypeConfidence.LOOSELY_INFERRED, GrailsElementKind.BUILD_CONFIG);
    }
    public void testBuildConfigClass2() throws Exception {
        String contents = "def blah = \"\"\nblah";
        int start = contents.lastIndexOf("blah");
        int end = start + "blah".length();
        assertTypeInConfigClass(contents, VariableScope.STRING_CLASS_NODE.getName(), TypeConfidence.INFERRED, GrailsElementKind.BUILD_CONFIG, start, end);
    }
    public void testDataSourceClass1() throws Exception {
        assertTypeInConfigClass("blah", "Search", TypeConfidence.LOOSELY_INFERRED, GrailsElementKind.DATA_SOURCE);
    }
    public void testDataSourceClass2() throws Exception {
        String contents = "def blah = \"\"\nblah";
        int start = contents.lastIndexOf("blah");
        int end = start + "blah".length();
        assertTypeInConfigClass(contents, VariableScope.STRING_CLASS_NODE.getName(), TypeConfidence.INFERRED, GrailsElementKind.DATA_SOURCE, start, end);
    }
    public void testBootStrapClass1() throws Exception {
        assertTypeInConfigClass("blah", "Search", TypeConfidence.LOOSELY_INFERRED, GrailsElementKind.BOOT_STRAP);
    }
    public void testBootStrapClass2() throws Exception {
        String contents = "def blah = \"\"\nblah";
        int start = contents.lastIndexOf("blah");
        int end = start + "blah".length();
        assertTypeInConfigClass(contents, VariableScope.STRING_CLASS_NODE.getName(), TypeConfidence.INFERRED, GrailsElementKind.BOOT_STRAP, start, end);
    }
    public void testURLMappingsClass1() throws Exception {
        assertTypeInConfigClass("blah", "Search", TypeConfidence.LOOSELY_INFERRED, GrailsElementKind.URL_MAPPINGS);
    }
    public void testURLMappingsClass2() throws Exception {
        String contents = "def blah = \"\"\nblah";
        int start = contents.lastIndexOf("blah");
        int end = start + "blah".length();
        assertTypeInConfigClass(contents, VariableScope.STRING_CLASS_NODE.getName(), TypeConfidence.INFERRED, GrailsElementKind.URL_MAPPINGS, start, end);
    }
    
    public void testDynamicFinders1() throws Exception {
        assertDynamicFinderType(true, "findBy", "p1");
    }
    
    public void testDynamicFinders2() throws Exception {
        assertDynamicFinderType(true, "findByP1", "p1");
    }

    public void testDynamicFinders3() throws Exception {
        assertDynamicFinderType(false, "findByP1AndP1", "p1");
    }
    
    public void testDynamicFinders4() throws Exception {
        assertDynamicFinderType(true, "findByP1AndP2", "p1", "p2");
    }

    public void testDynamicFinders5() throws Exception {
        assertDynamicFinderType(true, "findByP1Like", "p1", "p2");
    }
    
    public void testDynamicFinders6() throws Exception {
        assertDynamicFinderType(true, "findByP1AndP2Like", "p1", "p2");
    }
    
    public void testDynamicFinders6a() throws Exception {
        assertDynamicFinderType(true, "findByP1LikeAndP2Like", "p1", "p2");
    }
    
    public void testDynamicFinders6b() throws Exception {
        assertDynamicFinderType(true, "findByP1LikeAndP2", "p1", "p2");
    }
    
    public void testDynamicFinders7() throws Exception {
        assertDynamicFinderType(false, "findByP1");
    }
    
    public void testDynamicFinders7a() throws Exception {
        assertDynamicFinderType(false, "findByP1LikeAndP1", "p1", "p2");
    }
    
    public void testDynamicFinders8() throws Exception {
        assertDynamicFinderType(false, "findByp1", "p1", "p2");
    }
    
    public void testDynamicFinders9() throws Exception {
        assertDynamicFinderTypeArray("listOrderByP1AndP2", "p1", "p2");
    }
    
    public void testDynamicFinders10() throws Exception {
        assertDynamicFinderTypeArray("findAllByP1AndP2", "p1", "p2");
    }
    
    public void testDynamicFinders11() throws Exception {
        assertDynamicFinderTypeInt("countByP1AndP2", "p1", "p2");
    }
    
    public void testDynamicFinders12() throws Exception {
        assertDynamicFinderType("get", createDomainText("findAllByP1().get()", "p1", "p2"), "Search");
    }
    
    public void testDynamicFinders13() throws Exception {
    	// new kind of finder for 2.0
        if (GrailsTestsActivator.isGrails200OrLater()) {
            assertDynamicFinderType(true, "findOrCreateByP1", "p1", "p2");
        } else {
            assertDynamicFinderType(false, "findOrCreateByP1", "p1", "p2");
        }
    }
    
    public void testConstraintsBlock2() throws Exception {
        String contents = 
        		"class Search {\n" +
        		"  String name\n" +
        		"  static constraints = { name }\n" +
        		"}\n";
        int start = contents.lastIndexOf("name");
        int end = start + "name".length();
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, "Search");
    }
    
    public void testConstraintsBlock1() throws Exception {
        String contents = 
                "class Search {\n" +
                "  String name\n" +
                "  static constraints = { name() }\n" +
                "}\n";
        int start = contents.lastIndexOf("name");
        int end = start + "name".length();
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, "Search");
    }
    
    public void testDynamicFindersDeclaration1() throws Exception {
        assertDynamicFinderTypeDeclaration("countByP1AndP2", "p1", "p2");
    }
}
