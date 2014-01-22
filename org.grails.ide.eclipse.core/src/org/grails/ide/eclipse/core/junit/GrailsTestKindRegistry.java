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
package org.grails.ide.eclipse.core.junit;

import java.util.ArrayList;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;


/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class GrailsTestKindRegistry {

	public static void initialize() {
		TestKindRegistry registry = TestKindRegistry.getDefault();
		ArrayList<ITestKind> testKinds = registry.getAllKinds(); 
		for (int i = 0; i < testKinds.size(); i++) {
			ITestKind testKind = testKinds.get(i);
			String id = testKind.getId();
			if (TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(id)) {
				testKinds.set(i, new GrailsAwareTestKind(testKind));
			}
		}
	}
	
}
