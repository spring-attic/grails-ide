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
import org.codehaus.groovy.eclipse.test.TestProject;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.test.GrailsTestsActivator;
import org.grails.ide.eclipse.test.TestLogger;

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
        GroovyRuntime.addGroovyRuntime(testProject.getProject());
        ensureGrailsProject();
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
    
    protected void ensureGrailsProject() throws Exception {
        IProject project = testProject.getProject();
        IProjectDescription description = project .getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID, GroovyNature.GROOVY_NATURE, GrailsNature.NATURE_ID });
        project.setDescription(description, null);
        String[] files = GrailsTestsActivator.getURLDependencies();
        setAutoBuilding(false);
        for (String file : files) {
            addExternalJar(project, file);
        }
        setAutoBuilding(true);
    }

    /**
     * FIXADE move to {@link TestProject}
     */
    public void addEntry(IProject project, IClasspathEntry entryPath) throws JavaModelException {
        IClasspathEntry[] classpath = getClasspath(project);
        IClasspathEntry[] newClaspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, newClaspath, 0, classpath.length);
        newClaspath[classpath.length] = entryPath;
        setClasspath(project, newClaspath);
    }
    
    /**
     * FIXADE move to {@link TestProject}
     */
    public IClasspathEntry[] getClasspath(IProject project) {
        try {
            JavaProject javaProject = (JavaProject) JavaCore.create(project);
            return javaProject.getExpandedClasspath();
        } catch (JavaModelException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * FIXADE move to {@link TestProject}
     */
    public void addExternalJar(IProject project, String jar) throws JavaModelException {
        addExternalJar(project, jar, false);
    }
    

    /**
     * FIXADE move to {@link TestProject}
     */
    public void addExternalJar(IProject project, String jar, boolean isExported) throws JavaModelException {
        addEntry(project, JavaCore.newLibraryEntry(new Path(jar), null, null, isExported));
    }
    
    /**
     * FIXADE move to {@link TestProject}
     */
    public void setClasspath(IProject project, IClasspathEntry[] entries) throws JavaModelException {
        IJavaProject javaProject = JavaCore.create(project);
        javaProject.setRawClasspath(entries, null);
    }
    
    /**
     * FIXADE move to {@link TestProject}
     */
    public void setAutoBuilding(boolean value) {
        try {
            IWorkspace w = ResourcesPlugin.getWorkspace();
            IWorkspaceDescription d = w.getDescription();
            d.setAutoBuilding(value);
            w.setDescription(d);
        } catch (CoreException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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