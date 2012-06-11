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
package org.grails.ide.eclipse.ui.internal.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsCommandAdapter;
import org.springsource.ide.eclipse.commons.ui.SpringUIUtils;


/**
 * @author Kris De Volder
 */
public class OpenInterestingNewResourceListener extends GrailsCommandAdapter {

	private IProject project;
	private int greatestInterest = 0;
	private IResource interestingResource = null;

	public OpenInterestingNewResourceListener(IProject project) {
		this.project = project;
	}

	@Override
	public void changedResource(IResource resource) {
		newOrChangedResource(resource);
	}
	
	@Override
	public void newResource(IResource resource) {
		newOrChangedResource(resource);
	}
	
	private void newOrChangedResource(IResource resource) {
		int interestValue = howInteresting(resource);
		if (interestValue > greatestInterest) {
			greatestInterest = interestValue;
			interestingResource = resource;
		}
		System.out.println(resource);
	}

	/**
	 * How interesting is the resource? Return a value greater than 0 if the
	 * resource is interesting (so it should be opened). Only the one resource
	 * of greatest interesingness value will be opened.
	 */
	protected int howInteresting(IResource resource) {
		String path = resource.getFullPath().toString();
		if (!(resource instanceof IFile)) return 0;
		if (!resource.isAccessible()) return 0;
		
		//Only xml test-reports are interesting
		if (!path.contains("test-reports") && path.endsWith(".xml")) return 0;
		if (path.contains("TestSuites")) {
			return 2; //The "all included" report is most interesting
		}
		else {
			return 1; // other reports are less interesting
		}
	}

	public void finish() {
		GrailsCoreActivator.getDefault().removeGrailsCommandResourceListener(this);
		if (interestingResource != null) {
			// Only open resource if is in any source folder
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					SpringUIUtils.openInEditor((IFile) interestingResource, -1);
				}
			});
		}
	}

	public boolean supports(IProject project) {
		return this.project.equals(project);
	}

}
