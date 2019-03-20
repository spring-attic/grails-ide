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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import org.grails.ide.eclipse.editor.actions.JavaElementHyperlink;
import org.grails.ide.eclipse.editor.gsp.actions.GSPHyperlinkDetector;

/**
 * 
 * @author Andrew Eisenberg
 * @since 2.6.0
 */
public class GSPHyperlinkTests extends AbstractGSPTagsTest {
    GSPHyperlinkDetector detector = new GSPHyperlinkDetector();
    
    // standard tag libs not being found since there is
    // no source code for the grails jars
    public void _testStandardTag() throws Exception {
        assertHyperlink("<g:actionSubmit />", 2, "actionSubmit", true);
    }
    
    public void testCustomTag1() throws Exception {
        createTagLib("def select = { }");
        assertHyperlink("<g:select />", 2, "select", false);
    }
    
    public void testCustomTag2() throws Exception {
        createTagLib("def select = { }");
        assertHyperlink("<g:select>", 2, "select", false);
    }
    
    public void testCustomTag3() throws Exception {
        createTagLib("def select = { }");
        assertHyperlink("<g:select> </g:select>", 2, "select", false);
    }
    
    public void testCustomTag4() throws Exception {
        createTagLib("def select = { }");
        String contents = "<g:select> </g:select>";
        assertHyperlink(contents, contents.lastIndexOf("select"), "select", false);
    }
    
    public void testDefaultTag1() throws Exception {
        String contents = 
            "<g:if> </g:if>\n" +
            "<g:def> </g:def>\n" +
            "<g:renderInput> </g:renderInput>\n" +
            "<g:collect> </g:collect>\n" +
            "<g:each> </g:each>\n" +
            "<g:elseif> </g:elseif>\n" +
            "<g:else> </g:else>\n" +
            "<g:findall> </g:findall>\n" +
            "<g:grep> </g:grep>\n" +
    		"<g:unless> </g:unless>\n" +
            "<g:while> </g:while>";
        
        assertHyperlink(contents, contents.indexOf("if"), "GroovyIfTag", true);
        assertHyperlink(contents, contents.indexOf("def"), "GroovyDefTag", true);
        assertHyperlink(contents, contents.indexOf("renderInput"), "RenderInputTag", true);
        assertHyperlink(contents, contents.indexOf("collect"), "GroovyCollectTag", true);
        assertHyperlink(contents, contents.indexOf("each"), "GroovyEachTag", true);
        assertHyperlink(contents, contents.indexOf("else>"), "GroovyElseTag", true);
        assertHyperlink(contents, contents.indexOf("elseif"), "GroovyElseIfTag", true);
        assertHyperlink(contents, contents.indexOf("findall"), "GroovyFindAllTag", true);
        assertHyperlink(contents, contents.indexOf("grep"), "GroovyGrepTag", true);
        assertHyperlink(contents, contents.indexOf("while"), "GroovyWhileTag", true);
        // It looks like the 'unless' tag is not available in the version of grails that we are running against
//        assertHyperlink(contents, contents.indexOf("unless"), "GroovyUnlessTag", true);
    }
    
    public void testControllerLink1() throws Exception {
         createController("def flar = { }");
         String contents = "<g:link controller=\"nuthin\" />";
         assertHyperlink(contents, contents.indexOf("nuthin"), "NuthinController", false);
    }
    
    public void testControllerLink2() throws Exception {
        createController("def flar = { }");
        String contents = "<g:link controller=\"nuthin\" action=\"flar\"/>";
        assertHyperlink(contents, contents.indexOf("flar"), "flar", false);
    }
    
    public void testControllerLink3() throws Exception {
        createController("def flar = { }");
        String contents = "<g:link action=\"flar\"/>";
        assertHyperlink(contents, contents.indexOf("flar"), "flar", false);
    }
    
    public void testControllerLink4() throws Exception {
        createController("def flar() { }");
        String contents = "<g:link action=\"flar\"/>";
        assertHyperlink(contents, contents.indexOf("flar"), "flar", false);
    }
    
    public void testControllerLink5() throws Exception {
        createController("def flar(int a, int b) { }");
        String contents = "<g:link action=\"flar\"/>";
        assertHyperlink(contents, contents.indexOf("flar"), "flar", false);
    }
    
    private void assertHyperlink(String contents, int offset, String elementName, boolean isBinary) throws Exception {
        String fileName = "grails-app/views/nuthin/foo.gsp";
        IFile file = testProject.getProject().getFile(new Path(fileName));
        if (!file.exists()) {
            file = testProject.createFile(fileName, contents);
            waitForIndexes();
        }
        IEditorPart part = IDE.openEditor(Workbench.getInstance().getActiveWorkbenchWindow().getActivePage(), file);
        try {
            IHyperlink[] links = detector.detectHyperlinks(((StructuredTextEditor) part).getTextViewer(), new Region(offset, 0), true);
            assertNotNull("Should have found one hyperlink", links);
            assertEquals("Should have found one hyperlink", 1, links.length);
            JavaElementHyperlink link = (JavaElementHyperlink) links[0];
            IJavaElement element = link.getElement();
            assertEquals(elementName, element.getElementName());
            assertTrue("Element should exist: " + element, element.exists());
            if (isBinary) {
                assertTrue("Should be read only", element.isReadOnly());
            } else {
                assertFalse("Should be not read only", element.isReadOnly());
            }
            assertTrue("Should have structure known", element.isStructureKnown());
        } finally {
            ((StructuredTextEditor) part).close(false);
        }
    }
    
}
