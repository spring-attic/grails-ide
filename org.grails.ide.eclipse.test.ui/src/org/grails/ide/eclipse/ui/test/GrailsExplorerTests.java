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
package org.grails.ide.eclipse.ui.test;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.eclipse.dsl.DSLPreferences;
import org.codehaus.groovy.eclipse.dsl.GroovyDSLCoreActivator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotConditions;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;

import org.grails.ide.eclipse.explorer.elements.GrailsClasspathContainersFolder;
import org.grails.ide.eclipse.explorer.elements.GrailsPluginFolder;
import org.grails.ide.eclipse.explorer.types.GrailsContainerType;
import org.grails.ide.eclipse.explorer.types.GrailsContainerTypeManager;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @created Jul 8, 2010
 */
public class GrailsExplorerTests extends GrailsProjectHarness {

	@Override
	public void setupClass() throws Exception {
		// turn off Greclipse AUTO_ADD_DSL_SUPPORT: it breaks this test by
		// adding another node to the view, at a time which is unpredictable
		// (background job running
		// concurrently with this test).
		IPreferenceStore groovyPrefs = GroovyDSLCoreActivator.getDefault().getPreferenceStore();
		groovyPrefs.setValue(DSLPreferences.AUTO_ADD_DSL_SUPPORT, false);
		super.setupClass();
	}

	/**
	 * The expected content of a grails Project, listed in the order that they
	 * must appear
	 */
	private static final GrailsExplorerElementMatcher[] GRAILS_EXPLORER_CONTENT = new GrailsExplorerElementMatcher[] {
			getGrailsElementMatcher(GrailsContainerType.DOMAIN, true, IPackageFragmentRoot.class),
			getGrailsElementMatcher(GrailsContainerType.CONTROLLERS, true, IPackageFragmentRoot.class),
			getGrailsElementMatcher(GrailsContainerType.VIEWS, true, IFolder.class),
			getGrailsElementMatcher(GrailsContainerType.TAGLIB, true, IPackageFragmentRoot.class),
			getGrailsElementMatcher(GrailsContainerType.SERVICES, true, IPackageFragmentRoot.class),
			getGrailsElementMatcher(GrailsContainerType.UTILS, true, IPackageFragmentRoot.class),
			getGrailsElementMatcher(GrailsContainerType.SCRIPTS, true, IFolder.class),
			getGrailsElementMatcher(GrailsContainerType.I18N, true, IFolder.class),
			getGrailsElementMatcher(GrailsContainerType.CONF, true, IPackageFragmentRoot.class),
			
			getGrailsElementMatcher("src/java", true, IPackageFragmentRoot.class),
			getGrailsElementMatcher("src/groovy", true, IPackageFragmentRoot.class),
			getGrailsElementMatcher("test/unit", true, IPackageFragmentRoot.class),
			getGrailsElementMatcher("test/integration", true, IPackageFragmentRoot.class),

			getGrailsElementMatcher(GrailsContainerType.PLUGINS, true, GrailsPluginFolder.class),
			getGrailsElementMatcher(GrailsContainerType.CLASSPATH_CONTAINERS, true, GrailsClasspathContainersFolder.class),
			getGrailsElementMatcher("application.properties", true, IFile.class),
			
			// getGrailsElementMatcher("grails-app", true, IFolder.class),
			getGrailsElementMatcher("lib", true, IFolder.class),
			getGrailsElementMatcher("target", true, IFolder.class),
			getGrailsElementMatcher("web-app", true, IFolder.class),
	};

	protected List<GrailsExplorerElementMatcher> getExpectedProjectExplorerElementsInOrder() {

		List<GrailsExplorerElementMatcher> vals = new ArrayList<GrailsExplorerElementMatcher>(
				GRAILS_EXPLORER_CONTENT.length);

		for (GrailsExplorerElementMatcher label : GRAILS_EXPLORER_CONTENT) {
			vals.add(label);
		}
		return vals;
	}

	protected Image getGrailsTypeImage(final GrailsContainerType type) {
		final Image[] result = new Image[1];
		UIThreadRunnable.syncExec(bot.getDisplay(), new VoidResult() {
			public void run() {
				result[0] = GrailsContainerTypeManager.getInstance().getIcon(type);
			}
		});
		return result[0];
	}

	protected Image getActualImage(final SWTBotTreeItem item) {
		final Image[] result = new Image[1];
		UIThreadRunnable.syncExec(bot.getDisplay(), new VoidResult() {
			public void run() {
				result[0] = item.widget.getImage();
			}
		});
		return result[0];
	}

	protected void disposeImage(final Image image) {
		UIThreadRunnable.syncExec(bot.getDisplay(), new VoidResult() {
			public void run() {
				image.dispose();
			}
		});
	}

	protected boolean compareImages(Image image1, Image image2) {
		byte[] data1 = image1.getImageData().data;
		byte[] data2 = image2.getImageData().data;
		if (data1.length != data2.length) {
			return false;
		}

		for (int i = 0; i < data1.length; i++) {
			if (data1[i] != data2[i]) {
				return false;
			}
		}
		return true;
	}

	protected static class GrailsExplorerElementMatcher {
		private boolean matchExactly = false;

		private GrailsContainerType type;

		private final String label;

		private final Class<?> actualType;

		public Class<?> getActualType() {
			return actualType;
		}

		public GrailsExplorerElementMatcher(String label, boolean matchExactly, Class<?> actualType) {
			this.matchExactly = matchExactly;
			this.label = label;
			this.actualType = actualType;
		}

		public GrailsExplorerElementMatcher(GrailsContainerType type, boolean matchExactly, Class<?> actualType) {
			this(type.getStructureType().getDisplayName(), matchExactly, actualType);
			this.type = type;
		}

		public boolean matchesLabel(String toCompare) {
			if (matchExactly) {
				return label.equals(toCompare);
			}
			else {
				return toCompare.length() > label.length() ? toCompare.startsWith(label) : label.startsWith(toCompare);
			}
		}

		public String getLabel() {
			return label;
		}

		public boolean isGrailsReimaged() {
			return type != null;
		}

		public GrailsContainerType getGrailsType() {
			return type;
		}

	}

	@Override
	protected void waitForProjectBuild(String projectName) throws Exception {
		super.waitForProjectBuild(projectName);
		// In addition, wait for markers to disappear, for the icon tests
		SWTBotConditions.waitForProjectErrorMarkersCleared(bot, getTestProject().getName());
	}

	protected static GrailsExplorerElementMatcher getGrailsElementMatcher(String label, boolean matchExactly,
			Class<?> actualType) {
		return new GrailsExplorerElementMatcher(label, matchExactly, actualType);
	}

	protected static GrailsExplorerElementMatcher getGrailsElementMatcher(GrailsContainerType type,
			boolean matchExactly, Class<?> actualType) {
		return new GrailsExplorerElementMatcher(type, matchExactly, actualType);
	}

	//
	// ------------> TESTS <-------------
	//

	public void testProjectExplorerContentSize() throws Exception {
		SWTBotTreeItem[] actualProjectItems = getExpandedProjectTreeNode().getItems();

		List<GrailsExplorerElementMatcher> expected = getExpectedProjectExplorerElementsInOrder();
		assertEquals(expected.size(), actualProjectItems.length);
	}

	public void testProjectExplorerContentLabel() throws Exception {

		SWTBotTreeItem[] actualProjectItems = getExpandedProjectTreeNode().getItems();

		List<GrailsExplorerElementMatcher> expected = getExpectedProjectExplorerElementsInOrder();

		int i = 0;
		for (GrailsExplorerElementMatcher expectedElement : expected) {
			SWTBotTreeItem actualItem = actualProjectItems[i++];
			String actualLabel = actualItem.getText();
			boolean matches = expectedElement.matchesLabel(actualLabel);
			print(expectedElement.getLabel() + " MATCHES: " + matches + " " + actualLabel);
			assertTrue(expectedElement.getLabel() + " does NOT match: " + actualLabel, matches);

		}
	}

	public void testProjectExplorerContentIcon() throws Exception {

		SWTBotTreeItem[] actualProjectItems = getExpandedProjectTreeNode().getItems();

		List<GrailsExplorerElementMatcher> expected = getExpectedProjectExplorerElementsInOrder();

		int i = 0;
		for (GrailsExplorerElementMatcher expectedElement : expected) {
			SWTBotTreeItem actualItem = actualProjectItems[i++];
			String actualLabel = actualItem.getText();
			boolean matches = expectedElement.matchesLabel(actualLabel);
			print(expectedElement.getLabel() + " MATCHES: " + matches + " " + actualLabel);

			if (!matches) {
				SWTBotUtils.screenshot("icon_test_label_matching_" + expectedElement.getLabel());
			}
			assertTrue(matches);

			// Check the image for the Grails reimaged folders and logical
			// folders
			if (expectedElement.isGrailsReimaged()) {
				GrailsContainerType type = expectedElement.getGrailsType();
				Image expectedImage = getGrailsTypeImage(type);
				Image actualImage = getActualImage(actualItem);

				boolean areEqual = compareImages(expectedImage, actualImage);
				print("Is expected Grails folder icon equal to actual for: " + expectedElement.getLabel() + " --> "
						+ areEqual);
				if (!areEqual) {
					SWTBotUtils.screenshot("icon_test_icon_matching_" + expectedElement.getLabel());
				}
				assertTrue(areEqual);
				disposeImage(expectedImage);
			}
		}
	}

	public void testProjectExplorerContentType() throws Exception {
		SWTBotTreeItem[] actualProjectItems = getExpandedProjectTreeNode().getItems();
		List<GrailsExplorerElementMatcher> expected = getExpectedProjectExplorerElementsInOrder();
		int i = 0;
		for (SWTBotTreeItem item : actualProjectItems) {
			item = actualProjectItems[i];
			Object actualItem = getTreeItemObject(item);
			assertNotNull(actualItem);
			print("Expected type: " + expected.get(i).getActualType() + ". Actual type: " + actualItem.getClass());
			assertTrue(expected.get(i).getActualType().isInstance(actualItem));
			i++;
		}
	}

}
