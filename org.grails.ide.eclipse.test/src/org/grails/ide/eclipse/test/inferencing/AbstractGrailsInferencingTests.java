/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test.inferencing;

import java.util.Hashtable;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.groovy.tests.search.AbstractInferencingTest;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.groovy.search.TypeInferencingVisitorWithRequestor;
import org.eclipse.jdt.groovy.search.TypeLookupResult.TypeConfidence;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectServiceCache;
import org.grails.ide.eclipse.test.MockGrailsTestProjectUtils;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Feb 1, 2010
 */
public abstract class AbstractGrailsInferencingTests extends
        AbstractInferencingTest {
    private static final String DOMAIN_CLASS_SUFFIX = "\n}}";

    private static final String DOMAIN_CLASS_PREFIX = "class Search {\nString dummyProp\ndef foo() {\n";

    public AbstractGrailsInferencingTests(String name) {
        super(name);
    }

    protected void assertDeclarationTypeInDomainClass(String contents,
            String expectedType) throws JavaModelException {
        assertDeclarationTypeInDomainClass(contents, 0, contents.length(),
                expectedType);
    }

    protected void assertDeclarationTypeInDomainClass(String contents,
            int exprStart, int exprEnd, String expectedDeclaringType)
            throws JavaModelException {
        GroovyCompilationUnit unit = createDomainClass("Search",
                DOMAIN_CLASS_PREFIX + contents + DOMAIN_CLASS_SUFFIX);
        unit.becomeWorkingCopy(null);
        try {
            TypeInferencingVisitorWithRequestor visitor = factory
                    .createVisitor(unit);
            SearchRequestor requestor = new SearchRequestor(
                    DOMAIN_CLASS_PREFIX.length() + exprStart,
                    DOMAIN_CLASS_PREFIX.length() + exprEnd);
            visitor.visitCompilationUnit(requestor);

            assertNotNull("Did not find expected ASTNode", requestor.node);
            if (!expectedDeclaringType.equals(requestor.getDeclaringTypeName())) {
                fail(buildFailString(expectedDeclaringType, requestor));
            }
        } finally {
            unit.discardWorkingCopy();
        }
    }

    protected void assertDeclarationTypeInDomainClassNoPrefix(String contents,
            int exprStart, int exprEnd, String expectedDeclaringType)
            throws JavaModelException {
        GroovyCompilationUnit unit = createDomainClass("Search", contents);
        TypeInferencingVisitorWithRequestor visitor = factory
                .createVisitor(unit);
        SearchRequestor requestor = new SearchRequestor(exprStart, exprEnd);
        visitor.visitCompilationUnit(requestor);

        assertNotNull("Did not find expected ASTNode", requestor.node);
        if (!expectedDeclaringType.equals(requestor.getDeclaringTypeName())) {
            fail(buildFailString(expectedDeclaringType, requestor));
        }
    }

    protected void assertTypeInBuildConfig(String contents, int exprStart,
            int exprEnd, String expectedType) throws JavaModelException {
        assertTypeInConfigClass(contents, expectedType, null,
                GrailsElementKind.BUILD_CONFIG, exprStart, exprEnd);
    }

    protected void assertTypeInConfigClass(String contents,
            String expectedType, TypeConfidence expectedConfidence,
            GrailsElementKind configKind) throws JavaModelException {
        assertTypeInConfigClass(contents, expectedType, expectedConfidence,
                configKind, 0, contents.length());
    }

    protected void assertTypeInConfigClass(String contents,
            String expectedType, TypeConfidence expectedConfidence,
            GrailsElementKind configKind, int exprStart, int exprEnd)
            throws JavaModelException {
        GroovyCompilationUnit unit = createConfigClass("Search", contents,
                configKind);
        assertTypeInGrailsArtifact(exprStart, exprEnd, expectedType, unit,
                expectedConfidence);
    }

    protected void assertTypeInControllerClass(String contents, int exprStart,
            int exprEnd, String expectedType) throws JavaModelException {
        GroovyCompilationUnit unit = createControllerClass("SearchController",
                contents);
        assertTypeInGrailsArtifact(exprStart, exprEnd, expectedType, unit, null);
    }

    protected void assertTypeInControllerClass(String contents,
            String expectedType) throws JavaModelException {
        assertTypeInControllerClass(contents, 0, contents.length(),
                expectedType);
    }

    protected void assertTypeInDomainClass(String contents, String expectedType)
            throws JavaModelException {
        assertTypeInDomainClass(contents, 0, contents.length(), expectedType);
    }

    protected void assertTypeInDomainClass(String contents, int exprStart,
            int exprEnd, String expectedType) throws JavaModelException {
        GroovyCompilationUnit unit = createDomainClass("Search",
                DOMAIN_CLASS_PREFIX + contents + DOMAIN_CLASS_SUFFIX);
        assertTypeInGrailsArtifact(DOMAIN_CLASS_PREFIX.length() + exprStart,
                DOMAIN_CLASS_PREFIX.length() + exprEnd, expectedType, unit,
                null);
    }

    protected void assertTypeInDomainClassNoPrefix(String contents,
            int exprStart, int exprEnd, String expectedType)
            throws JavaModelException {
        GroovyCompilationUnit unit = createDomainClass("Search", contents);
        assertTypeInGrailsArtifact(exprStart, exprEnd, expectedType, unit, null);
    }

    protected void assertTypeInService(String contents, int exprStart,
            int exprEnd, String expectedType) throws JavaModelException {
        GroovyCompilationUnit unit = createServiceClass("Search", contents);
        assertTypeInGrailsArtifact(exprStart, exprEnd, expectedType, unit, null);
    }

    protected void assertTypeInService(String contents, String expectedType)
            throws JavaModelException {
        assertTypeInService(contents, 0, contents.length(), expectedType);
    }

    protected void assertTypeInTagLib(String contents, int exprStart,
            int exprEnd, String expectedType) throws JavaModelException {
        GroovyCompilationUnit unit = createTagLib("Search", contents);
        assertTypeInGrailsArtifact(exprStart, exprEnd, expectedType, unit, null);
    }

    protected void assertTypeInTagLib(String contents, String expectedType)
            throws JavaModelException {
        assertTypeInTagLib(contents, 0, contents.length(), expectedType);
    }

    protected String buildFailString(String expectedType,
            SearchRequestor requestor) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected type not found.\n");
        sb.append("Expected: " + expectedType + "\n");
        sb.append("Found: " + printTypeName(requestor.result.type) + "\n");
        sb.append("Declaring type: " + requestor.result.declaringType.getName()
                + "\n");
        sb.append("ASTNode: " + requestor.node + "\n");
        sb.append("Confidence: " + requestor.result.confidence + "\n");
        return sb.toString();
    }

    protected void cleanUpElementKinds(GroovyCompilationUnit... units) {
        for (GroovyCompilationUnit unit : units) {
            GrailsProject.removeExtraGrailsElement(unit);
        }
    }

    protected GroovyCompilationUnit createConfigClass(String name,
            String contents, GrailsElementKind configKind) {
        GroovyCompilationUnit unit = createUnit(name, contents);
        GrailsProject.addExtraGrailsElement(unit, configKind);
        return unit;
    }

    protected GroovyCompilationUnit createControllerClass(String name,
            String contents) throws JavaModelException {
        IPath root = env.addPackageFragmentRoot(project.getFullPath(),
                "grails-app/controllers");
        IPath path = env.addGroovyClassExtension(root, name, contents,
                defaultFileExtension);
        fullBuild(project.getFullPath());

        GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore
                .createCompilationUnitFrom(env.getWorkspace().getRoot()
                        .getFile(path));
        GrailsProject.addExtraGrailsElement(unit,
                GrailsElementKind.CONTROLLER_CLASS);
        return unit;
    }

    protected GroovyCompilationUnit createTestClass(String name, String contents)
            throws JavaModelException {
        IPath root = env.addPackageFragmentRoot(project.getFullPath(),
                "test/unit");
        IPath path = env.addGroovyClassExtension(root, name, contents,
                defaultFileExtension);
        fullBuild(project.getFullPath());

        GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore
                .createCompilationUnitFrom(env.getWorkspace().getRoot()
                        .getFile(path));
        GrailsProject.addExtraGrailsElement(unit, GrailsElementKind.UNIT_TEST);
        return unit;
    }

    protected GroovyCompilationUnit createDomainClass(String name,
            String contents) throws JavaModelException {
        IPath root = env.addPackageFragmentRoot(project.getFullPath(),
                "grails-app/domain");
        IPath path = env.addGroovyClassExtension(root, name, contents,
                defaultFileExtension);
        fullBuild(project.getFullPath());
        GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore
                .createCompilationUnitFrom(env.getWorkspace().getRoot()
                        .getFile(path));
        GrailsProject.addExtraGrailsElement(unit,
                GrailsElementKind.DOMAIN_CLASS);
        return unit;
    }

    protected String createDomainText(String finderName, String... properties) {
        return createDomainTextWithSuper(finderName, null, false, properties);
    }

    protected String createDomainTextWithSuper(String finderName,
            String superClass, boolean isSuper, String... properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ");
        if (isSuper) {
            sb.append("SearchSuper ");
        } else {
            sb.append("Search ");
        }
        if (superClass != null) {
            sb.append(" extends " + superClass);
        }
        sb.append("{\n");
        for (String property : properties) {
            sb.append("  def " + property + "\n");
        }
        sb.append("  def method() {\n" + "  " + finderName + "\n  }\n" + "}");
        return sb.toString();
    }

    protected GroovyCompilationUnit createServiceClass(String name,
            String contents) throws JavaModelException {
        IPath root = env.addPackageFragmentRoot(project.getFullPath(),
                "grails-app/services");
        IPath path = env.addGroovyClassExtension(root, name, contents,
                defaultFileExtension);
        fullBuild(project.getFullPath());
        GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore
                .createCompilationUnitFrom(env.getWorkspace().getRoot()
                        .getFile(path));
        GrailsProject.addExtraGrailsElement(unit,
                GrailsElementKind.SERVICE_CLASS);
        PerProjectServiceCache cache = GrailsCore.get().connect(project,
                PerProjectServiceCache.class);
        cache.addService(unit, name);
        return unit;
    }

    protected GroovyCompilationUnit createTagLib(String name, String contents) {
        GroovyCompilationUnit unit = createUnit(name, contents);
        GrailsProject.addExtraGrailsElement(unit,
                GrailsElementKind.TAGLIB_CLASS);
        return unit;
    }

    private void assertTypeInGrailsArtifact(int exprStart, int exprEnd,
            String expectedType, GroovyCompilationUnit unit,
            TypeConfidence expectedConfidence) throws JavaModelException {
        unit.becomeWorkingCopy(null);
        TypeInferencingVisitorWithRequestor visitor = factory
                .createVisitor(unit);
        SearchRequestor requestor = new SearchRequestor(exprStart, exprEnd);
        visitor.visitCompilationUnit(requestor);

        assertNotNull("Did not find expected ASTNode", requestor.node);
        String failString = buildFailString(expectedType, requestor);
        if (!expectedType.equals(printTypeName(requestor.result.type))) {
            fail(failString);
        }
        if (expectedConfidence != null) {
            assertEquals("Type confidence problem.\n" + failString,
                    expectedConfidence, requestor.result.confidence);
        }
        unit.discardWorkingCopy();
    }

    
    private void superSetup() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void setUp() throws Exception {
        GrailsProjectVersionFixer.testMode();
        GrailsTest.ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
        
        final Job setupJob = new Job("grails test setup job") {
            
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    superSetup();
                    MockGrailsTestProjectUtils.mockGrailsProject(project);
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "", "", e);
                }
            }
        };
        setupJob.setPriority(Job.SHORT);
        setupJob.schedule();
        
        new ACondition("Grails test setup") {
            @Override
            public boolean test() throws Exception {
                return setupJob.getResult()!=null;
            }
        }.waitFor(90000);
    }

    @Override
    protected void tearDown() throws Exception {
        ICompilationUnit[] units = JavaModelManager.getJavaModelManager()
                .getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, true);
        if (units != null) {
            for (int i = 0; i < units.length; i++) {
                if (units[i] instanceof GroovyCompilationUnit) {
                    GrailsProject
                            .removeExtraGrailsElement((GroovyCompilationUnit) units[i]);
                    units[i].delete(true, null);
                }
            }
        }
        
        // remove the project from the testing environment so that it is not auto-deleted
//        ((Hashtable) ReflectionUtils.getPrivateField(env.getClass(), "fProjects", env)).remove(project.getName());
        super.tearDown();
    }

    protected void assertDynamicFinderType(boolean expectSuccess,
            String finderName, String... properties) throws JavaModelException {
        assertDynamicFinderType(finderName,
                createDomainText(finderName, properties),
                expectSuccess ? "Search" : "java.lang.Object");
    }

    protected void assertDynamicFinderTypeArray(String finderName,
            String... properties) throws JavaModelException {
        assertDynamicFinderType(finderName,
                createDomainText(finderName, properties),
                "java.util.List<Search>");
    }

    protected void assertDynamicFinderTypeInt(String finderName,
            String... properties) throws JavaModelException {
        assertDynamicFinderType(finderName,
                createDomainText(finderName, properties), "java.lang.Integer");
    }

    protected void assertDynamicFinderType(String finderName, String contents,
            String typeName) throws JavaModelException {
        assertTypeInDomainClassNoPrefix(contents, contents.indexOf(finderName),
                contents.indexOf(finderName) + finderName.length(), typeName);
    }

    protected void assertDynamicFinderTypeDeclaration(String finderName,
            String... properties) throws JavaModelException {
        String contents = createDomainText(finderName, properties);
        assertDeclarationTypeInDomainClassNoPrefix(contents,
                contents.indexOf(finderName), contents.indexOf(finderName)
                        + finderName.length(), "Search");
    }
}
