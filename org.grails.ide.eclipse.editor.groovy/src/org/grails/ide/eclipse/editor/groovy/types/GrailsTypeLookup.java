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
 package org.grails.ide.eclipse.editor.groovy.types;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup;
import org.eclipse.jdt.groovy.search.ITypeLookup;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.DomainClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;

/**
 * A type lookup for Grails elements
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
public class GrailsTypeLookup extends AbstractSimplifiedTypeLookup implements ITypeLookup {
    
    private GrailsProject gp;
    private Map<String, IGrailsElement> elementCache;
    
    /**
     * FIXADE this should be pulled out into an {@link IGrailsElement} 
     * We need to create a TestClass grails element.
     * 
     *  this is the type of the model field that is used in
     *  unit tests.  Every time a controller action is invoked
     *  the model field's type is set to the type of whatever
     *  controller action has been invoked.  Otherwise, model
     *  is grabbed from the DSLD
     */
    private TypeAndDeclaration modelTypeAndDeclaration = null;
    private GrailsElementKind kind;


    @Override
    protected TypeAndDeclaration lookupTypeAndDeclaration(
            ClassNode declaringType, String name, VariableScope scope) {
    	if (this.kind.isConfigElement() && declaringType.getName().equals(VariableScope.OBJECT_CLASS_NODE.getName())) {
    	    // STS-2330 avoid underlines when inside of nested closures in generic config elements.
    		return new TypeAndDeclaration(VariableScope.OBJECT_CLASS_NODE, VariableScope.OBJECT_CLASS_NODE);
    	}
    	
    	// avoid inferencing controller names in strings, 
    	// but do inference names in strings for config elements
    	if (isQuotedString() && !this.kind.isConfigElement()) {
    	    return null;
    	}
        
        if (declaringType instanceof DomainClass.NamedQueryClassNode) {
            return ((DomainClass.NamedQueryClassNode) declaringType).lookupTypeAndDeclaration(declaringType, name, scope);
        } else if (name.equals("model")) {
            // special case for the model field in test cases.
            // no need to separate this out yet, but consider doing so if there are more special cases
            return modelTypeAndDeclaration;
        }
        
        String declaringTypeName = declaringType.getName();
        IGrailsElement declaringElt = elementCache.get(declaringTypeName);
        if (declaringElt == null) {
            declaringElt = gp.getGrailsElement(declaringType);
            elementCache.put(declaringTypeName, declaringElt);
        }
        TypeAndDeclaration tAndD = declaringElt.lookupTypeAndDeclaration(declaringType, name, scope);
        if (declaringElt.getKind() == GrailsElementKind.CONTROLLER_CLASS && ((ControllerClass) declaringElt).isControllerAction(name) && isInTestCase()) {
            // keep track of this so that the next time "model" is referenced, we know to use this, instead of any other kind of inferencing.
            modelTypeAndDeclaration = tAndD;
        }
        return tAndD;
    }

    public void initialize(GroovyCompilationUnit unit,
            VariableScope topLevelScope) {
        elementCache = new HashMap<String, IGrailsElement>(); 
        gp = GrailsWorkspaceCore.get().getGrailsProjectFor(unit);
        if (gp != null) {
            IGrailsElement rootElement = gp.getGrailsElement(unit);
            kind = gp.getElementKind(unit);
            ClassNode groovyClass = rootElement.getGroovyClass();
            if (groovyClass != null) {
                elementCache.put(groovyClass.getName(), rootElement);
                rootElement.initializeTypeLookup(topLevelScope);
            }
        } else {
            kind = GrailsElementKind.INVALID;
        }
    }
    
    private boolean isInTestCase() {
        return kind == GrailsElementKind.UNIT_TEST || kind == GrailsElementKind.INTEGRATION_TEST;
    }
}
