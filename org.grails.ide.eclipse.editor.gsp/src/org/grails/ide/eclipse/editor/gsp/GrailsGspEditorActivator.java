/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp;

import java.io.IOException;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jst.jsp.ui.internal.Logger;
import org.eclipse.jst.jsp.ui.internal.preferences.JSPUIPreferenceNames;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 */
public class GrailsGspEditorActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.grails.ide.eclipse.editor.gsp";

	public static final String GSP_CONTENT_TYPE = "org.grails.ide.eclipse.editor.gsp.gspsource";
	
	
	
	// The shared instance
	private static GrailsGspEditorActivator plugin;

	
	/**
     * The template store for the jsp editor. 
     */
    private TemplateStore fTemplateStore;
    
    /** 
     * The template context type registry for the jsp editor. 
     */
    private ContextTypeRegistry fContextTypeRegistry;

	/**
	 * The constructor
	 */
	public GrailsGspEditorActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static GrailsGspEditorActivator getDefault() {
		return plugin;
	}

	/**
     * Returns the template store for the jsp editor templates.
     * 
     * @return the template store for the jsp editor templates
     */
    public TemplateStore getTemplateStore() {
        if (fTemplateStore == null) {
            fTemplateStore= new ContributionTemplateStore(getTemplateContextRegistry(), getPreferenceStore(), JSPUIPreferenceNames.TEMPLATES_KEY);

            try {
                fTemplateStore.load();
            } catch (IOException e) {
                Logger.logException(e);
            }
        }       
        return fTemplateStore;
    }
    
    /**
     * Returns the template context type registry for the jsp plugin.
     * 
     * @return the template context type registry for the jsp plugin
     */
    public ContextTypeRegistry getTemplateContextRegistry() {
        if (fContextTypeRegistry == null) {
            ContributionContextTypeRegistry registry = new ContributionContextTypeRegistry();
            registry.addContextType("new_gsp");
            
            fContextTypeRegistry= registry;
        }

        return fContextTypeRegistry;
    }
}
