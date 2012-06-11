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

import junit.framework.Test;



/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @since 2.9.0
 */
public class ControllerReturnTypeInferencingTests extends AbstractGrailsInferencingTests {
    public static Test suite() {
        return buildTestSuite(ControllerReturnTypeInferencingTests.class);
    }

    public ControllerReturnTypeInferencingTests(String name) {
        super(name);
    }
    
    public void testReturnType1() throws Exception {
        createControllerClass("FlarController", 
                "class FlarController {\n" +
        		"  def list = { [first : [''], second : 9 ] }\n" +
        		"}");
        
        String contents = "new FlarController().list().first\n" +
        		"new FlarController().list().second";
        int start = contents.indexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClass(contents, start, end, "java.util.List<java.lang.String>");
        start = contents.indexOf("second");
        end = start + "second".length();
        assertTypeInDomainClass(contents, start, end, "java.lang.Integer");
        start = contents.indexOf("list");
        end = start + "list".length();
        assertTypeInDomainClass(contents, start, end, "java.util.Map");
    }
    public void testReturnType2() throws Exception {
        createControllerClass("FlarController", 
                "class FlarController {\n" +
                        "  def list() { [first : [''], second : 9 ] }\n" +
                "}");
        
        String contents = "new FlarController().list().first\n" +
                "new FlarController().list().second";
        int start = contents.indexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClass(contents, start, end, "java.util.List<java.lang.String>");
        start = contents.indexOf("second");
        end = start + "second".length();
        assertTypeInDomainClass(contents, start, end, "java.lang.Integer");
        start = contents.indexOf("list");
        end = start + "list".length();
        assertTypeInDomainClass(contents, start, end, "java.util.Map");
    }
    
    public void testReturnType3() throws Exception {
        createControllerClass("FlarController", 
                "class FlarController {\n" +
                        "  def list() { [first : [9:''], second : ['':9] ] }\n" +
                "}");
        
        String contents = "def x = new FlarController().list()\n" +
                "x.first\n" +
                "x.second";
        int start = contents.indexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClass(contents, start, end, "java.util.Map<java.lang.Integer,java.lang.String>");
        start = contents.indexOf("second");
        end = start + "second".length();
        assertTypeInDomainClass(contents, start, end, "java.util.Map<java.lang.String,java.lang.Integer>");
        start = contents.lastIndexOf("x");
        end = start + "x".length();
        assertTypeInDomainClass(contents, start, end, "java.util.Map");
    }
}
