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
package org.grails.ide.eclipse.editor.gsp.configuration;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jst.jsp.ui.internal.contentassist.JSPStructuredContentAssistProcessor;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.ui.internal.contentassist.ProposalComparator;

/**
 * Ensures that all proposals are sorted according to relevance.
 * @author Andrew Eisenberg
 * @created Aug 3, 2011
 */
public class GSPStructuredContentAssistProcessor extends
        JSPStructuredContentAssistProcessor {

    public GSPStructuredContentAssistProcessor(ContentAssistant assistant,
            String partitionTypeID, ITextViewer viewer) {
        super(assistant, partitionTypeID, viewer);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List filterAndSortProposals(List proposals,
            IProgressMonitor monitor,
            CompletionProposalInvocationContext context) {
        Collections.sort(proposals, new ProposalComparator());
        return proposals;
    }
}
