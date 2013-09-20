/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.editor.groovy.GrailsEditorGroovyActivator;
import org.grails.ide.eclipse.editor.groovy.elements.INavigableGrailsElement;
import org.grails.ide.eclipse.editor.gsp.wizard.NewGSPWizard;

/**
 * @author Andrew Eisenberg
 * @created Dec 3, 2010
 */
public class GoToGSPPopDialog extends PopupDialog {
    
    private final INavigableGrailsElement elt;
    
    private IFile result;

    private TableViewer list;

    public GoToGSPPopDialog(Shell parent, INavigableGrailsElement elt) {
        super(parent, SWT.NO_TRIM, true, true, true,
                true, true, "Go To GSP", "Select GSP");
        this.elt = elt;
    }


    // its a shame that these variables are private in the super class
    private final static GridLayoutFactory POPUP_LAYOUT_FACTORY = GridLayoutFactory
            .fillDefaults().margins(POPUP_MARGINWIDTH, POPUP_MARGINHEIGHT)
            .spacing(POPUP_HORIZONTALSPACING, POPUP_VERTICALSPACING);
    private final static GridDataFactory LAYOUTDATA_GRAB_BOTH = GridDataFactory
            .fillDefaults().grab(true, true);

    @Override
    protected Control createDialogArea(Composite parent) {
        
        Composite composite = (Composite) super.createDialogArea(parent);
        
        list = new TableViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        list.setLabelProvider(new WorkbenchLabelProvider());
        list.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                remmberItemAndClose(event.getSelection());
            }
            
        });
        
        POPUP_LAYOUT_FACTORY.applyTo(list.getTable());
        LAYOUTDATA_GRAB_BOTH.applyTo(list.getTable());
        
        list.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                remmberItemAndClose(event.getSelection());
            }
        });
        list.setContentProvider(new IStructuredContentProvider() {
            
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            
            public void dispose() {
                
            }

            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof IFolder) {
                    try {
                        List<IFile> files = NavigationUtils.findGSPsInFolder((IFolder) inputElement);
                        
                        return files.toArray();
                    } catch (CoreException e1) {
                        GrailsCoreActivator.log(e1);
                    }
                }
                return new Object[0];
            }
        });
        
        IFolder f = elt.getGSPFolder();
        if (f.isAccessible()) {
            list.setInput(f);
        } else {
            list.setInput(null);
        }
        
        Button b = new Button(composite, SWT.FLAT);
        b.setBackground(composite.getBackground());
        b.setText("Create...");
        b.addSelectionListener(new SelectionListener() {
            
            public void widgetSelected(SelectionEvent e) {
                openNewGSPWizard();
            }
            
            public void widgetDefaultSelected(SelectionEvent e) {
                openNewGSPWizard();
            }
        });
        
        return composite;
    }
    
    protected void openNewGSPWizard() {
        NewGSPWizard wizard = new NewGSPWizard();
        IFolder gspFolder = elt.getGSPFolder();
        if (gspFolder != null) {
            wizard.init(Workbench.getInstance(), new StructuredSelection(gspFolder));
            WizardDialog dialog = new WizardDialog(getParentShell(), wizard);
            close();
            dialog.open();
        } else {
            ErrorDialog.openError(getParentShell(), "Can't create GSP", "Can't create GSP file without Domain class.", 
                    new Status(IStatus.ERROR, GrailsEditorGroovyActivator.PLUGIN_ID, "Domain Class doesn't exist."));
            close();
        }
    }


    public IFile getResult() {
        return result;
    }
    
    protected void remmberItemAndClose(ISelection sel) {
        if (! sel.isEmpty() && sel instanceof StructuredSelection) {
            StructuredSelection ss = (StructuredSelection) sel;
            if (ss.size() == 1) {
                result = (IFile) ss.getFirstElement();
            }
        }
        close();
    }
    
    @Override
    public int open() {
        // the super class is not modal.
        // we want to block for the dialog
        int res = super.open();
        if (res == OK) {
            this.setBlockOnOpen(true);
            ReflectionUtils.executePrivateMethod(Window.class, "runEventLoop", new Class<?>[] { Shell.class }, this, new Object[] { getShell() });
        }
        return res;
    }
    
    @Override
    protected IDialogSettings getDialogSettings() {
        final IDialogSettings workbenchDialogSettings = WorkbenchPlugin
        .getDefault().getDialogSettings();
        IDialogSettings result = workbenchDialogSettings.getSection(getId());
        if (result == null) {
            result = workbenchDialogSettings.addNewSection(getId());
        }
        return result;
    }
    
    protected String getId() {
        return "org.grails.ide.eclipse.editor.gsp.gotoGSP"; //$NON-NLS-1$
    }
}
