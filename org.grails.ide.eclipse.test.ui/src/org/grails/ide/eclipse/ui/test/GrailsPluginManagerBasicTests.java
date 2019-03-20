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
package org.grails.ide.eclipse.ui.test;

import java.util.Collection;
import java.util.List;

import org.grails.ide.eclipse.core.internal.classpath.GrailsPlugin;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginsListManager;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.plugins.PluginManagerDialog.PluginColumnType;

import org.grails.ide.eclipse.ui.internal.dialogues.GrailsPluginManagerDialogue;

/**
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @created Jul 21, 2010
 */
public class GrailsPluginManagerBasicTests extends GrailsPluginManagerHarness {

	public void testPluginManagerContent() throws Exception {

		print("Getting plugin list manager for: " + getTestProject().getName());
		GrailsPluginsListManager manager = GrailsPluginsListManager
				.getGrailsPluginsListManager(getTestProject());
		Collection<GrailsPlugin> expectedPluginList = manager.generateList(false);
		int treeContentSize = bot.tree().getAllItems().length;
		int expectedListSize = expectedPluginList.size();
		print("Manager content size: " + treeContentSize);
		print("Expected size:" + expectedListSize);
		assertEquals(treeContentSize, expectedListSize);

	}

	public void testPluginManagerColumns() throws Exception {

		PluginColumnType[] types = GrailsPluginManagerDialogue.PluginColumnType
				.values();
		assertEquals(types.length, bot.tree().columnCount());
		// check column order
		List<String> actualColumnNames = bot.tree().columns();
		for (PluginColumnType expectedColumn : types) {
			assertEquals(expectedColumn.getName(),
					actualColumnNames.get(expectedColumn.ordinal()));
		}
	}

}
