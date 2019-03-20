/*******************************************************************************
 * Copyright (c) 2013 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test;

import org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.test.util.GrailsTest;

import static org.grails.ide.eclipse.core.model.GrailsVersion.*;
import static org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion.*;

/**
 * @author Kris De Volder
 */
public class GroovyCompilerVersionCheckTest extends GrailsTest {

	public void testGetRequiredGroovyVersion() throws Exception {
		doTest(V_1_3_8, _17);
		
		doTest(V_2_0_0, _18);
		doTest(V_2_0_1, _18);
		doTest(V_2_0_2, _18);
		doTest(V_2_0_3, _18);
		doTest(V_2_0_4, _18);
		
		doTest(V_2_1_0, _18);
		doTest(V_2_1_1, _18);

		doTest(V_2_2_0, _20);
		doTest(V_2_2_1, _20);
		
		doTest(V_2_3_0, _21);
		doTest(V_2_4_0_M2, _23);
		
	}

	private void doTest(GrailsVersion grailsv, SpecifiedVersion groovyv) {
		assertEquals(groovyv, GroovyCompilerVersionCheck.getRequiredGroovyVersion(grailsv));
	}
	
}
