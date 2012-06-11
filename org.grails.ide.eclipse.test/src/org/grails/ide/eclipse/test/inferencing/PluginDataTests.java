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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;
import org.eclipse.core.resources.IFile;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginVersion;
import org.grails.ide.eclipse.core.internal.classpath.PluginDescriptorParser;
import org.grails.ide.eclipse.core.model.ContributedMethod;
import org.grails.ide.eclipse.core.model.ContributedProperty;


/**
 * Tests that the {@link PluginDescriptorParser} 
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 31, 2010
 */
public class PluginDataTests extends EclipseTestCase {

    public void testPluginXmlNoExists() throws Exception {
        PluginDescriptorParser parser = new PluginDescriptorParser(testProject.getProject().getLocation().append("not_here.xml").toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
    }
    
    public void testEmptyPluginXml() throws Exception {
        IFile file = testProject.createFile("empty.xml", "");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
    }
    
    public void testMalformedXml() throws Exception {
        IFile file = testProject.createFile("plugin.xml", "<plugin name='test' version='1.2-M4' grailsVersion='1.2-M4 &gt; *'>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
    }
    
    public void testNoBody() throws Exception {
        IFile file = testProject.createFile("plugin.xml", "<plugin name='test' version='1.2-M4' grailsVersion='1.2-M4 &gt; *'></plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        assertEquals("test", data.getName());
        assertEquals("1.2-M4", data.getVersion());
        assertEquals("1.2-M4 > *", data.getRuntimeVersion());
    }
    
    public void testNoContributedPropertiesOrMethods() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test' version='1.2-M4' grailsVersion='1.2-M4 &gt; *'>" +
                  "<author>Mr. Foo Bar</author>" + 
                  "<title>Nothing, really</title>" + 
                  "<description>A plugin that provides nothing</description>" + 
                  "<documentation>http://nowhere.org</documentation>" + 
                "</plugin>");
          PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
          GrailsPluginVersion data = parser.parse();
          assertNotNull("Plugin data should not be null", data);
          assertFalse(data.hasContributedPropertiesOrMethods());
    }
    
    
    public void testNoAddedBehaviors() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
              "<plugin name='test' version='1.2-M4' grailsVersion='1.2-M4 &gt; *'>" +
                "<author>Mr. Foo Bar</author>" + 
                "<title>Nothing, really</title>" + 
                "<description>A plugin that provides nothing</description>" + 
                "<documentation>http://nowhere.org</documentation>" + 
              "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        assertEquals("test", data.getName());
        assertEquals("1.2-M4", data.getVersion());
        assertEquals("1.2-M4 > *", data.getRuntimeVersion());
        assertEquals("Mr. Foo Bar", data.getAuthor());
        assertEquals("Nothing, really", data.getTitle());
        assertEquals("A plugin that provides nothing", data.getDescription());
    }
    
    public void testMethodDomainBehaviors1() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "<argument type='java.lang.String' />" +
                "<argument type='java.lang.Object' />" +
                "</method>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Set<ContributedMethod> methods = data.getDomainMethods().get("methodMissing");
        assertEquals("Should have exactly 1 contributed method", 1, methods.size());
        ContributedMethod method = methods.iterator().next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        MethodNode mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        Parameter[] params = mockMethod.getParameters();
        assertEquals(ClassHelper.STRING_TYPE, params[0].getType());
        assertEquals(ClassHelper.OBJECT_TYPE, params[1].getType());
        assertEquals(2, params.length);
    }
    public void testMethodDomainBehaviors2() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "</method>" +
                "</behavior>" +
        "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Set<ContributedMethod> methods = data.getDomainMethods().get("methodMissing");
        assertEquals("Should have exactly 1 contributed method", 1, methods.size());
        ContributedMethod method = methods.iterator().next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        MethodNode mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        Parameter[] params = mockMethod.getParameters();
        assertEquals(0, params.length);
    }
    public void testMethodDomainBehaviors3() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "<argument type='java.lang.String' />" +
                "<argument type='java.lang.Object' />" +
                "</method>" +
                "<method name='methodMissing' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "</method>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Set<ContributedMethod> methods = data.getDomainMethods().get("methodMissing");
        assertEquals("Should have exactly 2 contributed method", 2, methods.size());
        
        Iterator<ContributedMethod> iter = methods.iterator();
        ContributedMethod method = iter.next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        MethodNode mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        Parameter[] params = mockMethod.getParameters();
        assertEquals(ClassHelper.STRING_TYPE, params[0].getType());
        assertEquals(ClassHelper.OBJECT_TYPE, params[1].getType());
        assertEquals(2, params.length);

        // second contributed method
        method = iter.next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        params = mockMethod.getParameters();
        assertEquals(0, params.length);
    }
    public void testPropertyDomainBehaviors1() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<property name='myProp' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "</property>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Map<String, ContributedProperty> properties = data.getDomainProperties();
        assertEquals("Should have exactly 1 contributed property", 1, properties.size());
        ContributedProperty property = properties.get("myProp");
        assertEquals("Wrong contributed by value", "test", property.getContributedBy());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, property.getType());
        
        PropertyNode mockProperty = property.createMockProperty(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockProperty.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockProperty.getType());
    }
    public void testPropertyDomainBehaviors2() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<property name='myProp' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "</property>" +
                "<property name='myProp2' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "</property>" +
                "</behavior>" +
        "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Map<String, ContributedProperty> properties = data.getDomainProperties();
        assertEquals("Should have exactly 2 contributed properties", 2, properties.size());
        ContributedProperty property = properties.get("myProp");
        assertEquals("Wrong contributed by value", "test", property.getContributedBy());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, property.getType());
        
        PropertyNode mockProperty = property.createMockProperty(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockProperty.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockProperty.getType());
        
        // and the second
        property = properties.get("myProp2");
        assertEquals("Wrong contributed by value", "test", property.getContributedBy());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, property.getType());
        
        mockProperty = property.createMockProperty(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockProperty.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockProperty.getType());
    }

    public void testMethodControllerBehaviors1() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Controller' type='java.util.List'>" +
                "<description />" +
                "<argument type='java.lang.String' />" +
                "<argument type='java.lang.Object' />" +
                "</method>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Set<ContributedMethod> methods = data.getControllerMethods().get("methodMissing");
        assertEquals("Should have exactly 1 contributed method", 1, methods.size());
        ContributedMethod method = methods.iterator().next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        MethodNode mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        Parameter[] params = mockMethod.getParameters();
        assertEquals(ClassHelper.STRING_TYPE, params[0].getType());
        assertEquals(ClassHelper.OBJECT_TYPE, params[1].getType());
        assertEquals(2, params.length);
    }
    public void testMethodControllerBehaviors2() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Controller' type='java.util.List'>" +
                "<description />" +
                "</method>" +
                "</behavior>" +
        "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Set<ContributedMethod> methods = data.getControllerMethods().get("methodMissing");
        assertEquals("Should have exactly 1 contributed method", 1, methods.size());
        ContributedMethod method = methods.iterator().next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        MethodNode mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        Parameter[] params = mockMethod.getParameters();
        assertEquals(0, params.length);
    }
    public void testMethodControllerBehaviors3() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Controller' type='java.util.List'>" +
                "<description />" +
                "<argument type='java.lang.String' />" +
                "<argument type='java.lang.Object' />" +
                "</method>" +
                "<method name='methodMissing' artefact='Controller' type='java.util.List'>" +
                "<description />" +
                "</method>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Set<ContributedMethod> methods = data.getControllerMethods().get("methodMissing");
        assertEquals("Should have exactly 2 contributed method", 2, methods.size());
        Iterator<ContributedMethod> iter = methods.iterator();
        ContributedMethod method = iter.next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        MethodNode mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        Parameter[] params = mockMethod.getParameters();
        assertEquals(ClassHelper.STRING_TYPE, params[0].getType());
        assertEquals(ClassHelper.OBJECT_TYPE, params[1].getType());
        assertEquals(2, params.length);

        // second contributed method
        method = iter.next();
        assertEquals("Wrong contributed by value", "test", method.getContributedBy());
        assertEquals("Wrong name", "methodMissing", method.getName());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, method.getReturnType());
        
        mockMethod = method.createMockMethod(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockMethod.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockMethod.getReturnType());
        params = mockMethod.getParameters();
        assertEquals(0, params.length);
    }
    public void testPropertyControllerBehaviors1() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<property name='myProp' artefact='controller' type='java.util.List'>" +
                "<description />" +
                "</property>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Map<String, ContributedProperty> properties = data.getControllerProperties();
        assertEquals("Should have exactly 1 contributed property", 1, properties.size());
        ContributedProperty property = properties.get("myProp");
        assertEquals("Wrong contributed by value", "test", property.getContributedBy());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, property.getType());
        
        PropertyNode mockProperty = property.createMockProperty(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockProperty.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockProperty.getType());
    }
    public void testPropertyControllerBehaviors2() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<property name='myProp' artefact='CONTROLLER' type='java.util.List'>" +
                "<description />" +
                "</property>" +
                "<property name='myProp2' artefact='cONTROLLER' type='java.util.List'>" +
                "<description />" +
                "</property>" +
                "</behavior>" +
        "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        Map<String, ContributedProperty> properties = data.getControllerProperties();
        assertEquals("Should have exactly 2 contributed properties", 2, properties.size());
        ContributedProperty property = properties.get("myProp");
        assertEquals("Wrong contributed by value", "test", property.getContributedBy());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, property.getType());
        
        PropertyNode mockProperty = property.createMockProperty(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockProperty.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockProperty.getType());
        
        // and the second
        property = properties.get("myProp2");
        assertEquals("Wrong contributed by value", "test", property.getContributedBy());
        assertEquals("Wrong return type", ClassHelper.LIST_TYPE, property.getType());
        
        mockProperty = property.createMockProperty(ClassHelper.PATTERN_TYPE);
        assertEquals(ClassHelper.PATTERN_TYPE, mockProperty.getDeclaringClass());
        assertEquals(ClassHelper.LIST_TYPE, mockProperty.getType());

    }

    public void testMalformedMethodControllerBehaviors() throws Exception {
        IFile file = testProject.createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Controller'>" +
                "<description />" +
                "<argument type='java.lang.String' />" +
                "<argument type='java.lang.Object' />" +
                "</method>" +
                "</behavior>" +
        "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        // no exception should be thrown.
        GrailsPluginVersion data = parser.parse();
        assertNotNull("Plugin data should not be null", data);
        assertEquals("Should have exactly 0 contributed methods", 0, data.getControllerMethods().size());
    }
}