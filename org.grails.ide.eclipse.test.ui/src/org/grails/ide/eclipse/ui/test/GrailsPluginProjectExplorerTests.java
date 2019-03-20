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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

/**
 * @author Nieraj Singh
 * @created Jul 15, 2010
 */
public class GrailsPluginProjectExplorerTests extends GrailsExplorerTests {

	protected SWTBotShell activateGrailsProjectWizardShell() {
		return activateFileNewWizardShell("Grails Plugin Project",
				"New Grails Plugin Project");
	}

	protected List<GrailsExplorerElementMatcher> getExpectedProjectExplorerElementsInOrder() {
		List<GrailsExplorerElementMatcher> list = super
				.getExpectedProjectExplorerElementsInOrder();
		list.add(new GrailsExplorerElementMatcher("GrailsPlugin.groovy", false,
				IFile.class) {

			public boolean matchesLabel(String toCompare) {
				return toCompare.contains(getLabel());
			}

		});

		return list;
	}
}
