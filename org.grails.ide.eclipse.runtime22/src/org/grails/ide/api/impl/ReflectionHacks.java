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
package org.grails.ide.api.impl;

import grails.build.logging.GrailsConsole;
import grails.build.logging.GrailsEclipseConsole;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Hacks that make things work in Grails 2.2 without changing the api. If our pull 
 * request is accepted before Grails 2.2 is released then these hacks aren't
 * necessary and any references to these methods should be easily replaced
 * by ordinary method calls.
 * 
 * @author Kris De Volder
 */
public class ReflectionHacks {

	public static void GrailsConsole_setInstance(GrailsConsole console) {
		//TODO: if our pull request makes it into Grails 2.2.0 then we can live without this reflection hackery) and replace it with:
		//GrailsConsole.setInstance(console);
		try {
			Method setInstance = GrailsConsole.class.getMethod("setInstance", GrailsConsole.class);
			setInstance.invoke(null, console);
			return;
		} catch (Throwable e) {
		}
		//Try something else..
		try {
			Field instance = GrailsConsole.class.getDeclaredField("instance");
			instance.setAccessible(true);
			instance.set(null, console);
			return;
		} catch (Throwable e) {
			//Nothing else we can try.
			throw new RuntimeException(e);
		}
	}

	public static GrailsConsole new_GrailsEclipseConsole() {
		try {
			Constructor<GrailsEclipseConsole> c = GrailsEclipseConsole.class.getDeclaredConstructor();
			c.setAccessible(true);
			return c.newInstance();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
