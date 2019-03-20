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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.FieldDeclarationMatch;
import org.eclipse.jdt.core.search.FieldReferenceMatch;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.ServiceClass;
import org.grails.ide.eclipse.refactoring.rename.ParticipantChangeManager;
import org.grails.ide.eclipse.refactoring.util.RefactoringUtils;

/**
 * Updates fields and field references if the field name looks like it is an autowired field for a given service
 * class.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class ServiceReferenceUpdater extends ExtraChangeComputer {
	
	private ServiceClass serviceClass;
	
	@Override
	public boolean initialize(GrailsProject project, ITypeRenaming r) {
		if (super.initialize(project, r)) {
			serviceClass = project.getServiceClass(renaming.getTarget());
			if (serviceClass!=null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void createChanges(final ParticipantChangeManager changes, final RefactoringStatus status, IProgressMonitor pm) {
		Assert.isNotNull(changes);
		try {
			IJavaSearchScope scope = RefactoringUtils.getSearchScope(project.getJavaProject());
			SearchEngine engine = new SearchEngine();

			final String fieldName = GrailsNameUtils.getPropertyName(renaming.getTarget().getFullyQualifiedName());
			final String fieldNewName = GrailsNameUtils.getPropertyName(renaming.getNewName());

			SearchPattern fieldByNamePat = SearchPattern.createPattern(fieldName, IJavaSearchConstants.FIELD , IJavaSearchConstants.ALL_OCCURRENCES, 
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);

			SearchRequestor req = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					try {
						if (match instanceof FieldDeclarationMatch || match instanceof FieldReferenceMatch) {
							Object el = match.getElement();
							if (el instanceof IJavaElement) {
								IJavaElement jel = (IJavaElement) el;
								ICompilationUnit cu = (ICompilationUnit) jel.getAncestor(IJavaElement.COMPILATION_UNIT);
								if (cu!=null) {
									TextChange cuChange = changes.getCUChange(cu);
									final int offset = match.getOffset();
									final int length = match.getLength();
									String text = cu.getBuffer().getText(offset, length);
									if (text.equals(fieldName)) {
										//Only perform the edit if the text we are about to replace is what we expect!
										cuChange.addEdit(new ReplaceEdit(offset, length, fieldNewName));
									}
								}
							}
						}
					} catch (Exception e) {
						//Ignore this match and log the exception...
						//but keep processing other matches!
						GrailsCoreActivator.log(e);
					}
				}

			};
			engine.search(fieldByNamePat, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, scope, 
					req, pm);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
	}

}
