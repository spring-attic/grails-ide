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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import org.grails.ide.eclipse.refactoring.preferences.GrailsRefactoringPreferences;
import org.grails.ide.eclipse.refactoring.rename.type.ITypeRenaming;
import org.grails.ide.eclipse.refactoring.rename.type.TypeRenaming;
import org.grails.ide.eclipse.refactoring.rename.type.GrailsTypeRenameParticipant.IGrailsTypeRenameConfigurer;

/**
 * Dialog GUI for configuring Grails type rename participant when it is used in the
 * context of a standard JDT rename refactoring (so we aren't able to use our own
 * refactoring and its customised wizard.
 * 
 * This is just a dummy implementation which pops up a warning that the refactoring
 * is not Grails aware and then disables any Grails related options.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class DialogBasedGrailsTypeRenameConfigurer implements IGrailsTypeRenameConfigurer {
	
	private static final Collection<ITypeRenaming> NO_RENAMINGS = Arrays.asList(new ITypeRenaming[0]);

//	public Collection<ITypeRenaming> chooseAdditionalRenamings(ITypeRenaming orgRen, Collection<ITypeRenaming> renamings, RefactoringStatus status) {
//		Shell shell = getShell();
//		if (shell==null) {
//			GrailsCoreActivator.log("Could not get a shell => could not ask for user input => Not renaming Grails related types");
//			return new ArrayList<ITypeRenaming>();
//		}
//		
//		String message = message(orgRen);
//		
//		final ListSelectionDialog dialog = new ListSelectionDialog(shell, renamings, new GeneralPurposeContentProvider(), new RenamingsLabelProvider(), message);
//		dialog.setTitle("Rename related Grails elements?");
//		dialog.setInitialElementSelections(new ArrayList<ITypeRenaming>(renamings));
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				dialog.open();
//			}
//		});
//		Collection<ITypeRenaming> selected = new ArrayList<ITypeRenaming>();
//		switch (dialog.getReturnCode()) {
//		case ListSelectionDialog.OK:
//			Object[] selectedObjs = dialog.getResult();
//			for (Object o : selectedObjs) {
//				selected.add((TypeRenaming) o);
//			}
//			break;
//		case ListSelectionDialog.CANCEL:
//		default:
//			status.addFatalError("User canceled operation");
//		}
//		return selected;
//	}

//	public static String message(ITypeRenaming orgRen) {
//		return "You are renaming '"+orgRen.getTarget().getFullyQualifiedName()+"' to '"+orgRen.getNewName()+"'.\n" +
//		"Grails naming conventions suggest also renaming related elements.";
//	}
//	
	public static Shell getShell() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb!=null) {
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			if (win!=null) {
				return win.getShell();
			}
		}
		
		//If the above failed, try another method of obtaining a shell.
		final Shell[] shell = new Shell[1];
		final Display display = Display.getDefault();
		display.syncExec(new Runnable() {
			public void run() {
				shell[0] = display.getActiveShell();
			}
		});
		return shell[0];
	}

	public boolean updateServiceReferences() {
		return false;
	}

	public boolean updateGSPs() {
		return false;
	}

	public Collection<ITypeRenaming> chooseAdditionalRenamings(ITypeRenaming orgType, Collection<ITypeRenaming> values, final RefactoringStatus status) {
		final boolean suppress = GrailsRefactoringPreferences.getSuppressGrailsAwareWArning();
		if (!suppress) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					MessageDialogWithToggle result = MessageDialogWithToggle.openOkCancelConfirm(getShell(), "Warning: Refactoring is not Grails Aware!", 
							"You are renaming a Grails type but the invoked refactoring is not 'Grails aware'.\n\n" +
							"To perform a Grails aware refactoring, click 'Cancel' and then trigger the refactoring " +
							"from the Groovy editor, or the Grails Project Explorer.\n\n" +
							"To proceed without Grails awareness, click 'OK'",
							"Don't show this again", suppress, null, null);
					GrailsRefactoringPreferences.setSuppressNonGrailsAwareWarning(result.getToggleState());
					if (result.getReturnCode()==MessageDialogWithToggle.CANCEL) {
						//Adding a fatal error to refactoring status seems to be the only way that participants can really force a refactoring to be canceled.
						status.addFatalError("Non Grails aware refactoring was canceled by the user.");
					}
				}
			});
		} else {
			status.addWarning("Warning: This refactoring is not Grails Aware!");
		}
		return NO_RENAMINGS;
	}

}
