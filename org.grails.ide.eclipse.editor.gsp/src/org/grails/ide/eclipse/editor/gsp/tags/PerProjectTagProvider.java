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
package org.grails.ide.eclipse.editor.gsp.tags;

import static org.grails.ide.eclipse.core.GrailsCoreActivator.trace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.FieldNode;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.jsp.core.internal.contentmodel.TaglibController;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.TLDCMDocumentManager;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.TaglibTracker;
import org.eclipse.jst.jsp.core.internal.domdocument.ElementImplForJSP;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;
import org.eclipse.wst.sse.core.internal.ltk.parser.TagMarker;
import org.eclipse.wst.sse.core.internal.model.ModelLifecycleEvent;
import org.eclipse.wst.sse.core.internal.provisional.IModelLifecycleListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;
import org.w3c.dom.Node;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.TagLibClass;
import org.grails.ide.eclipse.editor.gsp.parser.GSPSourceParser;
import org.grails.ide.eclipse.editor.gsp.tags.GSPTagJavaDocParser.GSPTagDescription;

/**
 * Provides all of the tags available for each project.
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 13, 2010
 */
@SuppressWarnings("nls")
public class PerProjectTagProvider implements IGrailsProjectInfo {
    // array of tag name/description/declaring class name, attribute names, and default attribute values
    // FIXADE this is getting disgusting.  I should move from String[][][] amd define my own type for this.
    private final static String[][][] builtInTagNames = new String[][][] {
        {{"def", "", "org.codehaus.groovy.grails.web.taglib.GroovyDefTag"}, {}, {}, {}, {"true"}},
        {{"else", "The logical else tag", "org.codehaus.groovy.grails.web.taglib.GroovyElseTag"}, {}, {}, {}, {"false"}},
        {{"renderInput", "", "org.codehaus.groovy.grails.web.taglib.RenderInputTag"}, {}, {}, {}, {"false"}},
        {
            {"collect", "Uses the Groovy JDK collect method to iterate over each element of the specified object transforming the result using the expression in the closure", "org.codehaus.groovy.grails.web.taglib.GroovyCollectTag"}, 
            {"in", "expr"}, 
            {"The object to iterative over", "A GPath expression"},
            {"true", "true"}, {"false"}}, 
        {
            {"each", "Uses the Groovy JDK each method to iterate over each element of the specified object.", "org.codehaus.groovy.grails.web.taglib.GroovyEachTag"}, 
            {"in", "status", "var"}, 
            {"The object to iterative over", "(optional) The name of a variable to store the iteration index in. For the first iteration this variable has a value of 0, for the next, 1, and so on. If this parameter is used, then var is required.", "(optional) The name of the item, defaults to \"it\"."},
            {"true", "false", "false"}, {"false"} },
        {
            {"elseif", "The logical elseif tag", "org.codehaus.groovy.grails.web.taglib.GroovyElseIfTag"}, 
            {"test", "env"}, 
            {"The expression to test", "A GPath expression"},
            {"true", "true"}, {"false"}},
        {
            {"findall", "Uses the Groovy JDK findAll method to iterate over each element of the specified object that match the GPath expression within the attribute \"expr\"", "org.codehaus.groovy.grails.web.taglib.GroovyFindAllTag"}, 
            {"in", "expr"}, 
            {"The object to iterative over", "A GPath Expression"},
            {"true", "true"}, {"false"}},
        {
            {"grep", "Uses the Groovy JDK grep method to iterate over each element of the specified object that match the specified \"filter\" attribute. The filter can be different instances such as classes, regex patterns etc.", "org.codehaus.groovy.grails.web.taglib.GroovyGrepTag"}, 
            {"in", "filter"}, 
            {"The object to iterative over", "The filter instance such as a class, regular expression or anything that implements isCase"},
            {"true", "true"}, {"false"}},
        {
            {"if", "The logical if tag to switch on an expression and/or current environment.", "org.codehaus.groovy.grails.web.taglib.GroovyIfTag"}, 
            {"test", "env"}, 
            {"The expression to test", "An environment name"},
            {"true", "false"}, {"false"}},
        {
            {"unless", "The logical unless tag to switch on an expression and/or current environment.", "org.codehaus.groovy.grails.web.taglib.GroovyUnlessTag"}, 
            {"test", "env"}, 
            {"The expression to test", "An environment name"},
            {"true", "false"}, {"false"}},
        {
            {"while", "Executes a condition in a loop until the condition returns false.", "org.codehaus.groovy.grails.web.taglib.GroovyWhileTag"}, 
            {"test"}, 
            {"The conditional expression"},
            {"true"}, {"false"}, {"false"}},
            
        {
            {"link", "Creates an html anchor tag with the href set based on the parameters specified.", null}, 
                    { "action", "controller", "id", "params", "url",
                            "fragment", "elementId", "mapping", "absolute",
                            "base", "class" },
                    {
                            "the name of the action to use in the link, if not specified the default action will be linked",
                            "the name of the controller to use in the link, if not specified the current controller will be linked",
                            "the id to use in the link",
                            "a map containing request parameters",
                            "a map containing the action, controller, id etc.",
                            "links to this anchor on the page", "", "", "", "",
                            "" },
                    { "false", "false", "false", "false", "false", "false",
                            "false", "false", "false", "false", "false" }, {"false"} },
        {
            {"set", "Set the value of a variable accessible with the GSP page.", null}, 
            {"scope", "var", "value" },
            {"Specifies the scope in which to set the variable (defaults to 'pageScope'", "Name of the variable to set", "The initial value to be assigned"},
            {"false", "true", "true"}, {"false"}},
    };
    
    
    class SharedListener implements IModelLifecycleListener {

        public void processPostModelEvent(ModelLifecycleEvent event) {
            if (event.getType() == ModelLifecycleEvent.MODEL_RELEASED) {
                disconnect(event.getModel());
            }
        }

        public void processPreModelEvent(ModelLifecycleEvent event) { }
        
    }

    private List<GSPTagLibDocument> allGroovyTagLibs;
    private Map<String, GSPTagLibDocument> tagNameToDocument;
    private boolean isInitialized = false;
    private Set<String> allPrefixes;
    
    private SharedListener listener;

    /**
     * one structure model per opened GSP editor
     */
    private final Set<IStructuredModel> connectedStructureModels;


    private IProject project;
    
    
    public PerProjectTagProvider() {
        connectedStructureModels = new HashSet<IStructuredModel>();
        listener = new SharedListener();
    }

    /**
     * Removes all tags from this provider.  
     * And uninitializes the tag trackers
     */
    private void uninitialize() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            trace("PerProjectTagProvider.uninitialize() : " + project.getName());
    
            isInitialized = false;
            allGroovyTagLibs = null;
            tagNameToDocument = null;
            uninitializeAllTagTrackers();
            
            IProject[] depending = getDependingProjects();
            for (IProject otherProject : depending) {
                PerProjectTagProvider otherProvider = GrailsCore.get().getInfo(otherProject, PerProjectTagProvider.class);
                if (otherProvider != null) {
                    otherProvider.uninitialize();
                }
            }
            // now just in case there are open documents, remove the tag markers
            for (IStructuredModel model : connectedStructureModels) {
                removeTagMarkers(model);
            }
            allPrefixes = null;
        }
    }

    /**
     * @return
     */
    private IProject[] getDependingProjects() {
        return project.getReferencingProjects();
    }

    private void ensureInitialized() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            if (!isInitialized) {
                initializeAllTags();
            }
        }
    }
    
    /**
     * Recreates the tags
     * @param javaProject
     */
    private void initializeAllTags() {
        trace("PerProjectTagProvider.initialize() : " + project.getName());
        
        GrailsProject grailsProject = GrailsWorkspaceCore.get().create(project);
        if (grailsProject == null) {
            // a gsp outside of a grails project
            // will not be able to initialize any taglibs here
            return;
        }
        allGroovyTagLibs = new ArrayList<GSPTagLibDocument>();
        tagNameToDocument = new HashMap<String, GSPTagLibDocument>();
        allPrefixes = new HashSet<String>();
        
        // these tags we can classload and access directly
        initializeBuiltInTags();
        
        // these tags have to be parsed
        initializeTaglibs(grailsProject.getStandardTagLibClasses());
        initializeTaglibs(grailsProject.getCustomTagLibClasses());
        initializeTaglibs(grailsProject.getPluginTagLibClasses());
        
        // now include tags from dependent inplace plugin projects
        initializeInPlacePluginProjects(grailsProject);

        // must set isInitialized to be true here so 
        // that we don't get into stack overflows when calling initializeAllTagTrackers
        isInitialized = true;
        initializeAllTagTrackers();
        
        // now just in case there are open documents, update the tag markers
        for (IStructuredModel model : connectedStructureModels) {
            removeTagMarkers(model);
            addTagMarkers(model);
        }
    }

    /**
     * @param grailsProject
     */
    private void initializeInPlacePluginProjects(GrailsProject grailsProject) {
        Collection<IProject> dependentProjects = grailsProject.getDependentGrailsProjects();
        for (IProject project : dependentProjects) {
            List<GSPTagLibDocument> dependentTags = new ArrayList<GSPTagLibDocument>();
            PerProjectTagProvider provider = GrailsCore.get().connect(project, PerProjectTagProvider.class);
            dependentTags.addAll(provider.getAllGroovyTagLibs());
            allPrefixes.addAll(provider.allPrefixes);
            
            for (Entry<String, GSPTagLibDocument> entry : provider.tagNameToDocument.entrySet()) {
                if (!tagNameToDocument.containsKey(entry.getKey())) {
                    tagNameToDocument.put(entry.getKey(), entry.getValue());
                }
            }
            allGroovyTagLibs.addAll(dependentTags);
        }
    }

    private void initializeAllTagTrackers() {
        for (IStructuredModel model : connectedStructureModels) {
            initializeTagTrackers(model);
        }
    }
    
    public void maybeReinitializeTagTrackers(IStructuredModel model) {
        initializeTagTrackers(model);
    }

    private void initializeTagTrackers(IStructuredModel model) {
        TLDCMDocumentManager mgr = TaglibController.getTLDCMDocumentManager(model.getStructuredDocument());
        if (mgr != null) {
            if (shouldReinitializerTagTrackers(mgr)) { 
                trace("PerProjectTagProvider.initializeTagTrackers() : " + model.getBaseLocation());
                ensureInitialized();
                List<TaglibTracker> trackers = mgr.getTaglibTrackers();
                for (GSPTagLibDocument taglib : allGroovyTagLibs) {
                    trackers.add(new TaglibTracker(taglib.getNamespace().getURI(), taglib.getNamespace().getPrefix(), 
                            taglib, new AllDocumentRegion(model.getStructuredDocument())));
                }
            } else {
                trace("PerProjectTagProvider.initializeTagTrackers() not needed : " + model.getBaseLocation());
            }
        }
    }
    
    private boolean shouldReinitializerTagTrackers(TLDCMDocumentManager mgr) {
        List<TaglibTracker> trackers = mgr.getTaglibTrackers();
        for (TaglibTracker tracker : trackers) {
            if (tracker.getStructuredDocumentRegion() instanceof AllDocumentRegion) {
                return false;
            }
        }
        return true;
    }
    
    private void uninitializeAllTagTrackers() {
        for (IStructuredModel model : connectedStructureModels) {
            uninitializeTagTrackers(model);
        }
    }
    
    private void uninitializeTagTrackers(IStructuredModel model) {
        IStructuredDocument structuredDocument = model.getStructuredDocument();
        TLDCMDocumentManager mgr = TaglibController.getTLDCMDocumentManager(structuredDocument);
        if (mgr != null) {
            trace("PerProjectTagProvider.uninitializeTagTrackers() : " + model.getBaseLocation());
            
            List<TaglibTracker> trackers = mgr.getTaglibTrackers();
            for (Iterator<TaglibTracker> iterator = trackers.iterator(); iterator.hasNext();) {
                TaglibTracker tracker = iterator.next();
                if (tracker.getStructuredDocumentRegion() instanceof AllDocumentRegion) {
                    iterator.remove();
                }
            }
        }
    }
    
   private void initializeBuiltInTags() {
       IJavaProject javaProject = JavaCore.create(getProject());
       GSPTagLibDocument builtInTags = new GSPTagLibDocument("g", getProject().getLocation().toOSString(), getProject().getLocationURI().toString()); 
       for (String[][] grailsTagName : builtInTagNames) {
           // we should try to get the handle identifier for this tag, but let's avoid it for now.
           BuiltInGSPTag tag = new BuiltInGSPTag(builtInTags, grailsTagName[0][0], grailsTagName[0][1], findTypeHandle(grailsTagName[0][2], javaProject), grailsTagName[1], grailsTagName[2], grailsTagName[3], grailsTagName[4][0]);
           tag.initialize();
           builtInTags.addTag(tag);
           tagNameToDocument.put(builtInTags.getNamespace().getPrefix() + ":" + tag.getNodeName(), builtInTags);
       }
       allGroovyTagLibs.add(builtInTags);
       allPrefixes.add("g");
   }

   protected String findTypeHandle(String grailsTagName, IJavaProject javaProject) {
       if (grailsTagName != null && javaProject != null) {
           try {
               IType type = javaProject.findType(grailsTagName);
               if (type != null) {
                   return type.getHandleIdentifier();
               }
           } catch (JavaModelException e) {
               GrailsCoreActivator.log(e);
           }
       }
       return null;
   }

    private void initializeTaglibs(List<TagLibClass> standardTagLibs) {
        for (TagLibClass tagLib : standardTagLibs) {
            String namespace = tagLib.getNamespace(); 
            allPrefixes.add(namespace);
            GSPTagLibDocument tagDocument = new GSPTagLibDocument(namespace, tagLib.getBaseLocation(), tagLib.getUri());
            List<FieldNode> tagFields = tagLib.getAllTagFields();
            for (FieldNode tagField : tagFields) {
                if (isIgnored(tagField, namespace)) {
                    continue;
                }
                
                IField jdtTagField = tagLib.getTagField(tagField.getName());
                String tagDefinitionIdentifier = null;
                if (jdtTagField != null) {
                    tagDefinitionIdentifier = jdtTagField.getHandleIdentifier();
                }
                GSPTagDescription tagDescription = gspTagJavaDocParser.parseJavaDoc(jdtTagField, tagField);
                GSPTag tag = new GSPTag(tagDocument, tagField, tagLib.getAttributesForTag(tagField), tagDescription, tagDefinitionIdentifier);
                tag.initialize();
                tagDocument.addTag(tag);
                tagNameToDocument.put(tagDocument.getNamespace().getPrefix() + ":" + tag.getNodeName(), tagDocument);
            }
            allGroovyTagLibs.add(tagDocument);
        }
    }
    
    private final static Set<String> IGNORED_TAG_NAMES = new HashSet<String>();
    private GSPTagJavaDocParser gspTagJavaDocParser = new GSPTagJavaDocParser();
    static {
        IGNORED_TAG_NAMES.add("link");
        IGNORED_TAG_NAMES.add("set");
    }
    
    /**
     * We have hardcoded some of the most important tags and we provide
     * some good documentation for them.  Don't want to overwrite them here.
     * @param tagField
     * @return
     */
    private boolean isIgnored(FieldNode tagField, String namespace) {
        return TagLibClass.DEFAULT_NAMESPACE.equals(namespace) && IGNORED_TAG_NAMES.contains(tagField.getName());
    }

    public GSPTagLibDocument getDocumentForTagName(String name) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            ensureInitialized();
            return tagNameToDocument.get(name);
        }
    }

    
    public void connect(IStructuredModel model) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            trace("PerProjectTagProvider.connect() : " + model.getBaseLocation());
            connectedStructureModels.add(model);
            initializeTagTrackers(model);
            addTagMarkers(model);
            model.addModelLifecycleListener(listener);
        }
    }
    
    public boolean isConnected(IStructuredModel model) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            return connectedStructureModels.contains(model);
        }
    }

    /**
     * @param model the model that is getting tag markers added to it
     */
    public void addTagMarkers(IStructuredModel model) {
        IStructuredDocument structuredDocument = model.getStructuredDocument();
        if (structuredDocument == null) {
            return;
        }
        RegionParser parser = structuredDocument.getParser();
        if (parser instanceof GSPSourceParser) {
            GSPSourceParser gspParser = (GSPSourceParser) parser;
            if (allPrefixes != null) {
                for (String prefix : allPrefixes) {
                    gspParser.addNestablePrefix(new TagMarker(prefix));
                }
            }
        }
    }
    public void disconnect(IStructuredModel model) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            trace("PerProjectTagProvider.disconnect() : " + model.getBaseLocation());
            connectedStructureModels.remove(model);
            uninitializeTagTrackers(model);
            removeTagMarkers(model);
            model.removeModelLifecycleListener(listener);
        }
    }

    public void removeTagMarkers(IStructuredModel model) {
        IStructuredDocument structuredDocument = model.getStructuredDocument();
        if (structuredDocument == null || allPrefixes == null) {
            return;
        }
        RegionParser parser = structuredDocument.getParser();
        if (parser instanceof GSPSourceParser) {
            GSPSourceParser gspParser = (GSPSourceParser) parser;
            for (String prefix : allPrefixes) {
                gspParser.removeNestablePrefix(new TagMarker(prefix));
            }
        }
    }
    
    /**
     * may return null
     * @param node
     * @return
     */
    public CMDocument getCorrespondingCMDocument(Node node) {
        ensureInitialized();
        CMDocument doc = (node instanceof ElementImplForJSP && tagNameToDocument!=null) ? tagNameToDocument.get(node.getNodeName()) : null;
        return doc;
    }
    
    /**
     * from {@link IGrailsProjectInfo}
     */
    public void dispose() {
        trace("PerProjectTagProvider.dispose() : " + project.getName());
        uninitialize();
    }

    /**
     * from {@link IGrailsProjectInfo}
     */
    public IProject getProject() {
        return this.project;
    }

    /**
     * from {@link IGrailsProjectInfo}
     */
    public void setProject(IProject project) {
        this.project = project;
    }

    /**
     * from {@link IGrailsProjectInfo}
     */
    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        for (GrailsElementKind kind : changeKinds) {
            switch(kind) {
                case TAGLIB_CLASS:
                case PROJECT:
                case CLASSPATH:
                    uninitialize();
                    return;
            }
        }
    }

    public List<GSPTagLibDocument> getAllGroovyTagLibs() {
        ensureInitialized();
        return allGroovyTagLibs;
    }
    
    /**
     * returns the gsp tag associated with the given name (namespace should
     * be included with the name), or 
     */
    public AbstractGSPTag getTagForName(String name) {
        GSPTagLibDocument doc = getDocumentForTagName(name);
        if (doc != null) {
            CMNode tag = doc.getElements().getNamedItem(name);
            if (tag instanceof AbstractGSPTag) {
                return (AbstractGSPTag) tag;
            }
        }
        return null;
    }

    /**
     * @return all known gsp namespace prefixes
     */
    public Set<String> getAllPrefixes() {
        return allPrefixes;
    }
}
