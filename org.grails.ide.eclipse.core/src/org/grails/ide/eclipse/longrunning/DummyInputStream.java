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
package org.grails.ide.eclipse.longrunning;

import java.io.IOException;
import java.io.InputStream;

/**
 * A singleton class, its only instance is a 'dummy' inputStream that doesn't provide any input.
 * (i.e. as soon as we try to read it, we get 'end of file'.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class DummyInputStream extends InputStream {

	public static final InputStream the = new DummyInputStream();

	/**
	 * Singleton, don't call this. Use 'DummyInputStream.the' instead.
	 */
	private DummyInputStream() {
	}

	@Override
	public int read() throws IOException {
		return -1;
	}

}
