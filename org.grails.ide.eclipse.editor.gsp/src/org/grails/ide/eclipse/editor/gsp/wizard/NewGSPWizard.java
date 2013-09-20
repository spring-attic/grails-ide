// COPIED_FROM org.eclipse.jst.jsp.ui.internal.wizard.NewJSPWizard
/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     GoPivotal, Inc.    - augmented for use with Grails
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.wizard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jst.jsp.core.internal.JSPCorePlugin;
import org.eclipse.jst.jsp.ui.internal.Logger;
import org.eclipse.jst.jsp.ui.internal.editor.JSPEditorPluginImageHelper;
import org.eclipse.jst.jsp.ui.internal.editor.JSPEditorPluginImages;
import org.eclipse.jst.jsp.ui.internal.wizard.NewJSPWizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;
import org.eclipse.wst.sse.core.utils.StringUtils;

/**
 * Copy from {@link NewJSPWizard}Change to use {@link NewGSPFileWizardPage} and use GSP-specific messages
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @created Nov 8, 2009
 */
public class NewGSPWizard extends Wizard implements INewWizard {

    private NewGSPFileWizardPage fNewFilePage;
    private NewGSPTemplatesWizardPage fNewFileTemplatesPage;
    private IStructuredSelection fSelection;
    private Display fDisplay;

    private boolean fShouldOpenEditorOnFinish = true;
    
    public void createPageControls(Composite pageContainer) {
        fDisplay = pageContainer.getDisplay();
        super.createPageControls(pageContainer);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=248424
    public void setOpenEditorOnFinish(boolean openEditor) {
        this.fShouldOpenEditorOnFinish = openEditor;
    }
    
    @Override
    public void addPages() {
        // GRAILS CHANGE
        // change text
        fNewFilePage = new NewGSPFileWizardPage("GSPWizardNewFileCreationPage", new StructuredSelection(IDE.computeSelectedResources(fSelection))); //$NON-NLS-1$ 
        fNewFilePage.setTitle("Grails Server Page");
        fNewFilePage.setDescription("Create a new Grails Server Page.");
        addPage(fNewFilePage);
        fNewFileTemplatesPage = new NewGSPTemplatesWizardPage();
        addPage(fNewFileTemplatesPage);
        // END GRAILS CHANGE
        
    }
    private String applyLineDelimiter(IFile file, String text) {
        String lineDelimiter = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, System.getProperty("line.separator"), new IScopeContext[] {new ProjectScope(file.getProject()), new InstanceScope() });//$NON-NLS-1$
        String convertedText = StringUtils.replace(text, "\r\n", "\n");
        convertedText = StringUtils.replace(convertedText, "\r", "\n");
        convertedText = StringUtils.replace(convertedText, "\n", lineDelimiter);
        return convertedText;
    }

    public void init(IWorkbench aWorkbench, IStructuredSelection aSelection) {
        fSelection = aSelection;
        // GRAILS CHANGE
        setWindowTitle("New Groovy Server Page");
        // END GRAILS CHANGE

        ImageDescriptor descriptor = JSPEditorPluginImageHelper.getInstance().getImageDescriptor(JSPEditorPluginImages.IMG_OBJ_WIZBAN_NEWJSPFILE);
        setDefaultPageImageDescriptor(descriptor);
    }

    private void openEditor(final IFile file) {
        if (file != null) {
            fDisplay.asyncExec(new Runnable() {
                public void run() {
                    if (!PlatformUI.isWorkbenchRunning())
                        return;
                    try {
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        IDE.openEditor(page, file, true);
                    }
                    catch (PartInitException e) {
                        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
                    }
                }
            });
        }
    }

    public boolean performFinish() {
        boolean performedOK = false;

        // save user options for next use
        // GRAILS CHANGE
        fNewFileTemplatesPage.saveLastSavedPreferences();

        // no file extension specified so add default extension
        String fileName = fNewFilePage.getFileName();
        if (fileName.lastIndexOf('.') == -1) {
            String newFileName = fNewFilePage.addDefaultExtension(fileName);
            fNewFilePage.setFileName(newFileName);
        }

        // create a new empty file
        IFile file = fNewFilePage.createNewFile();

        // if there was problem with creating file, it will be null, so make
        // sure to check
        if (file != null) {
            // put template contents into file
            // GRAILS CHANGE
            String templateString = fNewFileTemplatesPage.getTemplateString();
            if (templateString != null) {
                templateString = applyLineDelimiter(file, templateString);
                // determine the encoding for the new file
                Preferences preference = JSPCorePlugin.getDefault().getPluginPreferences();
                String charSet = preference.getString(CommonEncodingPreferenceNames.OUTPUT_CODESET);

                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    OutputStreamWriter outputStreamWriter = null;
                    if (charSet == null || charSet.trim().equals("")) { //$NON-NLS-1$
                        // just use default encoding
                        outputStreamWriter = new OutputStreamWriter(outputStream);
                    }
                    else {
                        outputStreamWriter = new OutputStreamWriter(outputStream, charSet);
                    }
                    outputStreamWriter.write(templateString);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    file.setContents(inputStream, true, false, null);
                    inputStream.close();
                }
                catch (Exception e) {
                    Logger.log(Logger.WARNING_DEBUG, "Could not create contents for new JSP file", e); //$NON-NLS-1$
                }
            }

            // open the file in editor
            if (fShouldOpenEditorOnFinish)
                openEditor(file);

            // everything's fine
            performedOK = true;
        }
        return performedOK;
    }

}
