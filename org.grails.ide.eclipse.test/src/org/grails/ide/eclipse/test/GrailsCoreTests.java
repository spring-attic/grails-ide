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
package org.grails.ide.eclipse.test;

import junit.framework.Test;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.INavigableGrailsElement;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 19, 2010
 */
public class GrailsCoreTests extends AbstractGrailsCoreTests {
    public static Test suite() {
        return buildTestSuite(GrailsCoreTests.class);
    }
    public GrailsCoreTests(String name) {
        super(name);
    }
    
    public void testProjectInfo() throws Exception {
        MockProjectInfo info = GrailsCore.get().connect(testProject.getProject(), MockProjectInfo.class);
        MockProjectInfo info2 = GrailsCore.get().connect(testProject.getProject(), MockProjectInfo.class);
        assertEquals("Project infos should not be recreated for same project", info, info2);
        
        MockProjectInfo2 info3 = GrailsCore.get().connect(testProject.getProject(), MockProjectInfo2.class);
        MockProjectInfo2 info4 = GrailsCore.get().connect(testProject.getProject(), MockProjectInfo2.class);
        assertEquals("Project infos should not be recreated for same project", info3, info4);
        
        GrailsCore.get().getInfo(testProject.getProject(), MockProjectInfo2.class).dispose();
        info4 = GrailsCore.get().connect(testProject.getProject(), MockProjectInfo2.class);
        assertEquals("Project infos should not be recreated for same project", info3, info4);

        // initial setting of the project
        assertEquals(1, info.setProjectCount);
        
        // nothing else should have been called
        assertEquals(0, info.disposeCount);
        assertEquals(0, info.projectChangedCount);
        
        // a resource change should bump up the affected count
        testProject.getProject().getFile(".classpath").touch(null);
        assertEquals(1, info.projectChangedCount);
        
        // closing should dispose
        testProject.getProject().close(null);
        assertEquals(1, info.disposeCount);
        assertEquals(1, info.projectChangedCount);

        
        // after re-opening, should not be connected any more
        testProject.getProject().open(null);
        testProject.getProject().getFile(".classpath");
        assertEquals(1, info.disposeCount);
        assertEquals(1, info.projectChangedCount);
        assertEquals(1, info.setProjectCount);
        
        info2 = GrailsCore.get().connect(testProject.getProject(), MockProjectInfo.class);
        assertNotSame("Project infos should be different after closing and reopening a project", info, info2);
    }
    
    public void testDomainClass() throws Exception {
        GroovyCompilationUnit unit = createDomainClass("Foo.groovy", "class Foo { }");
        assertDomainClass(unit);
    }

    public void testControllerClass() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FooController.groovy", "class FooController { }");
        assertControllerClass(unit);
    }

    public void testTaglibClass() throws Exception {
        GroovyCompilationUnit unit = createTaglibClass("FooTagLib.groovy", "class FooTagLib { }");
        assertTagLibClass(unit);
    }

    public void testServiceClass() throws Exception {
        GroovyCompilationUnit unit = createServiceClass("FooService.groovy", "class FooService { }");
        assertServiceClass(unit);
    }

    public void testDomainClassNavigation() throws Exception {
        GroovyCompilationUnit dunit = createDomainClass("Foo.groovy", "class Foo { }");
        
        assertFalse(GrailsWorkspaceCore.hasRelatedDomainClass(dunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedControllerClass(dunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedServiceClass(dunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedTagLibClass(dunit));
        
        INavigableGrailsElement elt = GrailsWorkspaceCore.get().getGrailsProjectFor(dunit).getDomainClass(dunit);
        assertNull(elt.getControllerClass());
        assertEquals(dunit, elt.getDomainClass().getCompilationUnit());
        assertNull(elt.getServiceClass());
        assertNull(elt.getTagLibClass());
        
        GroovyCompilationUnit cunit = createControllerClass("FooController.groovy", "class FooController { }");
        GroovyCompilationUnit tunit = createTaglibClass("FooTagLib.groovy", "class FooTagLib { }");
        GroovyCompilationUnit sunit = createServiceClass("FooService.groovy", "class FooService { }");
        
        assertEquals(dunit, elt.getDomainClass().getCompilationUnit());
        assertEquals(cunit, elt.getControllerClass().getCompilationUnit());
        assertEquals(tunit, elt.getTagLibClass().getCompilationUnit());
        assertEquals(sunit, elt.getServiceClass().getCompilationUnit());
    }
    
    public void testControllerClassNavigation() throws Exception {
        GroovyCompilationUnit cunit = createControllerClass("FooController.groovy", "class FooController { }");
        
        assertTrue(GrailsWorkspaceCore.hasRelatedDomainClass(cunit));
        assertFalse(GrailsWorkspaceCore.hasRelatedControllerClass(cunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedServiceClass(cunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedTagLibClass(cunit));
        
        INavigableGrailsElement elt = GrailsWorkspaceCore.get().getGrailsProjectFor(cunit).getControllerClass(cunit);
        assertEquals(cunit, elt.getControllerClass().getCompilationUnit());
        assertNull(elt.getDomainClass());
        assertNull(elt.getServiceClass());
        assertNull(elt.getTagLibClass());
        
        GroovyCompilationUnit dunit = createDomainClass("Foo.groovy", "class Foo { }");
        GroovyCompilationUnit tunit = createTaglibClass("FooTagLib.groovy", "class FooTagLib { }");
        GroovyCompilationUnit sunit = createServiceClass("FooService.groovy", "class FooService { }");
        
        assertEquals(dunit, elt.getDomainClass().getCompilationUnit());
        assertEquals(cunit, elt.getControllerClass().getCompilationUnit());
        assertEquals(tunit, elt.getTagLibClass().getCompilationUnit());
        assertEquals(sunit, elt.getServiceClass().getCompilationUnit());
    }
    
    public void testTagLibNavigation() throws Exception {
        GroovyCompilationUnit tunit = createTaglibClass("FooTagLib.groovy", "class FooTagLib { }");
        
        assertTrue(GrailsWorkspaceCore.hasRelatedDomainClass(tunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedControllerClass(tunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedServiceClass(tunit));
        assertFalse(GrailsWorkspaceCore.hasRelatedTagLibClass(tunit));
        
        INavigableGrailsElement elt = GrailsWorkspaceCore.get().getGrailsProjectFor(tunit).getTagLibClass(tunit);
        assertEquals(tunit, elt.getTagLibClass().getCompilationUnit());
        assertNull(elt.getDomainClass());
        assertNull(elt.getServiceClass());
        assertNull(elt.getControllerClass());
        
        GroovyCompilationUnit dunit = createDomainClass("Foo.groovy", "class Foo { }");
        GroovyCompilationUnit cunit = createControllerClass("FooController.groovy", "class FooController { }");
        GroovyCompilationUnit sunit = createServiceClass("FooService.groovy", "class FooService { }");
        
        assertEquals(dunit, elt.getDomainClass().getCompilationUnit());
        assertEquals(cunit, elt.getControllerClass().getCompilationUnit());
        assertEquals(tunit, elt.getTagLibClass().getCompilationUnit());
        assertEquals(sunit, elt.getServiceClass().getCompilationUnit());
    }
    
    public void testServicesNavigation() throws Exception {
        GroovyCompilationUnit sunit = createServiceClass("FooService.groovy", "class FooService { }");
        
        assertTrue(GrailsWorkspaceCore.hasRelatedDomainClass(sunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedControllerClass(sunit));
        assertFalse(GrailsWorkspaceCore.hasRelatedServiceClass(sunit));
        assertTrue(GrailsWorkspaceCore.hasRelatedTagLibClass(sunit));
        
        INavigableGrailsElement elt = GrailsWorkspaceCore.get().getGrailsProjectFor(sunit).getServiceClass(sunit);
        assertEquals(sunit, elt.getServiceClass().getCompilationUnit());
        assertNull(elt.getDomainClass());
        assertNull(elt.getTagLibClass());
        assertNull(elt.getControllerClass());
        
        GroovyCompilationUnit dunit = createDomainClass("Foo.groovy", "class Foo { }");
        GroovyCompilationUnit cunit = createControllerClass("FooController.groovy", "class FooController { }");
        GroovyCompilationUnit tunit = createTaglibClass("FooTagLib.groovy", "class FooTagLib { }");
        
        assertEquals(dunit, elt.getDomainClass().getCompilationUnit());
        assertEquals(cunit, elt.getControllerClass().getCompilationUnit());
        assertEquals(tunit, elt.getTagLibClass().getCompilationUnit());
        assertEquals(sunit, elt.getServiceClass().getCompilationUnit());
    }
    
    
    
}
