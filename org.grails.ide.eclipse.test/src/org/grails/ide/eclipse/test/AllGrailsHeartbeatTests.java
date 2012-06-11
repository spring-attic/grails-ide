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
package org.grails.ide.eclipse.test;

import junit.framework.Test;

/**
 * @author Kris De Volder
 * @since 2.6
 */
public class AllGrailsHeartbeatTests {
	
	public static Test suite() {
		return AllGrailsTests.suite(true);
	}

}
