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
package org.grails.ide.eclipse.core.internal.classpath;

import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.Plugin;

/**
 * Represents a published Grails plugin model, which among other things,
 * contains a list of published versions.
 * <p>
 * It specifies the latest version of the plugin as it would be installed IF the
 * plugin were installed without specifying a version number. NOTE that this is
 * NOT the same as being the most recent version of the plugin that is
 * available. There may be newer milestone versions available OR newer versions
 * that are only compatible with newer Grails. Therefore this model provides two
 * API for retrieving new versions:
 * <li>
 * The latest version available for the given version of Grails used by a
 * project. This is the version that is installed if a user installs the plugin
 * WITHOUT specifying a version number.</li>
 * <li>
 * The most recent version added, which may be a milestone or only available for
 * newer versions of Grails, which a user must manually specify when installing
 * the plugin. As the list of versions are ORDERED, the most recent version
 * added version should be the last version that was added to the list.</li>
 * </p>
 * <p>
 * This model represents both a published plugin as well as an in-place plugin.
 * In-place plugins usually only have one child version (i.e the only version
 * that is available), and this version is considered the latest version.
 * </p>
 * 
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class GrailsPlugin extends Plugin {

	public GrailsPlugin(String name) {
		super(name);
	}

}
