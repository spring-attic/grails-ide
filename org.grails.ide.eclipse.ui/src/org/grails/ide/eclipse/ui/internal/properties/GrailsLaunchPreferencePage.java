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
package org.grails.ide.eclipse.ui.internal.properties;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.MultipleInputDialog;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.ui.GrailsUiImages;

/**
 * Inspired by {@link EnvironmentTab}
 * @author Andrew Eisenberg
 * @since 2.6.1
 */
public class GrailsLaunchPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private class SystemProperty {
        private final String name;
        
        private String value;
        
        public SystemProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            boolean equal = false;
            if (obj instanceof SystemProperty) {
                SystemProperty var = (SystemProperty)obj;
                equal = var.getName().equals(name);
            }
            return equal;       
        }
        
        /**
         * Returns this variable's name, which serves as the key in the key/value
         * pair this variable represents
         * 
         * @return this variable's name
         */
        public String getName() {
            return name;
        }
            
        /**
         * Returns this variables value.
         * 
         * @return this variable's value
         */
        public String getValue() {
            return value;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return name.hashCode();
        }
        
        
        /**
         * Sets this variable's value
         * @param value
         */
        public void setValue(String value) {
            this.value = value;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return getName();
        }
    }
    
    /**
     * Content provider for the environment table
     */
    private class SystemPropertyContentProvider implements IStructuredContentProvider {
        public void dispose() {
        }
        public Object[] getElements(Object inputElement) {
            SystemProperty[] elements;
            Map<String, String> m = GrailsCoreActivator.getDefault().getUserSupliedLaunchSystemProperties();
            
            if (m != null && !m.isEmpty()) {
                elements = new SystemProperty[m.size()];
                String[] varNames = new String[m.size()];
                m.keySet().toArray(varNames);
                for (int i = 0; i < m.size(); i++) {
                    elements[i] = new SystemProperty(varNames[i], m.get(varNames[i]));
                }
            } else {
                elements = new SystemProperty[0];
            }
            return elements;
        }
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput == null){
                return;
            }
            if (viewer instanceof TableViewer){
                TableViewer tableViewer= (TableViewer) viewer;
                if (tableViewer.getTable().isDisposed()) {
                    return;
                }
                tableViewer.setComparator(new ViewerComparator() {
                    public int compare(Viewer iviewer, Object e1, Object e2) {
                        if (e1 == null) {
                            return -1;
                        } else if (e2 == null) {
                            return 1;
                        } else {
                            return ((SystemProperty)e1).getName().compareToIgnoreCase(((SystemProperty)e2).getName());
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Label provider for the environment table
     */
    private class SystemPropertyLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 0) {
                return GrailsUiImages.getImage(GrailsUiImages.IMG_OBJ_GRAILS);
            }
            return null;
        }
        public String getColumnText(Object element, int columnIndex)    {
            String result = null;
            if (element instanceof SystemProperty) {
                switch (columnIndex) {
                    case 0: // variable
                        result = ((SystemProperty) element).getName();
                        break;
                    case 1: // value
                        result = ((SystemProperty) element).getValue();
                        break;
                }
            }
            return result;
        }
    }


    private static final String P_VALUE = "value"; //$NON-NLS-1$
    private static final String P_VARIABLE = "variable"; //$NON-NLS-1$

    private Text commandTimeOut;
	private Text commandOutputLimit;
    private Button envAddButton;


    private Button envEditButton;

    
    private TableViewer environmentTable;
    

    private Button envRemoveButton;

    
    private String[] envTableColumnHeaders = {
        P_VARIABLE, 
        P_VALUE, 
    };


    private Button keepGrailsRunning;
    
    private Button cleanGrails20output;
	private Text jvmArgs;

    /**
     * Attempts to add the given variable. Returns whether the variable
     * was added or not (as when the user answers not to overwrite an
     * existing variable).
     * @param variable the variable to add
     * @return whether the variable was added
     */
    private boolean addVariable(SystemProperty variable) {
        String name= variable.getName();
        TableItem[] items = environmentTable.getTable().getItems();
        for (int i = 0; i < items.length; i++) {
            SystemProperty existingVariable = (SystemProperty) items[i].getData();
            if (existingVariable.getName().equals(name)) {
                boolean overWrite= MessageDialog.openQuestion(getShell(), LaunchConfigurationsMessages.EnvironmentTab_12, MessageFormat.format(LaunchConfigurationsMessages.EnvironmentTab_13, new String[] {name})); // 
                if (!overWrite) {
                    return false;
                }
                environmentTable.remove(existingVariable);
                break;
            }
        }
        environmentTable.add(variable);
        return true;
    }

    protected boolean validate() {
    	//Timeout value
        try {
            int value = getTimeOutValue();
            if (value<5000) {
                setErrorMessage("Set command timeout value to at least 5000 milliseconds");
                return false;
            }
        } catch (NumberFormatException e) {
            setErrorMessage("Command timeout must be an integer");
            return false;
        }
    	//Output limit
        try {
            int value = getOutputLimit();
            if (value<5000) {
                setErrorMessage("Set command output limit value to at least 5000 characters");
                return false;
            }
        } catch (NumberFormatException e) {
            setErrorMessage("Command output limit must be an integer");
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    @SuppressWarnings("restriction")
	@Override
    protected Control createContents(Composite _parent) {
    	GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
    	GridDataFactory grab = GridDataFactory.fillDefaults().grab(true, true);
    	Composite parent = SWTFactory.createComposite(_parent, 1, 1, GridData.FILL_HORIZONTAL);
    	
        // Create main composite
        Composite mainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
        
        grab.applyTo(parent);
        grab.applyTo(mainComposite);
        
        createEnvironmentTable(mainComposite);
        createTableButtons(mainComposite);
        environmentTable.setInput(GrailsCoreActivator.getDefault().getUserSupliedLaunchSystemProperties());
        grab.applyTo(environmentTable.getControl());
        
        keepGrailsRunning = SWTFactory.createCheckButton(mainComposite, "Keep external Grails running",
                null, GrailsCoreActivator.getDefault().getKeepRunning(), 2);
        
        keepGrailsRunning.setToolTipText(
                "Try to reuse an existing Grails process to execute multiple commands.\n" +
                "This speeds up execution of Grails commands.");
        cleanGrails20output = SWTFactory.createCheckButton(mainComposite, "Cleanup Grails 2.0 Output", null, 
        		GrailsCoreActivator.getDefault().getCleanOutput(), 2);
        
        Composite morePrefs = SWTFactory.createComposite(parent, 2, 1, SWT.NONE);
        grabHor.applyTo(morePrefs);
        
        SWTFactory.createLabel(morePrefs, "Default JVM Args", 1);
        jvmArgs = SWTFactory.createText(morePrefs, SWT.BORDER, 1);
        jvmArgs.setToolTipText("Default arguments to pass to the JVM used for running Grails."
        		+ "Note that if an individual launch configuration "
        		+ "defines its own JVM arguments than these defaults will be ignored."
        );
        grabHor.applyTo(jvmArgs);
        String jvmArgsStr = GrailsCoreActivator.getDefault().getJVMArgs();
        if (jvmArgsStr!=null) {
        	jvmArgs.setText(jvmArgsStr);
        }
        jvmArgs.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                boolean valid = validate();
                setValid(valid);
            }
        });
        
        SWTFactory.createLabel(morePrefs, "Grails Command Timeout [ms]:", 1);
        commandTimeOut = SWTFactory.createText(morePrefs, SWT.BORDER, 1);
        grabHor.applyTo(commandTimeOut);
        commandTimeOut.setText(""+GrailsCoreActivator.getDefault().getGrailsCommandTimeOut());
        commandTimeOut.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                boolean valid = validate();
                setValid(valid);
            }
        });
        //GridDataFactory.fillDefaults().grab(false, false).hint(150,SWT.DEFAULT).applyTo(commandTimeOut);
        
        SWTFactory.createLabel(morePrefs, "Grails Command Output Limit [chars]:", 1);
        commandOutputLimit = SWTFactory.createText(morePrefs, SWT.BORDER, 1);
        grabHor.applyTo(commandOutputLimit);
        commandOutputLimit.setText(""+GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit());
        commandOutputLimit.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                boolean valid = validate();
                setValid(valid);
            }
        });
        //GridDataFactory.fillDefaults().grab(false, false).hint(150,SWT.DEFAULT).applyTo(commandTimeOut);
        
        
        return parent;
    }

    
    /**
     * Creates and configures the table that displayed the key/value
     * pairs that comprise the environment.
     * @param parent the composite in which the table should be created
     */
    private void createEnvironmentTable(Composite parent) {
        Font font = parent.getFont();
        // Create label, add it to the parent to align the right side buttons with the top of the table
        SWTFactory.createLabel(parent, "System properties to be passed to the Grails process", 2);
        
        // Create table composite
        Composite tableComposite = SWTFactory.createComposite(parent, font, 1, 1, GridData.FILL_BOTH, 0, 0);
        // Create table
        environmentTable = new TableViewer(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        Table table = environmentTable.getTable();
        table.setLayout(new GridLayout());
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setFont(font);
        environmentTable.setContentProvider(new SystemPropertyContentProvider());
        environmentTable.setLabelProvider(new SystemPropertyLabelProvider());
        environmentTable.setColumnProperties(new String[] {P_VARIABLE, P_VALUE});
        environmentTable.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                handleTableSelectionChanged(event);
            }
        });
        environmentTable.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                if (!environmentTable.getSelection().isEmpty()) {
                    handleEnvEditButtonSelected();
                }
            }
        });
        // Create columns
        final TableColumn tc1 = new TableColumn(table, SWT.NONE, 0);
        tc1.setText(envTableColumnHeaders[0]);
        final TableColumn tc2 = new TableColumn(table, SWT.NONE, 1);
        tc2.setText(envTableColumnHeaders[1]);
        final Table tref = table;
        final Composite comp = tableComposite;
        tableComposite.addControlListener(new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                Rectangle area = comp.getClientArea();
                Point size = tref.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                ScrollBar vBar = tref.getVerticalBar();
                int width = area.width - tref.computeTrim(0,0,0,0).width - 2;
                if (size.y > area.height + tref.getHeaderHeight()) {
                    Point vBarSize = vBar.getSize();
                    width -= vBarSize.x;
                }
                Point oldSize = tref.getSize();
                if (oldSize.x > area.width) {
                    tc1.setWidth(width/2-1);
                    tc2.setWidth(width - tc1.getWidth());
                    tref.setSize(area.width, area.height);
                } else {
                    tref.setSize(area.width, area.height);
                    tc1.setWidth(width/2-1);
                    tc2.setWidth(width - tc1.getWidth());
                }
            }
        });
    }

    /**
     * Creates and returns a new push button with the given
     * label and/or image.
     * 
     * @param parent parent control
     * @param label button label or <code>null</code>
     * @param image image of <code>null</code>
     * 
     * @return a new push button
     */
    private Button createPushButton(Composite parent, String label, Image image) {
        return SWTFactory.createPushButton(parent, label, image);   
    }

    /**
     * Creates the add/edit/remove buttons for the environment table
     * @param parent the composite in which the buttons should be created
     */
    private void createTableButtons(Composite parent) {
        // Create button composite
        Composite buttonComposite = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END, 0, 0);

        // Create buttons
        envAddButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_New_4, null); 
        envAddButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                handleEnvAddButtonSelected();
            }
        });
        envEditButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Edit_5, null); 
        envEditButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                handleEnvEditButtonSelected();
            }
        });
        envEditButton.setEnabled(false);
        envRemoveButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Remove_6, null); 
        envRemoveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                handleEnvRemoveButtonSelected();
            }
        });
        envRemoveButton.setEnabled(false);
    }
    
    
    private boolean getKeepGrailsRunning() {
        return keepGrailsRunning.getSelection();
    }

    private int getTimeOutValue() throws NumberFormatException {
        return Integer.valueOf(commandTimeOut.getText());
    }
    
	private int getOutputLimit() throws NumberFormatException {
		return Integer.valueOf(commandOutputLimit.getText());
	}
    
    /**
     * Adds a new environment variable to the table.
     */
    private void handleEnvAddButtonSelected() {
        MultipleInputDialog dialog = new MultipleInputDialog(getShell(), "System property") {
            @Override
            public void createBrowseField(String labelText,
                    String initialValue, boolean allowEmpty) {
               super.createBrowseField(labelText, initialValue, allowEmpty);
            }
            @Override
            public void createVariablesField(String labelText,
                    String initialValue, boolean allowEmpty) {
                Label label = new Label(panel, SWT.NONE);
                label.setText(labelText);
                label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                
                Composite comp = new Composite(panel, SWT.NONE);
                GridLayout layout = new GridLayout();
                layout.marginHeight=0;
                layout.marginWidth=0;
                comp.setLayout(layout);
                comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                
                final Text text = new Text(comp, SWT.SINGLE | SWT.BORDER);
                GridData data = new GridData(GridData.FILL_HORIZONTAL);
                data.widthHint = 200;
                text.setLayoutData(data);
                text.setData(FIELD_NAME, labelText);

                // make sure rows are the same height on both panels.
                label.setSize(label.getSize().x, text.getSize().y); 
                
                if (initialValue != null) {
                    text.setText(initialValue);
                }

                if (!allowEmpty) {
                    validators.add(new Validator() {
                        public boolean validate() {
                            return !text.getText().equals(IInternalDebugCoreConstants.EMPTY_STRING);
                        }
                    });

                    text.addModifyListener(new ModifyListener() {
                        public void modifyText(ModifyEvent e) {
                            validateFields();
                        }
                    });
                }
                controlList.add(text);
            }
        }; 
        dialog.addTextField(P_VARIABLE, null, false);
        dialog.addVariablesField(P_VALUE, null, true);
        
        if (dialog.open() != Window.OK) {
            return;
        }
        
        String name = dialog.getStringValue(P_VARIABLE);
        String value = dialog.getStringValue(P_VALUE);
        
        if (name != null && value != null && name.length() > 0 && value.length() >0) {
            addVariable(new SystemProperty(name.trim(), value.trim()));
        }
    }

    /**
     * Creates an editor for the value of the selected environment variable.
     */
    private void handleEnvEditButtonSelected() {
        IStructuredSelection sel= (IStructuredSelection) environmentTable.getSelection();
        SystemProperty var= (SystemProperty) sel.getFirstElement();
        if (var == null) {
            return;
        }
        String originalName= var.getName();
        String value= var.getValue();
        MultipleInputDialog dialog= new MultipleInputDialog(getShell(), LaunchConfigurationsMessages.EnvironmentTab_11); 
        dialog.addTextField(P_VARIABLE, originalName, false);
        dialog.addVariablesField(P_VALUE, value, true);
        
        if (dialog.open() != Window.OK) {
            return;
        }
        String name= dialog.getStringValue(P_VARIABLE);
        value= dialog.getStringValue(P_VALUE);
        if (!originalName.equals(name)) {
            if (addVariable(new SystemProperty(name, value))) {
                environmentTable.remove(var);
            }
        } else {
            var.setValue(value);
            environmentTable.update(var, null);
        }
    }

    /**
     * Removes the selected environment variable from the table.
     */
    private void handleEnvRemoveButtonSelected() {
        IStructuredSelection sel = (IStructuredSelection) environmentTable.getSelection();
        environmentTable.getControl().setRedraw(false);
        for (Iterator i = sel.iterator(); i.hasNext(); ) {
            SystemProperty var = (SystemProperty) i.next();   
        environmentTable.remove(var);
        }
        environmentTable.getControl().setRedraw(true);
    }
    
    /**
     * Responds to a selection changed event in the environment table
     * @param event the selection change event
     */
    private void handleTableSelectionChanged(SelectionChangedEvent event) {
        int size = ((IStructuredSelection)event.getSelection()).size();
        envEditButton.setEnabled(size == 1);
        envRemoveButton.setEnabled(size > 0);
    }
    
    public void init(IWorkbench workbench) {
        
    }
    
    @Override
    protected void performDefaults() {
        super.performDefaults();
        GrailsCoreActivator.getDefault().setUserSupliedLaunchSystemProperties(Collections.EMPTY_MAP);
        GrailsCoreActivator.getDefault().setKeepGrailsRunning(GrailsCoreActivator.DEFAULT_KEEP_RUNNING_PREFERENCE);
        GrailsCoreActivator.getDefault().setJVMArgs(GrailsCoreActivator.DEFAULT_JVM_ARGS_PREFERENCE);
        environmentTable.setInput(GrailsCoreActivator.getDefault().getUserSupliedLaunchSystemProperties());
        cleanGrails20output.setSelection(GrailsCoreActivator.DEFAULT_CLEAN_OUTPUT_PREFERENCE);
    }
    
    @Override
    public boolean performOk() {
        TableItem[] items = environmentTable.getTable().getItems();
        Map<String,String> map = new HashMap<String, String>();
        for (TableItem item : items) {
            SystemProperty prop = (SystemProperty) item.getData();
            map.put(prop.getName(), prop.getValue());
        }
        final GrailsCoreActivator grailsCore = GrailsCoreActivator.getDefault();
		grailsCore.setUserSupliedLaunchSystemProperties(map);
        grailsCore.setKeepGrailsRunning(getKeepGrailsRunning());
        grailsCore.setGrailsCommandTimeOut(getTimeOutValue());
        grailsCore.setGrailsCommandOutputLimit(getOutputLimit());
        grailsCore.setCleanOutput(getCleanOutput());
        grailsCore.setJVMArgs(getJVMArgs());
        return true;
    }

	private String getJVMArgs() {
		if (jvmArgs!=null && !jvmArgs.isDisposed()) {
			String args = jvmArgs.getText().trim();
			if (!"".equals(args)) {
				return args;
			}
		}
		return null;
	}

	private boolean getCleanOutput() {
		return cleanGrails20output.getSelection();
	}


}
