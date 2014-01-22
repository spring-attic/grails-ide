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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 28, 2010
 */
@SuppressWarnings("nls")
public class PluginDescriptorParser implements IPluginParser {

	private static final String PROPERTY = "property";
	private static final String ARGUMENT = "argument";
	private static final String METHOD = "method";
	private static final String TYPE = "type";
	private static final String CONTROLLER = "controller";
	private static final String DOMAIN = "domain";
	private static final String ARTEFACT = "artefact";
	private static final String BEHAVIOR = "behavior";
	private static final String DESCRIPTION = "description";
	private static final String TITLE = "title";
	private static final String AUTHOR = "author";
	private static final String GRAILS_VERSION = "grailsVersion";
	private static final String VERSION = "version";
	private static final String NAME = "name";
	private static final String PLUGIN = "plugin";
	private static final String BEAHVIOR_TYPE_NAME = "org.grails.Behavior";

	private final String pluginFileLocation;

	public PluginDescriptorParser(String pluginFileLocation) {
		this.pluginFileLocation = pluginFileLocation;
	}

	public GrailsPluginVersion parse() {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		GrailsPluginVersion data = new GrailsPluginVersion();
		try {
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

			Document doc = docBuilder.parse(new File(pluginFileLocation)
					.toURI().toString());

			NodeList nodes = doc.getElementsByTagName(PLUGIN);
			if (nodes != null && nodes.getLength() > 0) {
				// should have exactly one element
				Node pluginNode = nodes.item(0);
				NamedNodeMap attrMap = pluginNode.getAttributes();
				if (attrMap != null) {
					Node attrNode = attrMap.getNamedItem(NAME);
					if (attrNode != null) {
						data.setName(attrNode.getTextContent());
					}
					attrNode = attrMap.getNamedItem(VERSION);
					if (attrNode != null) {
						data.setVersion(attrNode.getTextContent());
					}
					attrNode = attrMap.getNamedItem(GRAILS_VERSION);
					if (attrNode != null) {
						data.setRuntimeVersion(attrNode.getTextContent());
					}
				}

				NodeList childNodes = pluginNode.getChildNodes();
				if (childNodes != null) {
					for (int i = 0, length = childNodes.getLength(); i < length; i++) {
						Node childNode = childNodes.item(i);
						String nodeName = childNode.getNodeName();
						if (nodeName.equals(AUTHOR)) {
							data.setAuthor(handleSimpleNode(childNode));
						} else if (nodeName.equals(TITLE)) {
							data.setTitle(handleSimpleNode(childNode));
						} else if (nodeName.equals(DESCRIPTION)) {
							data.setDescription(handleSimpleNode(childNode));
						} else if (nodeName.equals(BEHAVIOR)) {
							handleBehavior(childNode, data);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			GrailsCoreActivator.log(e);
		} catch (ParserConfigurationException e) {
			GrailsCoreActivator.log(e);
		} catch (SAXException e) {
			GrailsCoreActivator.log(e);
		} catch (IOException e) {
			GrailsCoreActivator.log(e);
		}
		return data;
	}

	private void handleBehavior(Node behaviorNodeParent, GrailsPluginVersion data) {
		NodeList behaviorNodes = behaviorNodeParent.getChildNodes();
		if (behaviorNodes == null) {
			return;
		}
		for (int i = 0, length = behaviorNodes.getLength(); i < length; i++) {
			Node behaviorNode = behaviorNodes.item(i);
			NamedNodeMap attrs = behaviorNode.getAttributes();
			if (attrs == null) {
				continue;
			}

			Node attr = attrs.getNamedItem(ARTEFACT);
			String artefact;
			if (attr != null) {
				artefact = attr.getTextContent();
			} else {
				// malformed
				continue;
			}
			// any other artefacts we are interested in???
			boolean isDomain = artefact.toLowerCase().equals(DOMAIN);
			boolean isController = artefact.toLowerCase().equals(CONTROLLER);
			if (!(isDomain || isController)) {
				continue;
			}

			attr = attrs.getNamedItem(NAME);
			String name;
			if (attr != null) {
				name = attr.getTextContent();
			} else {
				// malformed
				continue;
			}

			attr = attrs.getNamedItem(TYPE);
			String typeName;
			if (attr != null) {
				typeName = attr.getTextContent();
			} else {
				// malformed
				continue;
			}

			ClassNode type = getClassForType(typeName);

			if (behaviorNode.getNodeName().equals(METHOD)) {
				List<ClassNode> paramTypes = new ArrayList<ClassNode>();
				// get arguments
				NodeList args = behaviorNode.getChildNodes();
				for (int iArgs = 0, lengthArgs = args.getLength(); iArgs < lengthArgs; iArgs++) {
					Node arg = args.item(iArgs);
					if (arg.getNodeName().equals(ARGUMENT)) {
						NamedNodeMap typeAttr = arg.getAttributes();
						if (typeAttr != null) {
							Node argType = typeAttr.getNamedItem(TYPE);
							if (argType != null) {
								ClassNode argClass = getClassForType(argType
										.getTextContent());
								paramTypes.add(argClass);
							}
						}
					}
				}
				ClassNode[] paramTypesArr = paramTypes
						.toArray(new ClassNode[paramTypes.size()]);
				if (isDomain) {
					data.addDomainMethod(name, type, paramTypesArr);
				} else if (isController) {
					data.addControllerMethod(name, type, paramTypesArr);
				}
			} else if (behaviorNode.getNodeName().equals(PROPERTY)) {
				if (isDomain) {
					data.addDomainProperty(name, type);
				} else if (isController) {
					data.addControllerProperty(name, type);
				}
			}
		}
	}

	/**
	 * We don't want to use a ClassHelper. should use PerProjectTypeCache
	 */
	private ClassNode getClassForType(String type) {
		if (type.equals(BEAHVIOR_TYPE_NAME)) {
			return null; // filled in later
		}
		return ClassHelper.make(type);
	}

	private String handleSimpleNode(Node node) {
		return node.getFirstChild().getTextContent();
	}
}
