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

import java.util.Map;

import junit.framework.Test;

import org.codehaus.groovy.ast.ClassNode;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.editor.groovy.types.PerProjectServiceCache;



/**
 * Tests for service injection
 * @author Andrew Eisenberg
 * @since 2.6.0
 */
public class GrailsServiceInferencingTests extends AbstractGrailsInferencingTests {
    public static Test suite() {
        return buildTestSuite(GrailsServiceInferencingTests.class);
    }

    public GrailsServiceInferencingTests(String name) {
        super(name);
    }
    
    private final String CONTENTS = "class Search { \n def myService\n def xxx() {\n myService.var } \n}";
    private final String CONTROLLER_CONTENTS = "class SearchController { \n def myService\n def xxx() {\n myService.var } \n}";
    
    // test services in service, taglib, domain, controller, and build config
    
    public void testServiceInjectionInDomainClass1() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("myService");
        assertTypeInDomainClassNoPrefix(CONTENTS, loc, loc + "myService".length(), "MyService");
    }
    public void testServiceInjectionInDomainClass2() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("var");
        assertTypeInDomainClassNoPrefix(CONTENTS, loc, loc + "var".length(), "java.lang.Boolean");
    }
    
    public void testServiceInjectionInControllerClass1() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTROLLER_CONTENTS.lastIndexOf("myService");
        assertTypeInControllerClass(CONTROLLER_CONTENTS, loc, loc + "myService".length(), "MyService");
    }
    public void testServiceInjectionInControllerClass2() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTROLLER_CONTENTS.lastIndexOf("var");
        assertTypeInControllerClass(CONTROLLER_CONTENTS, loc, loc + "var".length(), "java.lang.Boolean");
    }

    public void testServiceInjectionInTagLibClass1() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("myService");
        assertTypeInTagLib(CONTENTS, loc, loc + "myService".length(), "MyService");
    }
    public void testServiceInjectionInTagLibClass2() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("var");
        assertTypeInTagLib(CONTENTS, loc, loc + "var".length(), "java.lang.Boolean");
    }
    
    public void testServiceInjectionInServiceClass1() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("myService");
        assertTypeInService(CONTENTS, loc, loc + "myService".length(), "MyService");
    }
    public void testServiceInjectionInServiceClass2() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("var");
        assertTypeInService(CONTENTS, loc, loc + "var".length(), "java.lang.Boolean");
    }
    
    public void testServiceInjectionInBuildConfigClass1() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("myService");
        assertTypeInBuildConfig(CONTENTS, loc, loc + "myService".length(), "MyService");
    }
    public void testServiceInjectionInBuildConfigClass2() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("var");
        assertTypeInBuildConfig(CONTENTS, loc, loc + "var".length(), "java.lang.Boolean");
    }
    
    public void testServiceInjectionInDomainClassWithChange() throws Exception {
        createServiceClass("MyService", "class MyService { \n boolean var \n }");
        int loc = CONTENTS.lastIndexOf("var");
        assertTypeInDomainClassNoPrefix(CONTENTS, loc, loc + "var".length(), "java.lang.Boolean");

        // Now change the service and the service registry should be updated
        createServiceClass("MyService", "class MyService { \n String var \n }");
        loc = CONTENTS.lastIndexOf("var");
        assertTypeInDomainClassNoPrefix(CONTENTS, loc, loc + "var".length(), "java.lang.String");
    }

	private void assertService(String expectedService, Map<String, ClassNode> allServices) {
        assertTrue("Should have found "+expectedService+": \n" + allServices, allServices.containsKey(expectedService));
	}
    
}
