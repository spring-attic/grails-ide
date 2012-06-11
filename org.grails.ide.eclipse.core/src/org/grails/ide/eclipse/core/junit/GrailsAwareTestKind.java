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
package org.grails.ide.eclipse.core.junit;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitRuntimeClasspathEntry;
import org.eclipse.jdt.internal.junit.launcher.TestKind;

public class GrailsAwareTestKind extends TestKind {

	private ITestKind wrappee;
	private Grails20AwareTestFinder finder = null;
	
	public GrailsAwareTestKind(ITestKind wrappee) {
		super((IConfigurationElement)ReflectionUtils.getPrivateField(TestKind.class, "fElement", wrappee));
		this.wrappee = wrappee;
	}

	public ITestFinder getFinder() {
		if (finder==null) {
			finder = new Grails20AwareTestFinder(wrappee.getFinder());
		}
		return finder;
	}

	public String getId() {
		return wrappee.getId();
	}

	public String getDisplayName() {
		return wrappee.getDisplayName();
	}

	public String getFinderClassName() {
		return wrappee.getFinderClassName();
	}

	public String getLoaderPluginId() {
		return wrappee.getLoaderPluginId();
	}

	public String getLoaderClassName() {
		return wrappee.getLoaderClassName();
	}

	public String getPrecededKindId() {
		return wrappee.getPrecededKindId();
	}

	public boolean isNull() {
		return wrappee.isNull();
	}

	public JUnitRuntimeClasspathEntry[] getClasspathEntries() {
		return wrappee.getClasspathEntries();
	}

}
