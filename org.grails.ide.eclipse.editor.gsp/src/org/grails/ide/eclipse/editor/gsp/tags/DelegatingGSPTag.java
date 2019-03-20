/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.tags;

import java.util.Iterator;

import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMAttributeDeclarationImpl;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMNamedNodeMapImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;

/**
 * Delegates to another tag.  Used to ensure that content assist always shows the namespace prefix for tags
 * @author Andrew Eisenberg
 * @created Jun 29, 2010
 */
public class DelegatingGSPTag extends AbstractGSPTag {

    private final AbstractGSPTag delegate;
    
    public DelegatingGSPTag(AbstractGSPTag delegate) {
        super(delegate.getGSPTagLibDocument(), delegate.getTagDefinitionHandle());
        this.delegate = delegate;
        initialize();
    }

    @Override
    protected void initialize() {
        setNodeName(getGSPTagLibDocument().getImpliedPrefix() + ":" + delegate.getNodeName());
        setDisplayName(delegate.getNodeName());
        setDescription(delegate.getDescription());
        
        // attributes
        CMNamedNodeMap attributes = delegate.getAttributes();
        Iterator<CMAttributeDeclarationImpl> attrIter = attributes.iterator();
        while (attrIter.hasNext()) {
        	CMAttributeDeclarationImpl attr = attrIter.next();
        	((CMNamedNodeMapImpl) getAttributes()).setNamedItem(attr.getAttrName(), attr);
        }        	
    }
    
    @Override
    public int getContentType() {
        return delegate.getContentType();
    }
    
    // Never qualified since prefix is in node name
    @Override
    public Object getProperty(String propertyName) {
        if (propertyName.equals("https://org.eclipse.wst/cm/properties/nsPrefixQualification")) {
            return null;
        }
        return super.getProperty(propertyName);
    }

}
