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
package org.grails.ide.eclipse.editor.groovy.elements;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;


/**
 * A generic class for all the Grails elements inside of the config
 * folder.  Consider specializing if the need arises.
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jun 17, 2010
 */
public class GenericConfigElement extends AbstractGrailsElement {

    private final GrailsElementKind kind;
    
    public GenericConfigElement(GroovyCompilationUnit unit, GrailsElementKind kind) {
        super(unit);
        this.kind = kind;
    }

    public GrailsElementKind getKind() {
        return kind;
    }

    public void initializeTypeLookup(VariableScope scope) {
        // not sure if services can be injected in all config kinds, but 
        // let's just assume so and we can change later.
        populateInjectedServices(scope);
    }

    /**
     * We don't want any underlines in these files
     * So, return everything as Object.  This
     * is sent back to the inferencing engine as LOOSELY_INFERRED
     * and can be overridden if something more specific comes up
     */
    public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType, String name, VariableScope scope) {
        ClassNode groovyClass = getGroovyClass();
        if (groovyClass == null) {
            groovyClass = VariableScope.OBJECT_CLASS_NODE;
        }
        return new TypeAndDeclaration(declaringType != null ? declaringType : groovyClass, VariableScope.OBJECT_CLASS_NODE);
    }

}
