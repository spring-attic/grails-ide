// COPIED from spring-ide org.springframework.ide.eclipse.beans.ui.editor.util.CompilationUnitHelper
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
package org.grails.ide.eclipse.ui.contentassist.util;

import org.eclipse.jdt.core.WorkingCopyOwner;

/**
 * @author Christian Dupuis
 * @author Torsten Juergeleit
 */
class CompilationUnitHelper {

	private CompilationProblemRequestor fProblemRequestor = null;

	private WorkingCopyOwner fWorkingCopyOwner = null;

	private static CompilationUnitHelper instance;

	private CompilationUnitHelper() {
		// force use of instance
	}

	public synchronized static final CompilationUnitHelper getInstance() {

		if (instance == null)
			instance = new CompilationUnitHelper();
		return instance;
	}

	public CompilationProblemRequestor getProblemRequestor() {

		if (fProblemRequestor == null)
			fProblemRequestor = new CompilationProblemRequestor();
		return fProblemRequestor;
	}

	public WorkingCopyOwner getWorkingCopyOwner() {

		if (fWorkingCopyOwner == null) {
			fWorkingCopyOwner = new WorkingCopyOwner() {
				@Override
				public String toString() {
					return "Spring Beans IDE Working copy owner"; //$NON-NLS-1$
				}
			};
		}
		return fWorkingCopyOwner;
	}
}
