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
package org.grails.ide.eclipse.ui.console;

import java.util.LinkedList;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.ConsoleProvider;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.grails.ide.eclipse.longrunning.client.GrailsCommandExecution;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * Provides outputStreams that can be used by non-ui plugins to write stuff into a console view (this assuming that
 * you somehow inject an instance of this class into the core plugin.
 * <p>
 * See {@link GrailsUiActivator} for the code that injects this instance into {@link GrailsProcessManager} in GrailsCore plugin.
 * 
 * @since 2.6
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GrailsUIConsoleProvider extends ConsoleProvider {
	
	/**
	 * Maximum number of old consoles to keep open. If this number is exceeded the oldest console will
	 * be removed from the UI.
	 */
	private static final int MAX_SIZE = 5;

	public static Color getOutputColor() {
		return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR);
	}
	
	public static Color getErrorColor() {
		return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR);
	}
	
	public static Color getInputColor() {
		return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR);
	}
	
	private LinkedList<IOConsole> history = new LinkedList<IOConsole>();

	@Override
	public Console getConsole(String title, GrailsCommandExecution execution) {
		
		final GrailsIOConsole console = new GrailsIOConsole(title, execution);
		final IOConsoleInputStream in  = console.getInputStream();
		final IOConsoleOutputStream out = console.newOutputStream();
		final IOConsoleOutputStream err = console.newOutputStream();
		
		in.setColor(getInputColor());
		out.setColor(getOutputColor());
		err.setColor(getErrorColor());
		
		add(console);
		Console coreConsole = Console.make(in, out, err);
		return coreConsole;
	}

	private void add(IOConsole console) {
		history.addLast(console);
		if (history.size()>MAX_SIZE) {
			close(history.removeFirst());
		}
		console.activate();
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ console });
	}

	private void close(IOConsole console) {
		ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] {console});
	}

}
