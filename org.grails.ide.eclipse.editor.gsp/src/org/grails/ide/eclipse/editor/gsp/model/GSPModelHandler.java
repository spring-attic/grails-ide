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
package org.grails.ide.eclipse.editor.gsp.model;

import org.eclipse.jst.jsp.core.internal.modelhandler.ModelHandlerForJSP;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;

import org.grails.ide.eclipse.editor.gsp.parser.GSPDocumentLoader;

/**
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @created Nov 8, 2009
 */
public class GSPModelHandler extends ModelHandlerForJSP {
    public IModelLoader getModelLoader() {
        return new GSPModelLoader();
    }
    
    public IDocumentLoader getDocumentLoader() {
        return new GSPDocumentLoader();
    }

}
