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
package org.grails.ide.eclipse.editor.gsp.search;

import java.util.List;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.groovy.search.ITypeRequestor;
import org.eclipse.jdt.groovy.search.TypeInferencingVisitorFactory;
import org.eclipse.jdt.groovy.search.TypeInferencingVisitorWithRequestor;
import org.eclipse.jdt.groovy.search.TypeRequestorFactory;
import org.eclipse.jdt.internal.core.search.JavaSearchDocument;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.jdt.internal.core.search.matching.PossibleMatch;
import org.eclipse.jst.jsp.core.internal.java.IJSPTranslation;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslation;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.editor.gsp.translation.GSPTranslationAdapter;
import org.grails.ide.eclipse.editor.gsp.translation.GSPTranslationExtension;

/**
 * Some utilities for helping to search through GSPs
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class SearchInGSPs {

    private class MockPossibleMatch extends PossibleMatch {

        public MockPossibleMatch(GroovyCompilationUnit unit) {
            super(null, unit.getResource(), unit, new JavaSearchDocument(unit.getResource().getFullPath().toPortableString(), new JavaSearchParticipant()), false);
        }
    }
    
    private class GspMatchRequestor extends SearchRequestor {


        private final GSPTranslationExtension translation;
        private final IFile file;
        private final IGSPSearchRequestor requestor; 
        
        public GspMatchRequestor(GSPTranslationExtension translation, IFile file, IGSPSearchRequestor requestor) {
            this.translation = translation;
            this.file = file;
            this.requestor = requestor;
        }

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            int jspOffset = translation.getJspOffset(match.getOffset());
            if (jspOffset>=0) {
            	requestor.acceptMatch(file, jspOffset, match.getLength());
            }
        }
    }
    
    
    public void performSearch(IGSPSearchRequestor gspRequestor, IProgressMonitor monitor)
        throws CoreException {
        
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        List<IFile> toSearch = gspRequestor.getGSPsToSearch();
        
        if (toSearch.size() > 0) {
            // check to see if we should also search for tag references
            FindTagReferences findTagReferences = new FindTagReferences();
            boolean shouldSearchForTags = gspRequestor.searchForTags() && findTagReferences.shouldSearchForTagRefs(gspRequestor.elementToSearchFor());
           
            // for each gsp, get the translator, and do a search through the translated CU
            for (IFile file : toSearch) {
                monitor.subTask("Searching in " + file.getName());
                IStructuredModel model = getModel(file);
                if (model == null) {
                    continue;
                }
                
                if (shouldSearchForTags) {
                    findTagReferences.findTags(model, file, gspRequestor);
                }
                
                if (monitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
                
                GSPTranslationAdapter translationAdapter = getTranslation(model);
                try {
                    if (translationAdapter == null) {
                        continue;
                    }
                    JSPTranslation jsptranslation = translationAdapter.getJSPTranslation();
                    GSPTranslationExtension translation;
                    if (jsptranslation instanceof GSPTranslationExtension) {
                        translation = (GSPTranslationExtension) jsptranslation;
                    } else {
                        translation = null;
                    }
                    
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
        
                    if (translation != null) {
                        ICompilationUnit unit = translation.getCompilationUnit();
                        if (unit instanceof GroovyCompilationUnit) {
                            TypeInferencingVisitorFactory factory = new TypeInferencingVisitorFactory();
                            TypeInferencingVisitorWithRequestor visitor = factory.createVisitor((GroovyCompilationUnit) unit);
                            GspMatchRequestor gspInferencingRequestor = new GspMatchRequestor(translation, file, gspRequestor);
                            ITypeRequestor requestor = createRequestor(gspRequestor, (GroovyCompilationUnit) unit, gspInferencingRequestor);
                            // requestor might be null since we don't handle all kinds of searches
                            if (requestor != null) {
                                visitor.visitCompilationUnit(requestor);
                            }
                        }
                    }

                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
                } catch (Exception e) {
                    GrailsCoreActivator.log(e);
                } finally {
                    try {
                        // don't release the adapter.  That is handled by the factory
    //                    translationAdapter.release();
                        translationAdapter.getXMLModel().releaseFromRead();
                    } catch (Exception e) {
                        GrailsCoreActivator.log(e);
                    } 
                }
            }
        }
    }
    
    private ITypeRequestor createRequestor(IGSPSearchRequestor gspRequestor, GroovyCompilationUnit unit, SearchRequestor requestor) {
        // don't know what the match rule should be, so make it exact match
        SearchPattern pattern = SearchPattern.createPattern(gspRequestor.elementToSearchFor(), gspRequestor.limitTo(), SearchPattern.R_CASE_SENSITIVE);
        TypeRequestorFactory factory = new TypeRequestorFactory();
        return factory.createRequestor(new MockPossibleMatch(unit), pattern, requestor);
    }

    
    private GSPTranslationAdapter getTranslation(IStructuredModel model) {
        if (model == null || ! (model instanceof IDOMModel)) {
            return null;
        }
        IDOMModel jspModel = (IDOMModel) model;
        IDOMDocument xmlDoc = jspModel.getDocument();
        return (GSPTranslationAdapter) xmlDoc.getAdapterFor(IJSPTranslation.class);
    }
    
    private IStructuredModel getModel(IFile file) {
        try {
            return StructuredModelManager.getModelManager().getModelForRead(file);
        } catch (Exception e) {
            GrailsCoreActivator.log(e);
            return null;
        }
    }
    

}
