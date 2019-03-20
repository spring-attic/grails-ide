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
package org.grails.ide.eclipse.runtime.shared.longrunning;

import java.io.IOException;

/**
 * An internal error that should only be raised if there is some problem in the client not
 * respecting the communication protocol implicitly agreed upon between
 * {@link GrailsClient} and its corresponding (external) {@link GrailsProcess}.
 * <p>
 * Raising this exception means that a client is not sending the stuff a process expects. So it signifies
 * a bug in either the client or the process.
 * 
 * @author Kris De Volder
 */
public class ProtocolException extends IOException {

	private static final long serialVersionUID = 1L;

	public ProtocolException(String string) {
		super(string);
	}

}