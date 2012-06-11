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

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.gsp.tags.AbstractGSPTag;
import org.grails.ide.eclipse.editor.gsp.tags.GSPTagLibDocument;
import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 19, 2010
 */
public class GSPTagsTests extends AbstractGSPTagsTest {
    
    public void testBuiltInTags() throws Exception {
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("def");
        assertEquals(doc, provider.getDocumentForTagName("else"));
        assertEquals(doc, provider.getDocumentForTagName("renderInput"));
        assertEquals(doc, provider.getDocumentForTagName("collect"));
        assertEquals(doc, provider.getDocumentForTagName("each"));
        assertEquals(doc, provider.getDocumentForTagName("elseif"));
        assertEquals(doc, provider.getDocumentForTagName("findall"));
        assertEquals(doc, provider.getDocumentForTagName("grep"));
        assertEquals(doc, provider.getDocumentForTagName("if"));
        assertEquals(doc, provider.getDocumentForTagName("while"));
    }
    
    // perform some tests for checking and validating tags
    public void testTagLibExists1() throws Exception {
        GroovyCompilationUnit unit = createTagLib("static namespace = \"NUTHIN\"\n def nuthin = { }");
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        assertEquals("Wrong namespace for document " + doc, "NUTHIN", doc.getNamespace().getPrefix());

        doc = provider.getDocumentForTagName("NUTHIN:nuthin2");
        assertNull("Should not have found the document for the 'nuthin2' tag", doc);
        
        updateTagLib("static namespace = \"NUTHIN\"\n def nuthin2 = { }", unit);
        
        // now the tags should be switched
        doc = provider.getDocumentForTagName("NUTHIN:nuthin2");
        assertNotNull("Should have found the document for the 'nuthin2' tag", doc);
        assertEquals("Wrong namespace for document " + doc, "NUTHIN", doc.getNamespace().getPrefix());

        doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNull("Should not have found the document for the 'nuthin' tag", doc);
    }
    
    public void testTagLibExists2() throws Exception {
        GroovyCompilationUnit unit = createTagLib("static namespace = \"NUTHIN\"\n def nuthin = { attrs -> }");
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        assertEquals("Wrong namespace for document " + doc, "NUTHIN", doc.getNamespace().getPrefix());
        
        doc = provider.getDocumentForTagName("NUTHIN:nuthin2");
        assertNull("Should not have found the document for the 'nuthin2' tag", doc);
        
        updateTagLib("static namespace = \"NUTHIN\"\n def nuthin2 = { attrs -> }", unit);
        
        // now the tags should be switched
        doc = provider.getDocumentForTagName("NUTHIN:nuthin2");
        assertNotNull("Should have found the document for the 'nuthin2' tag", doc);
        assertEquals("Wrong namespace for document " + doc, "NUTHIN", doc.getNamespace().getPrefix());
        
        doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNull("Should not have found the document for the 'nuthin' tag", doc);
    }
    
    public void testTagLibExists3() throws Exception {
        GroovyCompilationUnit unit = createTagLib("static namespace = \"NUTHIN\"\n def nuthin = { attrs, body -> }");
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        assertEquals("Wrong namespace for document " + doc, "NUTHIN", doc.getNamespace().getPrefix());
        
        doc = provider.getDocumentForTagName("NUTHIN:nuthin2");
        assertNull("Should not have found the document for the 'nuthin2' tag", doc);
        
        updateTagLib("static namespace = \"NUTHIN\"\n def nuthin2 = { attrs, body -> }", unit);
        
        // now the tags should be switched
        doc = provider.getDocumentForTagName("NUTHIN:nuthin2");
        assertNotNull("Should have found the document for the 'nuthin2' tag", doc);
        assertEquals("Wrong namespace for document " + doc, "NUTHIN", doc.getNamespace().getPrefix());
        
        doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNull("Should not have found the document for the 'nuthin' tag", doc);
    }
    
    public void testAttrs1() throws Exception {
        createTagLib("static namespace = \"NUTHIN\"\n " +
        		"def nuthin = { attrs, body ->\n" +
        		"attrs['h']\n " +
        		"attrs[\"i\"]\n " +
        		"attrs.get('j')\n " +
        		"attrs.remove('k')\n " +
        		"attrs.l\n " +
        		"\"${attrs.m}\"\n " +
        		"\"attrs.${n}\"\n " +
        		"\"o\"\n " +
        		"}");
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        Object item = doc.getElements().getNamedItem("NUTHIN:nuthin");
        assertTrue("Should have found the 'nuthin' tag", item instanceof AbstractGSPTag);
        
        AbstractGSPTag tag = (AbstractGSPTag) item;
        assertNotNull("Should have found the 'h' tag", tag.getAttributes().getNamedItem("h"));
        assertNotNull("Should have found the 'i' tag", tag.getAttributes().getNamedItem("i"));
        assertNotNull("Should have found the 'j' tag", tag.getAttributes().getNamedItem("j"));
        assertNotNull("Should have found the 'k' tag", tag.getAttributes().getNamedItem("k"));
        assertNotNull("Should have found the 'l' tag", tag.getAttributes().getNamedItem("l"));
        assertNotNull("Should have found the 'm' tag", tag.getAttributes().getNamedItem("m"));

        assertNull("Should not have found the 'n' tag", tag.getAttributes().getNamedItem("n"));
        assertNull("Should not have found the 'o' tag", tag.getAttributes().getNamedItem("o"));
    }
    
    public void testEmpty1() throws Exception {
        createTagLib("static namespace = \"NUTHIN\"\n " +
                "def nuthin = { attrs -> }");
        
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        Object item = doc.getElements().getNamedItem("NUTHIN:nuthin");
        assertTrue("Should have found the 'nuthin' tag", item instanceof AbstractGSPTag);
        
        AbstractGSPTag tag = (AbstractGSPTag) item;
        assertEquals("Should be an empty tag", CMElementDeclaration.EMPTY, tag.getContentType());
    }
    
    public void testEmpty2() throws Exception {
        createTagLib("static namespace = \"NUTHIN\"\n " +
                "def nuthin = {  }");
        
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        Object item = doc.getElements().getNamedItem("NUTHIN:nuthin");
        assertTrue("Should have found the 'nuthin' tag", item instanceof AbstractGSPTag);
        
        AbstractGSPTag tag = (AbstractGSPTag) item;
        assertEquals("Should be an empty tag", CMElementDeclaration.EMPTY, tag.getContentType());
    }
    
    public void testEmpty3() throws Exception {
        createTagLib("static namespace = \"NUTHIN\"\n " +
                "/** */\ndef nuthin = { attrs -> }");
        
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        Object item = doc.getElements().getNamedItem("NUTHIN:nuthin");
        assertTrue("Should have found the 'nuthin' tag", item instanceof AbstractGSPTag);
        
        AbstractGSPTag tag = (AbstractGSPTag) item;
        assertEquals("Should be an empty tag", CMElementDeclaration.EMPTY, tag.getContentType());
    }
    
    public void testNotEmpty1() throws Exception {
        createTagLib("static namespace = \"NUTHIN\"\n " +
                "def nuthin = { attrs, body -> }");
        
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = GrailsCore.get().getInfo(testProject.getProject(), 
                PerProjectTagProvider.class);
        GSPTagLibDocument doc = provider.getDocumentForTagName("NUTHIN:nuthin");
        assertNotNull("Should have found the document for the 'nuthin' tag", doc);
        Object item = doc.getElements().getNamedItem("NUTHIN:nuthin");
        assertTrue("Should have found the 'nuthin' tag", item instanceof AbstractGSPTag);
        
        AbstractGSPTag tag = (AbstractGSPTag) item;
        assertEquals("Should be an empty tag", CMElementDeclaration.ANY, tag.getContentType());
    }


    private void updateTagLib(String contents, GroovyCompilationUnit unit) throws Exception {
        unit.getBuffer().setContents(("class NuthinTagLib {\n " + contents + "}").toCharArray());
        unit.save(null, true);
        fullProjectBuild();
    }
}
