/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test.gsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import org.grails.ide.eclipse.editor.gsp.parser.GSPTokenizer;

/**
 * Copy tests from org.eclipse.jst.jsp.ui.tests for the scanner
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @created Dec 7, 2009
 */
public class GSPTokenizerTest extends TestCase {
    private GSPTokenizer tokenizer = null;

    private void reset(Reader in) {
        tokenizer.reset(in);
    }

    private void reset(String filename) {
        Reader fileReader = null;
        try {
            fileReader = new InputStreamReader(getClass().getResourceAsStream(
                    filename), "utf8");
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }
        BufferedReader reader = new BufferedReader(fileReader);
        reset(reader);
    }

    protected void setUp() throws Exception {
        super.setUp();
        tokenizer = new GSPTokenizer();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        tokenizer = null;
    }

    public void test144807_AttrName() {
        String input = "";
        for (int i = 0; i < 400; i++) {
            input = input += "<a ";
        }
        try {
            reset(new StringReader(input));
            assertTrue("empty input", tokenizer.getNextToken() != null);
            while (tokenizer.getNextToken() != null) {
                // really, we just want to loop
            }
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }
    }

    public void test144807_AttrValue() {
        String input = "<a b=";
        for (int i = 0; i < 400; i++) {
            input = input += "<a ";
        }
        try {
            reset(new StringReader(input));
            assertTrue("empty input", tokenizer.getNextToken() != null);
            while (tokenizer.getNextToken() != null) {
                // really, we just want to loop
            }
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }
    }

    public void test144807_Equals() {
        String input = "<a b";
        for (int i = 0; i < 400; i++) {
            input = input += "<a ";
        }
        try {
            reset(new StringReader(input));
            assertTrue("empty input", tokenizer.getNextToken() != null);
            while (tokenizer.getNextToken() != null) {
                // really, we just want to loop
            }
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }
    }

    public void testInsertComment() {
        reset("jspcomment01.jsp");
        try {
            assertTrue("empty input", tokenizer.getNextToken() != null);
            while (tokenizer.getNextToken() != null) {
                // really, we just want to loop
            }
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        } catch (StackOverflowError e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }

        // success if StackOverFlowError does not occur with tokenizer.
        assertTrue(true);
    }

    // [260004]
    public void test26004() {
        String input = "<c:set var=\"foo\" value=\"${foo} bar #\" /> <div id=\"container\" >Test</div>";
        try {
            reset(new StringReader(input));
            ITextRegion region = tokenizer.getNextToken();
            assertTrue("empty input", region != null);
            while (region != null) {
                if (region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                    region = tokenizer.getNextToken();
                    assertNotNull("document consumed by trailing $ or #",
                            region);
                } else
                    region = tokenizer.getNextToken();
            }
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }
    }

    // [150794]
    public void test150794() {
        String input = "<a href=\"<jsp:getProperty/>\">";
        try {
            reset(new StringReader(input));
            ITextRegion region = tokenizer.getNextToken();
            assertTrue("empty input", region != null);
            while (region != null) {
                if (region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                    region = tokenizer.getNextToken();
                    assertNotNull("document consumed by embedded JSP tag",
                            region);
                } else
                    region = tokenizer.getNextToken();
            }
        } catch (IOException e) {
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            fail(s.toString());
        }
    }

    // Need to simulate typing characters into the document to cause the stack
    // overflow.
    // Test is irrelevant due to changes in [280496]
    /*
     * public void test265380() throws Exception { String projectName =
     * "bug_265380"; int oldDepth = BooleanStack.maxDepth; // Make the maxDepth
     * equivalent to that we'd see in a normal editor BooleanStack.maxDepth =
     * 100; // Create new project IProject project =
     * BundleResourceUtil.createSimpleProject(projectName, null, null);
     * assertTrue(project.exists());
     * BundleResourceUtil.copyBundleEntriesIntoWorkspace("/testfiles/" +
     * projectName, "/" + projectName); IFile file =
     * project.getFile("test265380.jsp"); assertTrue(file.exists());
     * 
     * IStructuredModel model =
     * StructuredModelManager.getModelManager().getModelForEdit(file);
     * 
     * try { IStructuredDocument jspDocument = model.getStructuredDocument();
     * 
     * // offset in the document to begin inserting text int offset = 414; //
     * String to insert character-by-character String cif =
     * "<c:out value=\"lorem ipsum\"></c:out>\n"; // It takes several tags to be
     * inserted before the stack was overflowed for (int i = 0; i < 10; i++) {
     * for (int j = 0; j < cif.length(); j++) jspDocument.replace(offset++, 0,
     * String.valueOf(cif.charAt(j))); } } catch (StackOverflowError e) {
     * fail("Stack overflow encountered while editing document."); } finally {
     * if (model != null) model.releaseFromEdit(); BooleanStack.maxDepth =
     * oldDepth; } }
     */
}
