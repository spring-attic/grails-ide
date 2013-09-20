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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Caches association between dependencies in classpath container and their sources and javadocs.
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class PerProjectAttachementsCache implements IGrailsProjectInfo {

	/**
	 * Determines key used to find attachements associated with a jar. We need to use
	 * this because the jar themselves aren't unique (the ones from buildsettings are in different
	 * locations than the one returned by the Grails 2.0 refresh-dependencies command.
	 */
	public static String getJarKey(String jar) {
		Path jarPath = new Path(jar);
		return jarPath.lastSegment();
	}

	/**
	 * A parsed xml dependency element from the xml file produced by grails refresh dependencies.
	 * 
	 * @author Kris De Volder
	 *
	 * @since 2.8
	 */
    public static class Dependency {
    	private String key;
    	private String sources;
    	private String javadoc;
    	
		public String getSources() {
			return sources;
		}
		public String getJavadoc() {
			return javadoc;
		}
		public Dependency(String jar, String sources, String javadoc) {
			this.key = getJarKey(jar);
			this.sources = sources;
			this.javadoc = javadoc;
		}
		/**
		 * True the element contains no real data (i.e. no source or javadoc associated with the jar.
		 */
		public boolean isEmpty() {
			return key==null || sources==null && javadoc==null;
		}
		public String getKey() {
			return key;
		}
	}

	private IProject project;
    
    private Map<String, Dependency> data;
    
    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        // don't care
    }

    public void dispose() {
    	synchronized (GrailsCore.get().getLockForProject(project)) {
    		data = null; //purge the data
    		// But do not purge the data file. See: https://issuetracker.springsource.com/browse/STS-2538
    		// When refresh dependencies is called all caches for that project will be disposed. See
    		// org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainerUpdateJob.runInWorkspace(IProgressMonitor)
    		// and issue STS-2247
    		
    		//    		String fileName = GrailsClasspathUtils.getDependencySourcesDescriptorName(project);
    		//    		if (fileName!=null) {
    		//    			//filename can be null when eclipse is shutting down, because we can't determine plugin state location.
    		//    			File dataFile = new File(fileName);
    		//    			if (dataFile.exists()) {
    		//    				dataFile.delete();
    		//    			}
    		//    		}
    	}
    }

    public void refreshData() {
        data = null;
    }
    
    public Dependency getAttachments(String dependency) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            if (data == null) {
                data = parseData(new File(GrailsClasspathUtils.getDependencySourcesDescriptorName(project)));
            }
            if (data!=null) {
            	return data.get(dependency);
            }
            return null;
        }
    }

    public IPath getSourceAttachement(String dependency) {
    	String key = getJarKey(dependency);
    	Dependency att = getAttachments(key);
    	if (att!=null) {
    		return new Path(att.getSources());
    	}
    	return null;
    }

    /**
     * This method parses attachement data from file. The method is only plublic to allow it to be used in unit testing.
     * In normal operation the data should only be accessed via the cache.
     */
	public static Map<String, Dependency> parseData(File file) {
		Map<String, Dependency> result = new HashMap<String, Dependency>();
		try {
			if (file!=null && file.exists()) {
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(file);
				NodeList allDeps = doc.getElementsByTagName("dependency");
				for (int i = 0; i < allDeps.getLength(); i++) {
					Node dep = allDeps.item(i);
					Dependency parsed = parseDependency(dep);
					if (parsed!=null) {
						result.put(parsed.getKey(), parsed);
					}
				}
				return result;
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		if (!result.isEmpty()) {
			return result; // in case of errors may still have partial data!
		}
		return null;
	}

	private static Dependency parseDependency(Node dep) {
		if (dep.getNodeType()==Node.ELEMENT_NODE) {
			Element el = (Element) dep;
			if (el.getNodeName().equals("dependency")) {
				Dependency parsed = new Dependency(
						DomUtils.getChildElementValueByTagName(el, "jar"),
						DomUtils.getChildElementValueByTagName(el, "source"), 
						DomUtils.getChildElementValueByTagName(el, "javadoc")); 
				if (!parsed.isEmpty()) {
					return parsed;
				}
			}
		}
		return null;
	}

	public static PerProjectAttachementsCache get(IProject project) {
		PerProjectAttachementsCache cache = GrailsCore.get().connect(project, PerProjectAttachementsCache.class);
		return cache;
	}
}
