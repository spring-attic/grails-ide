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
package org.grails.ide.eclipse.editor.groovy;

import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.eclipse.editor.highlighting.IHighlightingExtender;
import org.eclipse.jface.text.rules.IRule;

/**
 * @author Andrew Eisenberg
 */
public class GrailsSyntaxHighlighting implements IHighlightingExtender {

    public List<IRule> getAdditionalRules() {
        return null;
    }

    @SuppressWarnings("nls")
    public List<String> getAdditionalGJDKKeywords() {
        return Arrays.asList(
                // domain fields
                "constraints", "belongsTo", "hasMany", "nullable", "belongsTo", "mapping", "hasMany", "embedded", "transients", "id", "tablePerHierarchy", "version",
                // domain methods
                "list", "save", "delete", "get",
                // controller fields
                "log", "actionName", "actionUri", "controllerName", "controllerUri", "flash", "log", "params", "request", "response", "session", "servletContext",
                // controller methods
                "render", "redirect"
                );
        
    }

    public List<String> getAdditionalGroovyKeywords() {
        return null;
    }

}
