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
package org.grails.ide.eclipse.ui.internal.perspective;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * Perspective for Grails
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Kris De Volder
 * @version 2.3.3
 */
public class GrailsPerspective implements IPerspectiveFactory {
	
	public static final String ID = "org.grails.ide.eclipse.perspective";

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();

		IFolderLayout folder = layout.createFolder("left", IPageLayout.LEFT, (float) 0.25, editorArea); //$NON-NLS-1$
		folder.addPlaceholder(JavaUI.ID_PACKAGES);
		folder.addPlaceholder(JavaUI.ID_TYPE_HIERARCHY);
		folder.addPlaceholder("org.eclipse.ui.views.ResourceNavigator");
		folder.addView("org.eclipse.ui.navigator.ProjectExplorer");
		layout.addFastView("org.eclipse.jdt.junit.ResultView", (float) 0.25);
//		layout.addFastView("org.springframework.ide.eclipse.aop.ui.navigator.aopReferenceModelNavigator", (float) 0.25);
//		layout.addFastView("org.eclipse.contribution.xref.ui.views.XReferenceView", (float) 0.25);
		
		if (Platform.getBundle("org.grails.ide.eclipse.runonserver")!=null) {
			//Run on server is experimental, is not deployed in standard build, so don't add this to perspective unless
			//  the runonserver plugin is present in this STS install.
			IFolderLayout serverFolder = layout.createFolder("bottomleft", IPageLayout.BOTTOM, (float) 0.80, "left"); //$NON-NLS-1$
			serverFolder.addView("org.eclipse.wst.server.ui.ServersView");
		}

//		IFolderLayout serverFolder = layout.createFolder("server", IPageLayout.BOTTOM, (float) 0.80, "left");
//		serverFolder.addView("org.eclipse.wst.server.ui.ServersView");

		IFolderLayout tasklistFolder = layout.createFolder("topright", IPageLayout.RIGHT, (float) 0.75, editorArea); //$NON-NLS-1$
		tasklistFolder.addView("org.eclipse.mylyn.tasks.ui.views.tasks");
//		IFolderLayout springFolder = layout.createFolder("spring", IPageLayout.BOTTOM, (float) 0.50, "topright");
//		springFolder.addView("org.springframework.ide.eclipse.ui.navigator.springExplorer");

		IFolderLayout outlineFolder = layout.createFolder("middleright", IPageLayout.BOTTOM, (float) 0.55, "topright"); //$NON-NLS-1$
		outlineFolder.addView(IPageLayout.ID_OUTLINE);
		outlineFolder.addPlaceholder("org.eclipse.ui.texteditor.TemplatesView");

		IFolderLayout outputfolder = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.80, editorArea); //$NON-NLS-1$
		outputfolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		outputfolder.addView("org.eclipse.ui.views.AllMarkersView");
		outputfolder.addView(IProgressConstants.PROGRESS_VIEW_ID);
		outputfolder.addPlaceholder(IPageLayout.ID_PROBLEM_VIEW);
		outputfolder.addPlaceholder(IPageLayout.ID_TASK_LIST);
		outputfolder.addPlaceholder(JavaUI.ID_JAVADOC_VIEW);
		outputfolder.addPlaceholder(JavaUI.ID_SOURCE_VIEW);
		outputfolder.addPlaceholder("org.eclipse.search.ui.views.SearchView");
		outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		outputfolder.addPlaceholder("*");

		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

		// views - java
		layout.addShowViewShortcut(JavaUI.ID_PACKAGES);
		layout.addShowViewShortcut(JavaUI.ID_TYPE_HIERARCHY);
		layout.addShowViewShortcut(JavaUI.ID_SOURCE_VIEW);
		layout.addShowViewShortcut(JavaUI.ID_JAVADOC_VIEW);

		// views - search
		layout.addShowViewShortcut("org.eclipse.search.ui.views.SearchView");

		// views - debugging
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);

		// views - standard workbench
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		// layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut("org.eclipse.ui.views.AllMarkersView");
		// TODO e3.5 replace with IPageLayout.ID_PROJECT_EXPLORER
		layout.addShowViewShortcut("org.eclipse.ui.navigator.ProjectExplorer");
		// layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(IProgressConstants.PROGRESS_VIEW_ID);
		layout.addShowViewShortcut("org.eclipse.ui.texteditor.TemplatesView");

		// views - springsource views
//		layout.addShowViewShortcut("com.springsource.sts.ide.metadata.ui.RequestMappingView");
//		layout.addShowViewShortcut("org.springframework.ide.eclipse.aop.ui.navigator.aopReferenceModelNavigator");
//		layout.addShowViewShortcut("com.springsource.sts.roo.ui.rooShellView");
//		layout.addShowViewShortcut("org.springframework.ide.eclipse.aop.ui.tracing.eventTraceView");
//		layout.addShowViewShortcut("org.eclipse.contribution.xref.ui.views.XReferenceView");
		layout.addShowViewShortcut("org.eclipse.mylyn.tasks.ui.views.tasks");
		layout.addShowViewShortcut("org.eclipse.wst.server.ui.ServersView");

		// new files
		layout.addNewWizardShortcut("org.grails.ide.eclipse.ui.wizards.new.domainclass");
		layout.addNewWizardShortcut("org.grails.ide.eclipse.ui.wizards.new.controller");
		layout.addNewWizardShortcut("org.grails.ide.eclipse.ui.wizards.new.taglib");
		layout.addNewWizardShortcut("org.grails.ide.eclipse.ui.wizards.new.service");
		layout.addNewWizardShortcut("org.grails.ide.eclipse.ui.wizard.newGrailsProjectWizard");
		layout.addNewWizardShortcut("org.grails.ide.eclipse.ui.wizard.newGrailsPluginProjectWizard");
		
//		layout.addNewWizardShortcut("ajaspectwizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard"); //$NON-NLS-1$
//		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewClassCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.codehaus.groovy.eclipse.ui.groovyClassWizard");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewEnumCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewAnnotationCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.junit.wizards.NewTestCaseCreationWizard");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.codehaus.groovy.eclipse.ui.groovyJUnitWizard");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSourceFolderCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSnippetFileCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewJavaWorkingSetWizard"); //$NON-NLS-1$
//		layout.addNewWizardShortcut("org.springframework.ide.eclipse.beans.ui.wizards.newBeansConfig");
//		layout.addNewWizardShortcut("org.springframework.ide.eclipse.webflow.ui.wizard.newWebflowConfigWizard");
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard");//$NON-NLS-1$

		// new projects
//		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.JavaProjectWizard"); //$NON-NLS-1$
//		layout.addNewWizardShortcut("ajprojectwizard");
//		layout.addNewWizardShortcut("com.springsource.sts.wizard.template");
//		layout.addNewWizardShortcut("org.springframework.ide.eclipse.beans.ui.wizards.newSpringProject");
//		layout.addNewWizardShortcut("com.springsource.sts.roo.ui.wizard.newRooProjectWizard");
//		layout.addNewWizardShortcut("org.codehaus.groovy.eclipse.ui.groovyProjectWizard");
//		layout.addNewWizardShortcut("org.eclipse.wst.web.ui.internal.wizards.SimpleWebProjectWizard");
//		layout.addNewWizardShortcut("org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard");

		// new perspectives
		layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective");
		layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective");
		layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaHierarchyPerspective");
		layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaBrowsingPerspective");
		layout.addPerspectiveShortcut("com.springsource.sts.ide.perspective");

	}
}
