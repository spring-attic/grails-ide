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

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author Nieraj Singh
 */
public class GrailsPluginParser implements IPluginParser {

	private String pluginDescriptor;

	public GrailsPluginParser(String pluginDescriptor) {
		this.pluginDescriptor = pluginDescriptor;
	}

	public GrailsPluginVersion parse() {
		if (pluginDescriptor == null) {
			return null;
		}

		File file = new File(pluginDescriptor);

		GrailsPluginVersion data = null;
		if (file.exists()) {
			data = new PluginDescriptorParser(pluginDescriptor).parse();
		} else if (Path.EMPTY.isValidPath(pluginDescriptor)) {

			IPath path = new Path(pluginDescriptor);

			String lastSegment = path.lastSegment();
			// plugin.xml file doesn't exist, so strip it
			if (lastSegment != null && lastSegment.contains("plugin.xml")) {
				path = path.removeLastSegments(1);
			}
			data = new GroovyPluginParser(path).parse();
		}

		return data;
	}

}
