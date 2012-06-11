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
package org.grails.ide.eclipse.refactoring.rename.ui;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.grails.ide.eclipse.refactoring.rename.type.GrailsTypeRenameRefactoring;
import org.grails.ide.eclipse.refactoring.rename.type.ITypeRenaming;
import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsTypeRenameWizard extends RefactoringWizard {

	private static final GridDataFactory GRAB_HOR = GridDataFactory.fillDefaults().grab(true, false);
	private static final GridDataFactory GRAB_BOTH = GridDataFactory.fillDefaults().grab(true, true);
	
	private class NewNamePage extends UserInputWizardPage {
		
		private Text newNameInput;
		private CheckboxTableViewer additionalRenamingsViewer;
		public Button updateServiceRefs;
		private Button updateGSPs;

		NewNamePage() {
			super("Rename Grails Type");
		}
		
		@Override
		protected GrailsTypeRenameRefactoring getRefactoring() {
			return (GrailsTypeRenameRefactoring) super.getRefactoring();
		}
		
		protected void setNewName(String newName) {
			if (newNameInput!=null) {
				if (!newNameInput.getText().equals(newName)) {
					newNameInput.setText(newName);
				}
			}
			getRefactoring().setNewName(newName.trim());
			updateAdditionalRenamings();
			updateMessage();
		}

		private void updateAdditionalRenamings() {
			if (additionalRenamingsViewer!=null) {
				additionalRenamingsViewer.setInput(getRefactoring().getExtraRenamingsComputer().getExtraRenamings(new NullProgressMonitor()));
				additionalRenamingsViewer.setCheckedElements(getRefactoring().getChosenAdditionalRenamings());
			}
		}

		public void createControl(Composite _parent) {
			Composite parent= new Composite(_parent, SWT.NONE);

			setControl(parent);

			GridLayout layout= new GridLayout(1, false);
			parent.setLayout(layout);

			createNewNameInput(parent);
			createAdditionalRenamingsTable(parent);
			createUpdateServiceReferencesCheckbox(parent);
			createUpdateGSPCheckbox(parent);
			updateServiceRefsEnablement();
			updateMessage();
		}

		private void updateMessage() {
			setMessage(null);
			String oldName = getRefactoring().getTarget().getElementName();
			if (oldName.equals(getRefactoring().getNewName())) {
				setMessage("The new name should be different from the old name '"+oldName+"'", IMessageProvider.ERROR);
			} else {
				RefactoringStatus status = getRefactoring().getExtraRenamingsComputer().checkPreconditions();
				if (IStatus.OK < status.getSeverity()) {
					RefactoringStatusEntry entry = status.getEntryWithHighestSeverity();
					setMessage(entry.getMessage(), messageSeverity(entry));
				}
			}
		}

		private int messageSeverity(RefactoringStatusEntry entry) {
			int severity = entry.getSeverity();
			if (severity>=RefactoringStatus.ERROR) {
				return IMessageProvider.ERROR;
			} else if (severity>=RefactoringStatus.WARNING) {
				return IMessageProvider.WARNING;
			} else if (severity>=RefactoringStatus.INFO) {
				return IMessageProvider.INFORMATION;
			} else {
				return IMessageProvider.NONE;
			}
		}

		private void createNewNameInput(Composite _parent) {
			Composite parent = new Composite(_parent, SWT.NONE);
			GridLayout layout= new GridLayout(2, false);
			parent.setLayout(layout);
			GRAB_HOR.applyTo(parent);
			
			Label label= new Label(parent, SWT.NONE);
			label.setText("New name:");
						
			newNameInput = new Text(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
			GRAB_HOR.applyTo(newNameInput);
			newNameInput.setText(getRefactoring().getNewName());
			newNameInput.setSelection(0, newNameInput.getText().length());
			
			newNameInput.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					setNewName(newNameInput.getText());
				}
			});
		}
		
		private void createAdditionalRenamingsTable(Composite parent) {
			Label message = new Label(parent, SWT.NONE);
			message.setText("Grails naming conventions also suggest renaming related elements.");
			additionalRenamingsViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
			GRAB_BOTH.applyTo(additionalRenamingsViewer.getTable());

			Collection<ITypeRenaming> available = getRefactoring().getExtraRenamingsComputer().getExtraRenamings(new NullProgressMonitor());
			final ITypeRenaming[] availableArr = available.toArray(new ITypeRenaming[available.size()]);
			getRefactoring().setChosenAdditionalRenamings(availableArr); //All are chosen initially
			additionalRenamingsViewer.setContentProvider(new GeneralPurposeContentProvider());
			additionalRenamingsViewer.setLabelProvider(new RenamingsLabelProvider());
			additionalRenamingsViewer.setInput(available);
			additionalRenamingsViewer.setCheckedElements(availableArr);
			additionalRenamingsViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					Object[] checked = additionalRenamingsViewer.getCheckedElements();
					ITypeRenaming[] chosen = new ITypeRenaming[checked.length];
					for (int i = 0; i < chosen.length; i++) {
						chosen[i] = (ITypeRenaming) checked[i];
					}
					getRefactoring().setChosenAdditionalRenamings(chosen);
					updateServiceRefsEnablement();
					updatePreviewForcing();
				}
			});
		}
		
		private void updateServiceRefsEnablement() {
			GrailsTypeRenameRefactoring r = getRefactoring();
			if (updateServiceRefs!=null) {
				updateServiceRefs.setEnabled(r.isServiceRenaming());
			}
		}
		
		private void createUpdateServiceReferencesCheckbox(Composite parent) {
			updateServiceRefs = new Button(parent, SWT.CHECK);
			updateServiceRefs.setText("Rename service fields and references (forces preview)");
			updateServiceRefs.setToolTipText("Rename fields that have the same name as a renamed service class");
			updateServiceRefs.setSelection(getRefactoring().getUpdateServiceRefs());
			updateServiceRefs.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setUpdateServiceRefs(updateServiceRefs.getSelection());
				}
			});
		}

		private void setUpdateServiceRefs(boolean newValue) {
			GrailsTypeRenameRefactoring r = getRefactoring();
			if (r.getUpdateServiceRefs()!=newValue) {
				r.setUpdateServiceRefs(newValue);
				updatePreviewForcing();
			} 
		}

		private void setUpdateGSPs(boolean newValue) {
			GrailsTypeRenameRefactoring r = getRefactoring();
			if (r.getUpdateGSPs()!=newValue) {
				r.setUpdateGSPs(newValue);
				updatePreviewForcing();
			} 
		}
		
		private void createUpdateGSPCheckbox(Composite parent) {
			updateGSPs = new Button(parent, SWT.CHECK);
			updateGSPs.setText("Update GSPs (forces preview)");
			updateGSPs.setToolTipText("Rename fields that have the same name as a renamed service class");
			updateGSPs.setSelection(getRefactoring().getUpdateGSPs());
			updateGSPs.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setUpdateGSPs(updateGSPs.getSelection());
				}
			});
		}

		
	}
    protected GrailsTypeRenameRefactoring getMyRefactoring() {
    	//This method has a funny name because the super one is final *sigh*
        return (GrailsTypeRenameRefactoring) super.getRefactoring();
    }
	
    /**
     * Updates the enabled state of 'forcePreview' for the wizard, based on refactoring options.
     */
    private void updatePreviewForcing() {
        GrailsTypeRenameRefactoring r = getMyRefactoring();
        boolean shouldForce = r.shouldForcePreview();
        setForcePreviewReview(shouldForce);
    }

	
	/**
	 * @param refactoring
	 * @param flags
	 */
	public GrailsTypeRenameWizard(GrailsTypeRenameRefactoring refactoring) {
		super(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
		setDefaultPageTitle("Rename Grails Type: "+refactoring.getTarget().getFullyQualifiedName());
		setDefaultPageImageDescriptor(GrailsUiActivator.getImageDescriptor("icons/full/wizban/grails_wizban.png"));
	//	setForcePreviewReview(true);
		updatePreviewForcing();
	}

	@Override
	protected void addUserInputPages() {
		addPage(new NewNamePage());
	}

}
