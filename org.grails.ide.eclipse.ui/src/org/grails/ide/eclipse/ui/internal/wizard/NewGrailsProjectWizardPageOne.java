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
package org.grails.ide.eclipse.ui.internal.wizard;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstallListener;


/**
 * Largely copied from {@link NewJavaProjectWizardPageOne}
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class NewGrailsProjectWizardPageOne extends WizardPage {

	/**
	 * Request a project name. Fires an event whenever the text field is changed, regardless of its content.
	 */
	private final class NameGroup extends Observable implements IDialogFieldListener {

		protected final StringDialogField fNameField;

		public NameGroup() {
			// text field for project name
			fNameField = new StringDialogField();
			fNameField.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_NameGroup_label_text);
			fNameField.setDialogFieldListener(this);
		}

		public Control createControl(Composite composite) {
			Composite nameComposite = new Composite(composite, SWT.NONE);
			nameComposite.setFont(composite.getFont());
			nameComposite.setLayout(initGridLayout(new GridLayout(2, false), false));

			fNameField.doFillIntoGrid(nameComposite, 2);
			LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));

			return nameComposite;
		}

		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		public String getName() {
			return fNameField.getText().trim();
		}

		public void postSetFocus() {
			fNameField.postSetFocusOnDialogField(getShell().getDisplay());
		}

		public void setName(String name) {
			fNameField.setText(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.
		 * internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			fireEvent();
		}
	}

	private final class GrailsInstallGroup extends Observable {

		private Button useDefault;

		private Button useSpecific;

		private Combo grailsInstallCombo;

		public Control createControl(Composite composite) {
			Group grailsHomeGroup = new Group(composite, SWT.NONE);
			grailsHomeGroup.setFont(composite.getFont());
			grailsHomeGroup.setText("Grails Installation");
			grailsHomeGroup.setLayout(initGridLayout(new GridLayout(1, false), true));

			useDefault = new Button(grailsHomeGroup, SWT.RADIO);

			IGrailsInstall defaultInstall = GrailsCoreActivator.getDefault().getInstallManager()
					.getDefaultGrailsInstall();
			if (defaultInstall != null) {
				useDefault.setText(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_useDefaultGrailsInstallation + defaultInstall.getName() + "')");
			}
			else {
				setErrorMessage(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_noGrailsInstallation);
				setPageComplete(false);
				useDefault.setText(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_useDefaultGrailsInstallationNoCurrent);
			}
			useDefault.setSelection(true);
			useDefault.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					grailsInstallCombo.setEnabled(false);
				}
			});

			useSpecific = new Button(grailsHomeGroup, SWT.RADIO);
			useSpecific.setText("Use project specific Grails installation:");
			useSpecific.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					grailsInstallCombo.setEnabled(true);
				}
			});

			final Composite installComposite = new Composite(grailsHomeGroup, SWT.NULL);
			installComposite.setFont(composite.getFont());
			installComposite.setLayout(new GridLayout(3, false));
			installComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label options = new Label(installComposite, SWT.WRAP);
			options.setText("Install: ");
			options.setLayoutData(new GridData(GridData.BEGINNING));

			grailsInstallCombo = new Combo(installComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
			grailsInstallCombo.setItems(GrailsCoreActivator.getDefault().getInstallManager().getAllInstallNames());
			grailsInstallCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			String[] names = grailsInstallCombo.getItems();
			for (int i = 0; i < names.length; i++) {
				if (GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(names[i]).isDefault()) {
					grailsInstallCombo.select(i);
					break;
				}
			}
			grailsInstallCombo.setEnabled(false);
			grailsHomeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Link link = new Link(installComposite, SWT.NONE);
			link.setFont(composite.getFont());
			link.setText("<A>Configure Grails Installations....</A>"); //$NON-NLS-1$//$NON-NLS-2$
			link.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			link.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					openPreferences();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					openPreferences();
				}
			});

			return grailsHomeGroup;
		}

		public void refresh() {
			IGrailsInstall defaultInstall = GrailsCoreActivator.getDefault().getInstallManager()
					.getDefaultGrailsInstall();
			if (defaultInstall != null) {
				useDefault.setText(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_useDefaultGrailsInstallation + defaultInstall.getName() + "')");
			}
			else {
				setErrorMessage(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_noGrailsInstallation);
				setPageComplete(false);
				useDefault.setText(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_useDefaultGrailsInstallationNoCurrent);
			}
			grailsInstallCombo.setItems(GrailsCoreActivator.getDefault().getInstallManager().getAllInstallNames());
			String[] names = grailsInstallCombo.getItems();
			for (int i = 0; i < names.length; i++) {
				if (GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(names[i]).isDefault()) {
					grailsInstallCombo.select(i);
					break;
				}
			}
			setChanged();
			notifyObservers();
		}

		private void openPreferences() {
			String id = "org.grails.ide.eclipse.ui.preferencePage";
			PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, Collections.EMPTY_MAP).open();
		}

	}

	/**
	 * Request a location. Fires an event whenever the checkbox or the location field is changed, regardless of whether
	 * the change originates from the user or has been invoked programmatically.
	 */
	private final class LocationGroup extends Observable implements Observer, IStringButtonAdapter,
			IDialogFieldListener {

		protected final SelectionButtonDialogField fWorkspaceRadio;

		protected final SelectionButtonDialogField fExternalRadio;

		protected final StringButtonDialogField fLocation;

		private String fPreviousExternalLocation;

		private static final String DIALOGSTORE_LAST_EXTERNAL_LOC = JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$

		public LocationGroup() {
			fWorkspaceRadio = new SelectionButtonDialogField(SWT.RADIO);
			fWorkspaceRadio.setDialogFieldListener(this);
			fWorkspaceRadio.setLabelText("Use &default location");

			fExternalRadio = new SelectionButtonDialogField(SWT.RADIO);
			fExternalRadio.setLabelText("Use &external location");

			fLocation = new StringButtonDialogField(this);
			fLocation.setDialogFieldListener(this);
			fLocation.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LocationGroup_locationLabel_desc);
			fLocation.setButtonLabel(NewWizardMessages.NewJavaProjectWizardPageOne_LocationGroup_browseButton_desc);

			fExternalRadio.attachDialogField(fLocation);

			fWorkspaceRadio.setSelection(true);
			fExternalRadio.setSelection(false);

			fPreviousExternalLocation = ""; //$NON-NLS-1$
		}

		public Control createControl(Composite composite) {
			final int numColumns = 3;

			final Group group = new Group(composite, SWT.NONE);
			group.setLayout(initGridLayout(new GridLayout(numColumns, false), true));
			group.setText("Contents");

			fWorkspaceRadio.doFillIntoGrid(group, numColumns);
			fExternalRadio.doFillIntoGrid(group, numColumns);
			fLocation.doFillIntoGrid(group, numColumns);
			LayoutUtil.setHorizontalGrabbing(fLocation.getTextControl(null));

			return group;
		}

		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		protected String getDefaultPath(String name) {
			final IPath path = Platform.getLocation().append(name);
			return path.toOSString();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
		 */
		public void update(Observable o, Object arg) {
			if (isWorkspaceRadioSelected()) {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		public IPath getLocation() {
			if (isWorkspaceRadioSelected()) {
				return Platform.getLocation();
			}
			return Path.fromOSString(fLocation.getText().trim());
		}

		public boolean isWorkspaceRadioSelected() {
			return fWorkspaceRadio.isSelected();
		}

		/**
		 * Returns <code>true</code> if the location is the default location.
		 * 
		 * @return <code>true</code> if the location is the default location.
		 */
		public boolean isDefaultLocation() {
			IPath projectPath = fLocationGroup.getLocation();
			IPath workspacePath = Platform.getLocation(); 
			return 
				//location nested one level below workspace:
				projectPath.segmentCount()==workspacePath.segmentCount()+1
					&& workspacePath.isPrefixOf(projectPath)
				//or the workpsace path itself (returned when the 'default' location button was selected.
				|| projectPath.equals(workspacePath);
		}

		public void setLocation(IPath path) {
			fWorkspaceRadio.setSelection(path == null);
			if (path != null) {
				fLocation.setText(path.toOSString());
			}
			else {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt
		 * .internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			final DirectoryDialog dialog = new DirectoryDialog(getShell());
			dialog.setMessage(NewWizardMessages.NewJavaProjectWizardPageOne_directory_message);
			String directoryName = fLocation.getText().trim();
			if (directoryName.length() == 0) {
				String prevLocation = JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LAST_EXTERNAL_LOC);
				if (prevLocation != null) {
					directoryName = prevLocation;
				}
			}

			if (directoryName.length() > 0) {
				final File path = new File(directoryName);
				if (path.exists())
					dialog.setFilterPath(directoryName);
			}
			final String selectedDirectory = dialog.open();
			if (selectedDirectory != null) {
				String oldDirectory = new Path(fLocation.getText().trim()).lastSegment();
				fLocation.setText(selectedDirectory);
				String lastSegment = new Path(selectedDirectory).lastSegment();
				if (lastSegment != null
						&& (fNameGroup.getName().length() == 0 || fNameGroup.getName().equals(oldDirectory))) {
					fNameGroup.setName(lastSegment);
				}
				JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.
		 * internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fWorkspaceRadio) {
				final boolean checked = fWorkspaceRadio.isSelected();
				if (checked) {
					fPreviousExternalLocation = fLocation.getText();
					fLocation.setText(getDefaultPath(fNameGroup.getName()));
				}
				else {
					fLocation.setText(fPreviousExternalLocation);
				}
			}
			fireEvent();
		}
	}

	private final class WorkingSetGroup {

		private WorkingSetConfigurationBlock fWorkingSetBlock;

		public WorkingSetGroup() {
			String[] workingSetIds = new String[] { IWorkingSetIDs.JAVA, IWorkingSetIDs.RESOURCE };
			fWorkingSetBlock = new WorkingSetConfigurationBlock(workingSetIds, JavaPlugin.getDefault()
					.getDialogSettings());
			// fWorkingSetBlock.setDialogMessage(NewWizardMessages.NewJavaProjectWizardPageOne_WorkingSetSelection_message);
		}

		public Control createControl(Composite composite) {
			Group workingSetGroup = new Group(composite, SWT.NONE);
			workingSetGroup.setFont(composite.getFont());
			workingSetGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_WorkingSets_group);
			workingSetGroup.setLayout(new GridLayout(1, false));

			fWorkingSetBlock.createContent(workingSetGroup);

			return workingSetGroup;
		}

		public void setWorkingSets(IWorkingSet[] workingSets) {
			fWorkingSetBlock.setWorkingSets(workingSets);
		}

		public IWorkingSet[] getSelectedWorkingSets() {
			return fWorkingSetBlock.getSelectedWorkingSets();
		}
	}

	/**
	 * Validate this page and show appropriate warnings and error NewWizardMessages.
	 */
	private final class Validator implements Observer {

		public void update(Observable o, Object arg) {

			final IWorkspace workspace = JavaPlugin.getWorkspace();

			final String name = fNameGroup.getName();

			// check whether the project name field is empty
			if (name.length() == 0) {
				setErrorMessage(null);
				setMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_enterProjectName);
				setPageComplete(false);
				return;
			}

			// check whether the project name is valid
			IStatus nameStatus = workspace.validateName(name, IResource.PROJECT);
			if (nameStatus.isOK()) {
	             // there are further restrictions on Grails project names
                nameStatus = validateProjectName(name);
			}
			if (!nameStatus.isOK()) {
				setErrorMessage(nameStatus.getMessage());
				setPageComplete(false);
				return;
			}

			// check whether project already exists
			final IProject handle = workspace.getRoot().getProject(name);
			if (handle.exists()) {
				setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_projectAlreadyExists);
				setPageComplete(false);
				return;
			}

			IPath projectLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(name);
			if (projectLocation.toFile().exists()) {
				try {
					// correct casing
					String canonicalPath = projectLocation.toFile().getCanonicalPath();
					projectLocation = new Path(canonicalPath);
				}
				catch (IOException e) {
					JavaPlugin.log(e);
				}

				String existingName = projectLocation.lastSegment();
				if (!existingName.equals(fNameGroup.getName())) {
					setErrorMessage(Messages.format(
							NewWizardMessages.NewJavaProjectWizardPageOne_Message_invalidProjectNameForWorkspaceRoot,
							BasicElementLabels.getResourceName(existingName)));
					setPageComplete(false);
					return;
				}
			}

			final String location = fLocationGroup.getLocation().toOSString();

			// check whether location is empty
			if (location.length() == 0) {
				setErrorMessage(null);
				setMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_enterLocation);
				setPageComplete(false);
				return;
			}

			// check whether the location is a syntactically correct path
			if (!Path.EMPTY.isValidPath(location)) {
				setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_invalidDirectory);
				setPageComplete(false);
				return;
			}

			IPath projectPath = Path.fromOSString(location);
			
			if (fLocationGroup.isWorkspaceRadioSelected())
				projectPath = projectPath.append(fNameGroup.getName());

			boolean importing = false;
			if (projectPath.toFile().exists()) {// create from existing source
				if (Platform.getLocation().isPrefixOf(projectPath)) { // create from existing source in workspace
					if (!projectPath.toFile().exists()) {
						setErrorMessage(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_notExisingProjectOnWorkspaceRoot);
						setPageComplete(false);
						return;
					}
				}
				importing = true;
			}
			else if (!fLocationGroup.isWorkspaceRadioSelected()) {// create at non existing external location
				if (!canCreate(projectPath.toFile())) {
					setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_cannotCreateAtExternalLocation);
					setPageComplete(false);
					return;
				}

				// If we do not place the contents in the workspace validate the
				// location.
				final IStatus locationStatus = workspace.validateProjectLocation(handle, projectPath);
				if (!locationStatus.isOK()) {
					setErrorMessage(locationStatus.getMessage());
					setPageComplete(false);
					return;
				}
			}
            
            // Let other checks perform before this one.
            // If a location was specified, the project name must match the last segment of the location
            if (!projectPath.isEmpty()) {
                String expectedProjectName = projectPath.lastSegment();
                if ((name == null) || !name.equals(expectedProjectName)) {
                    setErrorMessage(NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_invalidProjectNameForExternalLocation);
                    setPageComplete(false);
                    return;
                }
			}

			if (GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall() == null) {
				setErrorMessage(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_noGrailsInstallationInWorkspacePreferences);
				setPageComplete(false);
				return;
			}

			setPageComplete(true);
			setErrorMessage(null);
			if (importing) {
				// Project exists, but is not in workspace therefore import it.
				setMessage(Messages
						.format(org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_importingExistingProject,
								BasicElementLabels.getResourceName(projectPath
										.lastSegment())));
			} else {
				setMessage(null);
			}
			
		
		}

		/**
		 * Validate a grails project name.  Plugin names can only contain word characters separated by hyphens.
		 * At this point, I don't think there are hard restrictions for app projects.
         * @param name
         * @return
         */
        private IStatus validateProjectName(String name) {
            if (isPluginWizard) {
                char[] nameArr = name.toCharArray(); 
                for (char c : nameArr) {
                    if (!Character.isLetter(c) && c != '-') {
                        return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Plugin names can only contain word characters separated by hyphens.");
                    }
                }
            }
            return Status.OK_STATUS;
        }

        private boolean canCreate(File file) {
					
			// Grails only accepts existing locations where a Grails application (project) 
			// can be created, therefore the parent of the given location must exist
			File parent = file.getParentFile();
			if (parent == null || !parent.exists()) {
				return false;
			}

			return parent.canWrite();
		}
	}

	private static final String PAGE_NAME = "NewGrailsProjectWizardPageOne"; //$NON-NLS-1$

	private final NameGroup fNameGroup;

	private final LocationGroup fLocationGroup;

	private final GrailsInstallGroup grailsInstallGroup;

	private final Validator fValidator;

	private final WorkingSetGroup fWorkingSetGroup;
	
	private final IGrailsInstallListener listener;
	
	private final boolean isPluginWizard;
	/**
	 * Creates a new {@link NewGrailsProjectWizardPageOne}.
	 * @param isPluginWizard TODO
	 */
	public NewGrailsProjectWizardPageOne(String title, String desc, boolean isPluginWizard) {
		super(PAGE_NAME);
		setPageComplete(false);
		setTitle(title);
		setDescription(desc);

		fNameGroup = new NameGroup();
		fLocationGroup = new LocationGroup();
		fWorkingSetGroup = new WorkingSetGroup();
		grailsInstallGroup = new GrailsInstallGroup();
		listener = new InstallChangeListener();
		GrailsCoreActivator.getDefault().getInstallManager().addGrailsInstallListener(listener);

		// establish connections
		fNameGroup.addObserver(fLocationGroup);

		// initialize all elements
		fNameGroup.notifyObservers();

		// create and connect validator
		fValidator = new Validator();
		fNameGroup.addObserver(fValidator);
		fLocationGroup.addObserver(fValidator);
		grailsInstallGroup.addObserver(fValidator);

		// initialize defaults
		setProjectName(""); //$NON-NLS-1$
		setProjectLocationURI(null);
		setWorkingSets(new IWorkingSet[0]);
		this.isPluginWizard = isPluginWizard;
	}
	
	@Override
	public void dispose() {
		GrailsCoreActivator.getDefault().getInstallManager().removeGrailsInstallListener(listener);
		super.dispose();
	}

	/**
	 * The wizard owning this page can call this method to initialize the fields from the current selection and active
	 * part.
	 * 
	 * @param selection used to initialize the fields
	 * @param activePart the (typically active) part to initialize the fields or <code>null</code>
	 */
	public void init(IStructuredSelection selection, IWorkbenchPart activePart) {
		setWorkingSets(getSelectedWorkingSet(selection, activePart));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite = new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		composite.setLayout(initGridLayout(new GridLayout(1, false), true));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		// create UI elements
		Control nameControl = createNameControl(composite);
		nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control grailsHomeControl = grailsInstallGroup.createControl(composite);
		grailsHomeControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control locationControl = createLocationControl(composite);
		locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control workingSetControl = createWorkingSetControl(composite);
		workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		setControl(composite);
	}

	protected void setControl(Control newControl) {
		Dialog.applyDialogFont(newControl);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(newControl, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE);

		super.setControl(newControl);
	}

	/**
	 * Creates the controls for the name field.
	 * 
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createNameControl(Composite composite) {
		return fNameGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the location field.
	 * 
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createLocationControl(Composite composite) {
		return fLocationGroup.createControl(composite);
	}

	/**
	 * Creates the controls for the working set selection.
	 * 
	 * @param composite the parent composite
	 * @return the created control
	 */
	protected Control createWorkingSetControl(Composite composite) {
		return fWorkingSetGroup.createControl(composite);
	}

	/**
	 * Gets a project name for the new project.
	 * 
	 * @return the new project resource handle
	 */
	public String getProjectName() {
		return fNameGroup.getName();
	}

	/**
	 * Sets the name of the new project
	 * 
	 * @param name the new name
	 */
	public void setProjectName(String name) {
		if (name == null)
			throw new IllegalArgumentException();

		fNameGroup.setName(name);
	}

	/**
	 * Returns the current project location path as entered by the user, or <code>null</code> if the project should be
	 * created in the workspace. Note that the last segment of the path is the project name.
	 * 
	 * @return the project location path or its anticipated initial value.
	 */
	public URI getProjectLocationURI() {
		if (fLocationGroup.isDefaultLocation()) {
			return null;
		}
		return URIUtil.toURI(fLocationGroup.getLocation());
	}

	public String getGrailsInstallName() {
		if (grailsInstallGroup.grailsInstallCombo.getSelectionIndex() >= 0) {
			return grailsInstallGroup.grailsInstallCombo.getItem(grailsInstallGroup.grailsInstallCombo
					.getSelectionIndex());
		}
		return null;
	}

	public boolean useDefaultGrailsInstall() {
		return grailsInstallGroup.useDefault.getSelection();
	}

	/**
	 * Sets the project location of the new project or <code>null</code> if the project should be created in the
	 * workspace
	 * 
	 * @param uri the new project location
	 */
	public void setProjectLocationURI(URI uri) {
		IPath path = uri != null ? URIUtil.toPath(uri) : null;
		fLocationGroup.setLocation(path);
	}

	/**
	 * Returns the working sets to which the new project should be added.
	 * 
	 * @return the selected working sets to which the new project should be added
	 */
	public IWorkingSet[] getWorkingSets() {
		return fWorkingSetGroup.getSelectedWorkingSets();
	}

	/**
	 * Sets the working sets to which the new project should be added.
	 * 
	 * @param workingSets the initial selected working sets
	 */
	public void setWorkingSets(IWorkingSet[] workingSets) {
		if (workingSets == null) {
			throw new IllegalArgumentException();
		}
		fWorkingSetGroup.setWorkingSets(workingSets);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fNameGroup.postSetFocus();
		}
	}

	private GridLayout initGridLayout(GridLayout layout, boolean margins) {
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		if (margins) {
			layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		}
		else {
			layout.marginWidth = 0;
			layout.marginHeight = 0;
		}
		return layout;
	}

	private static final IWorkingSet[] EMPTY_WORKING_SET_ARRAY = new IWorkingSet[0];

	private IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection, IWorkbenchPart activePart) {
		IWorkingSet[] selected = getSelectedWorkingSet(selection);
		if (selected != null && selected.length > 0) {
			for (int i = 0; i < selected.length; i++) {
				if (!isValidWorkingSet(selected[i]))
					return EMPTY_WORKING_SET_ARRAY;
			}
			return selected;
		}

		if (!(activePart instanceof PackageExplorerPart))
			return EMPTY_WORKING_SET_ARRAY;

		PackageExplorerPart explorerPart = (PackageExplorerPart) activePart;
		if (explorerPart.getRootMode() == PackageExplorerPart.PROJECTS_AS_ROOTS) {
			// Get active filter
			IWorkingSet filterWorkingSet = explorerPart.getFilterWorkingSet();
			if (filterWorkingSet == null)
				return EMPTY_WORKING_SET_ARRAY;

			if (!isValidWorkingSet(filterWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			return new IWorkingSet[] { filterWorkingSet };
		}
		else {
			// If we have been gone into a working set return the working set
			Object input = explorerPart.getViewPartInput();
			if (!(input instanceof IWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			IWorkingSet workingSet = (IWorkingSet) input;
			if (!isValidWorkingSet(workingSet))
				return EMPTY_WORKING_SET_ARRAY;

			return new IWorkingSet[] { workingSet };
		}
	}

	private IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof ITreeSelection))
			return EMPTY_WORKING_SET_ARRAY;

		ITreeSelection treeSelection = (ITreeSelection) selection;
		if (treeSelection.isEmpty())
			return EMPTY_WORKING_SET_ARRAY;

		List elements = treeSelection.toList();
		if (elements.size() == 1) {
			Object element = elements.get(0);
			TreePath[] paths = treeSelection.getPathsFor(element);
			if (paths.length != 1)
				return EMPTY_WORKING_SET_ARRAY;

			TreePath path = paths[0];
			if (path.getSegmentCount() == 0)
				return EMPTY_WORKING_SET_ARRAY;

			Object candidate = path.getSegment(0);
			if (!(candidate instanceof IWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			IWorkingSet workingSetCandidate = (IWorkingSet) candidate;
			if (isValidWorkingSet(workingSetCandidate))
				return new IWorkingSet[] { workingSetCandidate };

			return EMPTY_WORKING_SET_ARRAY;
		}

		ArrayList result = new ArrayList();
		for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
			Object element = iterator.next();
			if (element instanceof IWorkingSet && isValidWorkingSet((IWorkingSet) element)) {
				result.add(element);
			}
		}
		return (IWorkingSet[]) result.toArray(new IWorkingSet[result.size()]);
	}

	private static boolean isValidWorkingSet(IWorkingSet workingSet) {
		String id = workingSet.getId();
		if (!IWorkingSetIDs.JAVA.equals(id) && !IWorkingSetIDs.RESOURCE.equals(id))
			return false;

		if (workingSet.isAggregateWorkingSet())
			return false;

		return true;
	}

	private class InstallChangeListener implements IGrailsInstallListener {
		public void installsChanged(Set<IGrailsInstall> installs) {
			// Make sure that we run the refresh in the UI thread
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					grailsInstallGroup.refresh();
				}
			});
		}

		public void defaultInstallChanged(IGrailsInstall oldDefault, IGrailsInstall newDefault) {
			//Ignore
		}

	}

}
