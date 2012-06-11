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
package org.grails.ide.eclipse.refactoring.rename.type;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;
import org.grails.ide.eclipse.editor.gsp.search.IGSPSearchRequestor;
import org.grails.ide.eclipse.editor.gsp.search.SearchInGSPs;
import org.grails.ide.eclipse.refactoring.rename.ParticipantChangeManager;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GSPUpdater extends ExtraChangeComputer {
	
	/**
	 * @author Kris De Volder
	 * @since 2.7
	 */
	private class GSPSearchRequestor implements IGSPSearchRequestor {
		
		private ParticipantChangeManager changes;
		
		private int nameLen = renaming.getTarget().getElementName().length();

		public GSPSearchRequestor(ParticipantChangeManager changes) {
			this.changes = changes;
		}

		public boolean searchForTags() {
			return false;
		}

		public void acceptMatch(IFile file, int start, int length) {
			if (start>=0) {
				TextFileChange newChange = new TextFileChange("gsp reference change", file);
				MultiTextEdit edits = new MultiTextEdit();
				edits.addChild(new ReplaceEdit(start+length-nameLen, nameLen, renaming.getNewName()));
				newChange.setEdit(edits);
//				try { 
					changes.add(newChange);
//				} catch (MalformedTreeException e) {
//					// Ignore... this happens because the GSP search sometimes returns overlapping references
//					// where one is qualified name and the other is unqualified
//				}
			}
		}

		public int limitTo() {
			return IJavaSearchConstants.REFERENCES | IJavaSearchConstants.ALL_OCCURRENCES;
		}

		public IJavaElement elementToSearchFor() {
			return renaming.getTarget();
		}

		public List<IFile> getGSPsToSearch() {
			return project.getGSPFiles();
		}

	}

	private static final boolean DEBUG = false;
	private static void debug(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}
	
	private SearchInGSPs gspSearch = new SearchInGSPs();

	@Override
	public boolean initialize(GrailsProject project, ITypeRenaming renaming) {
		return super.initialize(project, renaming);
	}
	
	@Override
	protected void createChanges(ParticipantChangeManager changes, final RefactoringStatus status, IProgressMonitor pm) {
		Assert.isNotNull(changes);
		IType target = renaming.getTarget();
		IGrailsElement elem = GrailsWorkspaceCore.get().create(target);
		if (elem!=null) {
			if (elem.getKind()==GrailsElementKind.CONTROLLER_CLASS) {
				renameViewsFolder(changes, target);
			}
		}
		updateReferencesInGSPFiles(changes, status, pm);
	}

	private void updateReferencesInGSPFiles(ParticipantChangeManager changes, RefactoringStatus status, IProgressMonitor pm) {
		try {
			gspSearch.performSearch(new GSPSearchRequestor(changes), pm);
		} catch (CoreException e) {
			status.addError("Unexpected error updating references in GSPs, see error log for details");
			GrailsCoreActivator.log(e);
		}
	}

	private void renameViewsFolder(ParticipantChangeManager changes,
			IType target) {
		IFolder viewsFolder = project.getViewsFolder();
		if (viewsFolder.exists()) {
			IFolder domainViewFolder = viewsFolder.getFolder(viewsFolderNameFor(target));
			if (domainViewFolder.exists()) {
				String newName = viewsFolderNameFor(renaming.getNewName());
				debug("This folder should be renamed: "+domainViewFolder+" => "+newName);
				changes.add(new RenameResourceChange(domainViewFolder.getFullPath(), newName));
			}
		}
	}

	private String viewsFolderNameFor(IType target) {
		String controllerName = target.getElementName();
		return viewsFolderNameFor(controllerName);
	}

	private String viewsFolderNameFor(String controllerName) {
		return GrailsNameUtils.getPropertyName(GrailsNameUtils.getLogicalName(controllerName, "Controller"));
	}

}
