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
package org.grails.ide.eclipse.editor.groovy.elements;

import org.eclipse.core.resources.IResource;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;


/**
 * An generic element in a grails project.  It does not necessarily map to
 * an IJavaElement
 * @author Andrew Eisenberg
 * @created Dec 3, 2010
 */
public interface IGrailsArtifact {
    GrailsElementKind getKind();

    IResource getResource();
}
