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
package org.grails.ide.eclipse.editor.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;

/**
 * Based on org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlink
 */
public class JavaElementHyperlink implements IHyperlink {

    private final IJavaElement fElement;
    private final IRegion fRegion;

    /**
     * Creates a new Java element hyperlink.
     * 
     * @param region
     *            the region of the link
     * @param element
     *            the java element to open
     * @param qualify
     *            <code>true</code> if the hyper-link text should show a
     *            qualified name for element.
     */
    public JavaElementHyperlink(IRegion region, IJavaElement element) {
        fRegion = region;
        fElement = element;
    }

    public IRegion getHyperlinkRegion() {
        return fRegion;
    }

    public String getHyperlinkText() {
        String elementLabel = JavaElementLabels.getElementLabel(fElement, JavaElementLabels.ALL_POST_QUALIFIED);
        return "Open " + elementLabel;
    }

    public String getTypeLabel() {
        return null;
    }

    public void open() {
        try {
            JavaUI.openInEditor(fElement);
        }
        catch (PartInitException e) {
        }
        catch (JavaModelException e) {
        }
    }
    
    public IJavaElement getElement() {
        return fElement;
    }
}