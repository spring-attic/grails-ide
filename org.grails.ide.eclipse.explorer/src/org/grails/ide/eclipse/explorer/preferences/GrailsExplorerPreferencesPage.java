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
package org.grails.ide.eclipse.explorer.preferences;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;

import org.grails.ide.eclipse.explorer.GrailsExplorerPlugin;
import org.grails.ide.eclipse.explorer.internal.util.ImageManager;
import org.grails.ide.eclipse.explorer.internal.util.ImageUtils;
import org.grails.ide.eclipse.explorer.types.GrailsContainerTypeManager;

/**
 * Allows changing the sorting order in the GrailsProject explorer.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsExplorerPreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage {
	
	private static final ImageDescriptor PKG_FOLDER_IMG = ImageUtils.imageDescriptor("platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/packagefolder_obj.gif");
	private static final ImageDescriptor FILE_IMG = ImageUtils.imageDescriptor("platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/file_obj.gif");

	private static final String[] buttonLabels = {
		"Up",
		"Down"
	};

	
	public GrailsExplorerPreferencesPage() {
		super("Explorer Sorting Order", null);
	}
	
	private LabelProvider tableLabelProvider = new LabelProvider() {
		
		private ImageManager images = new ImageManager();
		
		@Override
		public String getText(Object element) {
			if (element instanceof GrailsProjectStructureTypes) {
				return ((GrailsProjectStructureTypes) element).getDisplayName();
			} else if (element instanceof String) {
				return (String)element;
			}
			return ""+element;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof GrailsProjectStructureTypes) {
				GrailsProjectStructureTypes type = (GrailsProjectStructureTypes)element;
				return GrailsContainerTypeManager.getInstance().getIcon(type);
			} else if ("application.properties".equals(element)) {
				return images.get(FILE_IMG);
			} else {
				return images.get(PKG_FOLDER_IMG);
			}
		}
		public void dispose() {
			if (images!=null) {
				images.dispose();
				images = null;
			}
		}
	};
	
	private ListDialogField list;

	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite _parent) {
		Composite mainComposite = new Composite(_parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(3, false);
		mainComposite.setLayout(layout);
		
//		GridDataFactory grabAll = GridDataFactory.fillDefaults().grab(true, true);
//		grabAll.applyTo(mainComposite);
		
		list = new ListDialogField(null, buttonLabels, tableLabelProvider);
		list.setUpButtonIndex(0);
		list.setDownButtonIndex(1);
		
		list.setLabelText("Order:");
		list.addElements(GrailsExplorerPlugin.getDefault().getPreferences().getOrderingConfig().asList());
		list.doFillIntoGrid(mainComposite, 3);
		
		//This must be placed *after* doFillIntoGrid
		list.getListControl(null).setToolTipText("Change the order in which elements appear in the Grails Project Explorer");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(list.getListControl(null));
//		grabAll.applyTo(list.getListControl(null));
		
		return mainComposite;
	}
	
	@Override
	public boolean performOk() {
		GrailsExplorerPreferences prefs = GrailsExplorerPlugin.getDefault().getPreferences();
		prefs.setOrderingConfig(getOrderingConfigInPage());
		return true;
	}

	private OrderingConfig getOrderingConfigInPage() {
		return OrderingConfig.fromList(list.getElements());
	}

	@Override
	protected void performDefaults() {
		setOrderingConfigInPage(OrderingConfig.DEFAULT);
	}

	private void setOrderingConfigInPage(OrderingConfig conf) {
		list.setElements(conf.asList());
	}
	
}
