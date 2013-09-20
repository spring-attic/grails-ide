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
package org.grails.ide.eclipse.explorer.internal.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ImageUtils {

	/**
	 * Creates an image descriptor from a platform URL String. For example:
	 * "platform:/plugin/org.grails.ide.eclipse.explorer/icons/full/obj16/config.gif"
	 */
	public static ImageDescriptor imageDescriptor(String url) {
		try {
			return ImageDescriptor.createFromURL(new URL(url));
		} catch (MalformedURLException e) {
			GrailsCoreActivator.log(e);
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

}
