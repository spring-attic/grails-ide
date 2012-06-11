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
package org.grails.ide.eclipse.ui.internal.dialogues;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.swt.widgets.Display;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck.IGroovyCompilerVersionCheckDialog;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.osgi.framework.Version;


/**
 * @author Kris De Volder
 * @since 2.6
 */
public class GroovyCompilerVersionCheckDialogProvider implements IGroovyCompilerVersionCheckDialog {

	public void openMessage(final IProject project, final GrailsVersion grailsVersion, final VersionRange requiredGroovyVersion, final Version actualGroovyVersion) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openWarning(null, "Incompatible Groovy Compiler Version",
						"Some projects in your workpace (e.g. '"+project.getName()+"') use Grails " + grailsVersion + "\n" +
						"You are using the Groovy "+ actualGroovyVersion +" compiler.\n" +
						"Grails requires version range " + requiredGroovyVersion +".\n" +
						"Various functionality may break if you use an incompatible compiler.\n" +
						"See Preferences >> Groovy >> Compiler to change the compiler version.");
			}
		});
	}

}
