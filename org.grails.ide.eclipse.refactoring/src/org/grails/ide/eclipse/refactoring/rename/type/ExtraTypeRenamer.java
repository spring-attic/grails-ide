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
package org.grails.ide.eclipse.refactoring.rename.type;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

/**
 * @author Kris De Volder
 * @since 2.7
 */
@SuppressWarnings("restriction")
public class ExtraTypeRenamer extends TypeRenaming {

	public ExtraTypeRenamer(ITypeRenaming renaming) {
		super(renaming.getTarget(), renaming.getNewName());
	}

	private RenameTypeProcessor processor = null;
	
	public RenameTypeProcessor getProcessor() {
		if (processor==null) {
			processor = new RenameTypeProcessor(getTarget());
			processor.setNewElementName(getNewName());
		}
		return processor;
	}

	public void checkConditions(RefactoringStatus status, IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		RenameTypeProcessor processor = getProcessor();
		status.merge(processor.checkInitialConditions(pm));
		status.merge(processor.checkFinalConditions(pm, context));
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		return getProcessor().createChange(pm);
	}
	
}
