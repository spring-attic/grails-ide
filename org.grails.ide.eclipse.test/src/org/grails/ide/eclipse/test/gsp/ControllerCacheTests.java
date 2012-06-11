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
package org.grails.ide.eclipse.test.gsp;

import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.groovy.controllers.ControllerCache;
import org.grails.ide.eclipse.editor.groovy.controllers.PerProjectControllerCache;
import org.grails.ide.eclipse.groovy.debug.tests.GroovyUtils;
import org.grails.ide.eclipse.test.GrailsTestsActivator;

/**
 * 
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class ControllerCacheTests extends AbstractGSPTagsTest {

    public void testControllerCacheEmpty1() throws Exception {
        assertControllerCacheContents("def index = { }");
    }
    
    public void testControllerCacheEmpty2() throws Exception {
        assertControllerCacheContents("def index2 = { [foo:'', bar:9] }\ndef index = { }");
    }
    
    public void testControllerCacheEmpty3() throws Exception {
        assertControllerCacheContents("def index = { }\ndef index2 = { [foo:'', bar:9] }");
    }
    
    public void testControllerCacheEmpty4() throws Exception {
        assertControllerCacheContents("def index = { 999 }");
    }
    
    public void testControllerCacheSimple1() throws Exception {
        assertControllerCacheContents("def index = { [foo:'', bar:9] }",
                "foo", "java.lang.String", "bar", "java.lang.Integer");
    }
    
    public void testControllerCacheSimple2() throws Exception {
        assertControllerCacheContents("def index = { def x = ''\ndef y = 9\n [foo:x, bar:y] }",
                "foo", "java.lang.String", "bar", "java.lang.Integer");
    }
    
    public void testControllerCacheMultipleReturn1() throws Exception {
        assertControllerCacheContents("def index = { \n" +
        		"def x = ''\n" +
        		"def y = 9\n " +
        		"if (true) {\n" +
        		"  [foo:x, bar:y]\n" +
        		"} else {" +
        		"  [foo:x, bar:y]\n" +
        		"} }",
                "foo", "java.lang.String", "bar", "java.lang.Integer");
    }
    
    public void testControllerCacheMultipleReturn2() throws Exception {
        assertControllerCacheContents("def index = { \n" +
                "def x = ''\n" +
                "def y = 9\n " +
                "if (true) {\n" +
                "  [foo:x, bar:y]\n" +
                "} else {" +
                "  [foo2:x, bar2:y]\n" +
                "} }",
                "foo", "java.lang.String", "bar", "java.lang.Integer", 
                "foo2", "java.lang.String", "bar2", "java.lang.Integer");
    }
    
    public void testControllerCacheGenerics1() throws Exception {
        String resultType;
        resultType = "java.util.Map<java.lang.Integer,java.lang.Integer>";
        assertControllerCacheContents("def index = { [foo:[''], bar:[9:9] ] }",
                "foo", "java.util.List<java.lang.String>", "bar", resultType);
    }

    
    // these tests are of the PerProjectControllerCache
    
    public void testPerProjectCC1() throws Exception {
        assertPerProjectCacheContents("def index = { def x = ''\ndef y = 9\n [foo:x, bar:y] }", "java.lang.String foo;\njava.lang.Integer bar;\n");
    }
    
    public void testPerProjectCC2() throws Exception {
        assertPerProjectCacheContents("def index2 = { [fsdfsafd : 888 ]}\n def index = { def x = ''\ndef y = 9\n [foo:x, bar:y] }", "java.lang.String foo;\njava.lang.Integer bar;\n");
    }
    
    public void testPerProjectCC3() throws Exception {
        assertPerProjectCacheContents("def index = { def x = new String[0]\ndef y = new int[0]\n [foo:x, bar:y] }", "java.lang.String[] foo;\njava.lang.Integer[] bar;\n");
    }
    
    public void testPerProjectCC4() throws Exception {
        assertPerProjectCacheContents("def index = { def x = new String[0][]\ndef y = new int[0][]\n [foo:x, bar:y] }", "java.lang.String[][] foo;\njava.lang.Integer[][] bar;\n");
    }
    
    private void assertControllerCacheContents(String controllerContents, String...nameClassPairs) throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            // controllers are now methods
            controllerContents = controllerContents.replaceAll(" = \\{", " () {");
        }
        
        ControllerCache cache = new ControllerCache(createController(controllerContents));
        Map<String, ClassNode> returnValues = cache.findReturnValuesForAction("index");
        assertEquals("Wrong number of values found in map: " + returnValues, nameClassPairs.length / 2, returnValues.size());
        for (int i = 0; i < nameClassPairs.length; i+=2) {
            assertEquals("Wrong class name", nameClassPairs[i+1], printTypeName(returnValues.get(nameClassPairs[i])));
        }
    }
    
    private void assertPerProjectCacheContents(String controllerContents, String expectedReturnVals) throws Exception {
        createController(controllerContents);
        PerProjectControllerCache cache = GrailsCore.get().connect(testProject.getProject(), PerProjectControllerCache.class);
        String returnVals = cache.findReturnValuesAsDeclarations("nuthin", "index");
        assertEquals(expectedReturnVals, returnVals);
    }
}
