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
package org.grails.ide.eclipse.refactoring.rename.ui;

import org.codehaus.groovy.eclipse.refactoring.actions.IRenameTarget;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * Abstract base class for Grails adapter factories that adapt various 'special' grails things
 * to IRenameTarget. This causes Greclipse to hook into the IRenameTarget when asked to
 * when performing a rename refactoring, thus replacing the the normal rename behavior
 * with something Grails specific for the adapted object.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public abstract class GrailsRenameTargetAdapterFactory  implements IAdapterFactory {

	private static final Class<?>[] types = {
		IRenameTarget.class
	};
	
	@SuppressWarnings("rawtypes")
	public Class[] getAdapterList() {
		return types;
	}

}
