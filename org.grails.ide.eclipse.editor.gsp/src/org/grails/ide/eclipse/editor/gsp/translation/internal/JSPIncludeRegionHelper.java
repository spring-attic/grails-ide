// COPIED_FROM org.eclipse.jst.jsp.core.internal.java.JSPIncludeRegionHelper
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

import org.eclipse.jst.jsp.core.internal.regions.DOMJSPRegionContexts;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;



/**
 * Extension of XMLJSPRegionHelper intended to parse entire included JSP files.
 * Basically it expands the rules for what tags are parsed as JSP.
 * @author Andrew Eisenberg
 */
class JSPIncludeRegionHelper extends XMLJSPRegionHelper {
	/**
	 * @param JSPTranslator
	 */
	public JSPIncludeRegionHelper(GSPTranslator translator, boolean appendAsIndirectSource) {
		super(translator, appendAsIndirectSource);
	}

	// these methods determine what content gets added to the local scriplet, expression, declaration buffers
	/*
	 * return true for elements whose contents we might want to add to the java file we are building
	 */
	protected boolean isJSPStartRegion(IStructuredDocumentRegion sdRegion) {
		String type = sdRegion.getFirstRegion().getType();
		return type == DOMRegionContext.XML_TAG_OPEN || type == DOMJSPRegionContexts.JSP_DECLARATION_OPEN || type == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN || type == DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN || type == DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN || type == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME;
	}

	protected boolean isDeclaration(String tagName) {
		return tagName.equalsIgnoreCase("jsp:declaration") //$NON-NLS-1$		
					|| tagName.equalsIgnoreCase("<%!"); //$NON-NLS-1$		
	}

	protected boolean isExpression(String tagName) {
		return tagName.equalsIgnoreCase("jsp:expression") //$NON-NLS-1$		
					|| tagName.equalsIgnoreCase("<%="); //$NON-NLS-1$
	}

	protected boolean isScriptlet(String tagName) {
		return tagName.equalsIgnoreCase("jsp:scriptlet") //$NON-NLS-1$		
					|| tagName.equalsIgnoreCase("<%") 
                    || tagName.equalsIgnoreCase("${"); //$NON-NLS-1$
	}

	protected boolean isIncludeDirective(String tagName) {
		return tagName.equalsIgnoreCase("jsp:directive.include") || //$NON-NLS-1$
					tagName.equalsIgnoreCase("include"); //$NON-NLS-1$
	}

	protected boolean isTaglibDirective(String tagName) {
		return tagName.equalsIgnoreCase("jsp:directive.taglib") //$NON-NLS-1$
					|| tagName.equalsIgnoreCase("taglib"); //$NON-NLS-1$
	}

	protected boolean isPageDirective(String tagName) {
		return tagName.equalsIgnoreCase("jsp:directive.page") //$NON-NLS-1$
					|| tagName.equalsIgnoreCase("page"); //$NON-NLS-1$
	}

	// different btwn XML-JSP and JSP tags		
	protected String getRegionName(IStructuredDocumentRegion sdRegion) {
		ITextRegion nameRegion = null;
		String nameStr = ""; //$NON-NLS-1$
		int size = sdRegion.getRegions().size();
		if (size > 1) {
			// presumably XML-JSP <jsp:scriptlet> | <jsp:expression> | <jsp:declaration>
			nameRegion = sdRegion.getRegions().get(1);
		}
		else if (size == 1) {
			// presumably JSP open <% | <%= | <%!
			nameRegion = sdRegion.getRegions().get(0);
		}
		if (nameRegion != null)
			nameStr = fTextToParse.substring(sdRegion.getStartOffset(nameRegion), sdRegion.getTextEndOffset(nameRegion));
		return nameStr.trim();
	}

	protected void processOtherRegions(IStructuredDocumentRegion sdRegion) {
		processIncludeDirective(sdRegion);
		processPageDirective(sdRegion);
	}

	protected void prepareText(IStructuredDocumentRegion sdRegion) {
		fStrippedText = fTextBefore = fTextToParse.substring(sdRegion.getStartOffset(), sdRegion.getEndOffset());
	}
}
