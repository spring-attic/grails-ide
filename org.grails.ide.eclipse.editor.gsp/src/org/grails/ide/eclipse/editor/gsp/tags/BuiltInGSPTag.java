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
package org.grails.ide.eclipse.editor.gsp.tags;




/**
 * Represents a gsp tag
 * @author Andrew Eisenberg
 * @created Dec 2, 2009
 */
class BuiltInGSPTag extends AbstractGSPTag {

    private final String tagName;
    private final String description;
    private final String[] attrNames;
    private final String[] attrDescriptions;
    private final String[] requireds;
    private final boolean isEmpty;
    public BuiltInGSPTag(GSPTagLibDocument doc, String tagName, String description, String tagDefinitionHandle, String[] attrNames, String[] defaultAttrValues, String[] optional, String isEmpty) {
        super(doc, tagDefinitionHandle);
        this.tagName = tagName;
        this.attrNames = attrNames;
        this.attrDescriptions = defaultAttrValues;
        this.description = description;
        this.requireds = optional;
        this.isEmpty = Boolean.valueOf(isEmpty);
    }
    
    protected void initialize() {
        setNodeName(tagName);
//        setNodeName(getGSPTagLibDocument().getImpliedPrefix() + ":" + tagName);
        setDisplayName(tagName);
        setDescription(description);
        addAttributes();
    }

    
    @Override
    public int getContentType() {
        if (isEmpty) {
            return EMPTY;
        } else {
            return super.getContentType();
        }
    }

    private void addAttributes() {
        for (int i = 0; i < attrNames.length; i++) {
            addAttribute(attrNames[i], attrDescriptions[i], Boolean.getBoolean(requireds[i]));
        }
    }
}
