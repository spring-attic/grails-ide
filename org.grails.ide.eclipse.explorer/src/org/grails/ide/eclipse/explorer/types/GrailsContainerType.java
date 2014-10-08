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
package org.grails.ide.eclipse.explorer.types;

import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.icons.IIcon;


/**
 * Represents a Grails folder type as seen in the Package Explorer. It also
 * includes logical folder types that have no physical folder counterparts, like
 * the top-level "Plugins" logical folder.
 * 
 * @author nisingh
 * 
 */
public enum GrailsContainerType implements IIcon {

	CONF(GrailsProjectStructureTypes.CONF,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/config.gif"), 
	DOMAIN(GrailsProjectStructureTypes.DOMAIN,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/domains.gif"), 
	CONTROLLERS(GrailsProjectStructureTypes.CONTROLLERS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/controllers.gif"), 
	TAGLIB(GrailsProjectStructureTypes.TAGLIB,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/taglib.gif"), 
	SERVICES(GrailsProjectStructureTypes.SERVICES,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/services.gif"), 
	VIEWS(GrailsProjectStructureTypes.VIEWS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/views.gif"), 
	ASSETS(GrailsProjectStructureTypes.ASSETS,
					"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/assets.png"), 
	I18N(GrailsProjectStructureTypes.I18N,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/i18n.gif"), 
	UTILS(GrailsProjectStructureTypes.UTILS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/utils.gif"), 
	SCRIPTS(GrailsProjectStructureTypes.SCRIPTS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/scripts.gif"), 
	TEST_REPORTS(GrailsProjectStructureTypes.TEST_REPORTS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/test_reports.gif"), 
	PLUGINS(GrailsProjectStructureTypes.PLUGINS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/plugins.gif"), 
	DEPENDENCY_PLUGIN(
			GrailsProjectStructureTypes.DEPENDENCY_PLUGIN,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/plugins_sub.gif"),
	CLASSPATH_CONTAINERS(GrailsProjectStructureTypes.CLASSPATH_CONTAINERS,
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/classpath_folder.gif");

	public GrailsProjectStructureTypes getStructureType() {
		return structureType;
	}

	public String getIconLocation() {
		return iconLocation;
	}

	private GrailsProjectStructureTypes structureType;
	private String iconLocation;

	private GrailsContainerType(GrailsProjectStructureTypes structureType,
			String iconLocation) {
		this.structureType = structureType;
		this.iconLocation = iconLocation;
	}

}
