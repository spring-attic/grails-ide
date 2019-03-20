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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jst.jsp.core.internal.contentmodel.tld.CMDocumentImpl;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.provisional.TLDDocument;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.provisional.TLDValidator;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamespace;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;


/**
 * A mock tag lib document that contains all of the default groovy tags
 * This class extends {@link CMDocumentImpl} so that casts can work 
 * properly
 * @author Andrew Eisenberg
 * @created Nov 27, 2009
 */
public class GSPTagLibDocument extends CMDocumentImpl implements TLDDocument {
    
    public class CMNamespaceImpl implements CMNamespace {
        private final String impliedPrefix;
        
        public CMNamespaceImpl(String prefix) {
            this.impliedPrefix = prefix;
        }

        public String getNodeName() {
            return "<none>"; //$NON-NLS-1$
        }

        public int getNodeType() {
            return CMNode.NAME_SPACE;
        }

        public String getPrefix() {
            return impliedPrefix;
        }
        
        public String getImpliedPrefix() {
            return impliedPrefix;
        }

        public Object getProperty(String property) {
            return null;
        }

        public String getURI() {
            return "<none>"; //$NON-NLS-1$
        }

        public boolean supports(String feature) {
            return false;
        }
    }

    private final class GSPTagNamedNodeMap implements CMNamedNodeMap {
        public Iterator<CMNode> iterator() {
            return allGSPTags.iterator();
        }

        public CMNode item(int index) {
            return allGSPTags.get(index);
        }

        public CMNode getNamedItem(String name) {
            return nameNodeMap.get(name);
        }

        public int getLength() {
            return allGSPTags.size();
        }
    }

    private Map<String, Object> properties = new HashMap<String, Object>(0);
    private List<CMNode> allGSPTags;
    private Map<String, CMNode> nameNodeMap;
    private final CMNamespaceImpl namespace;
    private List<Object> listeners;
    private final String baseLocation;
    private final String uri;
    private final String impliedNamespace;
    
    
    // null for now, but will eventually point to the TagLib
    // perhaps should use this for Object equality testing
    private String fLocationString;

     
    
    public GSPTagLibDocument(String namespacePrefix, String baseLocation, String uri) {
        super();
        this.namespace = new CMNamespaceImpl(namespacePrefix);
        allGSPTags = new ArrayList<CMNode>();
        nameNodeMap = new HashMap<String, CMNode>();
        this.baseLocation = this.fLocationString = baseLocation;
        this.uri = uri;
        setProperty("https://org.eclipse.wst/cm/properties/targetNamespaceURI", namespacePrefix);
        this.impliedNamespace = namespacePrefix;
    }
    
    public CMNamedNodeMap getElements() {
        return new GSPTagNamedNodeMap();
    }

    public CMNamedNodeMap getEntities() {
        return null;
    }

    public CMNamespace getNamespace() {
        return namespace;
    }

    public String getNodeName() {
        return "#gsp_tag_lib_document"; //$NON-NLS-1$
    }

    public int getNodeType() {
        return CMNode.DOCUMENT;
    }
    
    public void setProperty(String propertyName, Object value) {
        properties.put(propertyName, value);
    }

    public Object getProperty(String propertyName) {
        if (propertyName.equals(TLDDocument.CM_KIND)) {
            return TLDDocument.JSP_TLD;
        }
        return properties.get(propertyName);
    }

    public boolean supports(String propertyName) {
        if (TLDDocument.CM_KIND.equals(propertyName)) { 
            return true;
        }
        return properties.containsKey(propertyName);
    }

    void addTag(AbstractGSPTag tag) {
        allGSPTags.add(tag);
        nameNodeMap.put(getNamespace().getPrefix() + ":" + tag.getDisplayName(), tag); //$NON-NLS-1$
    }
    
    String getImpliedPrefix() {
        return impliedNamespace;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public String getDescription() {
        return "GSP Tag Library with namespace " + getUri();
    }

    public String getDisplayName() {
        return "GSP Tag Library with namespace " + getUri();
    }

    public List getExtensions() {
        return null;
    }

    public List getFunctions() {
        return null;
    }

    public String getInfo() {
        return getDescription();
    }

    public String getJspversion() {
        return "1.1";
    }

    public String getLargeIcon() {
        return null;
    }

    public List<Object> getListeners() {
        if (listeners == null)
            listeners = new ArrayList<Object>();
        return listeners;
    }

    public String getShortname() {
        return getNamespace().getPrefix();
    }

    public String getSmallIcon() {
        return null;
    }

    public String getTlibversion() {
        // should return the version from the application.properties file
        return "XXX";
    }

    public String getUri() {
        // can we get a file-based uri here?
        return uri;
    }

    public TLDValidator getValidator() {
        return null;
    }
    
    public String getLocationString() {
//        return baseLocation;
//        return fLocationString;
        // return null for now, but will need to change for 
        // hyperlinking.
        // see STS-1412 and STS-1408
        return null;
    }

    public void setLocationString(String url) {
        fLocationString = url;
    }
}
