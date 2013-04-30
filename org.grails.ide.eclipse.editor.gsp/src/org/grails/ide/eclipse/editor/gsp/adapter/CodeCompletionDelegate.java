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
package org.grails.ide.eclipse.editor.gsp.adapter;

import java.util.List;

import org.codehaus.groovy.eclipse.codeassist.proposals.GroovyJavaFieldCompletionProposal;
import org.codehaus.groovy.eclipse.codeassist.requestor.GroovyCompletionProposalComputer;
import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.codehaus.jdt.groovy.model.ICodeCompletionDelegate;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.codeassist.InternalCompletionContext;
import org.eclipse.jdt.internal.codeassist.InternalCompletionProposal;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

/**
 * Extends content assist so that it can be used within gsp pages.
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @created Dec 3, 2009
 */
public class CodeCompletionDelegate implements
        ICodeCompletionDelegate {

    public void codeComplete(ICompilationUnit unit, ICompilationUnit unitToSkip,
            int position, CompletionRequestor requestor,
            WorkingCopyOwner owner, ITypeRoot typeRoot, IProgressMonitor monitor)
            throws JavaModelException {

        InternalCompletionContext completionContext = new InternalCompletionContext();
        requestor.acceptContext(completionContext);
        
        GroovyCompletionProposalComputer computer = new GroovyCompletionProposalComputer();
        JavaContentAssistInvocationContext context = createContext((GroovyCompilationUnit) typeRoot, position);
        try {
            if (GrailsCoreActivator.testMode) {
                // on build server, GSPContentAssistTests are failing because
                // monitor is being canceled. Avoid that problem here
                monitor = new NullProgressMonitor() {
                    @Override
                    public void setCanceled(boolean cancelled) {
                        // no-op
                    }
                };
            }
            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }
            monitor.beginTask("Content assist", 1);
            List<ICompletionProposal> proposals = computer.computeCompletionProposals(context, monitor);
            for (ICompletionProposal proposal : proposals) {
                CompletionProposal cp = null;
                if (proposal instanceof LazyJavaCompletionProposal) {
                    cp = (CompletionProposal) ReflectionUtils.getPrivateField(LazyJavaCompletionProposal.class, "fProposal", proposal); //$NON-NLS-1$
                } else if (proposal instanceof GroovyJavaFieldCompletionProposal) {
                    cp = ((GroovyJavaFieldCompletionProposal) proposal).getProposal();
                } else if (proposal instanceof JavaCompletionProposal) {
                    cp = createMockProposal((JavaCompletionProposal) proposal);
                }
                if (cp != null) {
                    requestor.accept(cp);
                }
            }
        } catch (Exception e) {
            GroovyCore.logException("Exception with code completion", e); //$NON-NLS-1$
        }
    }

    /**
     * Create the assist context.  There are many fields that could be initialized.  In the future
     * they may need to be.  But for now, only do what is absolutely necessary
     */
    private JavaContentAssistInvocationContext createContext(GroovyCompilationUnit unit, int offset) {
        JavaContentAssistInvocationContext context = new JavaContentAssistInvocationContext(unit);
        ReflectionUtils.setPrivateField(ContentAssistInvocationContext.class, "fOffset", context, offset); //$NON-NLS-1$
        ReflectionUtils.setPrivateField(ContentAssistInvocationContext.class, "fDocument", context, new Document(new String(unit.getContents()))); //$NON-NLS-1$
        return context;
    }

    public boolean shouldCodeComplete(CompletionRequestor requestor,
            ITypeRoot typeRoot) {
        return requestor instanceof org.eclipse.jst.jsp.ui.internal.contentassist.JSPProposalCollector && typeRoot instanceof GroovyCompilationUnit;
    }
    
    /**
     * The proposals from fields and properties do not have an associated ICompletionProposal, so create a
     * mock one
     * @param javaProposal
     * @return
     */
    private CompletionProposal createMockProposal(JavaCompletionProposal javaProposal) {
        InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(CompletionProposal.FIELD_REF, javaProposal.getReplacementOffset());
        proposal.setDeclarationSignature(Signature.createTypeSignature("def", false).toCharArray()); //$NON-NLS-1$
        proposal.setFlags(Flags.AccPublic);
        proposal.setName(javaProposal.getReplacementString().toCharArray());
        proposal.setCompletion(javaProposal.getReplacementString().toCharArray());
        proposal.setSignature(Signature.createTypeSignature("def", false).toCharArray()); //$NON-NLS-1$
        proposal.setReplaceRange(javaProposal.getReplacementOffset(), javaProposal.getReplacementOffset()+1);
        proposal.setRelevance(5000);
        return proposal;
    }


}
