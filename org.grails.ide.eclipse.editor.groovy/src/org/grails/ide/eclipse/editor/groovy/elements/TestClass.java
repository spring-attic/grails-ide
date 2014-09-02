/*******************************************************************************
 * Copyright (c) 2007, 2010 SpringSource.  All rights reserved.
 *******************************************************************************/
package org.grails.ide.eclipse.editor.groovy.elements;

import groovyjarjarasm.asm.Opcodes;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectTypeCache;

/**
 * 
 * @author Vasiliy Kizhaev
 * @created Aug 26, 2014
 */
public class TestClass extends AbstractGrailsElement implements INavigableGrailsElement {
	public static final String SPEC = "Spec";
	public static final String TEST = "Test";
	
	private static final String CONTROLLER = "Controller";
	private static final String SERVICE = "Service";
	private static final String TAGLIB = "TagLib";
	
    private DomainClass cachedDomainClass;
    
    protected TestClass(GroovyCompilationUnit unit) {
        super(unit);
    }

    public GrailsElementKind getKind() {
        return GrailsElementKind.UNIT_TEST;
    }
    
    public ControllerClass getControllerClass() {
        String controllerName = getDomainClassName() + "Controller.groovy";
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getControllerClass(packageName, controllerName);
    }
    
    public DomainClass getDomainClass() {
        if (cachedDomainClass != null) {
            return cachedDomainClass;
        }
        return DomainClass.getDomainClassForElement(unit, getDomainClassName());
    }

    public TagLibClass getTagLibClass() {
        return TagLibClass.getTagLibClassForElement(unit, getDomainClassName());
    }
    
    public ServiceClass getServiceClass() {
    	return ServiceClass.getServiceClassForElement(unit, getDomainClassName());
    }
    
    public TestClass getTestClass() {
    	return this;
    }
    
    public IFolder getGSPFolder() {
        DomainClass d = getDomainClass();
        return d != null ? d.getGSPFolder() : null;
    }

    private String getDomainClassName() {
    	String originalName = unit.getElementName();
    	int suffixIndex = originalName.lastIndexOf(SPEC);
    	if (suffixIndex < 0) {
    		suffixIndex = originalName.lastIndexOf(TEST);
    	}
    	assert suffixIndex > 0;
    	
    	String testBaseClassName = originalName.substring(0, suffixIndex);
    	
    	if (testBaseClassName.contains(CONTROLLER)) {
    		return testBaseClassName.substring(0, testBaseClassName.lastIndexOf(CONTROLLER));
    	}
    	
    	if (testBaseClassName.contains(SERVICE)) {
    		return testBaseClassName.substring(0, testBaseClassName.lastIndexOf(SERVICE));
    	}
    	
    	if (testBaseClassName.contains(TAGLIB)) {
    		return testBaseClassName.substring(0, testBaseClassName.lastIndexOf(TAGLIB));
    	}
    	
    	return testBaseClassName;
    }
    
    public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
            String name, VariableScope scope) {
        if (name.equals("log")) {
            FieldNode cached = (FieldNode) getCachedMember(name);
            if (cached == null) {
                PerProjectTypeCache typeCache = GrailsCore.get().connect(this.unit.getJavaProject().getProject(), PerProjectTypeCache.class);
                if (typeCache == null) {
                    return null;
                }
                cached = new FieldNode(name, Opcodes.ACC_PUBLIC, typeCache.getClassNode("org.apache.commons.logging.Log"), declaringType, null);
                cached.setDeclaringClass(declaringType);
                super.cacheGeneratedMember(cached);
            }
            return new TypeAndDeclaration(cached.getType(), cached);
        }
        return null;
    }

    public void initializeTypeLookup(VariableScope scope) {
        populateInjectedServices(scope);
    }

    public String getAssociatedDomainClassName() {
        String className = getGroovyClass().getName();
        int cIndex = className.lastIndexOf(SPEC);
        if (cIndex < 0) {
        	cIndex = className.lastIndexOf(TEST);
        }
        assert cIndex > 0;
        className = className.substring(0, cIndex);
        return className;
    }
    
    /**
     * Finds a corresponding Test class for the given type name
     * @param unit {@link ICompilationUnit} of the original class
     * @param typeName simple name of the original class
     * @return a corresponding Test class
     */
    public static TestClass getTestClassForElement(INavigableGrailsElement element, ICompilationUnit unit, String primaryTypeName) {
    	String unitTestName = primaryTypeName + "Spec.groovy"; //$NON-NLS-1$
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        TestClass result = gp.getTestClass(packageName, unitTestName);
        if (result == null) {
        	unitTestName = primaryTypeName + "Test.groovy";
        	result = gp.getTestClass(packageName, unitTestName);
        }
        return result; 
    }
}
