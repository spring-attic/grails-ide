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
package org.grails.ide.eclipse.editor.groovy.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.eclipse.core.model.GroovyProjectFacade;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.util.Util;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.workspace.internal.GrailsProjectUtil;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectTypeCache;

/**
 * A Grails project and provides access to all grails elements managed by this project.
 * Create using {@link GrailsWorkspaceCore}.
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Nov 23, 2009
 */
@SuppressWarnings("nls")
public class GrailsProject {
    
    private final IJavaProject javaProject;
    private GroovyProjectFacade groovyProject;
    
    private Map<String, IGrailsElement> classNodeGrailsElementCache = new HashMap<String, IGrailsElement>();

    protected GrailsProject(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    
    /**
     * converts to a lowercase name.  Not exactly right.  Could be
     * a problem if name starts with a double capital.
     * @param type
     * @return
     */
    public static String getBeanName(IType type) {
        String typeName = type.getElementName();
        typeName = typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
        return typeName;
    }


    public GroovyProjectFacade getGroovyProject() {
        if (groovyProject == null) {
            groovyProject = new GroovyProjectFacade(javaProject);
        }
        return groovyProject;
    }
    
    /**
     * Converts this {@link ICompilationUnit} into a {@link GrailsElementKind} if
     * it corresponds to a standard Grails artifact.  Otherwise returns {@link GrailsElementKind#OTHER}.
     * Returns {@link GrailsElementKind#INVALID} if compilation unit is not from this Project or compilation unit
     * is not from a grails project.
     * @param unit
     * @return
     */
    public GrailsElementKind getElementKind(ICompilationUnit unit) {
        if (unit == null || (! unit.getJavaProject().equals(javaProject))) {
            return GrailsElementKind.INVALID;
        }
        
        if (! (unit instanceof GroovyCompilationUnit)) {
            return GrailsElementKind.OTHER;
        }
        
        IPackageFragmentRoot root = ((CompilationUnit) unit).getPackageFragmentRoot();
        String rootName = root.getElementName();
        if (GrailsElementKind.CONTROLLER_CLASS.getSourceFolder().equals(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.CONTROLLER_CLASS;
            } else {
                return GrailsElementKind.OTHER;
            }
        } else if (GrailsElementKind.DOMAIN_CLASS.getSourceFolder().equals(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.DOMAIN_CLASS;
            } else {
                return GrailsElementKind.OTHER;
            }
        } else if (GrailsElementKind.TAGLIB_CLASS.getSourceFolder().equals(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.TAGLIB_CLASS;
            } else {
                return GrailsElementKind.OTHER;
            }
        } else if (GrailsElementKind.SERVICE_CLASS.getSourceFolder().equals(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.SERVICE_CLASS;
            } else {
                return GrailsElementKind.OTHER;
            }
        } else if (GrailsElementKind.INTEGRATION_TEST.getSourceFolder().equals(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.INTEGRATION_TEST;
            } else {
                return GrailsElementKind.OTHER;
            }
        } else if (GrailsElementKind.UNIT_TEST.getSourceFolder().endsWith(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.UNIT_TEST;
            } else {
                return GrailsElementKind.OTHER;
            }
        } else if (GrailsElementKind.INTEGRATION_TEST.getSourceFolder().endsWith(rootName)) {
            if (primaryTypeExists(unit)) {
                return GrailsElementKind.INTEGRATION_TEST;
            } else {
                return GrailsElementKind.OTHER;
            }   
        } else if (rootName.equals(GrailsElementKind.CONF_FOLDER)) {
            String elementName = unit.getElementName();
            if (GrailsElementKind.BOOT_STRAP.getNameSuffix().equals(elementName)) {
                return GrailsElementKind.BOOT_STRAP;
            } else if (GrailsElementKind.BUILD_CONFIG.getNameSuffix().equals(elementName)) {
                return GrailsElementKind.BUILD_CONFIG;
            } else if (GrailsElementKind.CONFIG.getNameSuffix().equals(elementName)) {
                return GrailsElementKind.CONFIG;
            } else if (GrailsElementKind.DATA_SOURCE.getNameSuffix().equals(elementName)) {
                return GrailsElementKind.DATA_SOURCE;
            } else if (GrailsElementKind.URL_MAPPINGS.getNameSuffix().equals(elementName)) {
                return GrailsElementKind.URL_MAPPINGS;
            }
            
            // only for testing
        } else if (cuHandleToElementKind.containsKey(unit.getHandleIdentifier())) {
            return cuHandleToElementKind.get(unit.getHandleIdentifier());
        }
        return GrailsElementKind.OTHER;
    }
    
    private IType getPrimaryType(ICompilationUnit unit) {
        IType primaryType = unit.getType(getPrimaryTypeName(unit));
        return primaryType != null && primaryType.exists() ? primaryType : null;
    }

    /**
     * @param unit
     * @return true iff the primary type (that is the type whose name matches the compilation unit)
     * exists
     */
    private boolean primaryTypeExists(ICompilationUnit unit) {
        return getPrimaryType(unit) != null;
    }

    protected String getPrimaryTypeName(ICompilationUnit unit) {
        String elementName = unit.getElementName();
        // we already know that this unit ends with .groovy, so no need to check
        return Util.getNameWithoutJavaLikeExtension(elementName);
    }

    public ControllerClass getControllerClass(GroovyCompilationUnit unit) {
        if (getElementKind(unit) == GrailsElementKind.CONTROLLER_CLASS) {
            return new ControllerClass(unit);
        }
        return null;
    }

    public ServiceClass getServiceClass(IType target) {
        IJavaElement cu = target.getAncestor(IJavaElement.COMPILATION_UNIT);
        if (cu instanceof GroovyCompilationUnit) {
            GroovyCompilationUnit gcu = (GroovyCompilationUnit) cu;
            return getServiceClass(gcu);
        }
        return null;
    }
    
    public TestClass getTestClass(IType target) {
        IJavaElement cu = target.getAncestor(IJavaElement.COMPILATION_UNIT);
        if (cu instanceof GroovyCompilationUnit) {
            GroovyCompilationUnit gcu = (GroovyCompilationUnit) cu;
            return getTestClass(gcu);
        }
        return null;
    }

	public ControllerClass getControllerClass(IType target) {
        IJavaElement cu = target.getAncestor(IJavaElement.COMPILATION_UNIT);
        if (cu instanceof GroovyCompilationUnit) {
            GroovyCompilationUnit gcu = (GroovyCompilationUnit) cu;
            return getControllerClass(gcu);
        }
        return null;
	}
    
    public DomainClass getDomainClass(String packageName, String domainName) {
        return (DomainClass) getGrailsElement(packageName, domainName, GrailsElementKind.DOMAIN_CLASS.getSourceFolder(), GrailsElementKind.DOMAIN_CLASS);
    }
    
    public ServiceClass getServiceClass(String packageName, String serviceName) {
        return (ServiceClass) getGrailsElement(packageName, serviceName, GrailsElementKind.SERVICE_CLASS.getSourceFolder(), GrailsElementKind.SERVICE_CLASS);
    }
    
    public TestClass getTestClass(String packageName, String testName) {
    	TestClass result = (TestClass) getGrailsElement(packageName, testName, GrailsElementKind.UNIT_TEST.getSourceFolder(), GrailsElementKind.UNIT_TEST);
    	if (result == null) {
    		result = (TestClass) getGrailsElement(packageName, testName, GrailsElementKind.INTEGRATION_TEST.getSourceFolder(), GrailsElementKind.INTEGRATION_TEST);
    	}
    	return result;
    }
    
    public ControllerClass getControllerClass(String packageName, String controllerName) {
        return (ControllerClass) getGrailsElement(packageName, controllerName, GrailsElementKind.CONTROLLER_CLASS.getSourceFolder(), GrailsElementKind.CONTROLLER_CLASS);
    }
    
    public TagLibClass getTagLibClass(String packageName, String tagLibName) {
        return (TagLibClass) getGrailsElement(packageName, tagLibName, GrailsElementKind.TAGLIB_CLASS.getSourceFolder(), GrailsElementKind.TAGLIB_CLASS);
    }
    
    private IGrailsElement getGrailsElement(String packageName, String elementName, String locationString, GrailsElementKind kind) {
        String fullName;
        if (packageName.length() > 0) {
            fullName = "grails-app/" + locationString + "/" + packageName.replace('.', '/') + "/" + elementName;
        } else {
            fullName = "grails-app/" + locationString + "/" + elementName;
        }
        IFile file = javaProject.getProject().getFile(fullName);
        ICompilationUnit cUnit = JavaCore.createCompilationUnitFrom(file);
        if (cUnit instanceof GroovyCompilationUnit && cUnit.exists()) {
            switch (kind) {
                case DOMAIN_CLASS:
                    return getDomainClass((GroovyCompilationUnit) cUnit);
                case CONTROLLER_CLASS:
                    return getControllerClass((GroovyCompilationUnit) cUnit);
                case SERVICE_CLASS:
                    return getServiceClass((GroovyCompilationUnit) cUnit);
                case TAGLIB_CLASS:
                    return getTagLibClass((GroovyCompilationUnit) cUnit);
                case UNIT_TEST:
                    return getTestClass((GroovyCompilationUnit) cUnit);
                case INTEGRATION_TEST:
                    return getTestClass((GroovyCompilationUnit) cUnit);
            }
        }
        return null;
    }
    

    /**
     * Finds a controller class if it exists when the package is not known
     * @param name
     * @return
     */
    public ControllerClass getControllerClass(String name) {
        return (ControllerClass) getGrailsElement(name, GrailsElementKind.CONTROLLER_CLASS);
    }
                                                           
    
    /**
     * Gets a grails element when the package is not known.
     * Will scan all packages in the appropriate source folder and 
     * look for compilation units with the given name.  If more than
     * one of that name exists, but in different folders, then an arbitrary one
     * is returned.
     * 
     * Will return null if no unit of the given name exists
     */
    private IGrailsElement getGrailsElement(String name, GrailsElementKind kind) {
    	if (!name.endsWith(".groovy")) {
    		name = name+".groovy";
    	}
        // currently only implement for Controller class
        
        if (kind != GrailsElementKind.CONTROLLER_CLASS) {
            return null;
        }
        
        try {
            IPackageFragmentRoot root = javaProject.findPackageFragmentRoot(new Path("/" + javaProject.getElementName() + "/grails-app/controllers"));
            if (root == null) {
                return null;
            }
            IJavaElement[] children = root.getChildren();
            for (IJavaElement child : children) {
                if (child instanceof IPackageFragment) {
                    IPackageFragment frag = (IPackageFragment) child;
                    ICompilationUnit unit = frag.getCompilationUnit(name);
                    if (unit.exists() && unit instanceof GroovyCompilationUnit) {
                        return new ControllerClass((GroovyCompilationUnit) unit);
                    }
                }
            }
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
        return null;
    }
    
    public TagLibClass getTagLibClass(GroovyCompilationUnit cUnit) {
        return new TagLibClass(cUnit);
    }
    
    public DomainClass getDomainClass(GroovyCompilationUnit unit) {
        if (getElementKind(unit) == GrailsElementKind.DOMAIN_CLASS) {
            return new DomainClass(unit);
        }
        return null;
    }

    public ServiceClass getServiceClass(GroovyCompilationUnit unit) {
        if (getElementKind(unit) == GrailsElementKind.SERVICE_CLASS) {
            return new ServiceClass(unit);
        }
        return null;
    }
    
    public TestClass getTestClass(GroovyCompilationUnit unit) {
        if (getElementKind(unit) == GrailsElementKind.UNIT_TEST ||
        		getElementKind(unit) == GrailsElementKind.INTEGRATION_TEST) {
            return new TestClass(unit);
        }
        return null;
    }
    
    public IGrailsElement getGrailsElement(ICompilationUnit unit) {
        GrailsElementKind kind = getElementKind(unit);
        switch (kind) {
            case DOMAIN_CLASS:
                return new DomainClass((GroovyCompilationUnit) unit);
            case CONTROLLER_CLASS:
                return new ControllerClass((GroovyCompilationUnit) unit);
            case TAGLIB_CLASS:
                return new TagLibClass((GroovyCompilationUnit) unit);
            case SERVICE_CLASS:
                return new ServiceClass((GroovyCompilationUnit) unit);
            case UNIT_TEST:
            	return new TestClass((GroovyCompilationUnit) unit);
            case INTEGRATION_TEST:
            	return new TestClass((GroovyCompilationUnit) unit);
            case BUILD_CONFIG:
            case BOOT_STRAP:
            case CONFIG:
            case DATA_SOURCE:
            case URL_MAPPINGS:
                return new GenericConfigElement((GroovyCompilationUnit) unit, kind);
            case INVALID:
                return new InvalidGrailsElement();
        }
        return new OtherGrailsElement(unit);
    }
    
	public IGrailsElement getGrailsElement(IType type) {
		ICompilationUnit cu = type.getCompilationUnit();
		if (cu!=null) {
			if (cu.getElementName().equals(type.getElementName()+".groovy")) {
				return getGrailsElement(cu);
			}
		}
		return null;
	}

    /**
     * Finds the compilation unit of the type being completed
     * will return null if the completion type is binary
     * @param context
     * @return
     */
    private ICompilationUnit getUnit(ClassNode completionType) {
        IType type = getGroovyProject().groovyClassToJavaType(completionType);
        return type != null && !type.isBinary() ? type.getCompilationUnit() : null;
    }

    /**
     * create a grails element from a groovy class node
     * @param clazz
     * @return the corresponding grails element.  Will never return null, but
     * may return {@link InvalidGrailsElement}.
     */
    public IGrailsElement getGrailsElement(ClassNode clazz) {
        String classQualifiedName = clazz.getName();
        if (classNodeGrailsElementCache.containsKey(classQualifiedName)) {
            return classNodeGrailsElementCache.get(classQualifiedName);
        }
        ICompilationUnit unit = getUnit(clazz);
        IGrailsElement elt = getGrailsElement(unit);
        classNodeGrailsElementCache.put(classQualifiedName, elt);
        return elt;
    }
    
    public List<TagLibClass> getCustomTagLibClasses() {
        IFolder folder = javaProject.getProject().getFolder("grails-app/taglib");
        List<TagLibClass> taglibs = findTagLibClassesInFolder(folder);
        return taglibs;
    }


    /**
     * @param folder
     * @return
     */
    private List<TagLibClass> findTagLibClassesInFolder(IFolder folder) {
        List<TagLibClass> taglibs = new ArrayList<TagLibClass>();
        if (folder.exists()) {
            try {
                IPackageFragmentRoot root = (IPackageFragmentRoot) JavaCore.create(folder);
                if (root != null && root.exists()) {
                    for (IJavaElement elt : root.getChildren()) {
                        if (elt.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
                            IPackageFragment pack = (IPackageFragment) elt;
                            ICompilationUnit[] units = pack.getCompilationUnits();
                            for (ICompilationUnit unit : units) {
                                if (unit instanceof GroovyCompilationUnit && unit.getElementName().endsWith("TagLib.groovy")) {
                                    taglibs.add(getTagLibClass((GroovyCompilationUnit) unit));
                                }
                            }
                        } else if (elt.getElementType() == IJavaElement.COMPILATION_UNIT) {
                            ICompilationUnit unit = (ICompilationUnit) elt;
                            if (unit instanceof GroovyCompilationUnit && unit.getElementName().endsWith("TagLib.groovy")) {
                                taglibs.add(new TagLibClass((GroovyCompilationUnit) unit));
                            }
                        }
                    }
                } else {
                    GrailsCoreActivator.log("Problem when looking for tag libraries:\n" +
                            folder.getLocation().toOSString() + " is not a source folder.", 
                            new RuntimeException());
                }
            } catch (JavaModelException e) {
                GrailsCoreActivator.log("Exception when trying to get all taglib classes for " + javaProject.getElementName(), e);
            }
        }
        return taglibs;
    }
    
    /**
     * Returns all TagLibs supplied by plugins
     * @return
     */
    public List<TagLibClass> getPluginTagLibClasses() {
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            List<TagLibClass> taglibs = new ArrayList<TagLibClass>();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            for (IClasspathEntry entry : entries) {
            	switch (entry.getEntryKind()) {
                    case IClasspathEntry.CPE_SOURCE:
                        if (isPluginTagLibSourceFolder(entry.getPath())) {
                            IFolder folder = root.getFolder(entry.getPath());
                            if (folder.isAccessible()) {
                                taglibs.addAll(findTagLibClassesInFolder(folder));
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            return taglibs;
        } catch (JavaModelException e) {
            GrailsCoreActivator.log("Problem creating tag libraries from plugins", e);
        }
        return Collections.emptyList();
    }
    
    public List<IProject> getDependentGrailsProjects() {
        try {
			return GrailsProjectUtil.getDependentGrailsProjects(javaProject);
	    } catch (JavaModelException e) {
	        GrailsCoreActivator.log("Problem creating tag libraries from plugins", e);
	    }
        return Collections.EMPTY_LIST;
    }


	public Map<String, ClassNode> findAllServices() throws JavaModelException {
        PerProjectTypeCache typeCache = GrailsCore.get().connect(javaProject.getProject(), PerProjectTypeCache.class);
        if (typeCache != null) {
            return internalFindGrailsElementsForProject(typeCache, javaProject, GrailsElementKind.SERVICE_CLASS);
        } else {
            return Collections.emptyMap();
        }
    }
    
    /**
     * @return a map of all controllers in the current grails project, including those coming
     * from plugins.  The key of the map is the controller name as a bean name (ie- simple name of
     * class with first letter lower case with the
     *  
     * @throws JavaModelException
     */
    public Map<String, ClassNode> findAllControllers() throws JavaModelException {
        PerProjectTypeCache typeCache = GrailsCore.get().connect(javaProject.getProject(), PerProjectTypeCache.class);
        if (typeCache != null) {
            return internalFindGrailsElementsForProject(typeCache, javaProject, GrailsElementKind.CONTROLLER_CLASS);
        } else {
            return Collections.emptyMap();
        }
    }
    
    
    private Map<String, ClassNode> internalFindGrailsElementsForProject(PerProjectTypeCache typeCache, IJavaProject thisProject, GrailsElementKind kind)
            throws JavaModelException {
        Map<String, ClassNode> grailsElementMap = new HashMap<String, ClassNode>();
        IClasspathEntry[] rawEntries = thisProject.getRawClasspath();
        for (IClasspathEntry entry : rawEntries) {
            // this may not capture services in plugins because the source folder is linked
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getPath().lastSegment().equals(kind.getSourceFolder())) { //$NON-NLS-1$
                IPackageFragmentRoot root = thisProject.findPackageFragmentRoot(entry.getPath());
                if (root == null)  continue;  // something is wrong with this project
                
                // all CUs that end in Service are services
                IJavaElement[] frags = root.getChildren();
                for (IJavaElement elt : frags) {
                    if (elt instanceof IPackageFragment) {
                        IPackageFragment frag = (IPackageFragment) elt;
                        ICompilationUnit[] units = frag.getCompilationUnits();
                        for (ICompilationUnit unit : units) {
                            if (unit instanceof GroovyCompilationUnit && unit.getElementName().endsWith(kind.getNameSuffix())) { //$NON-NLS-1$
                                IType graileElementType = getPrimaryType(unit);
                                if (graileElementType != null) {
                                    grailsElementMap.put(getBeanName(graileElementType), typeCache.getClassNode(graileElementType.getFullyQualifiedName()));
                                }
                            }
                        }
                    }
                }
            } else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                // trawl through dependent grails projects
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().lastSegment());
                if (GrailsNature.isGrailsPluginProject(project)) {
                    IJavaProject otherProject = JavaCore.create(project);
                    grailsElementMap.putAll(internalFindGrailsElementsForProject(typeCache, otherProject, kind));
                }
            }
        }
        return grailsElementMap;
    }
    
    public IType findControllerFromSimpleName(String name) throws JavaModelException {
        return internalFindGrailsElementTypeForProject(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Controller", GrailsCore.get().connect(javaProject.getProject(), PerProjectTypeCache.class), javaProject, GrailsElementKind.CONTROLLER_CLASS);
    }
    
    private IType internalFindGrailsElementTypeForProject(String primaryTypeName, PerProjectTypeCache typeCache, IJavaProject thisProject, GrailsElementKind kind)
            throws JavaModelException {
        String unitName = primaryTypeName + ".groovy";
        IClasspathEntry[] rawEntries = thisProject.getRawClasspath();
        for (IClasspathEntry entry : rawEntries) {
            // this may not capture services in plugins because the source folder is linked
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getPath().lastSegment().equals(kind.getSourceFolder())) { //$NON-NLS-1$
                IPackageFragmentRoot root = thisProject.findPackageFragmentRoot(entry.getPath());
                if (root == null)  continue;  // something is wrong with this project
                
                // all CUs that end in Service are services
                IJavaElement[] frags = root.getChildren();
                for (IJavaElement elt : frags) {
                    if (elt instanceof IPackageFragment) {
                        IPackageFragment frag = (IPackageFragment) elt;
                        ICompilationUnit[] units = frag.getCompilationUnits();
                        for (ICompilationUnit unit : units) {
                            if (unit instanceof GroovyCompilationUnit && unit.getElementName().equals(unitName)) { //$NON-NLS-1$
                                IType grailsElementType = unit.getType(primaryTypeName);
                                if (grailsElementType.exists()) {
                                    return grailsElementType;
                                }
                            }
                        }
                    }
                }
            } else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                // trawl through dependent grails projects
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().lastSegment());
                if (GrailsNature.isGrailsPluginProject(project)) {
                    IJavaProject otherProject = JavaCore.create(project);
                    IType type = internalFindGrailsElementTypeForProject(primaryTypeName, typeCache, otherProject, kind);
                    if (type != null) {
                        return type;
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isPluginTagLibSourceFolder(IPath path) {
        return path.segmentCount() > 2 && path.lastSegment().equals("taglib") && path.segment(1).equals(".link_to_grails_plugins");
    }
    
    /**
     * return all the taglibs that are defined by grails itself.  
     * This does not include the most basic, built in ones
     * such as 'if' and 'while'
     */
    public List<TagLibClass> getStandardTagLibClasses() {
        List<TagLibClass> taglibs = new ArrayList<TagLibClass>();
        try {
            IPackageFragment[] frags = javaProject.getPackageFragments();
            for (IPackageFragment frag : frags) {
                if (frag.getElementName().equals("org.codehaus.groovy.grails.plugins.web.taglib") && frag.exists()) {
                    // STS-3841: Actually not only one fragment can be found
                    IClassFile[] classes = frag.getClassFiles();
                    for (IClassFile classFile : classes) {
                        if (classFile.getType().getElementName().endsWith("TagLib")) {
                            taglibs.add(new BinaryTagLibClass((ClassFile) classFile));
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            GrailsCoreActivator.log("Exception raised when looking for Grails tag libaries.  " +
                    "They typically reside in the package 'org.codehaus.groovy.grails.plugins.web.taglib'.",
                    e);
        }
        return taglibs;
    }
    
    
    /**
     * For testing
     */
    private static Map<String, GrailsElementKind> cuHandleToElementKind = new HashMap<String, GrailsElementKind>();

    /**
     * This class is not public API.  It is used for testing to populate grails projects with default
     * element kinds
     * @param compilationUnitHandle
     * @param kind
     */
    public static void addExtraGrailsElement(GroovyCompilationUnit unit, GrailsElementKind kind) {
        cuHandleToElementKind.put(unit.getHandleIdentifier(), kind);
    }
    /**
     * This class is not public api.  Used for testing only
     * @param compilationUnitHandle
     */
    public static void removeExtraGrailsElement(GroovyCompilationUnit unit) {
        cuHandleToElementKind.remove(unit.getHandleIdentifier());
    }

	public IJavaProject getJavaProject() {
		return getGroovyProject().getProject();
	}

	/**
	 * This is a handle only method.
	 * 
	 * @return IFolder referring to the 'views' folder in the Grails project.
	 */
	public IFolder getViewsFolder() {
		return getJavaProject().getProject().getFolder(new Path("grails-app/views"));
	}

	
	public GrailsVersion getGrailsVersion() {
	    return GrailsVersion.getGrailsVersion(javaProject.getProject());
	}
	
	public GrailsVersion getEclipseGrailsVersion() {
	    return GrailsVersion.getEclipseGrailsVersion(javaProject.getProject());
	}


	public List<IFile> getGSPFiles() {
		IFolder parent = getViewsFolder();
		final ArrayList<IFile> gspFiles = new ArrayList<IFile>();
		try {
			parent.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if ((resource.getType()&IResource.FILE)!=0) {
						if (resource.getName().endsWith(".gsp")) {
							gspFiles.add((IFile) resource);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
		return gspFiles;
	}


}
