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
package org.grails.ide.eclipse.editor.gsp.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;

import org.grails.ide.eclipse.editor.groovy.controllers.ITarget;

/**
 * This class performs content assist when inside of link tags.  Where appropriate, it shows 
 * all applicable controllers or their actions.
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0
 */
public class GSPLinkCompletionProposalComputer implements
        ICompletionProposalComputer {
    
    public GSPLinkCompletionProposalComputer() { }
    public void sessionStarted() { }

    /**
     * Completes proposals in a <g:link/> tag.  If inside of the controller attribute, then 
     * proposes all controller class names that match the prefix.  If inside of the action 
     * attribute, then proposes all of the actions inside the given controller that match the
     * prefix.  If there is no controller attribute (or it is invalid or indecipherable), then 
     * there will be no actions proposed.
     */
    public List<ICompletionProposal> computeCompletionProposals(
            CompletionProposalInvocationContext context,
            IProgressMonitor monitor) {
        TargetFinder finder = new TargetFinder(true);
        List<ITarget> possibleTargets = findTargets(context, finder);
        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>(possibleTargets.size());
        
        for (ITarget target : possibleTargets) {
            proposals.add(toCompletionProposal(target, finder.prefix, context.getInvocationOffset()));
        }
        return proposals;
    }

    private ICompletionProposal toCompletionProposal(ITarget target,
            String prefix, int invocationOffset) {
        return new CustomCompletionProposal(target.getName(), invocationOffset
                - prefix.length(), prefix.length(), target.getName().length(),
                target.getImage(), target.getDisplayString(),
                target.toContextInformation(), null, Integer.MAX_VALUE);
    }
    public List<IContextInformation> computeContextInformation(
            CompletionProposalInvocationContext context,
            IProgressMonitor monitor) {
        TargetFinder finder = new TargetFinder(false);
        List<ITarget> possibleTargets = findTargets(context, finder);
        List<IContextInformation> infos = new ArrayList<IContextInformation>(possibleTargets.size());
        
        for (ITarget target : possibleTargets) {
            infos.add(target.toContextInformation());
        }
        return infos;
    }
    
    private List<ITarget> findTargets(CompletionProposalInvocationContext context, TargetFinder finder) {
        if (context.getDocument() instanceof IStructuredDocument) {
            IStructuredDocument document = (IStructuredDocument) context.getDocument();
            return finder.findTargets(document, context.getInvocationOffset());
        }
        return Collections.emptyList();
    }



    public String getErrorMessage() {
        return "";
    }

    public void sessionEnded() {

    }

}
