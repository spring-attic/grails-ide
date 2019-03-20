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
package org.grails.ide.eclipse.editor.groovy.controllers;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import org.grails.ide.eclipse.editor.groovy.EditorPluginImageHelper;
import org.grails.ide.eclipse.editor.groovy.EditorPluginImages;

/**
 * 
 * @author Andrew Eisenberg
 * @created Jul 14, 2011
 */
public class ActionTarget implements ITarget {
    
    private final ControllerTarget controllerTarget;
    private final String actionName;
    private final boolean isMethod;


    public ActionTarget(ControllerTarget controllerTarget, String actionName, boolean isMethod) {
        this.controllerTarget = controllerTarget;
        this.actionName = actionName;
        this.isMethod = isMethod;
    }

    public String getName() {
        return actionName;
    }
    
    public String getDisplayString() {
        return actionName + " (action in " + controllerTarget.getName() + " controller)";
    }

    public IContextInformation toContextInformation() {
        return new ContextInformation(getImage(),
                getName(), getDisplayString());
    }

    public Image getImage() {
        return EditorPluginImageHelper.getInstance().getImage(
                EditorPluginImages.IMG_VIEWS);
    }

    public IJavaElement toJavaElement() throws JavaModelException {
        if (isMethod) {
            // grails 2.0+
            IType controllerType = controllerTarget.toJavaElement();
            for (IMethod method : controllerType.getMethods()) {
                // assume there is no overloading of actions.
                if (method.getElementName().equals(actionName)) {
                    return method;
                }
            }
            // just return the type
            return controllerType;
        } else {
            return controllerTarget.toJavaElement().getField(actionName);
        }
    }

}
