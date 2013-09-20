/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.explorer.types;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.icons.IIcon;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.icons.Icon;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.icons.IconManager;


/**
 * Manages Grails container types, including determining which container types
 * are logical top-level folders as well as generates icons for all logical
 * folders.
 * @author Nieraj Singh
 * @author Andy Clement
 */
public class GrailsContainerTypeManager extends IconManager {

	private static GrailsContainerTypeManager instance;

	public static final int OVERLAYED_WIDTH = 16;
	public static final int OVERLAYED_HEIGHT = 16;

	private GrailsContainerTypeManager() {
		//
	}

	public static GrailsContainerTypeManager getInstance() {
		if (instance == null) {
			instance = new GrailsContainerTypeManager();
		}
		return instance;
	}

	public static final IIcon LOCAL_PLUGIN_OVERLAY = new Icon(
			"platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/plugins_sub_local_ovr.gif");



	public Image getIcon(GrailsProjectStructureTypes type) {
		GrailsContainerType containerType = GrailsContainerType.valueOf(type.toString());
		return super.getIcon(containerType);
	}

	public Image getOverlayedImage(GrailsProjectStructureTypes type, IIcon overlay) {

		Image typeImage = getIcon(type);
		if (overlay == null) {
			return typeImage;
		}

		Image overlayImage = getIcon(overlay);

		if (overlayImage != null && typeImage != null) {
			ImageDescriptor overlayDescriptor = new OverlayedIconDescriptor(
					typeImage, overlayImage);
			return overlayDescriptor.createImage();
		}

		return typeImage;

	}



	static class OverlayedIconDescriptor extends CompositeImageDescriptor {

		private Image base;
		private Image overlay;

		public OverlayedIconDescriptor(Image base, Image overlay) {
			this.base = base;
			this.overlay = overlay;
		}

		protected void drawCompositeImage(int width, int height) {
			if (base == null || overlay == null) {
				return;
			}
			ImageData baseData = base.getImageData();
			if (baseData != null) {
				drawImage(baseData, 0, 0);
			}

			// draw the overlay image
			ImageData overlayData = overlay.getImageData();
			if (overlayData != null) {
				Point compositeSize = getSize();
				int xPos = overlayData.width < compositeSize.x ? overlayData.width
						: 0;
				int yPos = overlayData.height < compositeSize.y ? overlayData.height
						: 0;
				drawImage(overlayData, xPos, yPos);
			}

		}

		protected Point getSize() {
			return new Point(OVERLAYED_WIDTH, OVERLAYED_HEIGHT);
		}

	}

}
