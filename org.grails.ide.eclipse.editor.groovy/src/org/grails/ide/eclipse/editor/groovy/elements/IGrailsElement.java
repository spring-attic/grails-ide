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
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;


/**
 * A wrapper for a {@link GroovyCompilationUnit} that is managed by {@link GrailsWorkspaceCore} and a {@link GrailsProject}.
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Nov 23, 2009
 */
public interface IGrailsElement extends IGrailsArtifact {
    
    /**
     * Gets the Groovy {@link ClassNode} associated with this Grails Element
     * May return <code>null</code> if no {@link ClassNode} exists.
     */
    ClassNode getGroovyClass();
    
    ICompilationUnit getCompilationUnit();

    TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
            String name, VariableScope scope);

    void initializeTypeLookup(VariableScope scope);
    
}
