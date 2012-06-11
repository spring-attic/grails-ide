// COPIED_FROM org.eclipse.jst.jsp.core.internal.java.XMLJSPRegionHelper
/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     VMWare, Inc.    - augmented for use with Grails
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.translation.internal;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.jsp.core.internal.Logger;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.TLDCMDocumentManager;
import org.eclipse.jst.jsp.core.internal.java.EscapedTextUtil;
import org.eclipse.jst.jsp.core.internal.parser.JSPSourceParser;
import org.eclipse.jst.jsp.core.internal.provisional.JSP11Namespace;
import org.eclipse.jst.jsp.core.internal.provisional.contenttype.ContentTypeIdForJSP;
import org.eclipse.jst.jsp.core.internal.regions.DOMJSPRegionContexts;
import org.eclipse.jst.jsp.core.internal.util.FileContentCache;
import org.eclipse.wst.sse.core.internal.ltk.modelhandler.IModelHandler;
import org.eclipse.wst.sse.core.internal.ltk.parser.BlockMarker;
import org.eclipse.wst.sse.core.internal.ltk.parser.StructuredDocumentRegionHandler;
import org.eclipse.wst.sse.core.internal.modelhandler.ModelHandlerRegistry;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import org.grails.ide.eclipse.editor.gsp.parser.GSPDocumentLoader;
import org.grails.ide.eclipse.editor.gsp.parser.GSPSourceParser;



/**
 * Parser/helper class for GSPTranslator. Used for parsing XML-JSP regions (in
 * a script block) A lot of logic borrowed from TLDCMDocumentManager. There
 * should be only one XMLJSPRegionHelper per text file
 * @author Andrew Eisenberg
 */
class XMLJSPRegionHelper implements StructuredDocumentRegionHandler {
	private final GSPTranslator fTranslator;
	protected JSPSourceParser fLocalParser = null;
	protected String fTextToParse = null;
	// need this if not at the start of the document (eg. parsing just a
	// script block)
	protected int fStartOfTextToParse = 0;
	// name of the open tag that was last handled (if we are interested in it)
	protected String fTagname = null;
	protected String fTextBefore = ""; //$NON-NLS-1$
	protected String fUnescapedText = ""; //$NON-NLS-1$
	protected String fStrippedText = ""; //$NON-NLS-1$
	// for reconciling cursor position later
	int fPossibleOwner = GSPTranslator.SCRIPTLET;
	/**
	 * Determines whether translated source appends are indicated as
	 * "indirect", affecting how offsets are mapped.
	 */
	boolean fAppendAsIndirectSource;

	public XMLJSPRegionHelper(GSPTranslator translator, boolean appendAsIndirectSource) {
		getLocalParser().addStructuredDocumentRegionHandler(this);
		this.fTranslator = translator;
		fAppendAsIndirectSource = appendAsIndirectSource;
	}

	protected JSPSourceParser getLocalParser() {
		if (fLocalParser == null)
			fLocalParser = new GSPSourceParser();
		return fLocalParser;
	}

	public void addBlockMarker(BlockMarker marker) {
		fLocalParser.addBlockMarker(marker);
	}

	public void reset(String textToParse) {
		reset(textToParse, 0);
	}

	public void reset(String textToParse, int start) {
		fStartOfTextToParse = start;
		fTextToParse = textToParse;
	}

	public void forceParse() {
		String contents = fTextToParse;

		// GRAILS Change was:
//		IStructuredDocument document = (IStructuredDocument) new JSPDocumentLoader().createNewStructuredDocument();
		// new: use GSPDocumentLoader instead
		IStructuredDocument document = (IStructuredDocument) new GSPDocumentLoader().createNewStructuredDocument();
		// end
		if(contents != null && document != null) {		
			// from outer class
			List blockMarkers = this.fTranslator.getBlockMarkers();
			// this adds the current markers from the outer class list
			// to this parser so parsing works correctly
			for (int i = 0; i < blockMarkers.size(); i++) {
				addBlockMarker((BlockMarker) blockMarkers.get(i));
			}
			reset(contents);
	
			document.set(contents);
			IStructuredDocumentRegion cursor = document.getFirstStructuredDocumentRegion();
			while(cursor != null) {
				nodeParsed(cursor);
				cursor = cursor.getNext();
			}
		}
	}

	/*
	 * parse an entire file
	 * 
	 * @param filename @return
	 */
	public boolean parse(String filePathString) {
		boolean parsed = false;
		IStructuredDocument document = null;
		String contents = null;

		IPath filePath = new Path(filePathString);
		IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
		if (f == null || !f.isAccessible()) {
			f = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filePath);
		}
		if (f != null && f.isAccessible()) {
			/*
			 * using a real document allows us to pull out text in the
			 * translator for dealing with TEI variables
			 */
			try {
				IModelHandler handler = ModelHandlerRegistry.getInstance().getHandlerFor(f, false);
				if (handler == null)
					handler = ModelHandlerRegistry.getInstance().getHandlerForContentTypeId(ContentTypeIdForJSP.ContentTypeID_JSPFRAGMENT);
				document = (IStructuredDocument) handler.getDocumentLoader().createNewStructuredDocument();
				contents = FileContentCache.getInstance().getContents(f.getFullPath());
			}
			catch (CoreException e) {
				Logger.logException(e);
			}
		}
		if (contents != null && document != null) {
			// from outer class
			List blockMarkers = this.fTranslator.getBlockMarkers();
			// this adds the current markers from the outer class list
			// to this parser so parsing works correctly
			for (int i = 0; i < blockMarkers.size(); i++) {
				addBlockMarker((BlockMarker) blockMarkers.get(i));
			}
			reset(contents);
			// forces parse
			document.set(contents);
			IStructuredDocumentRegion cursor = document.getFirstStructuredDocumentRegion();
			while (cursor != null) {
				nodeParsed(cursor);
				cursor = cursor.getNext();
			}
			parsed = true;
		}
		return parsed;
	}


	/*
	 * listens to parser node parsed events adds to local scriplet,
	 * expression, declaration buffers determines which type of region the
	 * cursor is in, and adjusts cursor offset accordingly
	 */
	public void nodeParsed(IStructuredDocumentRegion sdRegion) {

		try {
			if (isJSPEndRegion(sdRegion)) {
				String nameStr = getRegionName(sdRegion);
				if (isPossibleCustomTag(nameStr)) {
					// this custom tag may define variables
					this.fTranslator.addTaglibVariables(nameStr, sdRegion);
				}
				fTagname = null;
			}
			else if (isJSPStartRegion(sdRegion)) {
				int illegalContent = hasIllegalContent(sdRegion);
				if (illegalContent >= 0)
					decodeRemainingRegions(sdRegion, illegalContent);
				String nameStr = getRegionName(sdRegion);
				if (sdRegion.getFirstRegion().getType() == DOMRegionContext.XML_TAG_OPEN) {
					if (isPossibleCustomTag(nameStr)) {
						// this custom tag may define variables
						this.fTranslator.addTaglibVariables(nameStr, sdRegion);
					}
				}
				if (isJSPRegion(nameStr))
					fTagname = nameStr;
				else
					fTagname = null;


				// this section assumes important content (to translate)
				// IS the opening tag

				// handle include and directive
				if (fTagname != null) {
					processOtherRegions(sdRegion);
				}


				// handle jsp:useBean
				if (fTagname != null && fTagname.equals(JSP11Namespace.ElementName.USEBEAN)) {
					processUseBean(sdRegion);
				}
			}
			else if (sdRegion.getFirstRegion().getType() == DOMJSPRegionContexts.JSP_CONTENT || sdRegion.getFirstRegion().getType() == DOMRegionContext.XML_CONTENT) {
				// this section assumes important content (to translate)
				// is AFTER the opening tag
				if (fTagname != null) {
					// assign contents to one of the tables
					if (isScriptlet(fTagname)) {
						processScriptlet(sdRegion);
					}
					else if (isExpression(fTagname)) {
						processExpression(sdRegion);
					}
					else if (isDeclaration(fTagname)) {
						processDeclaration(sdRegion);
					}
				}
			}
			else {
				fTagname = null;
				/*
				 * We may have been asked to decode a script block with an XML
				 * comment in it (a common provision for browsers not
				 * supporting client scripting). While
				 * scriptlets/expressions/declarations will be identified as
				 * part of the normal parsing process, the temporary document
				 * used here will be parsed differently, respecting the
				 * comment since it's not in a tag block region any more, and
				 * the custom tags in the comment will not be found. Run those
				 * comment text pieces through the translator on their own.
				 */
				Iterator regions = sdRegion.getRegions().iterator();
				while (regions.hasNext()) {
					ITextRegion region = (ITextRegion) regions.next();
					if (DOMRegionContext.XML_COMMENT_TEXT.equals(region.getType()) && region.getStart() != 0) {
						fTranslator.decodeScriptBlock(sdRegion.getFullText(region), sdRegion.getStartOffset(region));
					}
				}
			}
			// this updates cursor position
			checkCursorInRegion(sdRegion);
		}
		catch (NullPointerException e) {
			// logging this exception that I've seen a couple of times...
			// seems to happen during shutdown of unit tests, at which
			// point Logger has already been unloaded
			try {
				Logger.logException("XMLJSPRegionHelper: exception in node parsing", e); //$NON-NLS-1$
			}
			catch (NoClassDefFoundError ex) {
				// do nothing, since we're just ending
			}
		}
	}

	private void decodeRemainingRegions(IStructuredDocumentRegion sdRegion, int start) {
		ITextRegionList regionList = sdRegion.getRegions();
		if(regionList != null) {
			ITextRegion region = regionList.get(start);
			String text = sdRegion.getFullText();
			if (text != null && region.getStart() <= text.length())
				fTranslator.decodeScriptBlock(text.substring(region.getStart(), text.length()), 0);
		}
	}

	private int hasIllegalContent(IStructuredDocumentRegion sdRegion) {
		ITextRegionList list = sdRegion.getRegions();
		for (int i = 0; i < list.size(); i++) {
			ITextRegion region = list.get(i);
			String type = region.getType();
			if (type == DOMRegionContext.UNDEFINED)
				return i;
			if (type == DOMRegionContext.XML_END_TAG_OPEN || type == DOMRegionContext.XML_EMPTY_TAG_CLOSE || type == DOMJSPRegionContexts.JSP_DIRECTIVE_CLOSE)
				return -1;
		}
		return -1;
	}

	public void resetNodes() {
		// do nothing
	}

	private void checkCursorInRegion(IStructuredDocumentRegion sdRegion) {
		// if cursor is in this region...
		if (this.fTranslator.getSourcePosition() >= fStartOfTextToParse + sdRegion.getStartOffset() && this.fTranslator.getSourcePosition() <= fStartOfTextToParse + sdRegion.getEndOffset()) {
			int endOfNameTag = sdRegion.getStartOffset();
			int offset = fTextBefore.length() - fStrippedText.length();
			// offset in addtion to what's already in the buffer
			this.fTranslator.setRelativeOffset(this.fTranslator.getSourcePosition() - (fStartOfTextToParse + endOfNameTag) - offset);
			// outer class method
			this.fTranslator.setCursorOwner(fPossibleOwner);
			// add length of what's already in the buffer
			this.fTranslator.setRelativeOffset(this.fTranslator.getRelativeOffset() + this.fTranslator.getCursorOwner().length());
			if (fPossibleOwner == GSPTranslator.EXPRESSION) {
				// add length of expression prefix if necessary...
				this.fTranslator.setRelativeOffset(this.fTranslator.getRelativeOffset() + GSPTranslator.EXPRESSION_PREFIX.length());
			}
		}
	}

	protected void processDeclaration(IStructuredDocumentRegion sdRegion) {
		prepareText(sdRegion);
		IStructuredDocumentRegion currentNode = fTranslator.getCurrentNode();
		this.fTranslator.translateDeclarationString(fStrippedText, currentNode, currentNode.getStartOffset(), currentNode.getLength(), fAppendAsIndirectSource);
		fPossibleOwner = GSPTranslator.DECLARATION;
	}

	protected void processExpression(IStructuredDocumentRegion sdRegion) {
		prepareText(sdRegion);
		IStructuredDocumentRegion currentNode = fTranslator.getCurrentNode();
		this.fTranslator.translateExpressionString(fStrippedText, currentNode, currentNode.getStartOffset(), currentNode.getLength(), fAppendAsIndirectSource);
		fPossibleOwner = GSPTranslator.EXPRESSION;
	}

	protected void processScriptlet(IStructuredDocumentRegion sdRegion) {
		prepareText(sdRegion);
		IStructuredDocumentRegion currentNode = fTranslator.getCurrentNode();
		this.fTranslator.translateScriptletString(fStrippedText, currentNode, currentNode.getStartOffset(), currentNode.getLength(), fAppendAsIndirectSource);
		fPossibleOwner = GSPTranslator.SCRIPTLET;
	}

	/*
	 * Substitutes values for entity references, strips CDATA tags, and keeps
	 * track of string length(s) for cursor position calculation later. @param
	 * sdRegion
	 */
	protected void prepareText(IStructuredDocumentRegion sdRegion) {
		fTextBefore = fTextToParse.substring(sdRegion.getStartOffset(), sdRegion.getEndOffset());
		fUnescapedText = EscapedTextUtil.getUnescapedText(fTextBefore);
		fStrippedText = this.fTranslator.stripCDATA(fUnescapedText);
	}

	protected void processUseBean(IStructuredDocumentRegion sdRegion) {
		if (fTagname != null && isUseBean(fTagname)) {

			String beanClass, beanType, beanId, beanDecl = ""; //$NON-NLS-1$
			beanClass = getAttributeValue("class", sdRegion); //$NON-NLS-1$
			beanType = getAttributeValue("type", sdRegion); //$NON-NLS-1$
			beanId = getAttributeValue("id", sdRegion); //$NON-NLS-1$

			if (beanId != null && (beanType != null || beanClass != null)) {
				String prefix = null;
				if (beanType.length() != 0) {
					/* a type was specified */
					prefix = beanType + " " + beanId + " = "; //$NON-NLS-1$ //$NON-NLS-2$
				}
				else {
					/* no type was specified, use the concrete class value */
					prefix = beanClass + " " + beanId + " = "; //$NON-NLS-1$ //$NON-NLS-2$
				}
				/*
				 * Define as null by default. If a concrete class was
				 * specified, supply a default constructor invocation instead.
				 */
				String suffix = "null;\n"; //$NON-NLS-1$
				// 186771 - JSP Validator problem with included useBean
				if (beanClass.length() > 0) {
					suffix = "new " + beanClass + "();\n"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				beanDecl = prefix + suffix;
			}

			IStructuredDocumentRegion currentNode = fTranslator.getCurrentNode();
			this.fTranslator.translateScriptletString(beanDecl, currentNode, currentNode.getStartOffset(), currentNode.getLength(), fAppendAsIndirectSource);
			fPossibleOwner = GSPTranslator.SCRIPTLET;
		}
	}

	protected void processOtherRegions(IStructuredDocumentRegion sdRegion) {
		processIncludeDirective(sdRegion);
		processPageDirective(sdRegion);
	}

	protected void processIncludeDirective(IStructuredDocumentRegion sdRegion) {
		if (isIncludeDirective(fTagname)) {
			// the directive name region itself contains the attrs...
			if (sdRegion.getRegions().get(0).getType() == DOMRegionContext.XML_CONTENT)
				sdRegion = sdRegion.getPrevious();
			String fileLocation = getAttributeValue("file", sdRegion); //$NON-NLS-1$
			this.fTranslator.handleIncludeFile(fileLocation);
		}
		else if (isTaglibDirective(fTagname)) {
			// also add the ones created here to the parent document
			String prefix = getAttributeValue("prefix", sdRegion); //$NON-NLS-1$
			TLDCMDocumentManager documentManager = this.fTranslator.getTLDCMDocumentManager();
			if (documentManager != null) {
				List docs = documentManager.getCMDocumentTrackers(prefix, this.fTranslator.getCurrentNode().getStartOffset());
				Iterator it = docs.iterator();
				Iterator elements = null;
				CMNode node = null;
				CMDocument doc = null;
				BlockMarker marker = null;
				while (it.hasNext()) {
					doc = (CMDocument) it.next();
					elements = doc.getElements().iterator();
					while (elements.hasNext()) {
						node = (CMNode) elements.next();
						marker = new BlockMarker(node.getNodeName(), null, DOMJSPRegionContexts.JSP_CONTENT, true);
						// global scope is OK because we have encountered this
						// <@taglib> directive
						// so it all markers from it should will be in scope
						// add to this local parser
						addBlockMarker(marker);
						// add to outer class marker list, for
						this.fTranslator.getBlockMarkers().add(marker);
					}
				}
			}
		}
	}

	protected void processPageDirective(IStructuredDocumentRegion sdRegion) {
		if (isPageDirective(fTagname)) {
			this.fTranslator.translatePageDirectiveAttributes(sdRegion.getRegions().iterator(), sdRegion);
		}
	}

	/*
	 * convenience method to get an attribute value from attribute name
	 */
	protected String getAttributeValue(String attrName, IStructuredDocumentRegion sdRegion) {
		String sdRegionText = fTextToParse.substring(sdRegion.getStartOffset(), sdRegion.getEndOffset());
		String textRegionText, attrValue = ""; //$NON-NLS-1$
		Iterator it = sdRegion.getRegions().iterator();
		ITextRegion nameRegion, valueRegion = null;
		while (it.hasNext()) {
			nameRegion = (ITextRegion) it.next();
			if (nameRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
				textRegionText = sdRegionText.substring(nameRegion.getStart(), nameRegion.getTextEnd());
				if (textRegionText.equalsIgnoreCase(attrName)) {
					while (it.hasNext()) {
						valueRegion = (ITextRegion) it.next();
						if (valueRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
							attrValue = sdRegionText.substring(valueRegion.getStart(), valueRegion.getEnd());
							break; // inner
						}
					}
					break; // outer
				}
			}
		}
		return StringUtils.stripQuotes(attrValue);
	}

	// these methods determine what content gets added to the local scriplet,
	// expression, declaration buffers
	/*
	 * return true for elements whose contents we might want to add to the
	 * java file we are building
	 */
	protected boolean isJSPStartRegion(IStructuredDocumentRegion sdRegion) {
		return (sdRegion.getFirstRegion().getType() == DOMRegionContext.XML_TAG_OPEN || sdRegion.getFirstRegion().getType() == DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN);
	}

	private boolean isJSPEndRegion(IStructuredDocumentRegion sdRegion) {
		return (sdRegion.getFirstRegion().getType() == DOMRegionContext.XML_END_TAG_OPEN);
	}

	protected boolean isJSPRegion(String tagName) {
		return isDeclaration(tagName) || isExpression(tagName) || isScriptlet(tagName) || isUseBean(tagName) || isIncludeDirective(tagName) || isPossibleCustomTag(tagName) || isTaglibDirective(tagName) || isPageDirective(tagName);
	}

	protected boolean isDeclaration(String tagName) {
		return tagName.equalsIgnoreCase("jsp:declaration"); //$NON-NLS-1$		
	}

	protected boolean isExpression(String tagName) {
		return tagName.equalsIgnoreCase("jsp:expression"); //$NON-NLS-1$		
	}

	protected boolean isScriptlet(String tagName) {
		return tagName.equalsIgnoreCase("jsp:scriptlet"); //$NON-NLS-1$
	}

	protected boolean isUseBean(String tagName) {
		return tagName.equalsIgnoreCase("jsp:useBean"); //$NON-NLS-1$
	}

	protected boolean isIncludeDirective(String tagName) {
		return tagName.equalsIgnoreCase("jsp:directive.include"); //$NON-NLS-1$
	}

	protected boolean isPossibleCustomTag(String tagName) {
//		int colonIndex = tagName.indexOf(":");
//		if (colonIndex > 0) {
//			String prefix = tagName.substring(0, colonIndex);
//			if (prefix.equals("jsp")) { //$NON-NLS-1$
//				return false;
//			}
//			if (prefix.length() > 0) {
//				TagMarker[] prefixes = (TagMarker[]) fLocalParser.getNestablePrefixes().toArray(new TagMarker[0]);
//				for (int i = 0; i < prefixes.length; i++) {
//					if (prefix.equals(prefixes[i].getTagName())) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
		return tagName.indexOf(':') > -1 && !tagName.startsWith(GSPTranslator.JSP_PREFIX);
	}

	protected boolean isTaglibDirective(String tagName) {
		return tagName.equalsIgnoreCase("jsp:directive.taglib"); //$NON-NLS-1$
	}

	protected boolean isPageDirective(String tagName) {
		return tagName.equalsIgnoreCase("jsp:directive.page"); //$NON-NLS-1$
	}

	protected String getRegionName(IStructuredDocumentRegion sdRegion) {

		String nameStr = ""; //$NON-NLS-1$
		ITextRegionList regions = sdRegion.getRegions();
		for (int i = 0; i < regions.size(); i++) {
			ITextRegion r = regions.get(i);
			if (r.getType() == DOMRegionContext.XML_TAG_NAME) {
				nameStr = fTextToParse.substring(sdRegion.getStartOffset(r), sdRegion.getTextEndOffset(r));
				break;
			}
		}
		return nameStr.trim();
	}

	/**
	 * get the contents of a file as a String
	 * 
	 * @param filePath - the path to the file
	 * @return the contents, null if the file could not be found
	 */
	protected String getContents(String filePath) {
		IPath path = new Path(filePath);
		return FileContentCache.getInstance().getContents(path.makeAbsolute());
	}
}