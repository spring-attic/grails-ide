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
package org.grails.ide.eclipse.editor.gsp.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.QuerySpecification;

/**
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class GSPQueryParticipant implements IQueryParticipant {
    
    private GSPUISearchRequestor gspRequestor;
    
    public GSPQueryParticipant() {
    }


    public void search(ISearchRequestor requestor,
            QuerySpecification specification, IProgressMonitor monitor)
            throws CoreException {
        if (! (specification instanceof ElementQuerySpecification)) {
            return; 
        }
        if (gspRequestor == null) {
            gspRequestor = new GSPUISearchRequestor((ElementQuerySpecification) specification);
        }
        gspRequestor.setRequestor(requestor);
        new SearchInGSPs().performSearch(gspRequestor, monitor);
    }

    /**
     * One tick per gsp file
     */
    public int estimateTicks(QuerySpecification specification) {
        if (! (specification instanceof ElementQuerySpecification)) {
            return 0;
        }

        if (gspRequestor == null) {
            gspRequestor = new GSPUISearchRequestor((ElementQuerySpecification) specification);
        }
        return gspRequestor.getGSPsToSearch().size();
    }

    public IMatchPresentation getUIParticipant() {
        return new GSPMatchPresentation();
    }

    
}
