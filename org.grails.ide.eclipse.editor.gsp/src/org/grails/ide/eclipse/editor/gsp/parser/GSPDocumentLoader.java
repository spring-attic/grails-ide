/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.parser;

import org.eclipse.jst.jsp.core.internal.encoding.JSPDocumentLoader;
import org.eclipse.jst.jsp.core.internal.provisional.JSP11Namespace;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;

/**
 * @author Andrew Eisenberg
 * @created Dec 4, 2009
 */
public class GSPDocumentLoader extends JSPDocumentLoader {

    /**
     * override from super class to get GSPSourceParser in there
     */
    @Override
    public RegionParser getParser() {
        GSPSourceParser parser = new GSPSourceParser();
        // add default nestable tag list
        addNestablePrefix(parser, JSP11Namespace.JSP_TAG_PREFIX);
        return parser;
    }
}
