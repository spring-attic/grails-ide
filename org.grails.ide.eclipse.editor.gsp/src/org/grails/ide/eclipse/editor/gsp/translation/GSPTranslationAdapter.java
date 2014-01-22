// COPIED_FROM org.eclipse.jst.jsp.core.internal.java.JSPTranslationAdapter
/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pivotal Software, Inc.    - augmented for use with Grails
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.translation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jst.jsp.core.internal.Logger;
import org.eclipse.jst.jsp.core.internal.java.IJSPTranslation;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslationAdapter;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslationExtension;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import org.grails.ide.eclipse.editor.gsp.translation.internal.GSPTranslator;

/**
 * Copy from JSPTranslationAdapter
 * Use a {@link GSPTranslationExtension} instead of a {@link JSPTranslationExtension}Grails changes marked
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @created Nov 6, 2009
 */
public class GSPTranslationAdapter extends JSPTranslationAdapter {

    // for debugging
    private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jst.jsp.core/debug/jsptranslation")); //$NON-NLS-1$  //$NON-NLS-2$

    private IDocument fJspDocument = null;
    private IDocument fJavaDocument = null;
    private JSPTranslationExtension fJSPTranslation = null;
    private boolean fDocumentIsDirty = true;
    private IDOMModel fXMLModel;
    private GSPTranslator fTranslator = null;
    private NullProgressMonitor fTranslationMonitor = null;

    public GSPTranslationAdapter(IDOMModel xmlModel) {
        super(xmlModel);
        setXMLModel(xmlModel);
        initializeJavaPlugins();
    }

    /**
     * Initialize the required Java Plugins
     */
    protected void initializeJavaPlugins() {
        JavaCore.getPlugin();
    }

    public boolean isAdapterForType(Object type) {
        return type.equals(IJSPTranslation.class);
    }

    public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {
        // nothing to do
    }

    /**
     * Automatically set through the setXMLModel(XMLModel)
     * 
     * @param doc
     */
    private void setDocument(IDocument doc) {
        if (fJspDocument != null)
            fJspDocument.removeDocumentListener(this);
        if (doc != null) {
            doc.addDocumentListener(this);
            fJspDocument = doc;
        }
    }

    /**
     * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
        // do nothing
    }

    /**
     * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentChanged(DocumentEvent event) {
        // mark translation for rebuilding
        fDocumentIsDirty = true;
    }

    public void release() {

        if (fJspDocument != null)
            fJspDocument.removeDocumentListener(this);

        if (fTranslationMonitor != null)
            fTranslationMonitor.setCanceled(true);

        if (fJSPTranslation != null) {

            if (DEBUG)
                System.out.println("JSPTranslationAdapter releasing:" + fJSPTranslation); //$NON-NLS-1$

            fJSPTranslation.release();
        }
    }

    /**
     * Returns the JSPTranslation for this adapter.
     * 
     * @return a JSPTranslationExtension
     */
    public synchronized JSPTranslationExtension getJSPTranslation() {

        if (fJSPTranslation == null || fDocumentIsDirty) {
            GSPTranslator translator = null;
            if (getXMLModel() != null && getXMLModel().getIndexedRegion(0) != null) {
                translator = getTranslator((IDOMNode) getXMLModel().getIndexedRegion(0));
                translator.translate();
                StringBuffer javaContents = translator.getTranslation();
                fJavaDocument = new Document(javaContents.toString());
            }
            else {
                // empty document case
                translator = createTranslator();
				// GRAILS change : avoid NPE later by ensuring the fStructuredModel is not null
                translator.reset(getXMLModel().getDocument(), fTranslationMonitor);
				// GRAILS change end
                StringBuffer emptyContents = translator.getEmptyTranslation();
                fJavaDocument = new Document(emptyContents.toString());
            }
            // it's going to be rebuilt, so we release it here
            if (fJSPTranslation != null) {
                if (DEBUG)
                    System.out.println("JSPTranslationAdapter releasing:" + fJSPTranslation); //$NON-NLS-1$
                fJSPTranslation.release();
            }
            // GRAILS CHANGE
            // original code
//            fJSPTranslation = new JSPTranslationExtension(getXMLModel().getStructuredDocument(), fJavaDocument, getJavaProject(), translator);
            fJSPTranslation = new GSPTranslationExtension(getXMLModel().getStructuredDocument(), fJavaDocument, getJavaProject(), translator);
            // GRAILS CHANGE END
            fDocumentIsDirty = false;
        }
        return fJSPTranslation;
    }

    // GRAILS CHANGE
    // This method is marked package protected, but not used outside of this 
    // class.  We could mark this method as private, but want to keep consistency with
    // copied super-class.
    GSPTranslator createTranslator() {
        return new GSPTranslator(getGSPFile());
    }
    
    private IFile getGSPFile() {
        if (fXMLModel != null) {
            String baseLocation = fXMLModel.getBaseLocation();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath filePath = new Path(baseLocation);
            return root.getFile(filePath);
        } else {
            return null;
        }
    }
    

    /**
     * Returns the JSPTranslator for this adapter. If it's null, a new
     * translator is created with the xmlNode. Otherwise the
     * translator.reset(xmlNode) is called to reset the current local
     * translator.
     * 
     * @param xmlNode
     *            the first node of the JSP document to be translated
     * @return the JSPTranslator for this adapter (creates if null)
     */
    private GSPTranslator getTranslator(IDOMNode xmlNode) {
        if (fTranslator == null) {
            fTranslationMonitor = new NullProgressMonitor();
            fTranslator = createTranslator();
            fTranslator.reset(xmlNode, fTranslationMonitor);
        }
        else
            fTranslator.reset(xmlNode, fTranslationMonitor);
        return fTranslator;
    }

    /**
     * set the XMLModel for this adapter. Must be called.
     * 
     * @param xmlModel
     */
    public void setXMLModel(IDOMModel xmlModel) {
        fXMLModel = xmlModel;
        setDocument(fXMLModel.getStructuredDocument());
    }

    /**
     * @return the XMLModel for this adapter.
     */
    public IDOMModel getXMLModel() {
        return fXMLModel;
    }

    /**
     * Gets (or creates via JavaCore) a JavaProject based on the location of
     * this adapter's XMLModel. Returns null for non IFile based models.
     * 
     * @return the java project where
     */
    public IJavaProject getJavaProject() {

        IJavaProject javaProject = null;
        try {
            String baseLocation = getXMLModel().getBaseLocation();
            // 20041129 (pa) the base location changed for XML model
            // because of FileBuffers, so this code had to be updated
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=79686
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath filePath = new Path(baseLocation);
            IProject project = null;
            if (filePath.segmentCount() > 0) {
                project = root.getProject(filePath.segment(0));
            }
//          IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(baseLocation));
//          for (int i = 0; project == null && i < files.length; i++) {
//              if (files[i].getType() != IResource.PROJECT) {
//                  project = files[i].getProject();
//                  break;
//              }
//          }
            if (project != null) {
                javaProject = JavaCore.create(project);
            }
        }
        catch (Exception ex) {
            if (getXMLModel() != null)
                Logger.logException("(JSPTranslationAdapter) problem getting java project from the XMLModel's baseLocation > " + getXMLModel().getBaseLocation(), ex); //$NON-NLS-1$
            else
                Logger.logException("(JSPTranslationAdapter) problem getting java project", ex); //$NON-NLS-1$
        }
        return javaProject;
    }
}
