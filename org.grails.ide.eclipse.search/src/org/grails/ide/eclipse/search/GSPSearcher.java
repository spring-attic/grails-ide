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
package org.grails.ide.eclipse.search;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jst.jsp.core.internal.domdocument.ElementImplForJSP;
import org.eclipse.search.ui.text.Match;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.document.AttrImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.grails.ide.eclipse.editor.gsp.model.GSPStructuredModel;

/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
@SuppressWarnings("restriction")
public abstract class GSPSearcher extends FileSearcher {

	protected abstract void visit(GLinkTag glink);
	
	/**
	 * Helper class to represent and operate on GLink tags inside a .gsp file
	 * @author Kris De Volder
	 *
	 * @since 2.9
	 */
	protected class GLinkTag {
	
		private static final String CONTROLLER_ATTR_NAME = "controller";
		private static final String ACTION_ATTR_NAME = "action";
		
		public ElementImplForJSP node; //TODO: shouldn't be public. Instead provide some nicer abstract methods to work with the node.
		private IFile file; // The GSP file that contains this tag
	
		public GLinkTag(IFile file, Node item) {
			this.file = file;
			this.node = (ElementImplForJSP) item;
		}
	
		public String getActionName() {
			return getAttribute(ACTION_ATTR_NAME);
		}
		
		public String getControllerName() {
			String name = getAttribute(CONTROLLER_ATTR_NAME);
			if (name==null) {
				IPath path = file.getProjectRelativePath();
				//Expected path: "grails-app/views/<controller-name>/<view-name>.gsp
				if (path.segmentCount()==4) {
					if (path.segment(0).equals("grails-app")) {
						if (path.segment(1).equals("views")) {
							name = path.segment(2);
						}
					}
				}
			}
			return name;
		}
		
		/**
		 * Creates a search match object representing the text range where the action name is shown.
		 * May return null if the action name can not be found verbatim in the g:link action attribute
		 * attribute.
		 */
		public Match createActionMatch(String expectedValue) {
			return createAttributeMatch(ACTION_ATTR_NAME, expectedValue);
		}
		
		/**
		 * Creates a search match object representing the text range where the controller name is shown.
		 * May return null if the name can not be found verbatim in the g:link controller attribute.
		 * This may happen, for example, if the controller attribute is not explicitly specified in the g:link 
		 * (so the controller is taken from the context).
		 */
		public Match createControllerMatch(String expectedValue) {
			return createAttributeMatch(CONTROLLER_ATTR_NAME, expectedValue);
		}
		
		/**
		 * Creates a match that indicates the region of text corresponding to a specific attribute value.
		 * <p>
		 * This may return null if the expectedValue text can not be found verbatim in the node attribute
		 * text. This may happen if the attribuet's value was derived somehow instead of explicitly present
		 * in the node.
		 */
		private Match createAttributeMatch(String nodeAttributeName, String expectedValue) {
			Match match = null;
			try {
				AttrImpl attr = (AttrImpl) node.getAttributeNode(nodeAttributeName);
				if (attr!=null) {
					ITextRegion valueRegion = attr.getValueRegion();
					if (valueRegion!=null) {
						int start = attr.getValueRegionStartOffset(); //IMPORTANT: don't use valueRegion.getStart because that is wrong! (not relative to document!)
						String docValueText = node.getStructuredDocument().get(start, valueRegion.getLength());
						int relOffset = docValueText.indexOf(expectedValue);
						if (relOffset>=0) {
							match = createMatch(start+relOffset, expectedValue.length());
						}
					}
				}
			} catch (BadLocationException e) {
				GrailsCoreActivator.log(e);
			}
			return match;
		}
		
	
		private String getAttribute(String name) {
			String value = node.getAttribute(name);
			if (value.equals("")) {
				return null;
			}
			return value;
		}
	
		@Override
		public String toString() {
			return node.toString();
		}
	
	}

	public GSPSearcher(IFile gspFile) {
		super(gspFile);
	}
	
	private Match createMatch(int offset, int length) {
		return new Match(file, offset, length);
	}
	
	/**
	 * Compute changes needed to replaces all references to a given action (identified by a controller and action name) inside a given gsp file. 
	 * The changes are added to the given TextChange object.
	 */
	public void perform() throws IOException, CoreException {
		GSPStructuredModel model = (GSPStructuredModel) StructuredModelManager.getModelManager().getModelForRead(file);
		try {
			IDOMDocument document = model.getDocument();
			NodeList links = document.getElementsByTagName("g:link");
			for (int i = 0; i < links.getLength(); i++) {
				GSPSearcher.GLinkTag link = new GSPSearcher.GLinkTag(file, links.item(i));
				visit(link);
			}
		} finally {
			model.releaseFromRead();
		}
	}
}
