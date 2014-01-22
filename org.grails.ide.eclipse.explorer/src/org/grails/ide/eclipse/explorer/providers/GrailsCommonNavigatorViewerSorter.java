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
package org.grails.ide.eclipse.explorer.providers;


import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.grails.ide.eclipse.explorer.GrailsExplorerPlugin;
import org.grails.ide.eclipse.explorer.preferences.GrailsExplorerPreferences;
import org.grails.ide.eclipse.explorer.preferences.OrderingConfig;

/**
 * @author Kris De Volder
 */
public class GrailsCommonNavigatorViewerSorter extends ViewerSorter {
	
	JavaElementComparator javaComparator = new JavaElementComparator();	

	public GrailsCommonNavigatorViewerSorter() {
	}
	
	GrailsExplorerPreferences prefs;
	
	private OrderingConfig getOrderingConfig() { 
		if (prefs==null) {
			prefs = GrailsExplorerPlugin.getDefault().getPreferences();
		}
		return prefs.getOrderingConfig();
	}
	
	@Override
	public synchronized int compare(Viewer viewer, Object e1, Object e2) {
		OrderingConfig orderingConfig = getOrderingConfig();
		int sortCat1 = orderingConfig.getSortCat(e1);
		int sortCat2 = orderingConfig.getSortCat(e2);
		if (sortCat1>sortCat2) 
			return +1;
		else if (sortCat2>sortCat1) {
			return -1;
		} else {
			//This can only be reached if cat1 and cat2 are the same. We sort 
			//the elements within the category using a default java sorter.
			return javaComparator.compare(viewer, e1, e2);
		}
	}
}
