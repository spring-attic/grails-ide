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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.refactoring.rename.type.GrailsTypeRenameParticipant.IGrailsTypeRenameConfigurer;


/**
 * @author Kris De Volder
 * @since 2.7
 */
@SuppressWarnings("restriction")
public class GrailsTypeRenameRefactoring extends ProcessorBasedRefactoring implements ITypeRenaming {

	private static final boolean DEFAULT_UPDATE_SERVICE_REFS = true;
	private static final boolean DEFAULT_UPDATE_GSPS = true;
	
	private ExtraRenamingsComputer extraRenamingsComputer;
	private ITypeRenaming[] chosenAdditionalRenamings;
	private boolean updateServiceRefs = DEFAULT_UPDATE_SERVICE_REFS;
	private boolean updateGSPs = DEFAULT_UPDATE_GSPS;

	public GrailsTypeRenameRefactoring(IType target) {
		super(new RenameTypeProcessor(target));
		setNewName(target.getElementName());
		setChosenAdditionalRenamings(getExtraRenamingsComputer().getExtraRenamings(new NullProgressMonitor()));
	}

	public void setChosenAdditionalRenamings(Collection<ITypeRenaming> extraRenamings) {
		setChosenAdditionalRenamings(extraRenamings.toArray(new ITypeRenaming[extraRenamings.size()]));
	}

	public void setNewName(String newName) {
		this.getProcessor().setNewElementName(newName);
		extraRenamingsComputer = null;
		updateAdditionalRenamings();
	}
	
	private void updateAdditionalRenamings() {
		Collection<ITypeRenaming> newRenamings = getExtraRenamingsComputer().getExtraRenamings(new NullProgressMonitor());
		ITypeRenaming[] oldChosenRenamings = getChosenAdditionalRenamings();
		if (oldChosenRenamings!=null) {
			HashSet<IType> oldChosenTargets = new HashSet<IType>();
			for (ITypeRenaming or : oldChosenRenamings) {
				oldChosenTargets.add(or.getTarget());
			}
			List<ITypeRenaming> newChosenRenamings = new ArrayList<ITypeRenaming>();
			for (ITypeRenaming nr : newRenamings) {
				if (oldChosenTargets.contains(nr.getTarget())) {
					newChosenRenamings.add(nr);
				}
			}

			final ITypeRenaming[] newChosenRenamingsArr = newChosenRenamings.toArray(new ITypeRenaming[newChosenRenamings.size()]);
			setChosenAdditionalRenamings(newChosenRenamingsArr);
		}
	}
	
	@Override
	public RenameTypeProcessor getProcessor() {
		return (RenameTypeProcessor) super.getProcessor();
	}

	public ExtraRenamingsComputer getExtraRenamingsComputer() {
		if (extraRenamingsComputer==null) {
			extraRenamingsComputer = ExtraRenamingsComputer.create(this);
		}
		return extraRenamingsComputer;
	}

	public IType getTarget() {
		return getProcessor().getType();
	}
	
	public String getNewName() {
		return getProcessor().getNewElementName();
	}

	public void setChosenAdditionalRenamings(ITypeRenaming[] chosen) {
		chosenAdditionalRenamings = chosen;
	}

	public ITypeRenaming[] getChosenAdditionalRenamings() {
		return chosenAdditionalRenamings;
	}

	public IGrailsTypeRenameConfigurer getRenameConfigurer() {
		return new ConfigureFromGrailsTypeRenameRefactoring(this);
	}

	/**
	 * @return whether the option to update auto-wired service fields is enabled.
	 */
	public boolean getUpdateServiceRefs() {
		return updateServiceRefs;
	}

	/**
	 * @return whether the option to update GSP files is enabled.
	 */
	public boolean getUpdateGSPs() {
		return updateGSPs;
	}

	public void setUpdateServiceRefs(boolean doUpdate) {
		updateServiceRefs = doUpdate;
	}

	public void setUpdateGSPs(boolean newValue) {
		updateGSPs = newValue;
	}
	
	/**
	 * @return true if at least one Service class is being renamed by this refactoring.
	 */
	public boolean isServiceRenaming() {
		final ITypeRenaming[] extras = getChosenAdditionalRenamings();
		ArrayList<ITypeRenaming> renamings = new ArrayList<ITypeRenaming>();
		renamings.add(this);
		if (extras!=null) {
			renamings.addAll(Arrays.asList(extras));
		}
		for (ITypeRenaming r : renamings) {
			IType target = r.getTarget();
			if (GrailsWorkspaceCore.isServiceClass(target)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return whether preview forcing should be enabled for this refactoring.
	 */
	public boolean shouldForcePreview() {
		return getUpdateGSPs() || getUpdateServiceRefs() && isServiceRenaming(); 
	}


}
