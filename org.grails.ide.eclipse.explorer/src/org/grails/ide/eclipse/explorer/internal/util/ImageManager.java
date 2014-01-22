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
package org.grails.ide.eclipse.explorer.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * An instance of this class manages images created lazily from image descriptors. 
 * The client is required to call 'dispose' on this ImageManager to ensure
 * that the images are disposed when they are no longer needed.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ImageManager {

	private Map<ImageDescriptor, Image> images = null;
	
	/**
	 * Dispose all the images in this ImageManager (client code is expected to
	 * call this method when they are done using the images / manager.
	 */
	public void dispose() {
		if (images!=null) {
			for (Image img : images.values()) {
				img.dispose();
			}
			images = null;
		}
	}

	/**
	 * Retrieve an image, or create the image if this the first time the image is requested.
	 * <p>
	 * Tries its best to always return some kind of image, even on error cases, but could return null,
	 * in some rare error cases.
	 */
	public Image get(ImageDescriptor imgDesc) {
		if (images==null) {
			images = new HashMap<ImageDescriptor, Image>();
		}
		Image img = images.get(imgDesc);
		if (img==null) {
			img = imgDesc.createImage(true);
			if (img!=null) {
				images.put(imgDesc, img); 
			}
		}
		return img;
	}
	
}
