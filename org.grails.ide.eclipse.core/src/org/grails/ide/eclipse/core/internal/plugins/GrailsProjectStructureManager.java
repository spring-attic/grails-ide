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
package org.grails.ide.eclipse.core.internal.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Nieraj Singh
 */
public class GrailsProjectStructureManager {
	
	private static GrailsProjectStructureManager instance;
	
	private Set<String> allGrailsFolderNames;
	private Map<String, GrailsProjectStructureTypes> allTopLevelTypes;
	
	
	public static GrailsProjectStructureManager getInstance() {
		if (instance == null) {
			instance = new GrailsProjectStructureManager();
		}
		return instance;
	}
	
	public Set<String> getAllGrailsFolderNames() {
		if (allGrailsFolderNames == null) {
			allGrailsFolderNames = new HashSet<String>();
			GrailsProjectStructureTypes[] values = GrailsProjectStructureTypes.values();
			for (GrailsProjectStructureTypes value : values) {
				allGrailsFolderNames.add(value.getFolderName());
			}

			allGrailsFolderNames = Collections
					.unmodifiableSet(allGrailsFolderNames);
		}
		return allGrailsFolderNames;
	}

	public Set<GrailsProjectStructureTypes> getGrailsSourceFolders() {
		Set<GrailsProjectStructureTypes> fileFolders = new LinkedHashSet<GrailsProjectStructureTypes>();
		fileFolders.add(GrailsProjectStructureTypes.CONF);
		fileFolders.add(GrailsProjectStructureTypes.DOMAIN);
		fileFolders.add(GrailsProjectStructureTypes.UTILS);
		fileFolders.add(GrailsProjectStructureTypes.CONTROLLERS);
		fileFolders.add(GrailsProjectStructureTypes.TAGLIB);
		fileFolders.add(GrailsProjectStructureTypes.SERVICES);
		return fileFolders;
	}

	public Set<GrailsProjectStructureTypes> getGrailsFileFolders() {
		Set<GrailsProjectStructureTypes> fileFolders = new LinkedHashSet<GrailsProjectStructureTypes>();
		fileFolders.add(GrailsProjectStructureTypes.I18N);
		fileFolders.add(GrailsProjectStructureTypes.SCRIPTS);
		fileFolders.add(GrailsProjectStructureTypes.VIEWS);
		fileFolders.add(GrailsProjectStructureTypes.ASSETS);
		fileFolders.add(GrailsProjectStructureTypes.TEST_REPORTS);
		return fileFolders;
	}

	public Map<String, GrailsProjectStructureTypes> getAllTopLevelLogicalFolders() {
		if (allTopLevelTypes == null) {
			allTopLevelTypes = new HashMap<String, GrailsProjectStructureTypes>();
			GrailsProjectStructureTypes[] values = GrailsProjectStructureTypes.values();
			for (GrailsProjectStructureTypes type : values) {
				if (!getGrailsSourceFolders().contains(type)
						&& type != GrailsProjectStructureTypes.DEPENDENCY_PLUGIN) {
					allTopLevelTypes.put(type.getFolderName(), type);
				}
			}
			allTopLevelTypes = Collections.unmodifiableMap(allTopLevelTypes);
		}
		return allTopLevelTypes;
	}

}
