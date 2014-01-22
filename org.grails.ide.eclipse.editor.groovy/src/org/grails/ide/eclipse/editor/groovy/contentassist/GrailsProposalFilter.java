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
package org.grails.ide.eclipse.editor.groovy.contentassist;

import java.util.Iterator;
import java.util.List;

import org.codehaus.groovy.eclipse.codeassist.processors.IProposalFilter;
import org.codehaus.groovy.eclipse.codeassist.proposals.GroovyCategoryMethodProposal;
import org.codehaus.groovy.eclipse.codeassist.proposals.IGroovyProposal;
import org.codehaus.groovy.eclipse.codeassist.requestor.ContentAssistContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

public class GrailsProposalFilter implements IProposalFilter {

    public List<IGroovyProposal> filterProposals(
            List<IGroovyProposal> proposals, ContentAssistContext context,
            JavaContentAssistInvocationContext javaContext) {
        for (Iterator<IGroovyProposal> iter = proposals.iterator(); iter.hasNext();) {
            IGroovyProposal proposal = iter.next();
            if (proposal instanceof GroovyCategoryMethodProposal
                    && ((GroovyCategoryMethodProposal) proposal).getMethod().getName().equals("identity")) {
                iter.remove();
            }
        }
        return proposals;
    }

}
