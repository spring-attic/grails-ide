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

import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.CONTROLLER_CLASS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.DOMAIN_CLASS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.INTEGRATION_TEST;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.SERVICE_CLASS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.TAGLIB_CLASS;
import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.UNIT_TEST;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.refactoring.rename.ParticipantChangeManager;
import org.grails.ide.eclipse.refactoring.rename.ui.DialogBasedGrailsTypeRenameConfigurer;

/**
 * @author Kris De Volder
 * @since 2.7
 */
@SuppressWarnings("restriction")
public final class GrailsTypeRenameParticipant extends RenameParticipant implements ITypeRenaming {

	final public static boolean DEBUG = false;
	void debug(String string) {
		if (DEBUG) {
			System.out.println(this.getClass().getSimpleName()+": " +string);
		}
	}
	
	/**
	 * GrailsTypeRenameParticipant requires outside input to fully configure
	 * what extra changes to perform. This interface abstracts away from how precisely the decision is being made.
	 */
	public interface IGrailsTypeRenameConfigurer {
		Collection<ITypeRenaming> chooseAdditionalRenamings(ITypeRenaming orgType, Collection<ITypeRenaming> values, RefactoringStatus status);
		boolean updateServiceReferences();
		boolean updateGSPs();
	}
	
	protected GrailsProject project;
	protected GroovyCompilationUnit cu;
	protected IType type; 

	private Collection<ITypeRenaming> extraRenamings = null;
	private ArrayList<ExtraTypeRenamer> extraRenamers = null;
	
	private IGrailsTypeRenameConfigurer grailsRenameConfigurer = null;
	private ParticipantChangeManager changes = null;
	
	@Override
	protected boolean initialize(Object object) {
		if (object instanceof IType) {
			this.type = (IType) object;
			debug("initializing on type "+type.getElementName());
	
			project = GrailsWorkspaceCore.get().getGrailsProjectFor(type);
			if (project!=null) {
				debug("project = "+project);
				IJavaElement parent = type.getParent();
				if (parent instanceof GroovyCompilationUnit) {
					cu = (GroovyCompilationUnit) parent;
					debug("cu = "+cu);
					return isInteresting();
				}
			}
		}
		debug("not applicable");
		return false;
	}

	private IGrailsTypeRenameConfigurer getConfigurer() {
		if (grailsRenameConfigurer==null) {
			GrailsTypeRenameRefactoring refactoring = getGrailsTypeRefactoring();
			if (refactoring!=null) {
				grailsRenameConfigurer = refactoring.getRenameConfigurer();
			} else {
				grailsRenameConfigurer = new DialogBasedGrailsTypeRenameConfigurer();
			}
		}
		return grailsRenameConfigurer;
	}

	/**
	 * Subclass should implement this to specify whether the target element being renamed is interesting to the participant.
	 * By the time this method is called, the fields project, cu and type fields should have been initialised.
	 */
	protected boolean isInteresting() {
		GrailsElementKind kind = project.getElementKind(cu); 
		debug("kind = " + kind);
		return isInterestingKind(kind); 
	}

	protected boolean isInterestingKind(GrailsElementKind kind) {
		return DOMAIN_CLASS==kind
		    || CONTROLLER_CLASS == kind
			|| SERVICE_CLASS == kind
			|| TAGLIB_CLASS == kind
			|| UNIT_TEST == kind
			|| INTEGRATION_TEST == kind;
	}
	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		try {
			extraRenamings = computeExtraRenamings(status, pm);
			if (!status.hasFatalError()) {
				chooseAdditionalRenamings(status);
				checkAdditionalRenamings(status, pm, context);
				computeChanges(status, pm);
				computeExtraChanges(status, pm);
			}
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			status.addError("Internal error see error log for details");
		}
		return status;
	}

	/**
	 * Searches for extra references to renamed Grails types and does grails specific things to update those references.
	 * For example, when a service is renamed we should look for auto-wired fields that refer to this service by naming
	 * convention and also rename those fields and references to them.
	 */
	private void computeExtraChanges(RefactoringStatus status, IProgressMonitor pm) {
		computeExtraChanges(this, status, pm);
		for (ITypeRenaming r : extraRenamings) {
			computeExtraChanges(r, status, pm);
		}
	}

	private void computeExtraChanges(ITypeRenaming r, RefactoringStatus status, IProgressMonitor pm) {
		for (ExtraChangeComputer computer : getExtraChangeComputers()) {
			if (computer.initialize(project, r)) {
				computer.createChanges(changes, status, pm);
			}
		}
	}

	private Collection<ExtraChangeComputer> getExtraChangeComputers() {
		IGrailsTypeRenameConfigurer configurer = getConfigurer();
		ArrayList<ExtraChangeComputer> result = new ArrayList<ExtraChangeComputer>();
		if (configurer.updateServiceReferences()) {
			result.add(new ServiceReferenceUpdater());
		}
		if (configurer.updateGSPs()) {
			result.add(new GSPUpdater());
		}
		result.add(new ControllerReferenceUpdater());
		return result;
	}

	protected Collection<ITypeRenaming> computeExtraRenamings(RefactoringStatus status, IProgressMonitor pm) {
		GrailsTypeRenameRefactoring refactoring = getGrailsTypeRefactoring();
		ExtraRenamingsComputer extraRenamingsComputer;
		if (refactoring!=null) {
			extraRenamingsComputer = refactoring.getExtraRenamingsComputer();
			grailsRenameConfigurer = refactoring.getRenameConfigurer();
		} else {
			extraRenamingsComputer = ExtraRenamingsComputer.create(this);
			grailsRenameConfigurer = new DialogBasedGrailsTypeRenameConfigurer(); 
		}
		if (extraRenamingsComputer != null) {
			status.merge(extraRenamingsComputer.checkPreconditions());
			return extraRenamingsComputer.getExtraRenamings(pm);
		}
		return new ArrayList<ITypeRenaming>();
	}

	/**
	 * @return The GrailsTypeRenameRefactoring we are participating with, or null, if the refactoring we are
	 * partici[ating with is a different type of refactoring.
	 */
	private GrailsTypeRenameRefactoring getGrailsTypeRefactoring() {
		ProcessorBasedRefactoring refactoring = getProcessor().getRefactoring();
		if (refactoring instanceof GrailsTypeRenameRefactoring) {
			return (GrailsTypeRenameRefactoring) refactoring;
		}
		return null;
	}

	private void checkAdditionalRenamings(RefactoringStatus status, IProgressMonitor pm,
			CheckConditionsContext context) throws CoreException {
		extraRenamers = new ArrayList<ExtraTypeRenamer>();
		for (ITypeRenaming renaming : extraRenamings) {
			ExtraTypeRenamer renamer = new ExtraTypeRenamer(renaming);
			extraRenamers.add(renamer);
			renamer.checkConditions(status, pm, context);
		}
	}

	private void chooseAdditionalRenamings(RefactoringStatus status) {
		if (!extraRenamings.isEmpty()) {
			Collection<ITypeRenaming> chosenRenamings = grailsRenameConfigurer.chooseAdditionalRenamings(
					new TypeRenaming(type, getArguments().getNewName()),  
					extraRenamings, status);
			extraRenamings = new ArrayList<ITypeRenaming>();
			for (ITypeRenaming r : chosenRenamings) {
				extraRenamings.add(r);
			}
		}
	}

	private void computeChanges(RefactoringStatus status, IProgressMonitor pm) {
		try {
			changes = new ParticipantChangeManager(this);
			if (!extraRenamers.isEmpty()) {
				for (ExtraTypeRenamer ren : extraRenamers) {
					Change change = ren.createChange(pm);
					changes.add(change);
				}
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
	}

	@Override
	public Change createPreChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (changes!=null) {
			changes.copyExistingChangesTo(this);
			return changes.getNewTextChanges();
		}
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (changes!=null) {
			return changes.getOtherChanges();
		}
		return null;
	}

	public IType getTarget() {
		return type;
	}

	public String getNewName() {
		return getArguments().getNewName();
	}
	
	@Override
	public String getName() {
		return "Grails Type Rename Participant";
	}

}
