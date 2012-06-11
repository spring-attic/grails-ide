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
package org.grails.ide.eclipse.core.model;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.PropertyNode;

/**
 * A property contributed by a plugin
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 29, 2010
 */
public class ContributedProperty {
    private final String name;
    private final ClassNode type;
    private final int flags;
    private final String contributedBy;
    public ContributedProperty(String name, ClassNode type, int flags, String contributedBy) {
        this.name = name;
        this.type = type;
        this.flags = flags;
        this.contributedBy = contributedBy;
    }
    
    public String getContributedBy() {
        return contributedBy;
    }
    
    public ClassNode getType() {
        return type;
    }
    
    public PropertyNode createMockProperty(ClassNode declaringType) {
        PropertyNode newProperty = new PropertyNode(name, flags, type == null ? declaringType : type, declaringType, null, null, null); 
        newProperty.setDeclaringClass(declaringType);
        return newProperty;
    }
}
