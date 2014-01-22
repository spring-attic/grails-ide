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
package org.grails.ide.eclipse.editor.gsp.tags;

import java.util.Collection;
import java.util.Map.Entry;

import org.codehaus.groovy.ast.FieldNode;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMAttributeDeclarationImpl;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMNamedNodeMapImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;

import org.grails.ide.eclipse.editor.gsp.tags.GSPTagJavaDocParser.GSPTagDescription;

/**
 * Represents a gsp tag
 * that is part of a tag lib class
 * @author Andrew Eisenberg
 * @created Dec 2, 2009
 */
class GSPTag extends AbstractGSPTag {
    
    private final String tagName;
    private final Collection<String> attrs;
    private final GSPTagDescription tagDescription;
    
    public GSPTag(GSPTagLibDocument doc, FieldNode tagDef, Collection<String> attrs, GSPTagDescription tagDescription, String tagDefinitionHandle) {
        super(doc, tagDefinitionHandle);
        this.tagName = tagDef.getName();
        this.attrs = attrs;
        this.tagDescription = tagDescription;
    }
    
    @Override
    public int getContentType() {
        if (tagDescription != null && tagDescription.isEmpty) {
            return EMPTY;
        } else {
            return super.getContentType();
        }
    }

    

    @Override
    protected void initialize() {
        setNodeName(tagName);
//        setNodeName(getGSPTagLibDocument().getImpliedPrefix() + ":" + tagDef.getName());
        setDisplayName(tagName);
        
        if (tagDescription != null) {
            setDescription(tagDescription.description);
            for (Entry<String, String> attribute : tagDescription.attributes.entrySet()) {
                addAttribute(attribute.getKey(), attribute.getValue(), 
                        tagDescription.requiredAttributes.contains(attribute.getKey()));
            }
        }
        CMNamedNodeMap attributes = getAttributes();
        if (attrs != null) {
            for (String attrName : attrs) {
                if (attributes.getNamedItem(attrName) == null) {
                    addAttribute(attrName, attrName, false);
                }
            }
        }
    }
}
