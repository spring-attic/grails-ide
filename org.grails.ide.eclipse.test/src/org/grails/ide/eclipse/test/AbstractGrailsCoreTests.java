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

import org.codehaus.groovy.eclipse.core.model.GroovyRuntime;
import org.codehaus.groovy.eclipse.test.TestProject;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.tests.builder.BuilderTests;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;

/**
 * 
 * @author Andrew Eisenberg
 * @created Aug 16, 2011
 */
public class AbstractGrailsCoreTests extends BuilderTests {

    protected TestProject testProject;
    IPackageFragmentRoot domainRoot;
    IPackageFragmentRoot controllerRoot;
    IPackageFragmentRoot taglibRoot;
    IPackageFragmentRoot serviceRoot;
    IPackageFragmentRoot confRoot;
    TestLogger logger;

    public AbstractGrailsCoreTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        GrailsCoreActivator.setLogger(logger = new TestLogger());
        testProject = new TestProject();
        GroovyRuntime.addGroovyRuntime(testProject.getProject());
        testProject.addNature(GrailsNature.NATURE_ID);
        domainRoot = testProject.createSourceFolder("grails-app/domain", null);
        controllerRoot = testProject.createSourceFolder("grails-app/controllers", null);
        taglibRoot = testProject.createSourceFolder("grails-app/taglib", null);
        serviceRoot = testProject.createSourceFolder("grails-app/services", null);
        confRoot = testProject.createSourceFolder("grails-app/conf", null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        assertFalse("Should not have any errors logged.  Instead found:\n\n" + logger.getAllEntriesAsText(), logger.hasErrors());
        testProject.dispose();
        GrailsCoreActivator.setLogger(null);
    }

    protected GroovyCompilationUnit createDomainClass(String cuName, String source)
            throws JavaModelException, CoreException {
                IPackageFragment frag = domainRoot.createPackageFragment("frag", true, null);
                GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore.create(testProject.createGroovyType(frag, cuName, source));
                return unit;
            }

    protected GroovyCompilationUnit createControllerClass(String cuName, String source)
            throws JavaModelException, CoreException {
                IPackageFragment frag = controllerRoot.createPackageFragment("frag", true, null);
                GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore.create(testProject.createGroovyType(frag, cuName, source));
                return unit;
            }

    protected GroovyCompilationUnit createTaglibClass(String cuName, String source)
            throws JavaModelException, CoreException {
                IPackageFragment frag = taglibRoot.createPackageFragment("frag", true, null);
                GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore.create(testProject.createGroovyType(frag, cuName, source));
                return unit;
            }

    protected GroovyCompilationUnit createServiceClass(String cuName, String source)
            throws JavaModelException, CoreException {
                IPackageFragment frag = serviceRoot.createPackageFragment("frag", true, null);
                GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore.create(testProject.createGroovyType(frag, cuName, source));
                return unit;
            }
    
    protected GroovyCompilationUnit createUrlMapping(String source) throws JavaModelException, CoreException {
        IPackageFragment frag = confRoot.createPackageFragment("", true, null);
        GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore.create(testProject.createGroovyType(frag, "UrlMappings.groovy", source));
        return unit;
    }

    protected IFile createGsp(String folderName, String gspName, String contents) throws Exception {
        return testProject.createFile("grails-app/views/" + (folderName != null && folderName.length() > 0 ? folderName + "/": "") + gspName, contents);
    }
    
    /**
     * @param unit
     */
    protected void assertDomainClass(GroovyCompilationUnit unit) {
        assertTrue("Unit " + unit.getElementName() + " in package "
                + unit.getParent().getElementName() + "in source folder "
                + unit.getParent().getParent().getElementName()
                + " should be a domain class.", 
                GrailsWorkspaceCore.isDomainClass(unit));
    }

    /**
     * @param unit
     */
    protected void assertControllerClass(GroovyCompilationUnit unit) {
        assertTrue("Unit " + unit.getElementName() + " in package "
                + unit.getParent().getElementName() + "in source folder "
                + unit.getParent().getParent().getElementName()
                + " should be a controller class.", 
                GrailsWorkspaceCore.isControllerClass(unit));
    }

    /**
     * @param unit
     */
    protected void assertTagLibClass(GroovyCompilationUnit unit) {
        assertEquals("Unit " + unit.getElementName() + " in package '"
                + unit.getParent().getElementName() + "' in source folder '"
                + unit.getParent().getParent().getElementName()
                + "' should be a taglib class.", 
                GrailsElementKind.TAGLIB_CLASS,
                GrailsWorkspaceCore.get().create(testProject.getProject()).getElementKind(unit));
    }

    /**
     * @param unit
     */
    protected void assertServiceClass(GroovyCompilationUnit unit) {
        assertEquals("Unit " + unit.getElementName() + " in package '"
                + unit.getParent().getElementName() + "' in source folder '"
                + unit.getParent().getParent().getElementName()
                + "' should be a service class.", 
                GrailsElementKind.SERVICE_CLASS,
                GrailsWorkspaceCore.get().create(testProject.getProject()).getElementKind(unit));
    }

}