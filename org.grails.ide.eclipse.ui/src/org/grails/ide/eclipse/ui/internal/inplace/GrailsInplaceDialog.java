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
package org.grails.ide.eclipse.ui.internal.inplace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PopupList;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.springsource.ide.eclipse.commons.core.CommandHistoryProvider;
import org.springsource.ide.eclipse.commons.core.Entry;
import org.springsource.ide.eclipse.commons.core.ICommandHistory;
import org.springsource.ide.eclipse.commons.core.SpringCoreUtils;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.contentassist.ContentProposalAdapter;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.contentassist.IContentProposalListener2;
import org.springsource.ide.eclipse.commons.ui.CommandHistoryPopupList;

import org.grails.ide.eclipse.ui.GrailsUiActivator;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsCompletionUtils.GrailsProposalProvider;

/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Andy Clement
 * @author Kris De Volder
 * @author Nieraj Singh
 * @since 2.2.0
 */
@SuppressWarnings("restriction")
public class GrailsInplaceDialog {
	
	/**
	 * The maximum number of items that will be shown in the history menu.
	 * (Note that this number is independent from the number of entries
	 * actually stored in the history. This only limits the number of items
	 * shown in the GrailsInplaceDialog)
	 */
	public int MAX_HISTORY_ITEMS = 20;

	/**
	 * Currently active instance or null. Only one instance should be active/open at the
	 * same time.
	 */
	private static GrailsInplaceDialog instance;

	public static String title = "Grails Command Prompt";

	/**
	 * Get an instances of GrailsInplaceDialog. This method ensures only one instance 
	 * is open at a time, and returns a reference to the existing instance if one is
	 * already/still open. 
	 */
	public static GrailsInplaceDialog getInstance(Shell parent) {
		if (instance==null) {
			instance = new GrailsInplaceDialog(parent);
		}
		return instance;
	}
	

	/**
	 * Dialog constants telling whether this control can be resized or move.
	 */
	public static final String STORE_DISABLE_RESTORE_SIZE = "DISABLE_RESTORE_SIZE"; //$NON-NLS-1$

	public static final String STORE_DISABLE_RESTORE_LOCATION = "DISABLE_RESTORE_LOCATION"; //$NON-NLS-1$

	/**
	 * Dialog store constant for the location's x-coordinate, location's y-coordinate and the size's width and height.
	 */
	private static final String STORE_LOCATION_X = "location.x"; //$NON-NLS-1$

	private static final String STORE_LOCATION_Y = "location.y"; //$NON-NLS-1$

	private static final String STORE_SIZE_WIDTH = "size.width"; //$NON-NLS-1$

	private static final String STORE_SIZE_HEIGHT = "size.height"; //$NON-NLS-1$
	
	private static final String STORE_PINNED_STATE = "pinned";

	/**
	 * The name of the dialog store's section associated with the inplace XReference view.
	 */
	private final String sectionName = GrailsInplaceDialog.class.getName();

	/**
	 * Fields for text matching and filtering
	 */
	private Text commandText;

	private Font statusTextFont;

	private static IProject selectedProject; 
	  // Static is ok: only one instance exists.
	  // make static to remember last selection for new dialog

	/**
	 * Remembers the bounds for this information control.
	 */
	private Rectangle bounds;

	private Rectangle trim;

	/**
	 * Fields for view menu support.
	 */
	private ToolBar toolBar;

	private MenuManager viewMenuManager;

	private Label statusField;

	private boolean isDeactivateListenerActive = false;

	private Composite composite;
	private Composite line1, line2;

	private int shellStyle;

	private Listener deactivateListener;

	private Shell parentShell;

	private Shell dialogShell;

	private Label promptLabel;

	private Combo projectList;

	private Label projectLabel;
	
	/**
	 * Constructor which takes the parent shell
	 */
	private GrailsInplaceDialog(Shell parent) {
		parentShell = parent;
		shellStyle = SWT.RESIZE;
		isPinned = getDialogSettings().getBoolean(STORE_PINNED_STATE);
		initializeUI();
	}

	/**
	 * Open the dialog
	 */
	public void open() {
		if (dialogShell==null) {
			initializeUI();
		}
		dialogShell.open();
		dialogShell.forceActive();
	}

	private void initializeUI() {
		if (dialogShell != null) {
			close();
		}

		if (!grailsInstallAlreadyConfigured()) {
		    // user must configure grails install and then
		    // bring up the in place dialog again
		    askToConfigireGrailsInstall();
		    return;
		}
		
		createContents();
		createShell();
		
		createComposites();
		
		// creates the drop down menu and creates the actions
		commandText = createCommandText(line1);
		createViewMenu(line1);
		
		createProjectList(line2);
		createStatusField(line2);
		
		// set the tab order
		line1.setTabList(new Control[] { commandText });
		composite.setTabList(new Control[] { line1 });

		setInfoSystemColor();
		addListenersToShell();
		initializeBounds();
	}

	// ---- all to do with the command history ---------------
	
	private ICommandHistory commands = getCommandHistory();

	public static ICommandHistory getCommandHistory() {
		return CommandHistoryProvider.getCommandHistory(GrailsUiActivator.PLUGIN_ID, GrailsNature.NATURE_ID);
	}

	private boolean isPinned;

	private IContentProposalListener2 proposalListener;

	private ContentProposalAdapter contentProposer;
	
	private void commandHistoryPopup() {
		List<Entry> entries = commands.getRecentValid(MAX_HISTORY_ITEMS);
		
		Entry chosen = commandHistoryPopup(commandText, entries.toArray(new Entry[entries.size()]));
		if (chosen!=null) {
			setSelectedProject(chosen.getProject());
			setCommand(chosen.getCommand());
		}
	}
	
	private Entry commandHistoryPopup(Control showBelow, Entry[] entries) {
		if (commands.size()>1) {
			CommandHistoryPopupList popup = new CommandHistoryPopupList(dialogShell);
			popup.setLabelProvider(new CommandHistoryPopupList.LabelProvider() {
				@Override
				public String getLabel(Entry entry) {
					if (entry.getProject().equals(getSelectedProjectName()))
						return entry.getCommand();
					else
						return super.getLabel(entry);
				}
			});
			popup.setItems(entries);
//			if (initialSelection!=null)
//				popup.select(initialSelection);
			isDeactivateListenerActive = false; // otherwise dialog will be disposed when popup opens
			Entry selected = popup.open(showBelow.getDisplay().map(showBelow.getParent(), null, showBelow.getBounds()));
			isDeactivateListenerActive = true;
			return selected;
		}
		else if (commands.isEmpty()) {
			return null; 
		}
		else {
			return commands.getLast();
		}
	}
	
	private String popupMenu(Control showBelow,
			String[] choices, String initialSelection) {
		if (choices.length>1) {
			PopupList popup = new PopupList(dialogShell);
			popup.setItems(choices);
			if (initialSelection!=null)
				popup.select(initialSelection);
			isDeactivateListenerActive = false; // otherwise dialog will be disposed when popup opens
			String selected = popup.open(showBelow.getDisplay().map(showBelow.getParent(), null, showBelow.getBounds()));
			isDeactivateListenerActive = true;
			return selected;
		}
		return initialSelection;
	}

	private void addCommandToHistory(String text) {
		if ("".equals(text.trim())) return;
		commands.add(new Entry(text, getSelectedProjectName()));
	}

	// --------------------------------------------------------------------
	
	private void askToConfigireGrailsInstall() {
        boolean res = MessageDialog.openQuestion(parentShell, "No Grails install found", 
                "No Grails installation has been found.\n" +
                "Do you want to configure one now?");
        if (res) {
            openPreferences();
        }
    }

    private void openPreferences() {
        String id = "org.grails.ide.eclipse.ui.preferencePage";
        PreferencesUtil.createPreferenceDialogOn(parentShell, id, new String[] { id }, Collections.EMPTY_MAP).open();
    }

    /**
     * @return true iff there is at least one grails install already configured
     */
    private boolean grailsInstallAlreadyConfigured() {
        return GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall() != null;
    }

    private void createShell() {
		// Create the shell
		dialogShell = new Shell(parentShell, shellStyle);
		dialogShell.setText(title);

		// To handle "ESC" case
		dialogShell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent event) {
				event.doit = false; // don't close now
				dispose();
			}
		});

		Display display = dialogShell.getDisplay();
		dialogShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		int border = ((shellStyle & SWT.NO_TRIM) == 0) ? 0 : 1;
		dialogShell.setLayout(new BorderFillLayout(border));

	}

	private void createComposites() {
		// Composite for filter text and tree
		composite = new Composite(dialogShell, SWT.RESIZE);
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		line1 = new Composite(composite, SWT.NONE);
		layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		line1.setLayout(layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(line1);

		createHorizontalSeparator(composite);
		
		line2 = new Composite(composite, SWT.FILL);
		layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		line2.setLayout(layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(line2);
	}

	private void createHorizontalSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void setInfoSystemColor() {
		Display display = dialogShell.getDisplay();

		// set the foreground colour
		commandText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		promptLabel.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		composite.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		line1.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		toolBar.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		statusField.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		line2.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));

		// set the background colour
		commandText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		promptLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		composite.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		line1.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		toolBar.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		statusField.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		line2.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		projectLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		//projectList.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}

	// --------------------- adding listeners ---------------------------

	private void addListenersToShell() {
		dialogShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (statusTextFont!=null) 
					statusTextFont.dispose();
				
				close();
				// dialogShell = null;
				composite = null;
				commandText = null;
				statusTextFont = null;
			}
		});

		deactivateListener = new Listener() {
			public void handleEvent(Event event) {
				if (isDeactivateListenerActive && !isPinned())
					dispose();
			}
		};

		dialogShell.addListener(SWT.Deactivate, deactivateListener);
		isDeactivateListenerActive = true;
		dialogShell.addShellListener(new ShellAdapter() {
			@Override
			public void shellActivated(ShellEvent e) {
				if (e.widget == dialogShell && dialogShell.getShells().length == 0) {
					isDeactivateListenerActive = true;
					refreshProjects(); // force refresh of projects list
				}
			}
		});

		dialogShell.addControlListener(new ControlAdapter() {
			@Override
			public void controlMoved(ControlEvent e) {
				bounds = dialogShell.getBounds();
				if (trim != null) {
					Point location = composite.getLocation();
					bounds.x = bounds.x - trim.x + location.x;
					bounds.y = bounds.y - trim.y + location.y;
				}

			}

			@Override
			public void controlResized(ControlEvent e) {
				bounds = dialogShell.getBounds();
				if (trim != null) {
					Point location = composite.getLocation();
					bounds.x = bounds.x - trim.x + location.x;
					bounds.y = bounds.y - trim.y + location.y;
				}
			}
		});
	}

	// --------------------- creating and filling the menu
	// ---------------------------

	private void createViewMenu(Composite parent) {
		toolBar = new ToolBar(parent, SWT.FLAT);
		ToolItem viewMenuButton = new ToolItem(toolBar, SWT.PUSH, 0);

		GridData data = new GridData();
		data.horizontalAlignment = GridData.END;
		data.verticalAlignment = GridData.BEGINNING;
		toolBar.setLayoutData(data);

		viewMenuButton.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ELCL_VIEW_MENU));
		viewMenuButton.setDisabledImage(JavaPluginImages.get(JavaPluginImages.IMG_DLCL_VIEW_MENU));
		viewMenuButton.setToolTipText("Menu");
		viewMenuButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});
	}

	private void showViewMenu() {
		isDeactivateListenerActive = false;

		Menu aMenu = getViewMenuManager().createContextMenu(dialogShell);
		dialogShell.setMenu(aMenu);

		Rectangle bounds = toolBar.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = dialogShell.toDisplay(topLeft);
		aMenu.setLocation(topLeft.x, topLeft.y);
		aMenu.addMenuListener(new MenuListener() {

			public void menuHidden(MenuEvent e) {
				isDeactivateListenerActive = true;
			}

			public void menuShown(MenuEvent e) {
			}
		});
		aMenu.setVisible(true);
	}

	private MenuManager getViewMenuManager() {
		if (viewMenuManager == null) {
			viewMenuManager = new MenuManager();
			fillViewMenu(viewMenuManager);
		}
		return viewMenuManager;
	}

	private void fillViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new GroupMarker("SystemMenuStart")); //$NON-NLS-1$
		viewMenu.add(new MoveAction());
		viewMenu.add(new ResizeAction());
		viewMenu.add(new PinDownAction());
		viewMenu.add(new RememberBoundsAction());
		viewMenu.add(new Separator("SystemMenuEnd")); //$NON-NLS-1$
	}

	// --------------------- creating and handling the project selection list
	
	private IProject getSelectedProject() {
		return selectedProject;
	}

	private String getSelectedProjectName() {
		if (selectedProject==null) 
			return null;
		else
			return selectedProject.getName();
	}
	
	private void createProjectList(Composite parent) {
		projectLabel = new Label(parent, SWT.FILL);
		projectLabel.setText("Project: ");
		smallFont(projectLabel);
		
		projectList = new Combo(parent, SWT.READ_ONLY);
		smallFont(projectList);
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.FILL).grab(true, false).applyTo(projectList);
		projectList.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				handleProjectSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				handleProjectSelection();
			}
			private void handleProjectSelection() {
				if (!projectList.getText().equals(getSelectedProjectName())) {
					setSelectedProject(projectList.getText());
				}
			}
		});
		refreshProjects(); // force refresh of created projectList
	}

	private String[] getGrailsProjectNames() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		java.util.List<String> names = new ArrayList<String>();
		int i = 0;
		for (IProject project : projects) {
			// Test if the selected project has Grails nature
			if (isValidProject(project)) {
				names.add(project.getName());
				i++;
			}
		}
		if (selectedProject!=null && !isValidProject(selectedProject)) {
			names.add(selectedProject.getName()); // make sure selected project is always added! or it won't show in the dropdown!
		}
		return names.toArray(new String[names.size()]);
	}

	private boolean isValidProject(IProject project) {
		return project!=null && SpringCoreUtils.hasNature(project, GrailsNature.NATURE_ID);
	}

	// ----------------- creating and filling the status field ----------

	private void createStatusField(Composite parent) {

//		Composite comp = new Composite(parent, SWT.NONE);
//		GridLayout layout = new GridLayout(1, false);
//		layout.marginHeight = 0;
//		layout.marginWidth = 0;
//		comp.setLayout(layout);
//		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Status field label
		statusField = new Label(parent, SWT.RIGHT);
		refreshStatusField();
		GridDataFactory.swtDefaults().grab(false, false).align(SWT.RIGHT, SWT.CENTER).applyTo(statusField);
		smallFont(statusField);
	}
	
	private String getStatusMessage() {
		if (selectedProject==null)
			return "Select a Grails Project";
		if (!isValidProject(selectedProject))
			if (!selectedProject.exists()) 
				return "Selected project does not exist";
			else if (!selectedProject.isOpen())
				return "Selected project is not open";
			else {
				return "Selected project is not a Grails project"; 
			}
		if (isPinned())
			return "Pinned: Press 'Esc' to close";
		else
			return "Type Grails command and press 'Enter'";
	}

	private void refreshStatusField() {
		if (statusField!=null) {
			statusField.setText(getStatusMessage());
			statusField.getParent().layout();
		}
	}

	private void smallFont(Control widget) {
		if (statusTextFont==null) {
			Font font = widget.getFont();
			Display display = widget.getDisplay();
			FontData[] fontDatas = font.getFontData();
			for (FontData element : fontDatas) {
				element.setHeight(element.getHeight() * 9 / 10);
			}
			statusTextFont = new Font(display, fontDatas);
		}
		widget.setFont(statusTextFont);
	}
	
	// ----------- all to do with setting the bounds of the dialog -------------

	/**
	 * Initialize the shell's bounds.
	 */
	private void initializeBounds() {
		// if we don't remember the dialog bounds then reset
		// to be the defaults (behaves like inplace outline view)
		Rectangle oldBounds = restoreBounds();
		if (oldBounds != null) {
			Rectangle defaultBounds = getDefaultBounds();
			// Only use oldBounds if they are at least as large as the default
			if (oldBounds.width < defaultBounds.width) {
				oldBounds.width = defaultBounds.width;
				oldBounds.x = defaultBounds.x;
			}
			if (oldBounds.height < defaultBounds.height) {
				oldBounds.height = defaultBounds.height;
				oldBounds.y = defaultBounds.y;
			}
			dialogShell.setBounds(oldBounds);
			return;
		}
		dialogShell.setBounds(getDefaultBounds());
	}

	public Rectangle getDefaultBounds() {
		GC gc = new GC(composite);
		gc.setFont(composite.getFont());
		int width = gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();

		Point size = dialogShell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Point location = getDefaultLocation(size);
		size.x = Math.max(size.x, 65*width); // At least space for 60 something chars
		return new Rectangle(location.x, location.y, size.x, size.y);
	}

	private Point getDefaultLocation(Point initialSize) {
		Rectangle monitorBounds = getMonitorBounds();
		Point centerPoint;
		if (parentShell != null) {
			centerPoint = Geometry.centerPoint(parentShell.getBounds());
		}
		else {
			centerPoint = Geometry.centerPoint(monitorBounds);
		}

		return new Point(centerPoint.x - (initialSize.x / 2), Math.max(monitorBounds.y, Math.min(centerPoint.y
				- (initialSize.y * 2 / 3), monitorBounds.y + monitorBounds.height - initialSize.y)));
	}

	private Rectangle getMonitorBounds() {
		Monitor monitor = dialogShell.getDisplay().getPrimaryMonitor();
		if (parentShell != null) {
			monitor = parentShell.getMonitor();
		}

		Rectangle monitorBounds = monitor.getClientArea();
		return monitorBounds;
	}

	/**
	 * @return A rectangle that is considered "valid" for the placement of command prompt window with
	 * restored location. 
	 */
	private Rectangle getValidBounds() {
		if (parentShell!=null) {
			return parentShell.getBounds();
		} else {
			return getMonitorBounds();
		}
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings settings = GrailsUiActivator.getDefault().getDialogSettings().getSection(sectionName);
		if (settings == null)
			settings = GrailsUiActivator.getDefault().getDialogSettings().addNewSection(sectionName);

		return settings;
	}

	private void storeBounds() {
		IDialogSettings dialogSettings = getDialogSettings();

		boolean controlRestoresSize = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_SIZE);
		boolean controlRestoresLocation = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_LOCATION);

		if (bounds == null)
			return;

		if (controlRestoresSize) {
			dialogSettings.put(STORE_SIZE_WIDTH, bounds.width);
			dialogSettings.put(STORE_SIZE_HEIGHT, bounds.height);
		}
		if (controlRestoresLocation) {
			dialogSettings.put(STORE_LOCATION_X, bounds.x);
			dialogSettings.put(STORE_LOCATION_Y, bounds.y);
		}
	}

	private Rectangle restoreBounds() {

		IDialogSettings dialogSettings = getDialogSettings();

		boolean controlRestoresSize = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_SIZE);
		boolean controlRestoresLocation = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_LOCATION);

		Rectangle bounds = new Rectangle(-1, -1, -1, -1);

		if (controlRestoresSize) {
			try {
				bounds.width = dialogSettings.getInt(STORE_SIZE_WIDTH);
				bounds.height = dialogSettings.getInt(STORE_SIZE_HEIGHT);
			}
			catch (NumberFormatException ex) {
				bounds.width = -1;
				bounds.height = -1;
			}
		}

		if (controlRestoresLocation) {
			try {
				bounds.x = dialogSettings.getInt(STORE_LOCATION_X);
				bounds.y = dialogSettings.getInt(STORE_LOCATION_Y);
			}
			catch (NumberFormatException ex) {
				bounds.x = -1;
				bounds.y = -1;
			}
		}

		// sanity check
		if (bounds.x == -1 && bounds.y == -1 && bounds.width == -1 && bounds.height == -1) {
			return null;
		}

		if (!isValid(bounds)) {
			return null;
		} else {
			return bounds;
		}		
	}

	/**
	 * When restoring window coordinates persisted in preferences, this validity check is
	 * used to determine whether coordinates look reasonable enough to be reused 
	 * (to avoid having the prompt show up on the other, possibly turned off montitor) 
	 * <p>
	 * This implementation checks whether the bounds fall completely within the bounds
	 * of the 'parentShell' which is typically the Eclipse workbench window.
	 */
	private boolean isValid(Rectangle bounds) {
		Rectangle validBounds = getValidBounds();
		if (validBounds!=null && validBounds.intersects(bounds)) {
			//Must fall completely inside the valid bounds
			Rectangle intersection = validBounds.intersection(bounds);
			return intersection.width==bounds.width && intersection.height == bounds.height;
		}
		return false;
	}

	// ----------- all to do with filtering text

	private Text createCommandText(Composite parent) {
		promptLabel = new Label(parent, SWT.NONE);
		promptLabel.setText("grails> ");

		commandText = new Text(parent, SWT.NONE);

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		GC gc = new GC(parent);
		gc.setFont(parent.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		data.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.CENTER;
		commandText.setLayoutData(data);

		commandText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.ESC) { // ESC
					dispose();
					return;
				}

				if (e.doit && (e.keyCode == 0x0D || e.keyCode == SWT.KEYPAD_CR) && isDeactivateListenerActive) { // return
					e.doit = false;
					IProject project = getSelectedProject();
					if (!isValidProject(project)) {
						openInformationMessage("Invalid Project ", getStatusMessage());
					}
					else if (explainNewWizardToUser()) {
						return;
					}
					else {
						addCommandToHistory(commandText.getText());
						GrailsLaunchUtils.launch(JavaCore.create(getSelectedProject()), commandText.getText());
						if (!isPinned()) dispose();
					}
					return;
				}
				
				if (e.doit && e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					commandHistoryPopup();
					return;
				}
			}


		});
		

		refreshContentAssist();
		return commandText;
	}

	/**
	 * See STS-988: warn user when they are attempting to create plugin or app from the GrailsCommandPrompt.
	 * => Should use a 'new' wizard instead.
	 */
	private boolean explainNewWizardToUser() {
		String command = commandText.getText();
		if (command.contains("create-plugin")) {
			openInformationMessage("Nested projects are not supported in Eclipse", 
					"The Grails Command prompt tool is only intended to run commands in the context of an existing " +
					"Grails plugin or app project.\n\n" +
					"To execute the 'create-plugin' command, please use the 'new Grails Plugin Wizard' accessible from the 'New' menu");
			return true;
		} else if (command.contains("create-app")) {
			openInformationMessage("Nested projects are not supported in Eclipse",
					"The Grails Command prompt tool is intended to run commands in the context of an existing " +
					"Grails plugin or app project.\n\n" +
					"To execute the 'create-app' command, please use the 'new Grails Project Wizard' accessible from the 'New' menu");
			return true;
		}
		return false;
	}

	private void openInformationMessage(String title, String message) {
		Shell shell = dialogShell;
		if (shell.isDisposed())
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		isDeactivateListenerActive = false;
		try {
			MessageDialog.openInformation(shell, title, message);
		}
		finally {
			isDeactivateListenerActive = true;
		}
	}
	
	private boolean isPinned() {
		return isPinned;
	}

	private void setPinned(boolean pinit) {
		this.isPinned = pinit;
		refreshStatusField();
		getDialogSettings().put(STORE_PINNED_STATE, pinit); // Future dialogs start in same pinned state
	}

	/**
	 * Static inner class which sets the layout for the inplace view. Without this, the inplace view will not be
	 * populated.
	 * 
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl
	 */
	private static class BorderFillLayout extends Layout {

		/** The border widths. */
		final int fBorderSize;

		/**
		 * Creates a fill layout with a border.
		 */
		public BorderFillLayout(int borderSize) {
			if (borderSize < 0)
				throw new IllegalArgumentException();
			fBorderSize = borderSize;
		}

		/**
		 * Returns the border size.
		 */
		@SuppressWarnings("unused")
		public int getBorderSize() {
			return fBorderSize;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {

			Control[] children = composite.getChildren();
			Point minSize = new Point(0, 0);

			if (children != null) {
				for (Control element : children) {
					Point size = element.computeSize(wHint, hHint, flushCache);
					minSize.x = Math.max(minSize.x, size.x);
					minSize.y = Math.max(minSize.y, size.y);
				}
			}

			minSize.x += fBorderSize * 2 + 3;
			minSize.y += fBorderSize * 2;

			return minSize;
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {

			Control[] children = composite.getChildren();
			Point minSize = new Point(composite.getClientArea().width, composite.getClientArea().height);

			if (children != null) {
				for (Control child : children) {
					child.setSize(minSize.x - fBorderSize * 2, minSize.y - fBorderSize * 2);
					child.setLocation(fBorderSize, fBorderSize);
				}
			}
		}
	}

	// ---------- shuts down the dialog ---------------

	/**
	 * Close the dialog
	 */
	public void close() {
		storeBounds();
//		storeCommandHistory();
		toolBar = null;
		viewMenuManager = null;
	}

	public void dispose() {
		instance = null;
		commandText = null;
		if (dialogShell != null) {
			if (!dialogShell.isDisposed())
				dialogShell.dispose();
			dialogShell = null;
			parentShell = null;
			composite = null;
		}
	}

	// ------------------ moving actions --------------------------

	/**
	 * Move action for the dialog.
	 */
	private class MoveAction extends Action {

		MoveAction() {
			super("&Move", IAction.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			performTrackerAction(SWT.NONE);
			isDeactivateListenerActive = true;
		}

	}

	/**
	 * Remember bounds action for the dialog.
	 */
	private class RememberBoundsAction extends Action {

		RememberBoundsAction() {
			super("Remember Size and &Location", IAction.AS_CHECK_BOX);
			setChecked(!getDialogSettings().getBoolean(STORE_DISABLE_RESTORE_LOCATION));
		}

		@Override
		public void run() {
			IDialogSettings settings = getDialogSettings();

			boolean newValue = !isChecked();
			// store new value
			settings.put(STORE_DISABLE_RESTORE_LOCATION, newValue);
			settings.put(STORE_DISABLE_RESTORE_SIZE, newValue);

			isDeactivateListenerActive = true;
		}
	}

	/**
	 * Resize action for the dialog.
	 */
	private class ResizeAction extends Action {

		ResizeAction() {
			super("&Resize", IAction.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			performTrackerAction(SWT.RESIZE);
			isDeactivateListenerActive = true;
		}

	}

	/**
	 * Resize action for the dialog.
	 */
	private class PinDownAction extends Action {

		PinDownAction() {
			super("&Pin", IAction.AS_CHECK_BOX);
			setChecked(isPinned());
		}

		@Override
		public void run() {
			setPinned(isChecked());
		}

	}
	
	/**
	 * Perform the requested tracker action (resize or move).
	 * 
	 * @param style The track style (resize or move).
	 */
	private void performTrackerAction(int style) {
		Tracker tracker = new Tracker(dialogShell.getDisplay(), style);
		tracker.setStippled(true);
		Rectangle[] r = new Rectangle[] { dialogShell.getBounds() };
		tracker.setRectangles(r);
		isDeactivateListenerActive = false;
		if (tracker.open()) {
			dialogShell.setBounds(tracker.getRectangles()[0]);
			isDeactivateListenerActive = true;
		}
	}

	// -------------------- all to do with the contents of the view  ------------------

	private void createContents() {
	}

	public void setSelectedProject(IProject project) {
		if (project==null) return; // ignore null setting!
		selectedProject = project;
		refreshProjects();
		refreshContentAssist();
	}

	/**
	 * @param project
	 */
	private void refreshContentAssist() {
		if (isValidProject(selectedProject) && commandText!=null) { 
			if (contentProposer==null) {
				contentProposer = GrailsCompletionUtils.addTypeFieldAssistToText(this.commandText, selectedProject);
				if (contentProposer!=null) {
					contentProposer.addContentProposalListener(getProposalListener());
				}
			}
			else {
				GrailsProposalProvider gpp = (GrailsProposalProvider) contentProposer.getContentProposalProvider();
				gpp.setProject(selectedProject);
			}
		}
	}

	/**
	 * This listener is needed so that we can avoid closing the dialog when it gets deactivated during
	 * the selection of a choice from the content proposal popup. Without this listener, when user
	 * clicks in the popup this will deactivate the in place dialog, closing it, unless it is pinned. 
	 */
	private IContentProposalListener2 getProposalListener() {
		if (proposalListener==null) {
			proposalListener = new IContentProposalListener2() {
				public void proposalPopupOpened(ContentProposalAdapter adapter) {
					isDeactivateListenerActive = false;
				}
				
				public void proposalPopupClosed(ContentProposalAdapter adapter) {
					isDeactivateListenerActive = true;
				}
			};
		}
		return proposalListener;
	}

	/**
	 * Refresh the list of grails projects and put them into the projectList widget.
	 */
	private void refreshProjects() {
		if (projectList!=null) {
			String[] projectNames = getGrailsProjectNames();
			projectList.setItems(projectNames);
			if (getSelectedProject()==null && projectNames.length>0) {
				setSelectedProject(projectNames[0]);
			}
			if (getSelectedProject()!=null) {
				projectList.setText(selectedProject.getName());
			}
			projectList.getParent().layout();
			refreshStatusField();
		}
	}

	private void setSelectedProject(String projectName) {
		setSelectedProject(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName));
	}
	
	public boolean isOpen() {
		return dialogShell != null;
	}

	public void setupFromHistory(Entry entry) {
		setSelectedProject(entry.getProject());
		String command = entry.getCommand();
		setCommand(command);
		commandText.setSelection(0, command.length());
	}

	private void setCommand(String command) {
		commandText.setText(command);
		commandText.setSelection(command.length());
	}

	public static void closeIfNotPinned() {
		if (instance!=null && !instance.isPinned())
			instance.dispose();
	}

}
