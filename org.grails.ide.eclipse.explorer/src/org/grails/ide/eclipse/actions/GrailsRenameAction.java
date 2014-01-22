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
package org.grails.ide.eclipse.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.RenameAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

import org.codehaus.groovy.eclipse.refactoring.actions.IRenameTarget;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsRenameAction extends RenameAction {

	public GrailsRenameAction(IWorkbenchSite site) {
		super(site);
		// TODO Auto-generated constructor stub
	}

	public GrailsRenameAction(JavaEditor editor) {
		super(editor);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run(IStructuredSelection selection) {
		IJavaElement jel = getJavaElement(selection);
		if (jel!=null) {
			IRenameTarget specialTarget = (IRenameTarget) jel.getAdapter(IRenameTarget.class);
			if (specialTarget!=null) {
				if (specialTarget.performRenameAction(getShell(), null, false)) {
					return;
				}
			}
		}
		super.run(selection);
	}

	private static IJavaElement getJavaElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (! (first instanceof IJavaElement))
			return null;
		return (IJavaElement)first;
	}
	
}
