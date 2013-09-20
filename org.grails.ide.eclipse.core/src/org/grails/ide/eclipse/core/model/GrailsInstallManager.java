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
package org.grails.ide.eclipse.core.model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.springsource.ide.eclipse.commons.core.SpringCorePreferences;
import org.springsource.ide.eclipse.commons.core.SpringCoreUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * @author Christian Dupuis
 * @author Kris De Volder
 */
public class GrailsInstallManager {

	private Map<String, IGrailsInstall> installs = new ConcurrentHashMap<String, IGrailsInstall>();

	private List<IGrailsInstallListener> listeners = new ArrayList<IGrailsInstallListener>();

	public IGrailsInstall getGrailsInstall(IProject project) {
		if (project == null) {
			return getDefaultGrailsInstall();
		}
		if (!SpringCorePreferences.getProjectPreferences(project, GrailsCoreActivator.PLUGIN_ID).getBoolean(
				GrailsCoreActivator.PROJECT_PROPERTY_ID, false)) {
			return getDefaultGrailsInstall();
		}
		else if (SpringCorePreferences.getProjectPreferences(project, GrailsCoreActivator.PLUGIN_ID).getString(
				GrailsCoreActivator.GRAILS_INSTALL_PROPERTY, null) != null) {
			return getGrailsInstall(SpringCorePreferences.getProjectPreferences(project, GrailsCoreActivator.PLUGIN_ID)
					.getString(GrailsCoreActivator.GRAILS_INSTALL_PROPERTY, null));
		}
		return null;
	}

	public IGrailsInstall getGrailsInstall(String name) {
		return installs.get(name);
	}

	public IGrailsInstall getDefaultGrailsInstall() {
		for (IGrailsInstall install : installs.values()) {
			if (install.isDefault()) {
				return install;
			}
		}
		if (installs.size() > 0) {
			return installs.values().iterator().next();
		}
		return null;
	}

	public void start() {
		try {
			boolean readFromLegacyLocation = false;
			DocumentBuilder docBuilder = SpringCoreUtils.getDocumentBuilder();
			IPath grailsInstallFile = GrailsCoreActivator.getDefault().getStateLocation().append("grails.installs");
			if (!grailsInstallFile.toFile().exists()) {
				//Try legacy state location.
				//path looks like this: <workspace>/.metadata/.plugins/org.grails.ide.eclipse.core/grails.installs
				//legacypath like this:  <workspace>/.metadata/.plugins/OLD_PLUGIN_ID/grails.installs
				grailsInstallFile = grailsInstallFile.removeLastSegments(2).append(GrailsCoreActivator.OLD_PLUGIN_ID+"/grails.installs");
				readFromLegacyLocation = true;
			}
			if (grailsInstallFile.toFile().exists()) {
				Document doc = docBuilder.parse(grailsInstallFile.toFile());
				NodeList installNodes = doc.getElementsByTagName("install");
				for (int i = 0; i < installNodes.getLength(); i++) {
					Node installNode = installNodes.item(i);
					String name = null;
					String home = null;
					boolean isDefault = false;

					NodeList installChildren = installNode.getChildNodes();
					for (int j = 0; j < installChildren.getLength(); j++) {
						Node installChild = installChildren.item(j);
						if ("name".equals(installChild.getNodeName())) {
							name = installChild.getTextContent();
						}
						else if ("home".equals(installChild.getNodeName())) {
							home = installChild.getTextContent();
						}
					}

					Node defaultNode = installNode.getAttributes().getNamedItem("is-default");
					if (defaultNode != null && defaultNode.getNodeValue().equalsIgnoreCase("true")) {
						isDefault = true;
					}

					if (name != null && home != null) {
						DefaultGrailsInstall install = new DefaultGrailsInstall(home, name, isDefault);
						installs.put(name, install);
					}
					else {
						GrailsCoreActivator.log("Discarding Grails install [" + home + "] with name [" + name + "]",
								null);
					}
					if (readFromLegacyLocation) {
						//Make a copy in the new location (consistent with how other preferences files are migrated)
						save(doc);
					}
				}
			}
		}
		catch (SAXException e) {
			GrailsCoreActivator.log(e);
		}
		catch (IOException e) {
			GrailsCoreActivator.log(e);
		}
	}

	public void setGrailsInstalls(Set<IGrailsInstall> newInstalls) {
		IGrailsInstall oldDefault = getDefaultGrailsInstall();
		installs.clear();

		DocumentBuilder documentBuilder = SpringCoreUtils.getDocumentBuilder();
		Document document = documentBuilder.newDocument();

		Element root = document.createElement("installs");
		document.appendChild(root);

		for (IGrailsInstall install : newInstalls) {
			installs.put(install.getName(), install);

			Element installNode = document.createElement("install");
			root.appendChild(installNode);

			// add is-default attribute
			Attr isDefaultAttribute = document.createAttribute("is-default");
			installNode.setAttributeNode(isDefaultAttribute);
			isDefaultAttribute.setValue(Boolean.toString(install.isDefault()));

			// add home element
			Element homeNode = document.createElement("home");
			installNode.appendChild(homeNode);
			homeNode.appendChild(document.createTextNode(install.getHome()));

			Element nameNode = document.createElement("name");
			installNode.appendChild(nameNode);
			nameNode.appendChild(document.createTextNode(install.getName()));
		}

		save(document);
		
		IGrailsInstall newDefault = getDefaultGrailsInstall();
		for (IGrailsInstallListener listener : listeners) {
			listener.installsChanged(newInstalls);
			if (!equals(oldDefault, newDefault)) {
				listener.defaultInstallChanged(oldDefault, newDefault);
			}
		}
	}

	
	/**
	 * Equals comparison that is null pointer safe.
	 */
	private boolean equals(Object x, Object y) {
		if (x==null) {
			return y==null;
		} else {
			return x.equals(y);
		}
	}

	private void save(Document document) {
		try {
			IPath grailsInstallFile = GrailsCoreActivator.getDefault().getStateLocation().append("grails.installs");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

			Writer out = new OutputStreamWriter(new FileOutputStream(grailsInstallFile.toFile()), "ISO-8859-1");
			StreamResult result = new StreamResult(out);
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);
			out.close();
		} catch (IOException e) {
			GrailsCoreActivator.log(e);
		}
		catch (TransformerException e) {
			GrailsCoreActivator.log(e);
		}
	}

	public void addGrailsInstallListener(IGrailsInstallListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeGrailsInstallListener(IGrailsInstallListener listener) {
		listeners.remove(listener);
	}

	public Collection<IGrailsInstall> getAllInstalls() {
		Set<IGrailsInstall> newInstalls = new HashSet<IGrailsInstall>();
		for (IGrailsInstall install : installs.values()) {
			newInstalls.add(new DefaultGrailsInstall(install.getHome(), install.getName(), install.isDefault()));
		}
		return newInstalls;
	}

	public String[] getAllInstallNames() {
		Set<String> newInstalls = new HashSet<String>();
		for (IGrailsInstall install : installs.values()) {
			newInstalls.add(install.getName());
		}
		return newInstalls.toArray(new String[newInstalls.size()]);
	}

	/**
	 * @return A grails install of the specified version, or null if no such install is configured in the
	 * workspace.
	 */
	public IGrailsInstall getInstallFor(GrailsVersion grailsVersion) {
		Collection<IGrailsInstall> candidates = getAllInstalls();
		for (IGrailsInstall install : candidates) {
			if (grailsVersion.equals(install.getVersion())) {
				return install;
			}
		}
		return null;
	}

	public static void setGrailsInstall(IProject project, boolean isDefault,
			String grailsInstallName) {
		SpringCorePreferences.getProjectPreferences(project,
				GrailsCoreActivator.PLUGIN_ID).putBoolean(
				GrailsCoreActivator.PROJECT_PROPERTY_ID, !isDefault);
		if (grailsInstallName != null) {
			SpringCorePreferences.getProjectPreferences(project,
					GrailsCoreActivator.PLUGIN_ID).putString(
					GrailsCoreActivator.GRAILS_INSTALL_PROPERTY,
					grailsInstallName);
		}
	}
	
	public static boolean inheritsDefaultInstall(IProject project) {
		return !SpringCorePreferences.getProjectPreferences(project, GrailsCoreActivator.PLUGIN_ID)
			.getBoolean(GrailsCoreActivator.PROJECT_PROPERTY_ID, false);
	}

}
