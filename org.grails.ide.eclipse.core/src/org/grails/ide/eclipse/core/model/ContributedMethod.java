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
package org.grails.ide.eclipse.core.model;

import java.util.Arrays;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.BlockStatement;

/**
 * A method contributed from a plugin
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 29, 2010
 */
public class ContributedMethod {
    private final String name;
    private final ClassNode returnType;
    private final ClassNode[] parameterTypes;
    private final int flags;
    private final String contributedBy;
    
    
    public ContributedMethod(String name, ClassNode returnType,
            ClassNode[] parameterTypes, int flags, String contributedBy) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.flags = flags;
        this.contributedBy = contributedBy;
    }

    public String getName() {
        return name;
    }
    
    public ClassNode getReturnType() {
        return returnType;
    }
    
    public String getContributedBy() {
        return contributedBy;
    }
    
    private Parameter[] createParameters(ClassNode declaringType) {
        Parameter[] parameters = new Parameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = new Parameter(parameterTypes[i] == null ? declaringType : parameterTypes[i], "arg" + i);
        }
        return parameters;
    }
   
    public MethodNode createMockMethod(ClassNode declaringType) {
        Parameter[] parameters = createParameters(declaringType);
        MethodNode newMethod = new MethodNode(name, flags, returnType == null ? declaringType : returnType,
                parameters, new ClassNode[0], new BlockStatement());
        newMethod.setDeclaringClass(declaringType);
        return newMethod;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof ContributedMethod)) {
            return false;
        }
        ContributedMethod other = (ContributedMethod) obj;
        if ((returnType == null && other == null) || (returnType != null && returnType.equals(other.returnType))) { 
            // flags not included in equality test
            return name.equals(other.name) && 
                    Arrays.equals(parameterTypes, other.parameterTypes) &&
                    contributedBy.equals(other.contributedBy);
        } else {
            return false;
        }
        
    }
    
    @Override
    public int hashCode() {
        // flags not included in hashcode
        return name.hashCode() * (returnType != null ? returnType.hashCode() : 1) * 
                Arrays.hashCode(parameterTypes) * contributedBy.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ContributedMethod [name=");
        builder.append(name);
        builder.append(", returnType=");
        builder.append(returnType);
        builder.append(", parameterTypes=");
        builder.append(Arrays.toString(parameterTypes));
        builder.append(", flags=");
        builder.append(flags);
        builder.append(", contributedBy=");
        builder.append(contributedBy);
        builder.append("]");
        return builder.toString();
    }
}
