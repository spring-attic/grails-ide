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
package org.grails.ide.eclipse.editor.gsp.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.eclipse.GroovyPlugin;
import org.codehaus.groovy.eclipse.editor.GroovyConfiguration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jst.jsp.core.text.IJSPPartitions;
import org.eclipse.jst.jsp.ui.StructuredTextViewerConfigurationJSP;
import org.eclipse.jst.jsp.ui.internal.contentassist.JSPJavaContentAssistProcessor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.grails.ide.eclipse.editor.gsp.GrailsGspEditorActivator;

/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 */
public class GSPViewerConfiguration extends StructuredTextViewerConfigurationJSP {
    
    private final GroovyConfiguration groovyConfiguration;

    public GSPViewerConfiguration() {
        ReflectionUtils.setPrivateField(StructuredTextViewerConfigurationJSP.class, "fJavaSourceViewerConfiguration", this, groovyConfiguration = createGroovyConfiguration());
        ReflectionUtils.setPrivateField(StructuredTextViewerConfigurationJSP.class, "fLineStyleProviderForJava", this, new LineStyleProviderForGroovy());
    }
    
    @Override
    public IPresentationReconciler getPresentationReconciler(
            ISourceViewer sourceViewer) {
        return super.getPresentationReconciler(sourceViewer);
    }

    private GroovyConfiguration createGroovyConfiguration() {
        return new GroovyConfiguration(
                GroovyPlugin.getDefault().getTextTools().getColorManager(), 
                createCombinedPreferenceStore(), null);
    }

    private IPreferenceStore createCombinedPreferenceStore(/*IEditorInput input*/) {
        List<IPreferenceStore> stores= new ArrayList<IPreferenceStore>(3);

        stores.add(JavaPlugin.getDefault().getPreferenceStore());
        stores.add(new PreferencesAdapter(JavaCore.getPlugin().getPluginPreferences()));
        stores.add(EditorsUI.getPreferenceStore());
        stores.add(PlatformUI.getPreferenceStore());

        return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
    }

    
    @Override
    protected IContentAssistProcessor[] getContentAssistProcessors(
            ISourceViewer sourceViewer, String partitionType) {
        if (partitionType == IJSPPartitions.JSP_DEFAULT_EL) {
            // jsp el
            return new IContentAssistProcessor[]{new JSPJavaContentAssistProcessor()};
        }
        
        // don't call super.  Use our processor instead to ensure that proposals are properly sorted.
        IContentAssistProcessor processor = new GSPStructuredContentAssistProcessor(
                this.getContentAssistant(), partitionType, sourceViewer);
        return new IContentAssistProcessor[]{processor};
    }
    
    
    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        String[] contentTypesOrig = super.getConfiguredContentTypes(sourceViewer);
        String[] contentTypesGroovy = groovyConfiguration.getConfiguredContentTypes(sourceViewer);
        String[] contentTypesNew = new String[contentTypesOrig.length + contentTypesGroovy.length];
        System.arraycopy(contentTypesOrig, 0, contentTypesNew, 0, contentTypesOrig.length);
        System.arraycopy(contentTypesGroovy, 0, contentTypesNew, contentTypesOrig.length, contentTypesGroovy.length);
        return contentTypesNew;
    }
    
    protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
        Map targets = super.getHyperlinkDetectorTargets(sourceViewer);
        targets.put(GrailsGspEditorActivator.GSP_CONTENT_TYPE, null);
        return targets;
    }

}
