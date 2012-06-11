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
package org.grails.ide.eclipse.editor.gsp.search;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

/**
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public interface IGSPSearchRequestor {
    /**
     * @return true iff references to tags should be searched for (but only if the search target
     * is a field in a tag lib
     */
    boolean searchForTags();
    
    /**
     * Accepts a match in the given file at the given location
     * @param file
     * @param start
     * @param length
     */
    void acceptMatch(IFile file, int start, int length);
    
    
    /**
     * Returns what kind of occurrences the query should look for.
     * @return whether to search for references, declarations, etc
     * @see IJavaSearchConstants
     */
    int limitTo();
    
    
    /**
     * The JavaElement to search for.  Will be null if this is a pattern search (not supported yet)
     * @return
     */
    IJavaElement elementToSearchFor();
    
   
    List<IFile> getGSPsToSearch();
}
