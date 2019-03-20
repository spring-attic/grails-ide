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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.IField;

import org.grails.ide.eclipse.editor.gsp.tags.GSPTagJavaDocParser;
import org.grails.ide.eclipse.editor.gsp.tags.GSPTagJavaDocParser.GSPTagDescription;

/**
 * test {@link GSPTagJavaDocParser}
 * @author Andrew Eisenberg
 * @since 2.5.2
 */
public class GSPTagDocParserTests extends AbstractGSPTagsTest {
    
    public void testNoJavaDoc() throws Exception {
        checkTagDescription("def x = { }", "", 
                createAttrMap(), createRequired(), false);
    }
    
    public void testEmptyDoc() throws Exception {
        checkTagDescription("/**  */\ndef x = { }", "", 
                createAttrMap(), createRequired(), false);
    }
    
    public void testDescriptionOnly() throws Exception {
        checkTagDescription("/** Description */\ndef x = { }", "Description", 
                createAttrMap(), createRequired(), false);
    }
    
    public void testDescriptionOnly2() throws Exception {
        checkTagDescription("/**\n* Description  \n */\ndef x = { }", "Description", 
                createAttrMap(), createRequired(), false);
    }
    public void testAttr1() throws Exception {
        checkTagDescription("/**\n* @attr attr1   */\ndef x = { }", "<b>Known Attributes</b><br/>\n<b>attr1</b><br/>\n",
                createAttrMap("attr1", "<b>attr1</b>"), createRequired(), false);
    }
    public void testAttr2() throws Exception {
        checkTagDescription("/**\n* @attr attr1   some stuff */\ndef x = { }", "<b>Known Attributes</b><br/>\n<b>attr1</b> : some stuff<br/>\n", 
                createAttrMap("attr1", "<b>attr1</b><br/><br/>\nsome stuff"), createRequired(), false);
    }
    
    public void testAttr3() throws Exception {
        checkTagDescription("/**\n* @attr attr1 REQUIRED  some stuff */\ndef x = { }", "<b>Known Attributes</b><br/>\n<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n", 
                createAttrMap("attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff"), createRequired("attr1"), false);
    }
    
    public void testAttr4() throws Exception {
        checkTagDescription("/**\n* @attr attr1 required  some stuff */\ndef x = { }", "<b>Known Attributes</b><br/>\n<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n", 
                createAttrMap(
                        "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff"), 
                createRequired("attr1"), false);
    }
    
    public void testAttr5() throws Exception {
        checkTagDescription("/**\n* @attr attr1 required  some stuff\n@attr \n\nattr2\n\n  second attr */\ndef x = { }", 
                "<b>Known Attributes</b><br/>\n" + 
                "<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n" + 
                "<b>attr2</b> : second attr<br/>\n", 
                createAttrMap(
                        "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff", 
                        "attr2", "<b>attr2</b><br/><br/>\nsecond attr"), 
                createRequired("attr1"), false);
    }
    
    public void testDescriptionAndAttrs() throws Exception {
        checkTagDescription("/**\n* A very, very long\n* description\n * @attr attr1 required  some stuff\n@attr \n\nattr2\n\n  second attr \n*/\ndef x = { }", 
                "A very, very long description<br/><br/>\n" + 
                "<b>Known Attributes</b><br/>\n" + 
                "<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n" + 
                "<b>attr2</b> : second attr<br/>\n", 
                createAttrMap(
                        "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff", 
                        "attr2", "<b>attr2</b><br/><br/>\nsecond attr"), 
                createRequired("attr1"), false);
    }
    
    
    public void testIsEmpty1() throws Exception {
        checkTagDescription("/**\n* @emptyTag */\ndef x = { }", "",
                createAttrMap(), createRequired(), true);
    }

    public void testIsEmpty2() throws Exception {
        checkTagDescription("/**\n* @emptyTag\n * @attr attr1   */\ndef x = { }", "<b>Known Attributes</b><br/>\n<b>attr1</b><br/>\n",
                createAttrMap("attr1", "<b>attr1</b>"), createRequired(), true);
    }
    
    public void testIsEmpty3() throws Exception {
        checkTagDescription("/**\n * @attr attr1\n* @emptyTag   */\ndef x = { }", "<b>Known Attributes</b><br/>\n<b>attr1</b><br/>\n",
                createAttrMap("attr1", "<b>attr1</b>"), createRequired(), true);
    }
    
    public void testOtherTags1() throws Exception {
        checkTagDescription("/**\n* A very, very long\n* description\n* @attr attr1 required  some stuff\n@attr \n\nattr2\n\n  second attr \n@see nuthin*/\ndef x = { }", 
                "A very, very long description<br/><br/>\n" + 
                "<b>Known Attributes</b><br/>\n" + 
                "<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n" + 
                "<b>attr2</b> : second attr<br/>\n", 
                createAttrMap(
                        "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff", 
                        "attr2", "<b>attr2</b><br/><br/>\nsecond attr"), 
                createRequired("attr1"), false);
    }
    
    public void testOtherTags2() throws Exception {
        checkTagDescription("/**\n* A very, very long\n* description\n@author Me\n @attr attr1 required  some stuff\n@attr \n\nattr2\n\n  second attr \n@see nuthin*/\ndef x = { }", 
                "A very, very long description<br/><br/>\n" + 
                "<b>Known Attributes</b><br/>\n" + 
                "<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n" + 
                "<b>attr2</b> : second attr<br/>\n", 
                createAttrMap(
                        "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff", 
                        "attr2", "<b>attr2</b><br/><br/>\nsecond attr"), 
                createRequired("attr1"), false);
    }
    
    public void testOtherTags3() throws Exception {
        checkTagDescription("/**\n* A very, very long\n* description <b>bold</b> {@link Foo}\n@author Me\n @attr attr1 required  some stuff\n@attr \n\nattr2\n\n  second attr \n@see nuthin*/\ndef x = { }", 
                "A very, very long description <b>bold</b> {@link Foo}<br/><br/>\n" + 
                "<b>Known Attributes</b><br/>\n" + 
                "<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n" + 
                "<b>attr2</b> : second attr<br/>\n", 
                createAttrMap(
                        "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff", 
                        "attr2", "<b>attr2</b><br/><br/>\nsecond attr"), 
                        createRequired("attr1"), false);
    }
    
    public void testSTS2344() throws Exception {
        checkTagDescription("/**\n* </head><head>A very, very long\n* description <b>bold</b> {@link Foo}\n@author Me\n @attr attr1 required  some stuff\n@attr \n\nattr2\n\n  second attr \n@see nuthin*/\ndef x = { }", 
                "&lt;/head&gt;&lt;head&gt;A very, very long description <b>bold</b> {@link Foo}<br/><br/>\n" + 
                        "<b>Known Attributes</b><br/>\n" + 
                        "<b>attr1</b> : <b>REQUIRED </b>some stuff<br/>\n" + 
                        "<b>attr2</b> : second attr<br/>\n", 
                        createAttrMap(
                                "attr1", "<b>attr1</b><br/>\n<b>REQUIRED </b><br/><br/>\nsome stuff", 
                                "attr2", "<b>attr2</b><br/><br/>\nsecond attr"), 
                                createRequired("attr1"), false);
    }
    
    // STS-2344
    public void testHeadRegex() throws Exception {
        assertEquals("&lt;head&gt;", GSPTagJavaDocParser.OPEN_HEAD_PATTERN.matcher("<head>").replaceFirst(GSPTagJavaDocParser.OPEN_HEAD_REPLACE));
        assertEquals("fdsaafds&lt;head&gt;fdsafsd", GSPTagJavaDocParser.OPEN_HEAD_PATTERN.matcher("fdsaafds<head>fdsafsd").replaceFirst(GSPTagJavaDocParser.OPEN_HEAD_REPLACE));
        assertEquals("&lt;head&gt;", GSPTagJavaDocParser.OPEN_HEAD_PATTERN.matcher("<HEAD>").replaceFirst(GSPTagJavaDocParser.OPEN_HEAD_REPLACE));
        assertEquals("&lt;head&gt;", GSPTagJavaDocParser.OPEN_HEAD_PATTERN.matcher("<HeAd>").replaceFirst(GSPTagJavaDocParser.OPEN_HEAD_REPLACE));
        assertEquals("</head>", GSPTagJavaDocParser.OPEN_HEAD_PATTERN.matcher("</head>").replaceFirst(GSPTagJavaDocParser.OPEN_HEAD_REPLACE));

        assertEquals("&lt;/head&gt;", GSPTagJavaDocParser.CLOSE_HEAD_PATTERN.matcher("</head>").replaceFirst(GSPTagJavaDocParser.CLOSE_HEAD_REPLACE));
        assertEquals("fdsaafds&lt;/head&gt;fdsafsd", GSPTagJavaDocParser.CLOSE_HEAD_PATTERN.matcher("fdsaafds</head>fdsafsd").replaceFirst(GSPTagJavaDocParser.CLOSE_HEAD_REPLACE));
        assertEquals("&lt;/head&gt;", GSPTagJavaDocParser.CLOSE_HEAD_PATTERN.matcher("</HEAD>").replaceFirst(GSPTagJavaDocParser.CLOSE_HEAD_REPLACE));
        assertEquals("&lt;/head&gt;", GSPTagJavaDocParser.CLOSE_HEAD_PATTERN.matcher("</HeAd>").replaceFirst(GSPTagJavaDocParser.CLOSE_HEAD_REPLACE));
        assertEquals("<head>", GSPTagJavaDocParser.CLOSE_HEAD_PATTERN.matcher("<head>").replaceFirst(GSPTagJavaDocParser.CLOSE_HEAD_REPLACE));
    }
    
    private void checkNullTagDescription(String contents) throws Exception {
        GroovyCompilationUnit unit = createTagLib(contents);
        IField field = unit.getTypes()[0].getField("x");
        assertTagDescription(null, field);
    }

    private void checkTagDescription(String contents, String description, Map<String,String> attributes, Set<String> requiredAttributes, boolean isEmpty) throws Exception {
        GroovyCompilationUnit unit = createTagLib(contents);
        IField field = unit.getTypes()[0].getField("x");
        assertTagDescription(new GSPTagDescription(description, attributes, requiredAttributes, isEmpty), field);
    }
    

    private Map<String,String> createAttrMap(String...attrNameDescPairs) {
        Map<String,String> attrs = new HashMap<String, String>();
        for (int i = 0; i < attrNameDescPairs.length; i++) {
            attrs.put(attrNameDescPairs[i], attrNameDescPairs[++i]);
        }
        return attrs;
    }
    
    private Set<String> createRequired(String...required) {
        Set<String> requiredSet = new HashSet<String>();
        for (String req : required) {
            requiredSet.add(req);
        }
        return requiredSet;
    }
    
    private void assertTagDescription(GSPTagDescription expected, IField field) {
        GSPTagJavaDocParser parser = new GSPTagJavaDocParser();
        GSPTagDescription actual = parser.parseJavaDoc(field, null);
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals("Description", expected.description, actual.description);
        assertEquals("Attributes", expected.attributes, actual.attributes);
        assertEquals("Required attributes", expected.requiredAttributes, actual.requiredAttributes);
        assertEquals("IsEmpty", expected.isEmpty, actual.isEmpty);
    }
}