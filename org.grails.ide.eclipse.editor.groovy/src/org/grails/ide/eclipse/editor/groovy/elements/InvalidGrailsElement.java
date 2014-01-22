/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.groovy.elements;

import org.codehaus.groovy.ast.ClassNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;



/**
 * An invalid grails element.  Its source is either from another project or
 * binary
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Dec 9, 2009
 */
public class InvalidGrailsElement implements IGrailsElement {

    public ICompilationUnit getCompilationUnit() {
        return null;
    }

    public ClassNode getGroovyClass() {
        return null;
    }

    public GrailsElementKind getKind() {
        return GrailsElementKind.INVALID;
    }

    public void initializeTypeLookup(VariableScope scope) {

    }

    public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
            String name, VariableScope scope) {
        return null;
    }

    public IResource getResource() {
        return null;
    }

}
