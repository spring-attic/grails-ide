/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.translation;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslationAdapter;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslationAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @created Nov 6, 2009
 */
public class GSPTranslationAdapterFactory extends JSPTranslationAdapterFactory {

    // for debugging
    static final boolean DEBUG = Boolean.valueOf(Platform.getDebugOption("org.eclipse.jst.jsp.core/debug/jsptranslation")).booleanValue(); //$NON-NLS-1$;

    protected INodeAdapter createAdapter(INodeNotifier target) {
        JSPTranslationAdapter fAdapter = (JSPTranslationAdapter) ReflectionUtils.getPrivateField(JSPTranslationAdapterFactory.class, "fAdapter", this);
        if (target instanceof IDOMNode && fAdapter == null) {
            // use GSPTranslationAdapter instead of JSPTranslationAdapter
            fAdapter = new GSPTranslationAdapter(((IDOMNode) target).getModel());
            ReflectionUtils.setPrivateField(JSPTranslationAdapterFactory.class, "fAdapter", this, fAdapter);
            if(DEBUG) {
                System.out.println("(+) JSPTranslationAdapterFactory [" + this + "] created adapter: " + fAdapter); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return fAdapter;
    }

    
}
