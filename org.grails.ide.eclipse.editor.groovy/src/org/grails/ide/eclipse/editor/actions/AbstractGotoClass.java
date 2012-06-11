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
package org.grails.ide.eclipse.editor.actions;

import java.util.Collections;

import org.codehaus.groovy.eclipse.editor.GroovyEditor;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CommandFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsArtifact;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;
import org.grails.ide.eclipse.editor.groovy.elements.INavigableGrailsElement;
import org.grails.ide.eclipse.ui.internal.wizard.GrailsCommandWizard;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jun 2, 2010
 */
public abstract class AbstractGotoClass implements IEditorActionDelegate {

    protected GroovyCompilationUnit unit;
    protected GroovyEditor editor;
    
    protected abstract IGrailsArtifact navigateTo(INavigableGrailsElement elt);
    protected abstract boolean hasRelated();
    protected abstract String errorMessage();
    
    public void run(IAction action) {
        if (unit != null) {
            GrailsProject project = GrailsWorkspaceCore.get().getGrailsProjectFor(unit);
            if (project != null) {
                IGrailsArtifact gelt = project.getGrailsElement(unit);
                if (gelt instanceof INavigableGrailsElement) {
                    IGrailsArtifact elt = navigateTo((INavigableGrailsElement) gelt);
                    if (elt != null) {
                        try {
                            EditorUtility.openInEditor(elt.getResource());
                            return;
                        } catch (PartInitException e) {
                            GrailsCoreActivator.log(e);
                        }
                    } else {
                        if (openNewElementWizard((INavigableGrailsElement) gelt)) {
                            // wizard was open...don't show error on status bar
                            return;
                        }
                    }
                }
            }
        }
        IStatusLineManager statusLine = getStatusLineManager();
        if (statusLine != null) {
            statusLine.setErrorMessage(errorMessage());
        }
    }

    /**
     * Sub classes override to open a new element wizard for this artifact
     * @param elt
     * @return
     */
    protected boolean openNewElementWizard(INavigableGrailsElement elt) {
        if (getCommandName() == null) {
            return false;
        }
        ICompilationUnit unit = ((IGrailsElement) elt).getCompilationUnit();
        GrailsCommandWizard wizard = 
            new GrailsCommandWizard(Collections.singleton(unit.getJavaProject().getProject()), 
                    getCommand(elt.getAssociatedDomainClassName()));
        
        WizardDialog dialog = new WizardDialog(editor.getEditorSite().getShell(), 
                wizard);
        
        return dialog.open() == Window.OK;
    }
    private IFrameworkCommand getCommand(String name) {
        return CommandFactory.createCommandInstance(CommandFactory
                .createCommandDescriptor(
                        getCommandName(),
                        "Create a domain and associated integration test for the given base name.",
                        new ICommandParameterDescriptor[] { GrailsCommandFactory.createGrailsJavaNameParameterDescriptor(
                                "name",
                                "Enter a base domain class name, or use content assist.",
                                true, name) }));
    }
    public abstract String getCommandName();
    
    public void selectionChanged(IAction action, ISelection selection) {  }

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if (targetEditor instanceof GroovyEditor) {
            unit = ((GroovyEditor) targetEditor).getGroovyCompilationUnit();
            if (unit != null) {
                action.setEnabled(hasRelated());
                this.editor = (GroovyEditor) targetEditor;
                return;
            }
        }
        unit = null;
        editor = null;
        action.setEnabled(false);
    }
    
    private IStatusLineManager getStatusLineManager() {
        if (editor != null) {
            try {
                return editor.getEditorSite().getActionBars().getStatusLineManager();
            } catch (NullPointerException e) {
                // can ignore
            }
        }
        return null;
    }

}
