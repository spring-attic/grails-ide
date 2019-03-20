/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.testplugin.detailpane;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * A simple implementation of a custom detail pane, used by the test suite to
 * test the detail pane functionality.  Displays a colored label depending on
 * if the selected element is public/private/protected/other.  This detail
 * pane is intended to only be used for selections containing only java variables.
 * @author Andrew Eisenberg
 * @since 3.3
 * @see DetailPaneManagerTests
 * @see TestDetailPaneFactory
 * @see IDetailPane
 */
public class SimpleDetailPane implements IDetailPane {

	private Label theLabel;
	
	private Color colorPrivate;
	private Color colorProtected;
	private Color colorPublic;
	private Color colorOther;
	
	private static final int TYPE_PRIVATE = 1;
	private static final int TYPE_PROTECTED = 2;
	private static final int TYPE_PUBLIC = 3;
	private static final int TYPE_OTHER = 4;
	
	// Do not change these constants, they are compared against strings in DetailPaneManagerTests
	public static final String ID = "SimpleDetailPane";
	public static final String NAME = "Example Pane: Colorful Detail Pane";
	public static final String DESCRIPTION = "Example pane that displays a color for variables depending on their access level.";
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {
		theLabel = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		theLabel.setLayoutData(gd);
		return theLabel;
	}
	
	/**
	 * Returns the color to be used with the given type
	 * 
	 * @param typeId type of variable
	 * @return the color to use
	 */
	private Color getColor(int typeId){
		if (typeId == TYPE_PRIVATE){
			if (colorPrivate == null || colorPrivate.isDisposed()){
				colorPrivate = new Color(theLabel.getDisplay(),255,0,0);
			}
			return colorPrivate;
		}
		if (typeId == TYPE_PROTECTED){
			if (colorProtected == null || colorProtected.isDisposed()){
				colorProtected = new Color(theLabel.getDisplay(),0,0,255);
			}
			return colorProtected;
		}
		if (typeId == TYPE_PUBLIC){
			if (colorPublic == null || colorPublic.isDisposed()){
				colorPublic = new Color(theLabel.getDisplay(),0,255,0);
			}
			return colorPublic;
		}
		if (colorOther == null || colorOther.isDisposed()){
			colorOther = new Color(theLabel.getDisplay(),0,0,0);
		}
		return colorOther;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#display(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void display(IStructuredSelection selection) {
		if (selection == null || selection.size() == 0){
			theLabel.setBackground(theLabel.getParent().getBackground());
		} else {
			if (selection.getFirstElement() instanceof IJavaVariable){
				IJavaVariable var = (IJavaVariable)selection.getFirstElement();
				try{
					if (var.isPublic()){
						theLabel.setBackground(getColor(TYPE_PUBLIC));
						return;
					}
					if (var.isProtected()){
						theLabel.setBackground(getColor(TYPE_PROTECTED));
						return;
					}
					if (var.isPrivate()){
						theLabel.setBackground(getColor(TYPE_PRIVATE));
						return;
					}
					theLabel.setBackground(getColor(TYPE_OTHER));
					return;
					
				} catch (DebugException e){
					theLabel.setBackground(theLabel.getParent().getBackground());
					theLabel.setText(e.getMessage());
				}
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#dispose()
	 */
	public void dispose() {
		if (colorPrivate != null) colorPrivate.dispose();
		if (colorProtected != null) colorProtected.dispose();
		if (colorPublic != null) colorPublic.dispose();
		if (colorOther != null) colorOther.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#getDescription()
	 */
	public String getDescription() {
		return DESCRIPTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#getID()
	 */
	public String getID() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#getName()
	 */
	public String getName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#init(org.eclipse.ui.IWorkbenchPartSite)
	 */
	public void init(IWorkbenchPartSite partSite) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#setFocus()
	 */
	public boolean setFocus() {
		return false;
	}

}
