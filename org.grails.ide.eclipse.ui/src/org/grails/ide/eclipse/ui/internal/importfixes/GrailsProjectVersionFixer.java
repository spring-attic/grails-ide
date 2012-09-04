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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.eclipse.GroovyPlugin;
import org.codehaus.groovy.eclipse.core.preferences.PreferenceConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.IgnoredProjectsList;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainerUpdateJob;
import org.grails.ide.eclipse.core.internal.model.GrailsInstallWorkspaceConfigurator;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstallListener;
import org.grails.ide.eclipse.ui.internal.properties.GrailsInstallPropertyPage;
import org.grails.ide.eclipse.ui.internal.properties.GrailsInstallPropertyPage.IProjectInstallListener;
import org.grails.ide.eclipse.ui.internal.utils.Answer;
import org.springsource.ide.eclipse.commons.frameworks.core.legacyconversion.LegacyProjectConverter;

/**
 * This class provides listeners that detect problems in Grails projects because of
 * a discrepancy between their application.properties "grails version" and the
 * version of the Grails install the project is configured to use in STS.
 * <p>
 * The most common case it targets is the case where
 * a user imported an existing project into Eclipse. The project may have been
 * created inside or outside Eclipse with older versions of Grails. 
 * <p>
 * Another case it covers is discrepancies that arise because a user changes the
 * default grails install of their workspace. This may cause problems for projects
 * that implicitly use the default install.
 * <p>
 * Another case it covers is a discrepancy arrissing because a user changes the
 * project specific settings of a project to use a specific grails install.
 * <p> 
 * Whenever a problem is detected, appropriate action should be taken by the fixer 
 * based on the project's state and/or grails version, to help the user as much as 
 * possible to get this project correctly configured in the workspace.
 * @author Kris De Volder
 * @author Andy Clement
 * @since 2.5.2
 */
public class GrailsProjectVersionFixer {
	
	//TODO: The GrailsProjectVersionFixer contains a 'new grails project listener which may be useful as 
	// a reusable component to which parties interested in the appearance of new grails projects could
	// subscribe. It already has two clients (this version fixer and also the output folder fixer).

	public static boolean DEBUG = false;

	/**
	 * For testing purposes. If the value is set to a non-null value this value will be
	 * used to automatically answer questions rather than popup a dialog.
	 */
	public static Boolean globalAskToUpgradeAnswer = null;
	public static Boolean globalAskToConfigureAnswer = null;
	public static Boolean globalAskToConvertToGrailsProjectAnswer = null;
	
	/**
	 * For testing purposes. Some tests may not want the version fixer to interfere with changing
	 * versions in projects or the workspace at all, this flag is there so those tests can
	 * temporarily disable the fixer completely.
	 */
	private static boolean isEnabled = true;
	
	/**
	 * Listener that detects new projects in the workspace and fixes them.
	 */
	private IResourceChangeListener newProjectListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			//		debug(""+event);
			int type = event.getType();
			switch (type) {
			case IResourceChangeEvent.POST_CHANGE:
				IResourceDelta delta = event.getDelta();

				//Note: getAffected children cannot be used to get "open" events. Probably need to
				//  use visitor style if we want to respond to projects being opened in the workspace.
				new ProjectChangeHandler().projectsChanged(delta.getAffectedChildren());
				break;
			default:
				//			debug("evenType = "+type);
				//Not interesting
				break;
			}
		}
	};

	/**
	 * Listener that reacts to changes to the default grails install (when this happens will need to check
	 * projects that may be affected by the change.
	 */
	private IGrailsInstallListener installListener = new IGrailsInstallListener() {
		public void installChanged(Set<IGrailsInstall> installs) {
			if (installListenerEnabled
				&& !GrailsInstallWorkspaceConfigurator.isBusy()) {
				// GIWC.isBusy: avoids bug STS-1819: double migration dialog.
				//  It is 'safe' to ignore chnages to grails installs caused by the workspace configurator because
				//  it only adds new install but this shouldn't affect existing projects (unless they are already broken).
				new ProjectChangeHandler().defaultGrailsVersionChanged();
			}
		}
		
	};
	
	/**
	 * Listener that reacts to changes to project specific settings.
	 */
	private IProjectInstallListener projectInstallListener = new IProjectInstallListener() {
		public void projectInstallChanged(IProject project, boolean useDefault, String installName) {
			new ProjectChangeHandler().fix(project);
		}
	};
	
	/**
	 * To avoid funny interactions between the two listeners, we disable the installListener while processing 
	 * in response to the newProjectListener (reason for the interactions is that newProjectListener may
	 * ask user to configure a grails install which tends to change the default Grails install).
	 */
	private boolean installListenerEnabled = true;
	private GrailsOutputFolderFixer grailsOutputFolderFixer = null;

	public GrailsProjectVersionFixer() {
		//Disable Groovy legacy project conversion, we'll take care of it for grails projects and don't want to
		//have two convert dialog boxes popup at once.
		GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_ASK_TO_CONVERT_LEGACY_PROJECTS, false);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(newProjectListener);
		
		GrailsCoreActivator.getDefault().getInstallManager().addGrailsInstallListener(installListener);
		debug("Added resource change listener: "+newProjectListener);
		
		GrailsInstallPropertyPage.addProjectInstallListener(projectInstallListener);
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(newProjectListener);
		GrailsCoreActivator.getDefault().getInstallManager().removeGrailsInstallListener(installListener);
		debug("Removed resource change listener: "+this);
	}

	/**
	 * Change project settings to make project use a specific grails install.
	 * <p>
	 * Warning to allow this to be called in contexts where workspace locked, this doesn't actually
	 * set the install now, but schedules a job to set it later on.
	 */
	public static void setInstall(final IProject project, final IGrailsInstall install) {
		WorkspaceJob job = new WorkspaceJob("Configure project '"+project.getName()+"' to use Grails "+install.getVersionString()) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
				GrailsCommandUtils.eclipsifyProject(install, install.isDefault(), project);
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}
	
	private class ProjectChangeHandler {
		
		/**
		 * When set will to non-null value will answer any subsequent "upgrade?" questions automatically without
		 * popping up a dialog. This variable's 'scope' is the handling of a single resource change event.
		 * <p>
		 * Thus it will help when user does a "bulk" import of many projects, but will reset at the end of
		 * the import.
		 */
		private Answer<Boolean> askToUpgradeAnswer = new Answer<Boolean>(globalAskToUpgradeAnswer);
		
		/**
		 * When set will to non-null value will answer any subsequent "downgrade?" questions automatically without
		 * popping up a dialog. This variable's 'scope' is the handling of a single resource change event.
		 * <p>
		 * Thus it will help when user does a "bulk" import of many projects, but will reset at the end of
		 * the import.
		 */
		private Answer<Boolean> askToDowngradeAnswer = new Answer<Boolean>(globalAskToUpgradeAnswer);
		
		/**
		 * To avoid asking the user multiple times to install some version of Grails, remember the answers
		 * they gave for a particular install and don't ask them again about the same install.
		 * <p>
		 * These answers are remembered for the duration of a "batch" of project fixes triggered by a single
		 * resource change event, since this is mostly to avoid users having do decline to install some
		 * version of Grails over and over again when importing projects in "bulk" from some old workspace
		 * or a zip archive. 
		 */
		private Map<GrailsVersion, Answer<Boolean> > askToConfigureAnswers = new HashMap<GrailsVersion, Answer<Boolean>>();

		/**
		 * When set will to non-null value will answer any subsequent "conver to Grails project?" questions automatically without
		 * popping up a dialog. This variable's 'scope' is the handling of a single resource change event.
		 * <p>
		 * Thus it will help when user does a "bulk" import of many projects, but will reset at the end of
		 * the import.
		 */
		private Answer<Boolean> askToConvertToGrailsProjectAnswer = new Answer<Boolean>(globalAskToConvertToGrailsProjectAnswer);

		public void defaultGrailsVersionChanged() {
			GrailsVersion defaultVersion = GrailsVersion.getDefault();
			if (defaultVersion!=null) {
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject p : projects) {
					if (defaultVersion.equals(GrailsVersion.getEclipseGrailsVersion(p))) {
						//Only projects that are configured with the default install in eclipse should be considered for fixing
						// (other projects shouldn't be affected by changing the defaul grails version).
						fix(p);
					}
				}
			}
		}

		private void projectsChanged(IResourceDelta[] projectDeltas) {
			boolean saveEnabledState = installListenerEnabled;
			GrailsVersion orgDefault = GrailsVersion.getDefault();
			try {
				installListenerEnabled = false;
				for (IResourceDelta projectDelta : projectDeltas) {
					//debug(""+projectDelta+" kind:"+deltaKindString(projectDelta.getKind()));
					int kind = projectDelta.getKind();
					if (0 != (kind & (IResourceDelta.ADDED | IResourceDelta.CHANGED))) {
						IProject project = (IProject) projectDelta.getResource();
						IResourceDelta[] children = projectDelta.getAffectedChildren(IResourceDelta.ADDED);
						for (IResourceDelta child : children) {
							if (child.getResource().getName().equals("grails-app")) {
								debug("Seeing a new 'grails-app' folder in '"+project+"'");
								grailsOutputFolderFixer.fix(project);
								fix(project);
							}
						}
					}
				}
			} finally {
				installListenerEnabled = saveEnabledState;
				if (installListenerEnabled) {
					// if the default install has changed as result of our handling, we still need to consider the
					// effects of this on our projects, even though the installListener was temporarily disabled
					GrailsVersion currentDefault = GrailsVersion.getDefault();
					if (currentDefault!=null && !currentDefault.equals(orgDefault)) {
						new ProjectChangeHandler().defaultGrailsVersionChanged();
					}
				}
			}
		}

		private void fix(IProject project) {
			if (!IgnoredProjectsList.isIgnorable(project) && isEnabled && GrailsNature.looksLikeGrailsProject(project)) {
				debug("Project to fix? "+project);
				GrailsVersion grailsVersion = GrailsVersion.getGrailsVersion(project);
				debug("grails version = "+grailsVersion);
				GrailsVersion eclipseGrailsVersion = GrailsVersion.getEclipseGrailsVersion(project);
				debug("eclipse grails version = "+eclipseGrailsVersion);
				if (GrailsVersion.UNKNOWN.equals(eclipseGrailsVersion)) {
					//Imported project is configured to use an install not known in this workspace.
					handleUnknownEclipseVersion(project, grailsVersion);
				} else if (grailsVersion.compareTo(GrailsVersion.SMALLEST_SUPPORTED_VERSION)<0) {
					// These grails version aren't supported anymore in STS.
					handleUnsuportedVersion(project, grailsVersion);
				} else if (!grailsVersion.equals(eclipseGrailsVersion)) {
					// The project's grails version is different from the Eclipse STS install associated with the project
					handleMismatchingVersions(project, grailsVersion, eclipseGrailsVersion);
				} else if (!GrailsNature.isGrailsProject(project)) {
					// The project 'looked right' when based on versions, but this is accidental. It doesn't have Grails nature so we 
					// need to convert it to a Grails project.
					handleNoGrailsNature(project, grailsVersion);
				} else if (GrailsNature.hasOldGrailsNature(project)) {
				    // has both the new nature and the old nature
				    handleHasOldGrailsNature(project, grailsVersion);
				} else {
					GrailsClasspathContainerUpdateJob.scheduleClasspathContainerUpdateJob(project, true);
				}
			}
		}

		/**
		 * This method handles the case where a project looks like a Grails project but doesn't
		 * have grails nature. 
		 */
		private void handleNoGrailsNature(final IProject project, GrailsVersion grailsVersion) {
			debug("Grails project without grails nature detected: "+project);
			
			final IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall();
			// We are relying on the normal fixer to run upgrade etc. when the workspace default install doesn't match the declared version.
			// This means we should only be getting here if the following assert is true.
			Assert.isTrue(install.getVersion().equals(grailsVersion));
			boolean convert = askConvertToGrailsProject(project, grailsVersion);
			if (convert) {
				debug("Converting to Grails project");
				WorkspaceJob job = new WorkspaceJob("Convert to Grails project '"+project.getName()+"'") {
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						//This case is also where we will end up if we are importing a legacy Grails project
						//I.e. the project has a 'old' Grails nature, but not a new one, so looks like a 'no grails nature' project.
						monitor.beginTask("Converting to Grails project", 2);
						try {
							performLegacyConversion(project, new SubProgressMonitor(monitor, 1));
							GrailsCommandUtils.eclipsifyProject(install, install.isDefault(), project);
							monitor.worked(1);
							return Status.OK_STATUS;
						} finally {
							monitor.done();
						}
					}
				};
				job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
				job.setPriority(Job.INTERACTIVE);
				job.schedule();
			}
		}

		private void performLegacyConversion(IProject project, IProgressMonitor monitor) {
			if (GrailsNature.hasOldGrailsNature(project)) {
				LegacyProjectConverter converter = new LegacyProjectConverter(Collections.singletonList(project));
				converter.setSelectedLegacyProjects(new IProject[] {project});
				converter.convert(monitor);
			}
		}

		/**
		 * This method handles the case where a project has both the old and the new grails nature
		 */
		private void handleHasOldGrailsNature(final IProject project, GrailsVersion grailsVersion) {
		    debug("Grails project with old and new nature detected: "+project);
		    final IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall();
		    // We are relying on the normal fixer to run upgrade etc. when the workspace default install doesn't match the declared version.
		    // This means we should only be getting here if the following assert is true.
		    Assert.isTrue(install.getVersion().equals(grailsVersion));
		    boolean convert = askConvertToGrailsProject(project, grailsVersion);
		    if (convert) {
		        debug("Removing old grails nature");
		        WorkspaceJob job = new WorkspaceJob("Removing legacy Grails nature on project '"+project.getName()+"'") {
		            @Override
		            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		                GrailsCommandUtils.eclipsifyProject(install, install.isDefault(), project);
		                return Status.OK_STATUS;
		            }
		        };
		        job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		        job.setPriority(Job.INTERACTIVE);
		        job.schedule();
		    }
		}
		
		private void handleMismatchingVersions(final IProject project, GrailsVersion grailsVersion, GrailsVersion eclipseGrailsVersion) {
			final IGrailsInstall matchingInstall = grailsVersion.getInstall();
			if (matchingInstall!=null) {
				debug("Matching grails install exists");
				boolean upgrade = askToUpgradeFromSupportedInstalled(project, grailsVersion, eclipseGrailsVersion.getInstall());
				if (upgrade) {
					debug("Upgrading to newer version");
					upgradeProject(project, grailsVersion, eclipseGrailsVersion.getInstall());
				} else {
					debug("Configuring project to use older version");
					setInstall(project, matchingInstall);
				}
			} else {
				// No matching grails install for this project's grails version
				debug("Matching grails install DOES NOT exist");
				if (grailsVersion.compareTo(eclipseGrailsVersion)<0) {
					// a project upgrade could fix it
					IGrailsInstall newInstall = eclipseGrailsVersion.getInstall();
					boolean upgrade = askToUpgradeFromSupportedNotInstalled(project, grailsVersion, newInstall);
					if (upgrade) {
						debug("upgrading project to newer version");
						upgradeProject(project, grailsVersion, newInstall);
					} else {
						askAndConfigureGrails(project, grailsVersion);
					}
				} else {
					debug("Either there is no Grails install, or the default install is too old for the project");
					askAndConfigureGrails(project, grailsVersion);
				}
			}
		}
		
		public void handleUnknownEclipseVersion(IProject project, GrailsVersion grailsVersion) {
			//Gets called if an imported project has eclipse settings pointing to an unknown install.
			IGrailsInstall install = grailsVersion.getInstall();
			if (install!=null) {
				//Silently try to use the install configured in application.properties
				setInstall(project, install);
			} else {
				GrailsVersion defaultVersion = GrailsVersion.getDefault();
				handleMismatchingVersions(project, grailsVersion, defaultVersion);
			}
		}


		/**
		 * Called when the 'fix' is to configure a given grails version in the workspace and
		 * make the project use that install.
		 */
		private void askAndConfigureGrails(final IProject project,
				GrailsVersion grailsVersion) {
			boolean configureGrails = askToConfigureGrails(project, grailsVersion);
			if (configureGrails) {
				IGrailsInstall configured = configureGrails(grailsVersion);
				while (configured == null && askRetryConfigure(grailsVersion)) {
					configured = configureGrails(grailsVersion);
				}
				if (configured!=null) {
					setInstall(project, grailsVersion.getInstall());
				}
			}
		}

		private boolean askRetryConfigure(GrailsVersion grailsVersion) {
			return yesNoQuestion("Configure Grails "+grailsVersion, 
					"You did not configure a version "+grailsVersion+" install.\n\n"+
					"Do you want to try again?");
		}

		/**
		 * Gets active Shell. Warning this method should only be called from the UI thread.
		 */
		private Shell getShell() {
			return PlatformUI.getWorkbench().getDisplay().getActiveShell();
		}
		
		/**
		 * Opens the dialog box that lets a user define a grails install. Hoping that the user will 
		 * select the right version of Grails. 
		 * 
		 * @return a Grails install of the correct version if the user has configured one, null otherwise.
		 */
		private IGrailsInstall configureGrails(GrailsVersion grailsVersion) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					String id = "org.grails.ide.eclipse.ui.preferencePage";
					PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, Collections.EMPTY_MAP).open();
				}
			});
			return grailsVersion.getInstall();
		}

		/**
		 * Called when we find a Grails project that as a very old version number.
		 */
		private void handleUnsuportedVersion(IProject project, GrailsVersion oldVersion) {
			debug("Unsupported version detected");

			//Two cases: depending on default grails install
			IGrailsInstall defaultInstall = getDefaultInstall();
			if (defaultInstall!=null && GrailsVersion.SMALLEST_SUPPORTED_VERSION.isSatisfiedBy(defaultInstall.getVersionString())) {
				// Case 1: we have an acceptable default grails install... 
				boolean upgrade = askToUpgradeFromUnsupported(project, oldVersion, defaultInstall);
				if (upgrade) {
					debug("Scheduling upgrade job...");
					upgradeProject(project, oldVersion, defaultInstall);
					debug("Scheduling upgrade job DONE");
				}
			} else {
				// Case 2: we don't have an acceptable default grails install... 
				//TODO: implement something to handle this case? 
			}
		}

		private boolean askToUpgradeFromUnsupported(IProject project, GrailsVersion oldVersion, IGrailsInstall newInstall) {
			Assert.isLegal(oldVersion.compareTo(GrailsVersion.SMALLEST_SUPPORTED_VERSION)<0);
			return yesNoAllQuestion("Unsupported Grails version "+oldVersion.getVersionString(), 
					"The project "+project.getName()+" is defined as using Grails "+oldVersion.getVersionString()+".\n"+
					"The STS Grails tools require use of at least version "+GrailsVersion.SMALLEST_SUPPORTED_VERSION.getVersionString()+".\n" +
					"Running a 'grails upgrade' will upgrade the project to use Grails "+newInstall.getVersion()+"\n" +
					"Without an upgrade, the Grails tools will not work very well.\n\n"+
					"Run 'grails upgrade' on project '"+project.getName()+"' now?",
					askToUpgradeAnswer);
		}

		private boolean askToUpgradeFromSupportedNotInstalled(IProject project,
				GrailsVersion oldVersion, IGrailsInstall newInstall) {
			Assert.isLegal(oldVersion.compareTo(GrailsVersion.SMALLEST_SUPPORTED_VERSION)>=0);
			return yesNoAllQuestion("Grails version "+oldVersion.getVersionString()+" not installed.", 
					"The project "+project.getName()+" is defined as using Grails "+oldVersion.getVersionString()+".\n"+
					"No Grails install matching that version is configured in your workspace.\n"+
					"Running a 'grails upgrade' will upgrade the project to use Grails "+newInstall.getVersion()+"\n" +
					"Without an upgrade, you can still use the project if you install Grails "+oldVersion+"\n\n" +
					"Run 'grails upgrade' on project '"+project.getName()+"' now?",
					askToUpgradeAnswer);
		}

		private boolean askToUpgradeFromSupportedInstalled(IProject project,
				GrailsVersion oldVersion, IGrailsInstall newInstall) {
			Assert.isLegal(oldVersion.compareTo(GrailsVersion.SMALLEST_SUPPORTED_VERSION)>=0);
			GrailsVersion newVersion = newInstall.getVersion();
			boolean isDownGrade = oldVersion.compareTo(newVersion)>0;
			String upgradeString = isDownGrade ? "DOWNGRADE" : "upgrade";
			
			boolean usingDefault = newInstall!=null && newInstall.isDefault();
			final String newVersionMsg = 
				usingDefault 
					? "Your workspace default Grails install is version "+newVersion+".\n" 
					: "The install associated with the project is version "+newVersion+".\n";
			
			return yesNoAllQuestion("Grails version mismatch detected.", 
						"The project " + project.getName() + " is defined as using Grails " + oldVersion.getVersionString() + ".\n" + 
						newVersionMsg + 
						"Running a 'grails upgrade' will " + upgradeString + " the project to use " + newVersion + "\n" + 
						"Alternatively, we can configure your project to use Grails " + oldVersion + "\n\n" + 
						"Run 'grails upgrade' on project '" + project.getName() + "' now?", 
						isDownGrade ? askToDowngradeAnswer : askToUpgradeAnswer);
		}

		private boolean askConvertToGrailsProject(IProject project, GrailsVersion grailsVersion) {
			if (GrailsNature.hasOldGrailsNature(project)) {
				//Make the message more appropriate for the migration of  Legacy (pre STS 3.0.0) project
				return yesNoAllQuestion("Migrate Legacy STS Grails Project?",
						"The project "+project.getName()+" looks like a Grails project but...\n"+
						"It is configured for use in an older version of STS.\n"+
						"The project won't compile unless the project is migrated.\n" +
						"\n"+
						"Do you want to migrate the project now?",
						
						askToConvertToGrailsProjectAnswer);
			} else {
				return yesNoAllQuestion("Convert to Grails Project?",
						"The project "+project.getName()+" looks like a Grails project but...\n"+
						"is not configured for use with the STS Grails tools.\n"+
						"To fix this problem, the project needs to be converted to an STS Grails Project.\n" +
						"\n"+
						"Do you want to convert the project now?",
						askToConvertToGrailsProjectAnswer);
			}
		}

		
		/**
		 * Called when user elects to keep using an older grails version for their project,
		 * and this older version isn't currently configured in the workspace.
		 */
		private boolean askToConfigureGrails(IProject project, GrailsVersion oldVersion) {
			Assert.isLegal(oldVersion.compareTo(GrailsVersion.SMALLEST_SUPPORTED_VERSION)>=0, "GrailsVersion: "+oldVersion);
			Answer<Boolean> answer = askToConfigureAnswers.get(oldVersion);
			if (answer==null) {
				answer = new Answer<Boolean>(globalAskToConfigureAnswer);
				askToConfigureAnswers.put(oldVersion, answer);
			}
			if (answer.value!=null) {
				return answer.value;
			}
			answer.value = yesNoQuestion("Configure Grails "+oldVersion+"?", 
					"The project '"+project.getName()+"' requires Grails " + oldVersion +" " +
					"but a corresponding install is not configured in your workspace.\n" +
					"\n" +
					"If you configure the install now, I will also configure your project \n" +
					"'"+project.getName()+"' to use that install.\n" +
					"\n"+ 
					"Do you want to configure a Grails "+oldVersion+" install now?");
			return answer.value;
		}
		
		private void upgradeProject(final IProject project, GrailsVersion oldVersion, final IGrailsInstall newInstall) {
			WorkspaceJob job = new WorkspaceJob("Upgrade project '"+project.getName()+"'") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					monitor.beginTask("Upgrade project", 10);
					try {
						performLegacyConversion(project, new SubProgressMonitor(monitor, 1));
						debug("upgrade job starting...");
						GrailsCommandUtils.upgradeProject(project, newInstall);
						debug("upgrade job finished");
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
			job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
			job.setPriority(Job.INTERACTIVE);
			job.schedule();
		}

		private boolean yesNoAllQuestion(final String title, final String message, final Answer<Boolean> autoAnswer) {
			if (autoAnswer.value!=null) {
				return autoAnswer.value;
			}
			final boolean[] result = new boolean[1]; 
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result[0] = openUpgradeQuestion(title, message, autoAnswer);
				}
			});
			return result[0];
		}

		private boolean yesNoQuestion(final String title, final String message) {
			final Answer<Boolean> result = new Answer<Boolean>(); 
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result.value = MessageDialog.openQuestion(null, title, message);
				}
			});
			return result.value;
		}
		
		protected boolean openUpgradeQuestion(String title, String message, Answer<Boolean> autoAnswer) {
			MessageDialog dialog = new MessageDialog(null, title, null, message,
					MessageDialog.QUESTION, new String[] {  "No to All" , "Yes to All", "No", "Yes" }, 3);
			int button = dialog.open();
			switch (button) {
			case 3: /* yes */
				return true;
			case 2: /* no */
				return false;
			case 1: /* yes to all */
				autoAnswer.value = true;
				return true;
			case 0: /* no to all */
				autoAnswer.value = false;
				return false;
			}
			//Shouldn't reach here ever
			return false;
		}

		private IGrailsInstall getDefaultInstall() {
			return GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall();
		}

	}
	
	


	////////////////////////////////////////////////////
	// Debugging code

	private void debug(String string) {
		if (DEBUG) {
			System.out.println("GrailsImportedProjectFixer: "+string);
		}
	}

	private String deltaKindString(int kind) {
		switch (kind) {
		case IResourceDelta.ADDED:
			return "added";
		case IResourceDelta.OPEN:
			return "open";
		default:	
			return ""+kind;
		}
	}

	/**
	 * Prevents popup dialogs from the fixer, automatically answering 'no' to all questions. 
	 * The fixer is however still active! To completely disable the fixer, use the setEnabled()
	 * method instead.
	 */
	public static void testMode() {
		GrailsProjectVersionFixer.globalAskToUpgradeAnswer = false;
		GrailsProjectVersionFixer.globalAskToConvertToGrailsProjectAnswer = false;
		GrailsProjectVersionFixer.globalAskToConfigureAnswer = false;
	}

	/**
	 * For testing purposes only. Allows tests to disable the fixer to avoid the tests from
	 * being affected by popping up dialogs or making changes to projects in background at
	 * unpredictabled times, making the tests behave eratic.
	 */
	public static void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	/**
	 * For testing purposes only. In production, this should always be true.
	 */
	public static boolean isEnabled() {
		return isEnabled;
	}

	public void setGrailsOutputFolderFixer(GrailsOutputFolderFixer grailsOutputFolderFixer) {
		this.grailsOutputFolderFixer = grailsOutputFolderFixer;
	}

}
