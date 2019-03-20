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
package org.grails.ide.eclipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

/**
 * This code is copied from org.eclipse.jdt.internal.ui.navigator.JavaNavigatorRefactorActionProvider, with minor
 * modifications to hookup Grails aware refactorings from the project explorer.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
@SuppressWarnings("restriction")
public class GrailsRefactorActionProvider extends CommonActionProvider {

	private GrailsRefactorActionGroup fRefactorGroup;

	public void fillActionBars(IActionBars actionBars) {
		if (fRefactorGroup != null) {
			fRefactorGroup.fillActionBars(actionBars);
			fRefactorGroup.retargetFileMenuActions(actionBars);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		if (fRefactorGroup != null) {
			fRefactorGroup.fillContextMenu(menu);
		}
	}

	public void init(ICommonActionExtensionSite site) {
		ICommonViewerWorkbenchSite workbenchSite= null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite= (ICommonViewerWorkbenchSite) site.getViewSite();

		// we only initialize the refactor group when in a view part
		// (required for the constructor)
		if (workbenchSite != null) {
			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart= (IViewPart) workbenchSite.getPart();

				fRefactorGroup= new GrailsRefactorActionGroup(viewPart);
			}
		}
	}

	public void setContext(ActionContext context) {
		if (fRefactorGroup != null) {
			fRefactorGroup.setContext(context);
		}
	}

	/*
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 * @since 3.5
	 */
	public void dispose() {
		if (fRefactorGroup != null)
			fRefactorGroup.dispose();
		super.dispose();
	}

}
