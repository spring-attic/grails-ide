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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.eclipse.core.model.GroovyRuntime;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.test.MockGrailsTestProjectUtils;
import org.grails.ide.eclipse.test.TestLogger;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;

/**
 * @author Andrew Eisenberg
 * @created Jan 19, 2010
 */
public abstract class AbstractGSPTagsTest extends EclipseTestCase {

    List<IStructuredModel> models;
    protected TestLogger logger;

    public AbstractGSPTagsTest() {
        super();
    }

    public AbstractGSPTagsTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        GrailsProjectVersionFixer.testMode();
        MockGrailsTestProjectUtils.mockGrailsProject(testProject.getProject());
        models = new ArrayList<IStructuredModel>();
        logger = new TestLogger();
        GrailsCoreActivator.setLogger(logger);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        for (IStructuredModel model : models) {
            while(model.getReferenceCountForEdit() > 0) {
                try {
                    model.releaseFromEdit();
                } catch (Exception e) {
                    // ignore...already released
                }
            }
            while(model.getReferenceCountForRead() > 0) {
                try {
                    model.releaseFromRead();
                } catch (Exception e) {
                    // ignore...already released
                }
            }
        }
        GrailsCoreActivator.setLogger(null);
    }

    protected IStructuredModel createModel(String name, String contents) throws Exception {
        IFile file = testProject.createFile(name, contents);
        return createModel(file);
    }

    protected IStructuredModel createModel(IFile file) throws Exception {
        IStructuredModel model = StructuredModelManager.getModelManager().getModelForEdit(file);
        models.add(model);
        return model;
    }
    
    protected IStructuredModel reopenModel(String baseLocation) throws Exception {
        IFile file = testProject.getProject().getParent().getFile(new Path(baseLocation));
        IStructuredModel model = StructuredModelManager.getModelManager().getModelForEdit(file);
        models.add(model);
        return model;
    }
    
    protected GroovyCompilationUnit createTagLib(String contents) throws Exception {
        IPackageFragmentRoot root = testProject.createSourceFolder("grails-app/taglib", null);
        IPackageFragment pack = root.createPackageFragment("nuthin", true, null);
        IFile file = testProject.createGroovyType(pack, "NuthinTagLib.groovy", "class NuthinTagLib {\n " + contents + "}");
        return (GroovyCompilationUnit) JavaCore.createCompilationUnitFrom(file);
    }

    protected GroovyCompilationUnit createController(String contents) throws Exception {
        IPackageFragmentRoot root = testProject.createSourceFolder("grails-app/controllers", null);
        IPackageFragment pack = root.createPackageFragment("nuthin", true, null);
        IFile file = testProject.createGroovyType(pack, "NuthinController.groovy", "class NuthinController {\n " + contents + "}");
        return (GroovyCompilationUnit) JavaCore.createCompilationUnitFrom(file);
    }
    
    protected String printTypeName(ClassNode type) {
        return type != null ? type.getName() + printGenerics(type) : "null";
    }

    private String printGenerics(ClassNode type) {
        if (type.getGenericsTypes() == null || type.getGenericsTypes().length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        for (int i = 0; i < type.getGenericsTypes().length; i++) {
            GenericsType gt = type.getGenericsTypes()[i];
            sb.append(printTypeName(gt.getType()));
            if (i < type.getGenericsTypes().length-1) {
                sb.append(',');
            }
        }
        sb.append('>');
        return sb.toString();
    }

}