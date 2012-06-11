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
package org.grails.ide.eclipse.editor.gsp.adapter;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.codehaus.jdt.groovy.model.ICodeCompletionDelegate;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 */
@SuppressWarnings("rawtypes")
public class CodeCompleteAdapterFactory implements IAdapterFactory {

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ICodeCompletionDelegate.class && adaptableObject instanceof GroovyCompilationUnit) {
            return new CodeCompletionDelegate();
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { GroovyCompilationUnit.class };
    }

}
