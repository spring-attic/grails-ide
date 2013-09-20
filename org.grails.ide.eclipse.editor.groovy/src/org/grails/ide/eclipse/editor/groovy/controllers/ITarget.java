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
package org.grails.ide.eclipse.editor.groovy.controllers;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

/**
 * This class describes a target of either content assist
 * or of a hyperlink.
 * 
 * @author Andrew Eisenberg
 * @created Jul 12, 2011
 */
public interface ITarget {
    
//    ICompletionProposal toCompletionProposal(String prefix, int invocationOffset);
    
    IContextInformation toContextInformation();
    
    String getName();
    
    IJavaElement toJavaElement() throws JavaModelException;
    
    Image getImage();
    
    String getDisplayString();
}
