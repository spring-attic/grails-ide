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

import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.TagSupport;

import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMAttributeDeclarationImpl;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMElementDeclarationImpl;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMNamedNodeMapImpl;


/**
 * @author Andrew Eisenberg
 * @created Dec 4, 2009
 */
public abstract class AbstractGSPTag extends CMElementDeclarationImpl {
    
    /**
     * A JDT handle identifier that describes the 
     * definition of the tag (can be null if definition is 
     * not known)
     */
    protected String tagDefinitionHandle;
    
    public AbstractGSPTag(GSPTagLibDocument owner, String tagDefinitionHandle) {
        super(owner);
        // gsp attributes should never show warnings
        setDynamicAttributes("true"); //$NON-NLS-1$
        this.tagDefinitionHandle = tagDefinitionHandle;
        super.setDynamicAttributes(Boolean.TRUE.toString());
    }

    /**
     * Initialize the tag, including its name, and its attributes, and their descriptions
     * The NodeName must include the prefix.  The displayName is the name without the prefix
     */
    protected abstract void initialize();
    
    /**
     * All Grails tags must implement {@link IterationTag}
     * in order to ensure that the translation comes out as syntactically correct Groovy.
     * 
     * Groovy does not allow anonymous closures that look like block statements. So,
     * the iteration tag ensures that the translation occurs with a while loop around it.
     */
    @Override
    public String getTagclass() {
        return TagSupport.class.getCanonicalName();
    }
    
    @SuppressWarnings("nls")
    @Override
    public Object getProperty(String propertyName) {
        Object result;
        // always include the qualification of grails tags 
        if (propertyName.equals("http://org.eclipse.wst/cm/properties/nsPrefixQualification")) {
            return "qualified";
        } else if (propertyName.equals("CMDocument")) { //$NON-NLS-1$
          result = getOwnerDocument();       
        } else {
          result = super.getProperty(propertyName);
        }        
        return result;
    }
    
    GSPTagLibDocument getGSPTagLibDocument() {
        return (GSPTagLibDocument) getOwnerDocument();
    }
    
    protected void addAttribute(String name, String description, boolean isRequired) {
        CMAttributeDeclarationImpl attr = new CMAttributeDeclarationImpl(getOwnerDocument());
        attr.setNodeName(name);
        attr.setDescription(description);
        attr.setRequired(isRequired);
        
        ((CMNamedNodeMapImpl) getAttributes()).setNamedItem(name, attr);
    }
    
    public String getTagDefinitionHandle() {
        return tagDefinitionHandle;
    }
}
