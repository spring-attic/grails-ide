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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginVersion;
import org.grails.ide.eclipse.core.internal.classpath.PluginDescriptorParser;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.test.GrailsTestsActivator;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Dec 14, 2009
 */
public class PluginInferencingTests extends AbstractGrailsInferencingTests {
    
    static {
        ClassLoader l = PluginInferencingTests.class.getClassLoader();
        if (l instanceof DefaultClassLoader) {
            DefaultClassLoader dcl = (DefaultClassLoader) l;
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("" + PluginInferencingTests.class + " getting loaded by: " + dcl.getBundle());
        } else {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("" + PluginInferencingTests.class + " getting loaded by: " + l);
        }
    }
    
    public static Test suite() {
        return buildTestSuite(PluginInferencingTests.class);
    }
    
    public PluginInferencingTests(String name) {
        super(name);
    }
    
    
    @Override
    protected void tearDown() throws Exception {
        PerProjectPluginCache pluginCache = GrailsCore.get().connect(project, PerProjectPluginCache.class);
        pluginCache.flushExtraPluginFiles();
        super.tearDown();
    }
    
    public void testDomainPluginInferencing() throws Exception {
        populate();
        if (GrailsTestsActivator.isGrails200OrLater()) {
            // this test is not really working in 2.0.0 or later.
            // THat's ok since this kind of plugin support is deprecated
        	if (GrailsVersion.MOST_RECENT==GrailsVersion.V_2_0_2) {
        		//But it works for 2.0.2?
                assertTypeInDomainClass("methodMissing", "java.util.List<java.lang.Object<E>>");
        	} else {
        		assertTypeInDomainClass("methodMissing", "java.lang.Object");
        	}
        } else {
            assertTypeInDomainClass("methodMissing", "java.util.List<java.lang.Object<E>>");
        }
    }
    public void testControllerPluginInferencing() throws Exception {
        populate();
        assertTypeInControllerClass("methodMissing", "java.util.Collection");
    }
    
    private void populate() throws Exception {
        IFile file = createFile("plugin.xml", 
                "<plugin name='test'>" +
                "<behavior>" +
                "<method name='methodMissing' artefact='Domain' type='java.util.List'>" +
                "<description />" +
                "<argument type='java.lang.String' />" +
                "<argument type='java.lang.Object' />" +
                "</method>" +
                "<method name='methodMissing' artefact='Controller' type='java.util.Collection'>" +
                "<description />" +
                "</method>" +
                "</behavior>" +
                "</plugin>");
        PluginDescriptorParser parser = new PluginDescriptorParser(file.getLocation().toOSString());
        GrailsPluginVersion data = parser.parse();
        assertPluginData(data);

        PerProjectPluginCache pluginCache = GrailsCore.get().connect(project, PerProjectPluginCache.class);
        pluginCache.addExtraPluginFile(file);
    }

    private void assertPluginData(GrailsPluginVersion data) {
        assertEquals("Should have one controller method contributed.", 1, data.getControllerMethods().size());
        assertEquals("Wrong name for contributed method", "methodMissing", data.getControllerMethods().values().iterator().next().iterator().next().getName());
        assertEquals("Should have no controller properties contributed.", 0, data.getControllerProperties().size());
        
        assertEquals("Should have one domain method contributed.", 1, data.getDomainMethods().size());
        assertEquals("Wrong name for contributed method", "methodMissing", data.getDomainMethods().values().iterator().next().iterator().next().getName());
        assertEquals("Should have no domain properties contributed.", 0, data.getDomainProperties().size());
        
        System.out.println(data);
    }

    private IFile createFile(String name, String contents) throws Exception {
        String encoding = null;
        try {
            encoding = project.getDefaultCharset(); // get project encoding as file is not accessible
        } catch (CoreException ce) {
            // use no encoding
        }
        InputStream stream = new ByteArrayInputStream(encoding == null ? contents.getBytes() : contents.getBytes(encoding));
        IFile file= project.getFile(new Path(name));
        file.create(stream, true, null);
        return file;
    }
    
    
    @Override
    protected String buildFailString(String expectedType,
            SearchRequestor requestor) {
        String fail = super.buildFailString(expectedType, requestor);
        PerProjectPluginCache pluginCache = GrailsCore.get().connect(project, PerProjectPluginCache.class);
        fail += "\n-------------\nPlugin Cache:\n" + pluginCache.toString();
        return fail;
    }
}
