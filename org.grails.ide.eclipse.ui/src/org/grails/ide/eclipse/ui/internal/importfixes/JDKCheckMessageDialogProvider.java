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
package org.grails.ide.eclipse.ui.internal.importfixes;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.grails.ide.eclipse.commands.JDKCheck;
import org.grails.ide.eclipse.commands.JDKCheck.IJDKCheckMessageDialog;

import org.grails.ide.eclipse.ui.internal.utils.Answer;

/**
 * This class provides the UI associated with the {@link JDKCheck} class in GrailsCore.
 * 
 * @since 2.6
 * @author Kris De Volder
 */
public class JDKCheckMessageDialogProvider implements IJDKCheckMessageDialog {
	
	public boolean openMessage(final IProject project) {
		final Answer<Boolean> answer = new Answer<Boolean>(true);
		Display.getDefault().syncExec(new Runnable() {
			//Note: *must* use synchExec, not asynchExec. We have to be sure that the dialog's answer
			//  has been put into the answer.value before we return the answer.value.
			public void run() {
				MessageDialogWithToggle result = MessageDialogWithToggle.openOkCancelConfirm(null, "Grails requires a JDK",
						"Grails requires a JDK. Trying to run grails commands with only a " +
						"JRE may fail with cryptic error messages, or no error messages at all.\n" +
						"\n" +
						projectInfo(project),
						"Don't show this again",
						JDKCheck.isDisabled(),
						null, null);
				JDKCheck.setDisabled(result.getToggleState());
				answer.value = result.getReturnCode() == Window.OK;
			}

			private String projectInfo(IProject project) {
				if (project!=null) {
					return "The project "+project.getName()+" appears to be using a JRE.\n"+
					"\n" +
					"Please ensure that both:\n"+
					"  - a JDK is configured in your workspace.\n"+
					"  - project '"+project.getName()+" is using it.";
				} else {
					return "The default JVM configured in your workspace appears to be a JRE (no 'tools.jar').\n"+
					"\n" +
					"Please ensure that a JDK is configured as the default JVM in your workspace.";
				}
			}
		});
		return answer.value;
	}
	
}
