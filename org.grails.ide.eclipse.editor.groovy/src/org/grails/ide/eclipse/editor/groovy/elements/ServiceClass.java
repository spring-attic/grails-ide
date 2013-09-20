/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.groovy.elements;

import groovyjarjarasm.asm.Opcodes;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectTypeCache;

/**
 * A Grails Service class
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created June 1, 2010
 */
public class ServiceClass extends AbstractGrailsElement implements INavigableGrailsElement {

    public ServiceClass(GroovyCompilationUnit unit) {
        super(unit);
    }

    public GrailsElementKind getKind() {
        return GrailsElementKind.SERVICE_CLASS;
    }

    public void initializeTypeLookup(VariableScope scope) {
        populateInjectedServices(scope);
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
    
    public DomainClass getDomainClass() {
        String origName = unit.getElementName();
        int tagLibIndex = origName.lastIndexOf("Service"); //$NON-NLS-1$
        String domainName = origName.substring(0, tagLibIndex) + ".groovy"; //$NON-NLS-1$
        String packageName = unit.getParent().getElementName();

        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = new GrailsProject(javaProject);
        return gp.getDomainClass(packageName, domainName);
    }

    public ControllerClass getControllerClass() {
        String origName = unit.getElementName();
        int controllerIndex = origName.lastIndexOf("Service"); //$NON-NLS-1$
        String controllerName = origName.substring(0, controllerIndex) + "Controller.groovy"; //$NON-NLS-1$
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getControllerClass(packageName, controllerName);
    }
    
    public TagLibClass getTagLibClass() {
        String origName = unit.getElementName();
        int serviceIndex = origName.lastIndexOf("Service"); //$NON-NLS-1$
        String serviceName = origName.substring(0, serviceIndex) + "TagLib.groovy"; //$NON-NLS-1$
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getTagLibClass(packageName, serviceName);
    }

    public ServiceClass getServiceClass() {
        return this;
    }
    
    public IFolder getGSPFolder() {
        DomainClass d = getDomainClass();
        return d != null ? d.getGSPFolder() : null;
    }
    
    public String getAssociatedDomainClassName() {
        String className = getGroovyClass().getName();
        int cIndex = className.lastIndexOf("Service");
        className = className.substring(0, cIndex);
        return className;
    }

}
