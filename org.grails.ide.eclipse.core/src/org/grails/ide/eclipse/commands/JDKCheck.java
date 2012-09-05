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
package org.grails.ide.eclipse.commands;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * Performs a "JDK versus JRE" check that is executed before Grails command executions. If problem is detected
 * (JRE is used) it shows a message warning the user before executing the command.
 * 
 * Addresses STS-796: "Grails run command and dependency calculation requires JDK"
 * 
 * @since 2.6
 * @author Kris De Volder
 */
public class JDKCheck {

	private static final String CHECK_DISABLED_PREFERENCE = JDKCheck.class.getName()+".disable";
	private static final boolean CHECK_DISABLED_DEFAULT = false;
	
	private static IEclipsePreferences prefs() {
		return InstanceScope.INSTANCE.getNode(GrailsCoreActivator.PLUGIN_ID);
	}
	
	/**
	 * The implementation that opens up the dialog belongs in the UI plugin, so we make this
	 * an interface here.
	 */
	public static interface IJDKCheckMessageDialog {
		
		/**
		 * Open a dialog with a message warning that only JRE is being used.
		 * @param project The project assocuated with executed command, may be null for commands 
		 * not associated with a project.
		 * @return 
		 */
		boolean openMessage(IProject project);
	}

	/**
	 * Grails UI plugin provides the real implementation of this!
	 * <p>
	 * This dummy implementation, is here instead of a null pointer and gets used only in case we forget 
	 * to initialise this, or get message before UI is initialised.
	 */
	public static IJDKCheckMessageDialog ui = new IJDKCheckMessageDialog() {
		public boolean openMessage(IProject project) {
			return true;
		}
	};
	
	public static void setDisabled(boolean isDisabled) {
		prefs().putBoolean(CHECK_DISABLED_PREFERENCE, isDisabled);
	}
	
	public static boolean isDisabled() {
		return prefs().getBoolean(CHECK_DISABLED_PREFERENCE, CHECK_DISABLED_DEFAULT);
	}
	
	public static boolean isEnabled() {
		boolean result = !isDisabled();
		return result;
	}

	/**
	 * If dialog is enabled, checks project and shows dialog if problem with JRE is detected. 
	 * @return true if command should proceed (no problem, or user chooses to proceed despite warning).
	 */
	public static boolean check(final IProject project) {
		try {
			if (isEnabled() && hasTheJREProblem(project)) {
				return ui.openMessage(project);
			}
		} catch (ThreadDeath e) {
			throw e;
		} catch (Throwable e) {
			setDisabled(true); // disable this code if it is throwing unexpected exceptions.
			GrailsCoreActivator.log(e);
		}
		return true;
	}

	/**
	 * Check if the JVM associated with given project is a JRE (which is a problem). 
	 * <p>
	 * This method is conservative and returns 'false' (meaning 'no problem') if there is some problem/uncertainty
	 * determining whether the JVM is a JRE. This is to avoid spurious popups if there is some problem
	 * other than the very specific case we are trying to check for.
	 * 
	 * @param project  Project associated with command that is being executed, or null (for commands not
	 * associated with a project, such as "create-app", in this case we check the default VM install.
	 */
	private static boolean hasTheJREProblem(IProject project) {
		String os = System.getProperty("os.name");
		try {
			if (os!=null) {
				if (os.startsWith("Windows") || os.equals("Linux")) {
					//Note: See http://lopica.sourceforge.net/os.html (list of OS names)
					//Only consider OS's where we are relatively confident that the check is valid
					IVMInstall jvm = getVM(project);
					if (jvm!=null) {
						File javaHome = jvm.getInstallLocation();
						if (javaHome!=null && javaHome.exists()) {
							File toolsJar = new File(new File(javaHome, "lib"), "tools.jar");
							return !toolsJar.exists();
						}
					}
				}
			}
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
		return false; // Conservatively assume check for 'not a JRE' is passing if unknown OS, or Mac OS, or something went wrong,
				 // or Java home doesn't exist  etc.
	}

	private static IVMInstall getVM(IProject project) throws CoreException {
		if (project==null) {
			return JavaRuntime.getDefaultVMInstall();
		} else {
			return JavaRuntime.getVMInstall(JavaCore.create(project));
		}
	}

}
