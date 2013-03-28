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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jst.jsp.core.internal.regions.DOMJSPRegionContexts;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.ltk.modelhandler.IModelHandler;
import org.eclipse.wst.sse.core.internal.ltk.parser.BlockMarker;
import org.eclipse.wst.sse.core.internal.ltk.parser.BlockTagParser;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegionList;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionCollection;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.text.CoreNodeList;
import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import org.grails.ide.eclipse.editor.gsp.model.GSPModelHandler;

/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 */
@SuppressWarnings("nls")
public class ScannerUnitTests extends TestCase {

    public static boolean checkComplexRegionTypes(ITextRegionList regions, String[] contexts, String[][] embeddedContexts) {

		int embedCount = 0;
		Iterator<ITextRegion> iterator = regions.iterator();
		for (int i = 0; i < contexts.length; i++) {
			if (!iterator.hasNext())
				return false;
			ITextRegion region = iterator.next();
			assertEquals("context " + i + " incorrect", contexts[i], region.getType());
			if (region instanceof ITextRegionContainer) {
				ITextRegionContainer container = (ITextRegionContainer) region;
				boolean embeddedResult = checkSimpleRegionCount(container, embeddedContexts[embedCount].length) && checkSimpleRegionTypes(container.getRegions(), embeddedContexts[embedCount]);
				embedCount++;
				assertTrue(embeddedResult);
			}
		}
		return true;
	}

	public static boolean checkModelLength(IStructuredDocument document, int length) {
		return checkModelLength(document.getLastStructuredDocumentRegion(), length);
	}

	public static boolean checkModelLength(IStructuredDocumentRegion region, int length) {
		return region.getEndOffset() == length;
	}

	public static boolean checkSimpleRegionCount(ITextRegionContainer regionContainer, int size) {
		// yes, this looks silly, but it makes it easier to find the exact
		// failure point
		assertEquals("region count", size, regionContainer.getNumberOfRegions());
		return true;
	}

	public static boolean checkSimpleRegionCounts(IStructuredDocumentRegionList regionContainers, int[] sizes) {
		int containers = regionContainers.getLength();
		assertEquals("different number of containers", sizes.length, containers);
		for (int i = 0; i < sizes.length; i++) {
			assertEquals("container " + i + " is different", sizes[i], regionContainers.item(i).getNumberOfRegions());
			continue;
		}
		return true;
	}

	public static boolean checkSimpleRegionTypes(ITextRegionList regions, String[] contexts) {
		for (int i = 0; i < contexts.length; i++) {
			ITextRegion region = regions.get(i);
			assertEquals("region type " + i, contexts[i], region.getType());
		}
		return true;
	}

	public static String loadChars(InputStream input) {

		StringBuffer s = new StringBuffer();
		try {
			int c = -1;
			while ((c = input.read()) >= 0) {
				if (c > 255)
					break;
				s.append((char) c);
			}
			input.close();
		}
		catch (IOException e) {
			System.out.println("An I/O error occured while scanning :");
			System.out.println(e);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return s.toString();
	}


	protected static Test suite() {
		return new TestSuite(ScannerUnitTests.class);
	}

    public static void verifyLengths(int startOffset, IStructuredDocumentRegion firstDocumentRegion, String text) {
        verifyLengths(startOffset, firstDocumentRegion, text, 0);
    }
	public static void verifyLengths(int startOffset, IStructuredDocumentRegion firstDocumentRegion, String text, int lastEnd) {
		IStructuredDocumentRegion holdRegion = firstDocumentRegion;
		assertTrue("document does not start at expected offset", holdRegion.getStartOffset() == startOffset);
		while (holdRegion != null && holdRegion.getEndOffset() > 0) {
			assertTrue("zero-length StructuredDocumentRegion found", holdRegion.getStartOffset() == lastEnd);
			assertTrue("TextRegionless StructuredDocumentRegion found", holdRegion.getNumberOfRegions() > 0);
			ITextRegionList list = holdRegion.getRegions();
			int index = 0;
			for (int i = 0; i < list.size(); i++) {
				ITextRegion region = list.get(i);
				assertTrue("text region seams don't match", region.getStart() == index);
				index += region.getLength();
			}
			lastEnd = holdRegion.getEndOffset();
			holdRegion = holdRegion.getNext();
		}

		holdRegion = firstDocumentRegion;
		while (holdRegion != null && holdRegion.getNext() != null) {
			holdRegion = holdRegion.getNext();
		}
		checkModelLength(holdRegion, text.length());
	}

	public static void verifyLengths(int startOffset, IStructuredDocumentRegionList list, String text) {
		verifyLengths(startOffset, list.item(0), text);
	}

	public static void verifyLengths(IStructuredDocument document, String text) {
		verifyLengths(0, document.getFirstStructuredDocumentRegion(), text);
	}

	public static void verifyLengths(IStructuredModel model, String text) {
		verifyLengths(model.getStructuredDocument(), text);
	}

	protected IStructuredDocument fModel;
	protected String input;
	private BlockTagParser parser;
	protected Object type;

	public ScannerUnitTests(String name) {

		super(name);
	}

	protected void appendTagBlock(StringBuffer buffer, String tagname, int length) {

		buffer.append('<');
		buffer.append(tagname);
		buffer.append('>');
		for (int i = 0; i < length; i++)
			buffer.append('_');
		buffer.append("</");
		buffer.append(tagname);
		buffer.append('>');
	}

	protected void setUp() {

		setUpXML(null);
	}

	@SuppressWarnings("deprecation")
    protected IStructuredDocumentRegionList setUpJSP(String text) {

		setupModel(new GSPModelHandler());
		parser.addBlockMarker(new BlockMarker("script", null, DOMRegionContext.BLOCK_TEXT, false));
		parser.addBlockMarker(new BlockMarker("style", null, DOMRegionContext.BLOCK_TEXT, false));
		parser.addBlockMarker(new BlockMarker("disallowJSP", null, DOMRegionContext.BLOCK_TEXT, true, false));

		/*
		 * IStructuredDocumentRegionList nodes = setUpJSP("content <script>
		 * <%= expression %> </script> <a> </a> <foo:disallowJSP> <%= %>
		 * </foo:disallowJSP> >"); parser.addBlockMarker(new
		 * BlockMarker("jsp:declaration", null,
		 * XMLJSPRegionContexts.JSP_CONTENT, true)); parser.addBlockMarker(new
		 * BlockMarker("jsp:expression", null,
		 * XMLJSPRegionContexts.JSP_CONTENT, true)); parser.addBlockMarker(new
		 * BlockMarker("jsp:scriptlet", null,
		 * XMLJSPRegionContexts.JSP_CONTENT, true));
		 */
		input = text;
		fModel.set(input);
		return fModel.getRegionList();
	}

	protected void setupModel(IModelHandler contentType) {

		fModel = (IStructuredDocument) contentType.getDocumentLoader().createNewStructuredDocument();
		this.parser = (BlockTagParser) fModel.getParser();
	}

	@SuppressWarnings("deprecation")
    protected IStructuredDocumentRegionList setUpXML(String text) {

		setupModel(new ModelHandlerForXML());
		parser.addBlockMarker(new BlockMarker("script", null, DOMRegionContext.BLOCK_TEXT, false));
		parser.addBlockMarker(new BlockMarker("style", null, DOMRegionContext.BLOCK_TEXT, false));
		parser.addBlockMarker(new BlockMarker("disallowJSP", null, DOMRegionContext.BLOCK_TEXT, true, false));
		input = text;
		fModel.set(input);
		return fModel.getRegionList();
	}

	protected void testBlockScanBufferBoundaries(String contentTypeID) {
		IStructuredDocument document = null;
		// the interesting offsets are around 16k (16384)
		// for (int i = 16500; i > 16100; i--) {
		// for (int i = 17000; i > 15000; i--) {
		for (int i = 16384 + 25; i > 16364 - 25; i--) {
			StringBuffer text = new StringBuffer();
			document = StructuredModelManager.getModelManager().createStructuredDocumentFor(contentTypeID);
			appendTagBlock(text, "script", i);
			String string = text.toString();
			try {
				document.setText(this, string);
				verifyLengths(document, string);
				assertTrue("too few document regions [run value " + i + "] ", new CoreNodeList(document.getFirstStructuredDocumentRegion()).getLength() == 3);
				verifyLengths(document, string);
				IStructuredDocumentRegion startTag = document.getFirstStructuredDocumentRegion();
				IStructuredDocumentRegion middleBlock = startTag.getNext();
				// IStructuredDocumentRegion endTag = middleBlock.getNext();
				assertTrue("not block text in middle", middleBlock.getFirstRegion().getType() == DOMRegionContext.BLOCK_TEXT);
			}
			catch (Exception e) {
				assertNull("exception caught" + e, e);
			}
			// System.gc();
		}
	}

	public void testBlockScanBufferBoundariesForHTML() {
		testBlockScanBufferBoundaries("org.eclipse.wst.html.core.htmlsource");
	}

	public void testBlockScanBufferBoundariesForJSP() {
		testBlockScanBufferBoundaries("org.eclipse.jst.jsp.core.jspsource");
	}

	private void testBlockTag(IStructuredDocumentRegionList nodes) {
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 6, 1, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.BLOCK_TEXT}) && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testBlockTag_JSP() {
		testBlockTag(setUpJSP("begin <scrIPt type=\"pl2\"> </scrip t></scripts></scripts <///<!- ---></sCrIPt> end"));
	}

	/**
	 * Check block tag scanning between XML content
	 */
	public void testBlockTag_XML() {

		testBlockTag(setUpXML("begin <scrIPt type=\"pl2\"> </scrip t></scripts></scripts <///<!- ---></sCrIPt> end"));
	}

	/**
	 * Check block tag scanning between XML content
	 */
	public void testBlockTagWithJSPExpressions() {

		IStructuredDocumentRegionList nodes = setUpJSP("begin <script type=\"pl2\"> <%= \"expression\"%> </scrIPt> <a></a> <disallowJSP> <%= \"expression\" %> </disallowJSP> end");

		// OLD
		// boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 6,
		// 3, 3, 1, 3, 3, 1, 3, 3, 3, 1});
		// NEW
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 6, 3, 3, 1, 3, 3, 1, 3, 1, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		int i = 0;
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE}) && checkComplexRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.BLOCK_TEXT, DOMRegionContext.BLOCK_TEXT, DOMRegionContext.BLOCK_TEXT}, new String[][]{{DOMJSPRegionContexts.JSP_EXPRESSION_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE}}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT})
					&& checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE})
					// CORRECT BEHAVIOR
					&& checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.BLOCK_TEXT})
					// OLD, BROKEN BEHAVIOR
					// && checkSimpleRegionTypes(nodes.item(i++).getRegions(),
					// new
					// String[]{
					// XMLRegionContext.BLOCK_TEXT,
					// XMLRegionContext.BLOCK_TEXT,
					// XMLRegionContext.BLOCK_TEXT})
					&& checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(i++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	private void testBufferUnderRun_1(IStructuredDocumentRegionList nodes) {

		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 6, 2});
		assertTrue("IStructuredDocumentRegion region count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_TEXT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testBufferUnderRun_1_JSP() {
		testBufferUnderRun_1(setUpJSP("content <tag a=b/><!--c"));
	}

	/**
	 * Check for buffer under-runs on block scanning
	 */
	public void testBufferUnderRun_1_XML() {
		testBufferUnderRun_1(setUpXML("content <tag a=b/><!--c"));
	}

	private void testBufferUnderRun_2(IStructuredDocumentRegionList nodes) {

		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 6, 1});
		assertTrue("IStructuredDocumentRegion region count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testBufferUnderRun_2_JSP() {

		testBufferUnderRun_2(setUpJSP("content <tag a=b/><!--"));
	}

	/**
	 * Check for buffer under-runs on block scanning
	 */
	public void testBufferUnderRun_2_XML() {

		testBufferUnderRun_2(setUpXML("content <tag a=b/><!--"));
	}

	private void testCDATA(IStructuredDocumentRegionList nodes) {

		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_CDATA_OPEN, DOMRegionContext.XML_CDATA_TEXT, DOMRegionContext.XML_CDATA_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testCDATA_JSP() {

		testCDATA(setUpJSP("a <![CDATA[<>!!<!!></&&&--<!--]]> b"));
	}

	/**
	 * Check CDATA section scanning between content
	 */
	public void testCDATA_XML() {

		testCDATA(setUpXML("a <![CDATA[<>!!<!!></&&&--<!--]]> b"));
	}

	public void testCDATAinBlockJSP1() {
		setUpJSP("<script><![CDATA[ contents]]></script>");
	}

	public void testCDATAinBlockJSP2() {
		setUpJSP("<script><![CDATA[</script>]]></script>");
	}

	public void testCDATAinBlockJSP2a() {
		setUpJSP("<script><![CDATA[contents</script>]]></script>");
	}

	public void testCDATAinBlockJSP2b() {
		setUpJSP("<script><![CDATA[</script>contents]]></script>");
	}

	public void testCDATAinBlockJSP3() {
		setUpJSP("<script><![CDATA[]]></script>");
	}

	public void testCDATAinBlockJSP4() {
		setUpJSP("<script><![CDATA[ ]]>");
	}

	public void testCDATAinBlockJSP5() {
		setUpJSP("<script><![CDATA[ ]]]>");
	}

	public void testCDATAinBlockJSP6() {
		setUpJSP("<script><![CDATA[ ]]");
	}

	public void testCDATAinBlockJSP7() {
		setUpJSP("<script><![CDATA[ ");
	}

	public void testCDATAinBlockJSP8() {
		setUpJSP("<script><![CDATA[");
	}

	public void testCDATAinBlockXML1() {
		setUpXML("<script><![CDATA[ ]]></script>");
	}

	public void testCDATAinBlockXML2() {
		setUpXML("<script><![CDATA[</script>]]></script>");
	}

	public void testCDATAinBlockXML3() {
		setUpXML("<script><![CDATA[]]></script>");
	}

	public void testCDATAinBlockXML4() {
		setUpXML("<script><![CDATA[ ]]>");
	}

	public void testCDATAinBlockXML5() {
		setUpXML("<script><![CDATA[ ]]]>");
	}

	public void testCDATAinBlockXML6() {
		setUpXML("<script><![CDATA[ ]]");
	}

	public void testCDATAinBlockXML7() {
		setUpXML("<script><![CDATA[ ");
	}

	public void testCDATAinBlockXML8() {
		setUpXML("<script><![CDATA[");
	}

	/**
	 * Check comments between XML content
	 */
	public void testComments_JSP() {
		String text = "a <!-- --><!----><%-- --%> b";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 2, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_TEXT, DOMRegionContext.XML_COMMENT_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_CLOSE}) && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{DOMJSPRegionContexts.JSP_COMMENT_OPEN, DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMJSPRegionContexts.JSP_COMMENT_CLOSE}) && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	/**
	 * Check comments between JSP content
	 */
	public void testComments_XML() {
		String text = "a <!-- --><<!---->b";
		IStructuredDocumentRegionList nodes = setUpXML(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 1, 2, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_TEXT, DOMRegionContext.XML_COMMENT_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN}) && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_CLOSE}) && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	private void testContent(IStructuredDocumentRegionList nodes) {
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	/**
	 * Check simple content scanning
	 */
	public void testContentJSP() {

		testContent(setUpJSP("hello world"));
	}

	public void testContentXML() {

		testContent(setUpXML("hello world"));
	}
	
	public void testDirectiveInTagBody() {
		String text = "<BODY <%@ include file=\"commonEventHandlers.jspf\" %> dir=\"ltr\"> ";
		IStructuredDocumentRegionList documentRegionList = setUpJSP(text);
		verifyLengths(0, documentRegionList, text);

		checkSimpleRegionTypes(documentRegionList.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE});
		ITextRegionCollection coll = (ITextRegionCollection) documentRegionList.item(0).getRegions().get(2);
		checkSimpleRegionTypes(coll.getRegions(), new String[]{DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN, DOMRegionContext.WHITE_SPACE, DOMJSPRegionContexts.JSP_DIRECTIVE_NAME, DOMRegionContext.WHITE_SPACE, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.WHITE_SPACE, DOMJSPRegionContexts.JSP_DIRECTIVE_CLOSE});
	}

	public void testDollarsign_Leading() {
		IStructuredDocumentRegionList nodes = setUpJSP("<a type=\"$ \"/>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testDollarsign_Single() {
		IStructuredDocumentRegionList nodes = setUpJSP("<a type=\"$\"/>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testDollarsign_SingleWithSpaces() {
		IStructuredDocumentRegionList nodes = setUpJSP("<a type=\" $ \"/>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}


	public void testEmbeddedJSPDeclaration() {

		IStructuredDocumentRegionList nodes = setUpJSP("content <foo bar=\"<%! int foo; %>\" baz=\"il\">");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 9});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkComplexRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE,}, new String[][]{{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMJSPRegionContexts.JSP_DECLARATION_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.WHITE_SPACE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testEmbeddedJSPDeclarationInCDATA() {
		IStructuredDocumentRegionList nodes = setUpJSP("<![CDATA[<%!%>]]>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkComplexRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CDATA_OPEN, DOMRegionContext.XML_CDATA_TEXT, DOMRegionContext.XML_CDATA_CLOSE,}, new String[][]{{DOMJSPRegionContexts.JSP_DECLARATION_OPEN, DOMJSPRegionContexts.JSP_CLOSE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testEmbeddedJSPExpression() {
		IStructuredDocumentRegionList nodes = setUpJSP("content <foo bar=\"<%= \"Hello, World\"%>\" baz=\"il\">");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 9});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkComplexRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE,}, new String[][]{{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMJSPRegionContexts.JSP_EXPRESSION_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.WHITE_SPACE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testEmbeddedJSPExpressionInCDATA() {
		IStructuredDocumentRegionList nodes = setUpJSP("<![CDATA[<%=%>]]>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkComplexRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CDATA_OPEN, DOMRegionContext.XML_CDATA_TEXT, DOMRegionContext.XML_CDATA_CLOSE,}, new String[][]{{DOMJSPRegionContexts.JSP_EXPRESSION_OPEN, DOMJSPRegionContexts.JSP_CLOSE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testEmbeddedJSPScriptlet() {
		IStructuredDocumentRegionList nodes = setUpJSP("content <foo bar=\"<%  %>\" baz=\"il\">");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 9});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkComplexRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE,}, new String[][]{{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.WHITE_SPACE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testEmbeddedJSPScriptletInCDATA() {

		IStructuredDocumentRegionList nodes = setUpJSP("<![CDATA[<%%>]]>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkComplexRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CDATA_OPEN, DOMRegionContext.XML_CDATA_TEXT, DOMRegionContext.XML_CDATA_CLOSE,}, new String[][]{{DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN, DOMJSPRegionContexts.JSP_CLOSE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testEmbeddedTagInAttr() {

		IStructuredDocumentRegionList nodes = setUpJSP("<a href=\"<jsp:getProperty/>\">");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkComplexRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE}, new String[][]{{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_EMPTY_TAG_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE}});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testJSP_DHTMLimport() {
		String text = "<a> <?import type=\"foo\">";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3, 1, 6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_PI_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_PI_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes, text);
	}

	public void testJSP_PI() {
		String text = "begin <?php asda;lsgjalg;lasjlajglajslkajlgajsljgaljglaj?>end";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 4, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);

		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_PI_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_PI_CONTENT, DOMRegionContext.XML_PI_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testJSPAmpersandInTagNameInAttValue() {
		String text = "<a href=\"<a&b>\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{7, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count (tag)", sizeCheck);
		sizeCheck = checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 4);
		assertTrue("IStructuredDocumentRegion and ITextRegion count (broken embedded tag)", sizeCheck);

		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_CLOSE});
		assertTrue("region context type check (tag)", typeCheck);
		typeCheck = checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.UNDEFINED});
		assertTrue("region context type check (broken embedded tag)", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check (content)", typeCheck);
		verifyLengths(0, nodes, text);
	}

	public void testJSPCommentInXMLComment() {
		String text = "s<!--\n<%--c--%>\n-->\n<html>\n<body><script> <%--c--%> </script>\n";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 5, 1, 3, 1, 3, 3, 3, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		int item = 0;
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT})
					&& checkComplexRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_TEXT, DOMRegionContext.XML_COMMENT_TEXT, DOMRegionContext.XML_COMMENT_TEXT,
								DOMRegionContext.XML_COMMENT_CLOSE}, new String[][]{{DOMJSPRegionContexts.JSP_COMMENT_OPEN, DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMJSPRegionContexts.JSP_COMMENT_CLOSE}})
					&& checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT})
					&& checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE})
					&& checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT})
					&& checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE})
					&& checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE})
					&& checkComplexRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.BLOCK_TEXT, DOMRegionContext.BLOCK_TEXT, DOMRegionContext.BLOCK_TEXT}, new String[][]{{DOMJSPRegionContexts.JSP_COMMENT_OPEN,
								DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMJSPRegionContexts.JSP_COMMENT_CLOSE}})
					&& checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE})
					&& checkSimpleRegionTypes(nodes.item(9).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testJSPCommentAtStartOfXMLCommentAndBlockTag() {
		String text = "s<!--<%--c--%>\n-->\n<html>\n<body><script><%--c--%> </script>\n";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 4, 1, 3, 1, 3, 3, 2, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		int item = 0;
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		typeCheck = typeCheck && checkComplexRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_COMMENT_OPEN, DOMRegionContext.XML_COMMENT_TEXT, DOMRegionContext.XML_COMMENT_TEXT, DOMRegionContext.XML_COMMENT_CLOSE}, new String[][]{{DOMJSPRegionContexts.JSP_COMMENT_OPEN, DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMJSPRegionContexts.JSP_COMMENT_CLOSE}});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		typeCheck = typeCheck && checkComplexRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.BLOCK_TEXT, DOMRegionContext.BLOCK_TEXT}, new String[][]{{DOMJSPRegionContexts.JSP_COMMENT_OPEN, DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMJSPRegionContexts.JSP_COMMENT_CLOSE}});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testJSPCommentinXMLTag() {
		String text = "s<a <%--c--%> b=c/>\n";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 7, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		int item = 0;
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		typeCheck = typeCheck && checkComplexRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE},new String[][]{{DOMJSPRegionContexts.JSP_COMMENT_OPEN, DOMJSPRegionContexts.JSP_COMMENT_TEXT, DOMJSPRegionContexts.JSP_COMMENT_CLOSE, DOMRegionContext.WHITE_SPACE}});
		typeCheck = typeCheck && checkSimpleRegionTypes(nodes.item(item++).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	/**
	 * Check that jsp:directive.(include|page|taglib) are recognized and
	 * treated properly
	 */
	public void testJSPDirectiveTags() {
		String text = "begin <jsp:directive.taglib> <jsp:directive.page a> <jsp:directive.include a=> <jsp:directive.pages a=b> end";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 1, 4, 1, 5, 1, 6, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMJSPRegionContexts.JSP_DIRECTIVE_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMJSPRegionContexts.JSP_DIRECTIVE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(5).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMJSPRegionContexts.JSP_DIRECTIVE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_CLOSE})
					&& checkSimpleRegionTypes(nodes.item(6).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(7).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(8).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testJSPDollarsign_Trailing() {
		String text = "<a type=\" $\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testJSPDollarsign_TrailingInContent() {
		String text = "nnn$<a type=\" $\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 1, 6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check (content)", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check (content)", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check (tag)", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	/**
	 * Check JSP code sections between HTML content
	 */
	public void testJSPExpression() {
		IStructuredDocumentRegionList nodes = setUpJSP("begin <%= \"Hello,World\" %> end");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 1, 1, 1, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMJSPRegionContexts.JSP_EXPRESSION_OPEN}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMJSPRegionContexts.JSP_CONTENT}) && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{DOMJSPRegionContexts.JSP_CLOSE}) && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testJSPGreaterThanInAttValue() {
		IStructuredDocumentRegionList nodes = setUpJSP("<a type=\">next\"/>");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		assertTrue("region is not somple", !(nodes.item(0).getRegions().get(4) instanceof ITextRegionContainer));
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testJSPInvalidTagNameInAttValue() {
		String text = "S<a type=\"a<4\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});

		assertTrue("IStructuredDocumentRegion and ITextRegion count (tag)", sizeCheck);
		sizeCheck = checkSimpleRegionCount(((ITextRegionContainer) nodes.item(1).getRegions().get(4)), 5);
		assertTrue("IStructuredDocumentRegion and ITextRegion count (att value)", sizeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check (tag)", typeCheck);
		typeCheck = checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(1).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE});
		assertTrue("region context type check (att value)", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testJSPLessThanInAttValue() {
		// bails out of the attribute value with an UNDEFINED region for the
		// end quote
		String text = "<button label=\"<previous\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		sizeCheck = checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 4);
		assertTrue("IStructuredDocumentRegion and ITextRegion count (att value)", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		typeCheck = checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.UNDEFINED});
		assertTrue("region context type check (att value)", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	/**
	 * Check JSP code sections, as a tag, scanning between HTML content
	 */
	// public void testJSPScriptletTag() {
	// IStructuredDocumentRegionList nodes = setUpJSP("begin <jsp:scriptlet>
	// int foo = bar; //<jsp:Scriptlet>//</jsp:scriptlets</jsp:scriptlet>
	// end");
	//	
	// boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 1,
	// 3,
	// 1});
	// assertTrue("IStructuredDocumentRegion and ITextRegion count",
	// sizeCheck);
	//
	// boolean typeCheck =
	// checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{
	// XMLRegionContext.XML_CONTENT})
	// && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{
	// XMLRegionContext.XML_TAG_OPEN,
	// XMLRegionContext.XML_TAG_NAME,
	// XMLRegionContext.XML_TAG_CLOSE})
	// && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{
	// XMLJSPRegionContexts.JSP_CONTENT})
	// && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{
	// XMLRegionContext.XML_END_TAG_OPEN,
	// XMLRegionContext.XML_TAG_NAME,
	// XMLRegionContext.XML_TAG_CLOSE})
	// && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_CONTENT});
	// assertTrue("region context type check", typeCheck);
	//		
	// verifyModelLength();
	// }
	/**
	 * Check JSP code sections, as a tag, scanning between HTML content
	 */
	// public void testJSPTextTag() {
	// IStructuredDocumentRegionList nodes = setUpJSP("begin <jsp:text> int
	// foo
	// = bar; //<jsp:TEXT>//</jsp:Texts</jsp:text> <a> </a> end");
	//	
	// boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 1,
	// 3,
	// 1, 3, 1, 3, 1});
	// assertTrue("IStructuredDocumentRegion and ITextRegion count",
	// sizeCheck);
	//
	// boolean typeCheck =
	// checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_CONTENT})
	// && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_TAG_OPEN,
	// XMLJSPRegionContexts.XML_TAG_NAME,
	// XMLJSPRegionContexts.XML_TAG_CLOSE})
	// && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_CDATA_TEXT})
	// && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_END_TAG_OPEN,
	// XMLJSPRegionContexts.XML_TAG_NAME,
	// XMLJSPRegionContexts.XML_TAG_CLOSE})
	// && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_CONTENT})
	// && checkSimpleRegionTypes(nodes.item(5).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_TAG_OPEN,
	// XMLJSPRegionContexts.XML_TAG_NAME,
	// XMLJSPRegionContexts.XML_TAG_CLOSE})
	// && checkSimpleRegionTypes(nodes.item(6).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_CONTENT})
	// && checkSimpleRegionTypes(nodes.item(7).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_END_TAG_OPEN,
	// XMLJSPRegionContexts.XML_TAG_NAME,
	// XMLJSPRegionContexts.XML_TAG_CLOSE})
	// && checkSimpleRegionTypes(nodes.item(8).getRegions(), new String[]{
	// XMLJSPRegionContexts.XML_CONTENT});
	// assertTrue("region context type check", typeCheck);
	//		
	// verifyModelLength();
	// }
	/**
	 * Check that jsp:root is recognized and treated properly
	 */
	public void testJSPRootTag() {

		IStructuredDocumentRegionList nodes = setUpJSP("begin <jsp:root> <jsp:roots a> <jsp:roo a=> </jsp:root a=b><a>end");
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 3, 1, 4, 1, 5, 1, 6, 3, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMJSPRegionContexts.JSP_ROOT_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(3).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(4).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(5).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_CLOSE})
					&& checkSimpleRegionTypes(nodes.item(6).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(7).getRegions(), new String[]{DOMRegionContext.XML_END_TAG_OPEN, DOMJSPRegionContexts.JSP_ROOT_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(8).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(9).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testJSPTagInAttValue() {
		String text = "<a type=\"<a/>\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		sizeCheck = checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 5);
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check (tag)", typeCheck);
		typeCheck = checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_EMPTY_TAG_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE});
		assertTrue("region context type check (att value)", typeCheck);
		verifyLengths(0, nodes, text);
	}

	public void testNothinginBlockJSP9() {
		String text = "<script>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testNothinginBlockXML9() {
		String text = "<script>";
		IStructuredDocumentRegionList nodes = setUpXML(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	private void testSimpleTag(IStructuredDocumentRegionList nodes) {
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 10, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_TAG_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyModelLength();
	}

	public void testSimpleTag_JSP() {
		testSimpleTag(setUpJSP("0 <tagname attr1 attr2=value2 attr3=\"value3\"> 1"));
	}

	/**
	 * Check simple tag scanning between XML content
	 */
	public void testSimpleTag_XML() {
		testSimpleTag(setUpXML("0 <tagname attr1 attr2=value2 attr3=\"value3\"> 1"));
	}

	public void testVBLinContent() {
		String text = "#{out.foo}";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		sizeCheck = checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(0)), 3);
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		typeCheck = checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(0)).getRegions(), new String[]{DOMJSPRegionContexts.JSP_VBL_OPEN, DOMJSPRegionContexts.JSP_VBL_CONTENT, DOMJSPRegionContexts.JSP_VBL_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testVBLtolerance_transparency_Dquote() {
		String text = "<a type=\"#{out.foo}\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		sizeCheck &= checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 5);
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		typeCheck &= checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMJSPRegionContexts.JSP_VBL_OPEN, DOMJSPRegionContexts.JSP_VBL_CONTENT, DOMJSPRegionContexts.JSP_VBL_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testVBLtolerance_transparency_DquoteWithSpaces() {
		// note: whitespace on either side returns a WHITE_SPACE context
		String text = "<a type=\"_#{out.foo}_\"/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		sizeCheck &= checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 7);
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		typeCheck &= checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMJSPRegionContexts.JSP_VBL_OPEN, DOMJSPRegionContexts.JSP_VBL_CONTENT, DOMJSPRegionContexts.JSP_VBL_CLOSE, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testVBLtolerance_transparency_Squote() {
		String text = "<a type='#{out.foo}'/>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		sizeCheck &= checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 5);
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		typeCheck &= checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE, DOMJSPRegionContexts.JSP_VBL_OPEN, DOMJSPRegionContexts.JSP_VBL_CONTENT, DOMJSPRegionContexts.JSP_VBL_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes, text);
	}

	public void __DISABLED__testXML_DHTMLimport() {
		//TODO: Fix and re-enable this test
		String text = "<a> <?import type=\"foo\">";
		IStructuredDocumentRegionList nodes = setUpXML(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{3, 1, 6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		typeCheck = checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_PI_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_PI_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes, text);
	}

	public void testXML_PI() {
		String text = "begin <?php asda;lsgjalg;lasjlajglajslkajlgajsljgaljglaj?>end";
		IStructuredDocumentRegionList nodes = setUpXML(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 4, 1});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);

		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_CONTENT}) && checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMRegionContext.XML_PI_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_PI_CONTENT, DOMRegionContext.XML_PI_CLOSE}) && checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMRegionContext.XML_CONTENT});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testXMLDollarsign_Trailing() {
		String text = "<a type=\" $\"/>";
		IStructuredDocumentRegionList nodes = setUpXML(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
		assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
		boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
		assertTrue("region context type check", typeCheck);
		verifyLengths(0, nodes.item(0), text);
	}

	public void testUndefinedRegionContainer() {
		// see RATLC RATLC00284776
		String text = "<option <elms:inputValue value=\"<%=uomvox.uomID%>\"/><%=uomvox.uomID.equals(uomID) ? \" selected\" : \"\"%>>";
		IStructuredDocumentRegionList nodes = setUpJSP(text);
		boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{5});
		assertTrue("IStructuredDocumentRegion and overall ITextRegion count", sizeCheck);
		// checkComplexRegionTypes(nodes.item(0).getRegions(), new
		// String[]{XMLRegionContext.XML_TAG_OPEN,
		// XMLRegionContext.XML_TAG_NAME,
		// XMLRegionContext.XML_TAG_ATTRIBUTE_NAME,
		// XMLRegionContext.UNDEFINED, XMLRegionContext.XML_TAG_CLOSE}, new
		// String[][] {{}});
		verifyEmbeddedContainerParentage(nodes);
		verifyLengths(0, nodes.item(0), text);
	}

	/**
	 * @param nodes
	 */
	private void verifyEmbeddedContainerParentage(IStructuredDocumentRegionList nodes) {
		for (int i = 0; i < nodes.getLength(); i++) {
			IStructuredDocumentRegion r = nodes.item(i);
			ITextRegionList list = r.getRegions();
			for (int j = 0; j < list.size(); j++) {
				ITextRegion region = list.get(j);
				if (region instanceof ITextRegionContainer) {
					assertNotNull("parent is null for " + region, ((ITextRegionContainer) region).getParent());
				}
			}
		}
	}

	protected boolean verifyModelLength() {
		return checkModelLength(fModel, input.length());
	}
	
	// And now for GSP-specific tests
	public void testJSPinContent() {
	    String text = "<% out.foo %>";
	    IStructuredDocumentRegionList nodes = setUpJSP(text);
	    boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 1, 1});
	    assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
	    boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN}) &&
                            checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMJSPRegionContexts.JSP_CONTENT}) &&
                            checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMJSPRegionContexts.JSP_CLOSE});
	    assertTrue("region context type check", typeCheck);
	    assertTrue("region context type check", typeCheck);
	    verifyLengths(0, nodes, text);
        verifyLengths(0, nodes.item(0), "<%", 0);
        verifyLengths(2, nodes.item(1), "out.foo", 2);
        verifyLengths(11, nodes.item(2), "%>", 11);
	}
	public void testGSPinContent() {
	    String text = "${out.foo}";
        IStructuredDocumentRegionList nodes = setUpJSP(text);
        boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, 1, 1});
        assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
        boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN}) &&
                            checkSimpleRegionTypes(nodes.item(1).getRegions(), new String[]{DOMJSPRegionContexts.JSP_CONTENT}) &&
                            checkSimpleRegionTypes(nodes.item(2).getRegions(), new String[]{DOMJSPRegionContexts.JSP_CLOSE});
        assertTrue("region context type check", typeCheck);
        assertTrue("region context type check", typeCheck);
        verifyLengths(0, nodes, text);
        verifyLengths(0, nodes.item(0), "${", 0);
        verifyLengths(2, nodes.item(1), "out.foo", 2);
        verifyLengths(9, nodes.item(2), "}", 9);
	}
	

	public void testGSPtolerance_transparency_Dquote() {
	    IStructuredDocumentRegionList nodes = setUpJSP("<a type=\"${out.foo}\"/>");
	    boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
	    sizeCheck &= checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 5);
	    assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
	    boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
	    typeCheck &= checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE});
	    assertTrue("region context type check", typeCheck);
	    verifyModelLength();
	}

	public void testGSPtolerance_transparency_DquoteWithSpaces() {
	    // note: whitespace on either side returns a WHITE_SPACE context
	    IStructuredDocumentRegionList nodes = setUpJSP("<a type=\"_${out.foo}_\"/>");
	    boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
	    sizeCheck &= checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 7);
	    assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
	    boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
	    typeCheck &= checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE});
	    assertTrue("region context type check", typeCheck);
	    verifyModelLength();
	}

	public void testGSPtolerance_transparency_Squote() {
	    IStructuredDocumentRegionList nodes = setUpJSP("<a type='${out.foo}'/>");
	    boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{6});
	    sizeCheck &= checkSimpleRegionCount(((ITextRegionContainer) nodes.item(0).getRegions().get(4)), 5);
	    assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
	    boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(), new String[]{DOMRegionContext.XML_TAG_OPEN, DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS, DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE, DOMRegionContext.XML_EMPTY_TAG_CLOSE});
	    typeCheck &= checkSimpleRegionTypes(((ITextRegionContainer) nodes.item(0).getRegions().get(4)).getRegions(), new String[]{DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE, DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN, DOMJSPRegionContexts.JSP_CONTENT, DOMJSPRegionContexts.JSP_CLOSE, DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE});
	    assertTrue("region context type check", typeCheck);
	    verifyModelLength();
	}

    private void testSimpleTagRegions(IStructuredDocumentRegionList nodes, String[] contexts) {
        boolean sizeCheck = checkSimpleRegionCounts(nodes, new int[]{1, contexts.length, 1});
        assertTrue("IStructuredDocumentRegion and ITextRegion count", sizeCheck);
        
        boolean typeCheck = checkSimpleRegionTypes(nodes.item(0).getRegions(),
                new String[] { DOMRegionContext.XML_CONTENT })
                && checkSimpleRegionTypes(nodes.item(1).getRegions(),
                        contexts)
                && checkSimpleRegionTypes(nodes.item(2).getRegions(),
                        new String[] { DOMRegionContext.XML_CONTENT });
        assertTrue("region context type check", typeCheck);
        verifyModelLength();
    }
    
	public void testNoSlashyTag_GSP() {
	    String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.XML_EMPTY_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <tag/> 1"), contexts);
	}

	public void testSlashyTag_GSP1() {
        String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.XML_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <tag/name> 1"), contexts);
	}

	public void testSlashyTag_GSP2() {
        String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.XML_EMPTY_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <tag/name /> 1"), contexts);
	}
	public void testSlashyTag_GSP3() {
	    String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
	            DOMRegionContext.XML_TAG_NAME,
	            DOMRegionContext.XML_EMPTY_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <tag/name /> 1"), contexts);
	}
    public void testSlashyTag_GSP4() {
        String[] contexts = new String[] { DOMRegionContext.XML_END_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.XML_TAG_CLOSE };
        testSimpleTagRegions(setUpJSP("0 </tag/a/b/c> 1"), contexts);
    }
    public void testSlashyTag_GSP5() {
        String[] contexts = new String[] { DOMRegionContext.XML_END_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.XML_TAG_CLOSE };
        testSimpleTagRegions(setUpJSP("0 </tag:/:/b/c> 1"), contexts);
    }

    public void testBadSlashyTag_GSP1() {
        String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.UNDEFINED,
                DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
                DOMRegionContext.XML_TAG_CLOSE };
        testSimpleTagRegions(setUpJSP("0 <tag /name> 1"), contexts);
    }
    public void testBadSlashyTag_GSP2() {
        String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.UNDEFINED,
                DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
                DOMRegionContext.XML_EMPTY_TAG_CLOSE };
        testSimpleTagRegions(setUpJSP("0 <tag /name /> 1"), contexts);
	}

	public void testBadSlashyTag_GSP3() {
        String[] contexts = new String[] { DOMRegionContext.XML_END_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.UNDEFINED,
                DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
                DOMRegionContext.XML_TAG_CLOSE };
        testSimpleTagRegions(setUpJSP("0 </tag /name> 1"), contexts);
	}
	public void testBadSlashyTag_GSP4() {
	    String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
	            DOMRegionContext.UNDEFINED,
	            DOMRegionContext.XML_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <tag/ > 1"), contexts);
	}
	public void testBadSlashyTag_GSP5() {
	    String[] contexts = new String[] { DOMRegionContext.XML_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
	            DOMRegionContext.UNDEFINED,
	            DOMRegionContext.XML_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <tag attr/ > 1"), contexts);
	}
	public void testBadSlashyTag_GSP6() {
	    String[] contexts = new String[] { DOMRegionContext.XML_END_TAG_OPEN,
	            DOMRegionContext.UNDEFINED,
                DOMRegionContext.XML_TAG_NAME,
	            DOMRegionContext.XML_TAG_CLOSE };
	    testSimpleTagRegions(setUpJSP("0 <//tag > 1"), contexts);
	}
    public void testBadSlashyTag_GSP7() {
        String[] contexts = new String[] { DOMRegionContext.XML_END_TAG_OPEN,
                DOMRegionContext.XML_TAG_NAME,
                DOMRegionContext.UNDEFINED,
                DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
                DOMRegionContext.XML_TAG_CLOSE };
        testSimpleTagRegions(setUpJSP("0 </a//tag > 1"), contexts);
    }
}
