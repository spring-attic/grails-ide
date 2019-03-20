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

public enum GrailsProjectStructureTypes {

	CONF("conf", "grails-app/conf"), 
	DOMAIN("domain", "grails-app/domain"), 
	CONTROLLERS("controllers", "grails-app/controllers"), 
	TAGLIB("taglib","grails-app/taglib"), 
	SERVICES("services", "grails-app/services"), 
	VIEWS("views", "grails-app/views"), 
	ASSETS("assets", "grails-app/assets"),
	I18N("i18n", "grails-app/i18n"), 
	UTILS("utils", "grails-app/utils"),
	
	SCRIPTS("scripts", "scripts"), 
	TEST_REPORTS("test reports","target/test-reports"), 
	PLUGINS("plugins", "plugins"), 
	DEPENDENCY_PLUGIN("dependency plugin", "dependencyplugin"),
	CLASSPATH_CONTAINERS("classpath", "classpath");

	private String displayName;
	private String folderName;

	private GrailsProjectStructureTypes(String displayName, String folderName) {
		this.displayName = displayName;
		this.folderName = folderName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFolderName() {
		return folderName;
	}

}
