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
package org.grails.ide.eclipse.editor.groovy.controllers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import org.grails.ide.eclipse.editor.groovy.EditorPluginImageHelper;
import org.grails.ide.eclipse.editor.groovy.EditorPluginImages;

/**
 * A hyperlink and completion target for controllers
 * @author Andrew Eisenberg
 * @created Jul 12, 2011
 */
public class ControllerTarget implements ITarget {

    private final String controllerQualifiedName;
    
    // initially null...calculated later if required
    private IType controllerType;

    private final String controllerName;

    private final IJavaProject project;

    public ControllerTarget(String controllerQualifiedName, String controllerName, IJavaProject project) {
        this.controllerQualifiedName = controllerQualifiedName;
        this.controllerName = controllerName;
        this.project = project;
    }

    public Image getImage() {
        return EditorPluginImageHelper.getInstance().getImage(
                EditorPluginImages.IMG_CONTROLLERS);
    }

    public IType toJavaElement() throws JavaModelException {
        if (controllerType == null) {
            controllerType = project.findType(controllerQualifiedName, (IProgressMonitor) null);
        }
        return controllerType;
    }

    public String getName() {
        return controllerName;
    }

    public String getDisplayString() {
        return controllerName + " (controller)";
    }

    public IContextInformation toContextInformation() {
        return new ContextInformation(getImage(),
                getName(), getDisplayString());
    }
}
