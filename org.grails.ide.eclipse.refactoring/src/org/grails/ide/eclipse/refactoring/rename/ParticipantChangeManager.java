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
package org.grails.ide.eclipse.refactoring.rename;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Manages changes made by a participant. This is a little tricky because participant must keep changes to
 * files that no one else has modified separate from those that others have already modified.
 * <p>
 * Also it is important that text changes are returned as "preChanges" to ensure that they get executed before
 * the files that they are modifying may be moved elsewhere.
 * <p>
 * This class is meant to help doing that so that the participant doesn't have to worry about this. Changes
 * added to the manager will be examined recursively. Changes to something that someone else is also changing
 * will be merged with the existing changes in the participant. Other changes will be kept separately and
 * managed in similar way. In the end, after the participant has added all the changes they must request the
 * "new" changes that weren't already merged into the host refactoring's state and return them.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class ParticipantChangeManager {
	
//	private RefactoringParticipant participant;
	private ArrayList<Change> otherChanges; //Contains all non-text-based changes made by the participant
	private Map<Object, TextChange> textChanges = new HashMap<Object, TextChange>();
	private String name;
	private RefactoringParticipant participant;
	
	public ParticipantChangeManager(RefactoringParticipant participant) {
		this.name = participant.getName();
//		this.participant = participant;
//		this.newTextChanges = new CompositeChange("Update references for Grails related Types");
		this.otherChanges = new ArrayList<Change>();
	}

	/**
	 * @return The changes that were *not* merged with changes made by others. This returns only
	 * those changes that aren't text changes.
	 */
	public CompositeChange getOtherChanges() {
		if (!otherChanges.isEmpty()) {
			return new CompositeChange(name+ " other changes", 
			  otherChanges.toArray(new Change[otherChanges.size()]));
		}
		return null;
	}

	public void add(Change change) {
		if (change instanceof CompositeChange) {
			//composite... look at the children
			for (Change child : ((CompositeChange) change).getChildren()) {
				add(child);
			}
		} else if (change instanceof TextChange) {
			//non-composite text edit
			TextChange extra = (TextChange) change;
			
			Object el = change.getModifiedElement();
			TextChange existing = getTextChange(el);
			if (existing==null) {
				addNewTextChange(extra);
			} else {
				merge(existing, extra);
			}
		} else {
			//Something that's neither composite nor a text change... keep it as "new".
			addNewChange(change);
		}
	}

	private void addNewTextChange(TextChange change) {
		Object el = change.getModifiedElement();
		Assert.isLegal(!textChanges.containsKey(el));
		if (change.getParent()!=null) {
			ReflectionUtils.setPrivateField(Change.class, "fParent", change, null); //Hack! needs to be null to lift it out of one change tree into another!
		}
		textChanges.put(el, change);
	}

	private void addNewChange(Change change) {
		if (change.getParent()!=null) {
			ReflectionUtils.setPrivateField(Change.class, "fParent", change, null); //Hack! needs to be null to lift it out of one change tree into another!
		}
		otherChanges.add(change);
	}

	/**
	 * @return An existing text change associated with given element or null, if no text change
	 * is associated with that element so far.
	 */
	private TextChange getTextChange(Object element) {
		TextChange change = textChanges.get(element);
		return change;
	}
	
	private void merge(TextChange existing, TextChange extra) {
		TextEdit extraEdit = extra.getEdit();
		addEdit(existing, extraEdit);
	}

	private void addEdit(TextChange existing, TextEdit extraEdit) {
		if (existing.getEdit()==null) {
			existing.setEdit(new MultiTextEdit());
		}
		Assert.isLegal(existing.getEdit() instanceof MultiTextEdit);
		if (extraEdit instanceof MultiTextEdit) {
			for (TextEdit child : extraEdit.getChildren()) {
				addEdit(existing, child);
			}
		} else {
			existing.addEdit(extraEdit.copy());
		}
	}
	
	/**
	 * This method must be called to process all the text changes, adding changes to
	 * files that the refactoring also modifies to the refactoring's changes and
	 * removing them from this Change manager.
	 * <p>
	 * This method should be called from participant's createPreChange method, before
	 * requesting the new text changes.
	 */
	public void copyExistingChangesTo(RefactoringParticipant participant) {
		Assert.isLegal(this.participant==null || this.participant==participant);
		if (this.participant==null) {
			Assert.isNotNull(participant);
			this.participant = participant;
			Iterator<Object> keys = textChanges.keySet().iterator();
			while (keys.hasNext()) {
				Object key = keys.next(); 
				TextChange existing = participant.getTextChange(key);
				if (existing!=null) {
					merge(existing, textChanges.get(key));
					keys.remove();
				}
			}
		}
	}

	public CompositeChange getNewTextChanges() {
		Assert.isLegal(this.participant!=null, "The method 'copyExistingChangesTo' must be called before this one!");
		if (!textChanges.isEmpty()) {
			boolean addedOne = false;
			CompositeChange composed = new CompositeChange(name + " text changes");
			for (TextChange c : textChanges.values()) {
				if (!isEmpty(c)) {
					composed.add(c);
					addedOne = true;
				}
			}
			if (addedOne) {
				return composed;
			}
		}
		return null;
	}

	private boolean isEmpty(TextChange c) {
		TextEdit contents = c.getEdit();
		if (contents==null) {
			return true;
		} else if (contents instanceof MultiTextEdit) {
			return !contents.hasChildren();
		}
		return false;
	}

	public TextChange getCUChange(ICompilationUnit compilationUnit) {
		TextChange existing = getTextChange(compilationUnit);
		if (existing!=null) {
			return existing;
		}
		CompilationUnitChange newChange = new CompilationUnitChange(compilationUnit.getElementName(), compilationUnit);
		newChange.setEdit(new MultiTextEdit());
		addNewTextChange(newChange);
		return newChange;
	}
	
	public TextChange getFileChange(IFile file) {
		TextChange existing = getTextChange(file);
		if (existing!=null) {
			return existing;
		}
		TextFileChange newChange = new TextFileChange(file.getName(), file);
		newChange.setEdit(new MultiTextEdit());
		addNewTextChange(newChange);
		return newChange;
	}

	public TextChange getTextChangeFor(Object el) {
		if (el instanceof IJavaElement) {
			IJavaElement jel = (IJavaElement)el;
			ICompilationUnit cu = (ICompilationUnit) jel.getAncestor(IJavaElement.COMPILATION_UNIT);
			return getCUChange(cu);
		} else if (el instanceof IFile) {
			return getFileChange((IFile) el);
		}
		return null;
	}
}
