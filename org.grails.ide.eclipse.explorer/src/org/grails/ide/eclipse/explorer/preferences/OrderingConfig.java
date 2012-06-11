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
package org.grails.ide.eclipse.explorer.preferences;

import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.CLASSPATH_CONTAINERS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.CONF;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.CONTROLLERS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.DOMAIN;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.I18N;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.PLUGINS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.SCRIPTS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.SERVICES;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.TAGLIB;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.TEST_REPORTS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.UTILS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes.VIEWS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.grails.ide.eclipse.core.util.ArrayEncoder;

import org.grails.ide.eclipse.explorer.elements.ILogicalFolder;
import org.grails.ide.eclipse.explorer.providers.GrailsCommonNavigatorViewerSorter;

/**
 * An instance of this class represents info that configures the {@link GrailsCommonNavigatorViewerSorter}.
 * <p>
 * Essentially it is a list of categories. 
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class OrderingConfig {

	private static void debug(String string) {
//		System.out.println(string);
	}
	
	/**
	 * An array containing both Strings and GrailsProjectStructureTypes. String are used to determine sorting 
	 * order of specific packageFragment root (like src/java, src/groovy) or IFile or IFolder elements for
	 * which there is no corresponding element in {@link GrailsProjectStructureTypes}.
	 */
	private Object[] categories;
	
	/**
	 * Maps a 'sort key' to a category used for sorting. Lower number means it show higher-up in the view.
	 */
	private Map<String, Integer> sortKeyMap = null;

	private int defaultSortCat; //larger than any of the "at the top" items cat. 
	
	private OrderingConfig(Object... atTheTop) {
		this.categories = atTheTop;
	}

	private void lazyInit() {
		if (sortKeyMap==null) {
			defaultSortCat = categories.length;
			sortKeyMap = new HashMap<String, Integer>(categories.length);
			for (int i = 0; i < categories.length; i++) {
				sortKeyMap.put(configElementToKey(categories[i]), i);
			}
		}
	}
	
	/**
	 * This array defines the default sort order for all the "special" elements in the viewer. The special elements
	 * should always come before "the rest of them". The rest of them are sorted by a basic Java element
	 * sorter. 
	 */
	public static final OrderingConfig DEFAULT = new OrderingConfig(
		DOMAIN,
		CONTROLLERS,
		VIEWS,
		TAGLIB,
		SERVICES,
		UTILS,
		SCRIPTS,
		I18N,
		CONF,
		
		"src/java",
		"src/groovy",
		"test/unit",
		"test/integration",
		TEST_REPORTS, 
		
		PLUGINS, 
		CLASSPATH_CONTAINERS,
		"application.properties"
		
		//Then the rest of them in no particular order
	);

	public int size() {
		return categories.length;
	}

	public Object get(int i) {
		return categories[i];
	}

	/**
	 * Passed to the list viewer in the UI.
	 */
	public List<Object> asList() {
		return Arrays.asList(categories);
	}

	/**
	 * Convert back from a list from the viewer in the UI.
	 */
	public static OrderingConfig fromList(List<Object> elements) {
		return new OrderingConfig(elements.toArray());
	}
	
	/**
	 * Converts an element found in the viewer to a key that can be looked up in sortKeyMap
	 */
	private static String viewerElementToKey(Object viewerElement) {
		IResource rsrc = toResource(viewerElement);
		if (rsrc!=null) {
			IPath path = rsrc.getProjectRelativePath();
			return path.toString();
		} else if (viewerElement instanceof ILogicalFolder) {
			return ((ILogicalFolder) viewerElement).getType().getFolderName();
		}
		return null;
	}

	private static IResource toResource(Object object) {
		if (object instanceof IResource) {
			return (IResource) object;
		} else if (object instanceof IPackageFragmentRoot) {
			try {
				return ((IPackageFragmentRoot) object).getCorrespondingResource();
			} catch (JavaModelException e) {
				GrailsCoreActivator.log(e);
			}
		}
		return null;
	}

	/**
	 * Convert a config element object into a String key (that can
	 * be derived also from viewer elements) and looked up in sortKeyMap.
	 */
	private static String configElementToKey(Object o) {
		if (o instanceof String) {
			return (String)o;
		} else if (o instanceof GrailsProjectStructureTypes) {
			return ((GrailsProjectStructureTypes) o).getFolderName();
		}
		throw new IllegalArgumentException("The sort config aray must only contain Strings (project relative resource path)" +
				" or GrailsProjectStructureTypes");
	}

	/**
	 * Given a viewer element determine its 'sort category' as specified by this configuration.
	 */
	public int getSortCat(Object viewerElement) {
		lazyInit();
//		debug(">>> getSortCat "+object);
		String key = viewerElementToKey(viewerElement);
		if (key!=null) {
			Integer cat = sortKeyMap.get(key);
			if (cat != null) {
				debug("<<< getSortCat "+cat);
				return cat;
			}
		}
//		debug("<<< getSortCat "+DEFAULT_SORT_CAT);
		return defaultSortCat; //is larger than any other one in the map!
	}
	
	/**
	 * Encodes this config into a single String that can be stored into an eclipse preferences store, File etc.
	 */
	public String toSaveString() {
		String[] encodedStrs = new String[categories.length];
		for (int i = 0; i < encodedStrs.length; i++) {
			encodedStrs[i] = toSaveString(categories[i]);
		}
		return ArrayEncoder.encode(encodedStrs);
	}

	private String toSaveString(Object configElement) {
		if (configElement instanceof String) {
			return "S"+configElement;
		} else if (configElement instanceof GrailsProjectStructureTypes) {
			return "E"+configElement;
		} else {
			throw new IllegalArgumentException("OrderingConfig elements should only be String or GrailsProjectStructureTypes");
		}
	}
	
	public static OrderingConfig fromSaveString(String encoded) {
		try {
			String[] encodedStrs = ArrayEncoder.decode(encoded);
			Object[] configElements = new Object[encodedStrs.length];
			for (int i = 0; i < configElements.length; i++) {
				configElements[i] = elementFromSaveString(encodedStrs[i]);
			}
			return new OrderingConfig(configElements);
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
			return DEFAULT;
		}
	}

	private static Object elementFromSaveString(String encoded) throws DecodeException {
		if (encoded.length()>0) {
			char kind = encoded.charAt(0);
			switch (kind) {
			case 'E':
				return GrailsProjectStructureTypes.valueOf(encoded.substring(1));
			case 'S':
				return encoded.substring(1);
			default:
			}
		} 
		throw new DecodeException("Can't decode ordering config element from: "+encoded);
	}

	private static class DecodeException extends Exception {
		private static final long serialVersionUID = 1L;
		public DecodeException(String string) {
			super(string);
		}
	}

}
