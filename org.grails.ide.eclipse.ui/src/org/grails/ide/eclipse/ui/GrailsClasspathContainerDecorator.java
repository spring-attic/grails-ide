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
package org.grails.ide.eclipse.ui;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;


/**
 * @author Christian Dupuis
 * @since 2.2.0
 */
@SuppressWarnings("restriction")
public class GrailsClasspathContainerDecorator extends LabelProvider implements ILightweightLabelDecorator {

	/**
	 * Decorates the given <code>element</code>.
	 */
	public void decorate(Object element, IDecoration decoration) {
		// decorate the class path container and add the originating target runtime
		if (element instanceof ClassPathContainer) {
			ClassPathContainer container = (ClassPathContainer) element;
			if (container.getClasspathEntry().getPath().equals(GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH)) {
				try {
					if (container.getJavaProject().getProject().isAccessible() && container.getJavaProject().isOpen()) {
						GrailsClasspathContainer cpContainer = (GrailsClasspathContainer) JavaCore
								.getClasspathContainer(GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH, container
										.getJavaProject());
						decoration.addSuffix(cpContainer.getDescriptionSuffix());
					}
				}
				catch (JavaModelException e) {
				}
			}
		}
	}

}
