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
package org.grails.ide.eclipse.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsExecutor;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.junit.GrailsTestKindRegistry;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.IGrailsCommandListener;
import org.grails.ide.eclipse.core.model.IGrailsCommandResourceChangeListener;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.springsource.ide.eclipse.commons.core.FileUtil;

/**
 * @author Christian Dupuis
 * @author Andy Clement
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsCoreActivator extends Plugin {
	

	private class GroovyResourceChangeListener implements IResourceChangeListener {

		protected class GroovyResourceVisitor implements IResourceDeltaVisitor {

			protected boolean resourceAdded(IResource resource) {
				if (resource instanceof IFile) {
					for (IGrailsCommandResourceChangeListener listener : commandResourceListeners) {
						if (listener.supports(resource.getProject())) {
							listener.newResource(resource);
						}
					}
				}
				return true;
			}

			protected boolean resourceChanged(IResource resource, int flags) {
				if (resource instanceof IFile) {
					for (IGrailsCommandResourceChangeListener listener : commandResourceListeners) {
						if (listener.supports(resource.getProject())) {
							listener.changedResource(resource);
						}
					}
				}
				return true;
			}

			public final boolean visit(IResourceDelta delta) throws CoreException {
				IResource resource = delta.getResource();
				if (resource != null) {
					switch (delta.getKind()) {
					case IResourceDelta.ADDED:
						return resourceAdded(resource);
					case IResourceDelta.CHANGED:
						return resourceChanged(resource, delta.getFlags());
					}
				}
				return false;
			}

		}

		private static final int VISITOR_FLAGS = IResourceDelta.ADDED | IResourceDelta.CHANGED;

		protected IResourceDeltaVisitor getVisitor() {
			return new GroovyResourceVisitor();
		}

		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getSource() instanceof IWorkspace) {
				int eventType = event.getType();
				switch (eventType) {
				case IResourceChangeEvent.POST_CHANGE:
					IResourceDelta delta = event.getDelta();
					if (delta != null) {
						try {
							delta.accept(getVisitor(), VISITOR_FLAGS);
						}
						catch (CoreException e) {
							log("Error while traversing resource change delta", e);
						}
					}
					break;
				}
			}
			else if (event.getSource() instanceof IProject) {
				int eventType = event.getType();
				switch (eventType) {
				case IResourceChangeEvent.POST_CHANGE:
					IResourceDelta delta = event.getDelta();
					if (delta != null) {
						try {
							delta.accept(getVisitor(), VISITOR_FLAGS);
						}
						catch (CoreException e) {
							log("Error while traversing resource change delta", e);
						}
					}
					break;
				}
			}

		}

	}

	// The plug-in ID
	public static final String PLUGIN_ID = "org.grails.ide.eclipse.core";
	public static final String OLD_PLUGIN_ID = "com.springsource.sts.grails.core";

	private static GrailsCoreActivator plugin;
	
	public static final String GRAILS_INSTALL_PROPERTY = PLUGIN_ID + ".install.name";

	public static final String GRAILS_LAUNCH_SYSTEM_PROPERTIES = PLUGIN_ID + ".launch.properties";

	/** The identifier for enablement of project versus workspace settings */
	public static final String PROJECT_PROPERTY_ID = "use.default.install";

	private static final String GRAILS_COMMAND_TIMEOUT_PREFERENCE = PLUGIN_ID + ".COMMAND_TIMEOUT";

	public static final String PATH_VARIABLE_NAME = "GRAILS_ROOT";
	private static final String KEEP_RUNNING_PREFERENCE = PLUGIN_ID + ".KEEP_RUNNING";
	
	public static final boolean DEFAULT_KEEP_RUNNING_PREFERENCE = true; //From now on this will be on by default
	private static final String GRAILS_COMMAND_OUTPUT_LIMIT_PREFERENCE = PLUGIN_ID+".OUTPUT_LIMIT";

	private static final int DEFAULT_GRAILS_COMMAND_OUTPUT_LIMIT_PREFERENCE = 200000;;
	private static final String CLEAN_OUTPUT_PREFERENCE = PLUGIN_ID+".cleanOutput";

	public static final boolean DEFAULT_CLEAN_OUTPUT_PREFERENCE = true;

	public static IStatus createErrorStatus(String message, Throwable exception) {
		if (message == null) {
			message = "";
		}
		return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception);
	}

	public static IStatus createWarningStatus(String message, Throwable exception) {
		if (message == null) {
			message = "";
		}
		return new Status(IStatus.WARNING, PLUGIN_ID, 0, message, exception);
	}

	public static GrailsCoreActivator getDefault() {
		return plugin;
	}

	public static void log(IStatus status) {
		if (logger == null) {
			getDefault().getLog().log(status);
		}
		else {
			logger.logEntry(status);
		}
	}

	public static void log(String string) {
		try {
			throw new Error(string);
		} catch (Error e) {
			log(e);
		}
	}
	public static void log(String message, Throwable exception) {
		IStatus status = createErrorStatus(message, exception);
		log(status);
	}

	public static void log(Throwable exception) {
		log(createErrorStatus("Internal Error", exception));
	}

	public static void logWarning(String message, Throwable exception) {
		log(GrailsCoreActivator.createWarningStatus(message, exception));
	}

	public static void setLogger(ILogger logger) {
		GrailsCoreActivator.logger = logger;
	}

	public static void testMode(boolean onBuildSite) {
		// test flag used by Grails stuff to know if we are in test mode.
		testMode = true;
		isOnBuildSite = onBuildSite;
	}

	public static void trace(String message) {
		if (logger != null) {
			logger.logEntry(new Status(IStatus.OK, PLUGIN_ID, message));
		}
	}

	private GrailsInstallManager installManager;

	private GroovyResourceChangeListener resourceChangeListener = new GroovyResourceChangeListener();

	private List<IGrailsCommandResourceChangeListener> commandResourceListeners = new CopyOnWriteArrayList<IGrailsCommandResourceChangeListener>();

	private List<IGrailsCommandListener> commandListeners = new CopyOnWriteArrayList<IGrailsCommandListener>();

	private static ILogger logger;

	public static boolean testMode = false; // Set to true while running 'core' tests.

	public static boolean isOnBuildSite; // Should be set to true when running on the build site

	/**
	 * During testing, we use a 'fake' home for grails command execution to avoid state sharing with concurrent test builds.
	 */
	private File fakeUserHome;
    public static final String GRAILS_RESOURCES_PLUGIN_ID = "org.grails.ide.eclipse.resources";

	public void addGrailsCommandListener(IGrailsCommandListener listener) {
		commandListeners.add(listener);
	}

	public void addGrailsCommandResourceListener(IGrailsCommandResourceChangeListener listener) {
		commandResourceListeners.add(listener);
	}

	/**
     * Replace all '\u001D' (group separator) with '=' 
     * and '\u001E' (unit separator) with ','
     * 
     * @param key
     * @return
     */
    private String decode(String key) {
        key = key.replace('\u001D', '=');
        key = key.replace('\u001E', ',');
        return key;
    }

	/**
	 * Replace all '=' with '\u001D' (group separator) 
	 * and ',' with '\u001E' (unit separator)
	 * 
     * @param key
     * @return
     */
    private String encode(String key) {
        key = key.replace('=', '\u001D');
        key = key.replace(',', '\u001E');
        return key;
    }

	public boolean getCleanOutput() {
		return getPreferences().getBoolean(CLEAN_OUTPUT_PREFERENCE, DEFAULT_CLEAN_OUTPUT_PREFERENCE);
	}

	/**
	 * @return The upper bound, in number of characters of Grails command output that is kept for any 
	 * grails command.
	 */
	public int getGrailsCommandOutputLimit() {
		return getPreferences().getInt(GRAILS_COMMAND_OUTPUT_LIMIT_PREFERENCE, DEFAULT_GRAILS_COMMAND_OUTPUT_LIMIT_PREFERENCE);
	}

	public int getGrailsCommandTimeOut() {
		return getPreferences().getInt(GRAILS_COMMAND_TIMEOUT_PREFERENCE,
				GrailsCommand.DEFAULT_TIMEOUT);
	}

	public GrailsInstallManager getInstallManager() {
		if (installManager==null) {
			GrailsInstallManager newManager = new GrailsInstallManager();
			newManager.start();
			installManager = newManager;
		}
		return installManager;
	}

	public boolean getKeepRunning() {
		return getPreferences().getBoolean(KEEP_RUNNING_PREFERENCE, DEFAULT_KEEP_RUNNING_PREFERENCE);
	}

	/**
	 * Get system properties that should be passed to any Grails command. This includes all the
	 * user supplied props plus some additional ones that may need to be added automatically.
	 */
	public Map<String, String> getLaunchSystemProperties() {
		Map<String, String> props = getUserSupliedLaunchSystemProperties();
		if (GrailsCoreActivator.testMode) {
			GrailsLaunchArgumentUtils.addProxyProperties(props);
			try {
				props.put("user.home", getUserHome().getCanonicalPath());
			} catch (IOException e) {
				GrailsCoreActivator.log(e);
			}
		}
		//if (isWindows()) {//STS-2552: jline UnixTerminal causes grails and STS to be 'suspended' on Linux systems.
							// This is because Unix job control will suspend background jobs that try to access the terminal.
							// So to avoid this problem we now do this for any OS not just Windows.
		
			//If we don't set this property on windows then Jline will
			//instantiate "WindowsConsole" based on "os.name" property, 
			//but the "WindowsConsole doesn't work well when inside 
			//the Eclipse console UI.
			if (!props.containsKey("jline.terminal")) { // don't overwrite if user defined it.
				props.put("jline.terminal", "jline.UnsupportedTerminal");
			}
		//}
		return props;
	}
	
	private IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
	}
	
	/**
	 * Get the 'fake' 'user.home' used by grails commands during testing.
	 */
	public File getUserHome() {
		if (!testMode) {
			return new File(System.getProperty("user.home")); 
		} else {
			try {
				if (fakeUserHome==null) {
					fakeUserHome = FileUtil.createTempDirectory("fakeHome");
				}
			} catch (IOException e) {
				fakeUserHome = new File(System.getProperty("user.home")); 
			}
			return fakeUserHome;
		}
	}

	/**
	 * Get the system properties required for a launching of a grails command, as set
	 * by the user on the Grails >> Launch preferences page.
	 * <p>
	 * Note additional properties may be set automatically by Grails, this map contains
	 * only the ones specified by the user.
	 * 
	 * @return Map of user supplied system properties to be set on any grails command's execution
	 */
	public Map<String, String> getUserSupliedLaunchSystemProperties() {
	    String str = getPreferences().get(GRAILS_LAUNCH_SYSTEM_PROPERTIES, "");
	    String[] props = str.split(",");
	    Map<String, String> propsMap = new HashMap<String, String>(props.length*2);
	    for (String prop : props) {
            String[] nameValue = prop.split("=");
            if (nameValue.length == 2) {
                propsMap.put(decode(nameValue[0]), decode(nameValue[1]));
            }
        }
	    return propsMap;
	}
	
	private boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("windows");
	}

	public void notifyCommandFinish(IProject project) {
		if (project != null) {
			for (IGrailsCommandResourceChangeListener listener : commandResourceListeners) {
				if (listener.supports(project)) {
					listener.finish();
				}
			}
			for (IGrailsCommandListener listener : commandListeners) {
				if (listener.supports(project)) {
					listener.finish();
				}
			}
		}
	}

	public void notifyCommandStart(IProject project) {
		if (project != null) {
			for (IGrailsCommandResourceChangeListener listener : commandResourceListeners) {
				if (listener.supports(project)) {
					listener.start();
				}
			}
			for (IGrailsCommandListener listener : commandListeners) {
				if (listener.supports(project)) {
					listener.start();
				}
			}
		}
	}
	
	private void putBooleanPref(String key, boolean value) {
		IEclipsePreferences prefs = getPreferences();
		prefs.putBoolean(key, value);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			GrailsCoreActivator.log(e);
		}
	}
	
	private void putIntPref(String key, int value) {
		IEclipsePreferences prefs = getPreferences();
		prefs.putInt(key, value);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			GrailsCoreActivator.log(e);
		}
	}

	public void removeGrailsCommandListener(IGrailsCommandListener listener) {
		commandListeners.remove(listener);
	}

	public void removeGrailsCommandResourceListener(IGrailsCommandResourceChangeListener listener) {
		commandResourceListeners.remove(listener);
	}
	
	public void setCleanOutput(boolean cleanOutput) {
		putBooleanPref(CLEAN_OUTPUT_PREFERENCE, cleanOutput);
	}

    public void setGrailsCommandOutputLimit(int maxCharacterCount) {
		Assert.isLegal(maxCharacterCount>0);
		int orgValue = getGrailsCommandOutputLimit();
		if (orgValue!=maxCharacterCount) {
			putIntPref(GRAILS_COMMAND_OUTPUT_LIMIT_PREFERENCE, maxCharacterCount);
		}
	}
    
    public void setGrailsCommandTimeOut(int timeOutValue) {
		putIntPref(GRAILS_COMMAND_TIMEOUT_PREFERENCE, timeOutValue);
	}

	public void setKeepGrailsRunning(boolean newValue) {
		boolean orgValue = getKeepRunning();
		if (newValue!=orgValue) {
			putBooleanPref(KEEP_RUNNING_PREFERENCE, newValue);
			GrailsExecutor.shutDownIfNeeded(); //force new executor creation next time
		}
	}
    

	/**
	 * Set the system properties required for launching of a grails command, as set
	 * by the user on the Grails >> Launch preferences page.
	 */
	public void setUserSupliedLaunchSystemProperties(Map<String, String> props) {
	    StringBuilder sb = new StringBuilder();
	    for (Entry<String,String> entry : props.entrySet()) {
	        sb.append(encode(entry.getKey()) + "=" + encode(entry.getValue()) + ",");
        }
	    // remove trailing ,
	    if (sb.length() > 0) {
	        sb.replace(sb.length()-1, sb.length(), "");
	    }
	    IEclipsePreferences node = getPreferences();
        node.put(GRAILS_LAUNCH_SYSTEM_PROPERTIES, sb.toString());
        try {
            node.flush();
        } catch (BackingStoreException e) {
            log(e);
        }
        // For system properties to propagate to external process, it must be restarted.
        LongRunningProcessGrailsExecutor.shutDownIfNeeded();
	}

    /// Testing mode code only....
    
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		GrailsCore.get().initialize();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
		GrailsClasspathUtils.createPathVariableIfRequired();
		Bundle refactoringBundle = Platform.getBundle("org.grails.ide.eclipse.refactoring");
		if (refactoringBundle!=null) {
			refactoringBundle.start();
		}
		GrailsTestKindRegistry.initialize();
	}

	public void stop(BundleContext context) throws Exception {
		GrailsExecutor.shutDownIfNeeded();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		plugin = null;
		GrailsCore.get().dispose();
		super.stop(context);
	}


}
