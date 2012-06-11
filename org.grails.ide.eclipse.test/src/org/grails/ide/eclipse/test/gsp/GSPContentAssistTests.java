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
package org.grails.ide.eclipse.test.gsp;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalComputerRegistry;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;

import org.grails.ide.eclipse.editor.gsp.configuration.GSPViewerConfiguration;
import org.grails.ide.eclipse.test.GrailsTestsActivator;

/**
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0
 */
public class GSPContentAssistTests extends AbstractGSPTagsTest {

    private static final String FLAR_CONTROLLER_2_0_0 = "def flar = { }";
    private static final String FLAR_CONTROLLER_1_3_7 = "def flar() { }";
    private static final String SOME_CONTROLLER_2_0_0 = "def some = { [ first : 1, second : [1] ] }";
    private static final String SOME_CONTROLLER_1_3_7 = "def some() { [ first : 1, second : [1] ] }";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // ensure that proposal categories are initialized
        CompletionProposalComputerRegistry.getDefault().getProposalCategories();
    }
    
    public void testControllerActionsReturn1() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(SOME_CONTROLLER_1_3_7);
        } else {
            createController(SOME_CONTROLLER_2_0_0);
        }
        doTest("${f}", "${first}", "first", "${f}".indexOf('}'));
    }

    public void testControllerActionsReturn2() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(SOME_CONTROLLER_1_3_7);
        } else {
            createController(SOME_CONTROLLER_2_0_0);
        }
        doTest("${s}", "${second}", "second", "${s}".indexOf('}'));
    }

    public void testControllerActionsReturn3() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(SOME_CONTROLLER_1_3_7);
        } else {
            createController(SOME_CONTROLLER_2_0_0);
        }
        doTest("${first.do}", "${first.doubleValue()}", "doubleValue", "${first.do}".indexOf('}'));
    }

    public void testControllerActionsLink1() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(FLAR_CONTROLLER_1_3_7);
        } else {
            createController(FLAR_CONTROLLER_2_0_0);
        }
        String initial = "<html><body><g:link controller=\"\"></g:link></body></html>";
        String expected = initial.replace("\"\"", "\"nuthin\"");
        doTest(initial, expected, "nuthin", initial.indexOf("\">"));
    }

    public void testControllerActionsLink2() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(FLAR_CONTROLLER_1_3_7);
        } else {
            createController(FLAR_CONTROLLER_2_0_0);
        }
        String initial = "<html><body><g:link controller=\"nuthin\" action=\"\"></g:link></body></html>";
        String expected = initial.replace("\"\"", "\"flar\"");
        doTest(initial, expected, "flar", initial.indexOf("\">"));
    }

    public void testControllerActionsLink3() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(FLAR_CONTROLLER_1_3_7);
        } else {
            createController(FLAR_CONTROLLER_2_0_0);
        }
        String initial = "<html><body><g:link action=\"\" controller=\"nuthin\"></g:link></body></html>";
        String expected = initial.replace("\"\"", "\"flar\"");
        doTest(initial, expected, "flar", initial.indexOf("\" "));
    }

    public void testControllerActionsLink4() throws Exception {
        if (GrailsTestsActivator.isGrails200OrLater()) {
            createController(FLAR_CONTROLLER_1_3_7);
        } else {
            createController(FLAR_CONTROLLER_2_0_0);
        }
        String initial = "<html><body><g:link action=\"\"></g:link></body></html>";
        String expected = initial.replace("\"\"", "\"flar\"");
        doTest(initial, expected, "flar", initial.indexOf("\">"));
    }

    public void testSimple() throws Exception {
        doTest("", "<html></html>", "html", 0);
    } 
    
    public void testBuiltInGsp1() throws Exception {
        doTest("<html><body></body></html>", "<html><body><g:if></g:if></body></html>", "g:if");
    } 
    public void testBuiltInGsp2() throws Exception {
        doTest("<html><body></body></html>", "<html><body><g:set></g:set></body></html>", "g:set");
    } 

    // standard tag libs not being found since there is
    // no source code for the grails jars
    public void _testStandardGsp1() throws Exception {
        doTest("<html><body></body></html>", "<html><body><g:field type=\"\"/></body></html>", "g:field");
    } 
    public void testCustomGsp1() throws Exception {
        createTagLib("static namespace = 'nuthin'\n" +
        		"def foo = { }");
        doTest("<html><body></body></html>", "<html><body><nuthin:foo/></body></html>", "nuthin:foo");
    } 
    
    public void testCustomGsp2() throws Exception {
        createTagLib("static namespace = 'nuthin'\n" +
                "def foo = { attrs, body -> }");
        doTest("<html><body></body></html>", "<html><body><nuthin:foo></nuthin:foo></body></html>", "nuthin:foo");
    }

    public void testCustomGsp3() throws Exception {
        createTagLib("static namespace = 'nuthin'\n" +
                "/**\n* @attr attr1 REQUIRED  some stuff */\ndef foo = { attrs, body -> }");
        doTest("<html><body></body></html>", "<html><body><nuthin:foo attr1=\"\"></nuthin:foo></body></html>", "nuthin:foo");
    }

    public void testInScriptlet1() throws Exception {
        String contents = "<html><body>${f}</body></html>";
        int location = contents.indexOf('}')-1;
        doTest(contents, contents.replace("${f}", "${flash}"), "flash", location);
    }
    
    public void testInScriptlet2() throws Exception {
        String contents = "<html><body>${application.}</body></html>";
        int location = contents.indexOf('}');
        doTest(contents, contents.replace("${application.}", "${application.minorVersion}"), "minorVersion", location);
    }

    public void testInScriptletDef() throws Exception {
        String contents = "<html><body><g:def var=\"foo\" value=\"\"/>${f}</body></html>";
        int location = contents.indexOf('}');
        doTest(contents, contents.replace("${f}", "${foo}"), "foo", location);
    }
    
    public void testInScriptletSet() throws Exception {
        String contents = "<html><body><g:set var=\"foo\" value=\"\"/>${f}</body></html>";
        int location = contents.indexOf('}');
        doTest(contents, contents.replace("${f}", "${foo}"), "foo", location);
    }
    
    public void testInScriptletInferencing() throws Exception {
        String contents = "<html><body>${def x = 9\n9.do}</body></html>";
        int location = contents.indexOf('}');
        doTest(contents, contents.replace("9.do", "9.doubleValue()"), "doubleValue", location);
    }
    
    public void testInScriptletCreateImport() throws Exception {
        String contents = "<html><body>${HT}</body></html>";
        String newContents = "<%@page import=\"javax.swing.text.html.HTML\"%>\n" +
        		"<html><body>${HTML}</body></html>";
        int location = contents.indexOf('}');
        doTest(contents, newContents, "HTML", location);
    }
    
    private void doTest(String initial, String expected, String proposalToFind) throws Exception {
        doTest(initial, expected, proposalToFind, -1);
    }
    
    private void doTest(String initial, String expected, String proposalToFind, int location) throws Exception {
        if (location == -1) {
            location = initial.indexOf("</body></html>");
        }
        StructuredTextViewer viewer = setUpViewer(initial);
        ICompletionProposal[] proposals = performContentAssist(viewer, location);
        ICompletionProposal proposal = findProposal(proposalToFind, proposals);
        assertProposalApplication(viewer, expected, proposal);
        tearDownViewer(viewer);
    }
    
    private ICompletionProposal[]  performContentAssist(StructuredTextViewer viewer, int location) throws Exception {
        GSPViewerConfiguration configuration = new GSPViewerConfiguration(); 
        viewer.configure(configuration);
        IContentAssistant contentAssistant = configuration.getContentAssistant(viewer);
        contentAssistant.install(viewer);
        String contentType = TextUtilities.getContentType(viewer.getDocument(), "org.eclipse.wst.sse.core.default_structured_text_partitioning", location, true);
        ICompletionProposal[] proposals = contentAssistant.getContentAssistProcessor(contentType).computeCompletionProposals(viewer, location);
        contentAssistant.uninstall();
        return proposals;
    }
    
    
    private ICompletionProposal findProposal(String expectedProposalText, ICompletionProposal[] proposals) {
        for (ICompletionProposal proposal : proposals) {
            if (proposal.getDisplayString().startsWith(expectedProposalText)) {
                return proposal;
            }
        }
        fail("Did not find expected proposal: " + expectedProposalText + "\nInstead found:\n" + printProposals(proposals));
        return null;
    }

    /**
     * @param proposals
     * @return
     */
    private String printProposals(ICompletionProposal[] proposals) {
        StringBuilder sb = new StringBuilder();
        for (ICompletionProposal proposal : proposals) {
            sb.append(proposal.getDisplayString() + "\n");
        }
        return sb.toString();
    }

    private void assertProposalApplication(StructuredTextViewer viewer, String expected, ICompletionProposal proposal) {
        if (proposal instanceof CustomCompletionProposal) {
            ((CustomCompletionProposal) proposal).apply(viewer, '\0', 0, 0);
            assertEquals("Invalid proposal application", expected, viewer.getDocument().get());
        } else {
            IDocument document = viewer.getDocument();
            proposal.apply(document);
            assertEquals("Invalid proposal application", expected, document.get());
        }
    }
    
    private StructuredTextViewer setUpViewer(String contents) throws Exception {
        IStructuredModel scratchModel = createModel("grails-app/views/nuthin/some.gsp", contents);
        StructuredTextViewer viewer = new StructuredTextViewer(new Shell(), null, null, false, SWT.NONE);
        viewer.setDocument(scratchModel.getStructuredDocument());
        return viewer;
    }
    
    private void tearDownViewer(StructuredTextViewer viewer) {
        viewer.getTextWidget().getShell().dispose();
    }
    
}
