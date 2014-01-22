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

import groovyjarjarasm.asm.Opcodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ClassNode;
import org.grails.ide.eclipse.core.model.ContributedMethod;
import org.grails.ide.eclipse.core.model.ContributedProperty;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * Plugin data for a grails plugin. We don't capture everything here. Only the
 * basic information and the information specific
 * <p>
 * This contains data for a Grails plugin dependency, and as such, is distinct from
 * the Grails plugin model that is used by other components such as the Grails plugin manager or
 * Grails project explorer
 * </p>
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 28, 2010
 */
public class GrailsPluginVersion extends PluginVersion {

	// these are the really important ones
	// property name to type
	private Map<String, ContributedProperty> domainProperties;
	private Map<String, ContributedProperty> controllerProperties;
	// method name, to types (first is the return type, after are types of the
	// arguments)
	private Map<String, Set<ContributedMethod>> domainMethods;
	private Map<String, Set<ContributedMethod>> controllerMethods;

	public GrailsPluginVersion() {
	}

	/**
	 * Return true if there is at least one domain or controller method or
	 * property. Return false if all are empty (no domain or controller methods
	 * or properties)
	 * 
	 * @return
	 */
	public boolean hasContributedPropertiesOrMethods() {
		return !getDomainMethods().isEmpty()
				|| !getDomainProperties().isEmpty()
				|| !getControllerProperties().isEmpty()
				|| !getControllerMethods().isEmpty();
	}

	void addDomainProperty(String propertyName, ClassNode type) {
		if (this.domainProperties == null) {
			domainProperties = new HashMap<String, ContributedProperty>();
		}
		domainProperties.put(propertyName, new ContributedProperty(
				propertyName, type, Opcodes.ACC_PUBLIC, getName()));
	}

	void addControllerProperty(String propertyName, ClassNode type) {
		if (this.controllerProperties == null) {
			controllerProperties = new HashMap<String, ContributedProperty>();
		}
		controllerProperties.put(propertyName, new ContributedProperty(
				propertyName, type, Opcodes.ACC_PUBLIC, getName()));
	}

	void addDomainMethod(String methodName, ClassNode returnType,
			ClassNode[] parameterTypes) {
		if (this.domainMethods == null) {
			domainMethods = new HashMap<String, Set<ContributedMethod>>();
		}
		Set<ContributedMethod> methods = domainMethods.get(methodName);
		if (methods == null) {
			methods = new LinkedHashSet<ContributedMethod>();
			domainMethods.put(methodName, methods);
		}
		methods.add(new ContributedMethod(methodName, returnType,
				parameterTypes, Opcodes.ACC_PUBLIC, getName()));
	}

	void addControllerMethod(String methodName, ClassNode returnType,
			ClassNode[] parameterTypes) {
		if (this.controllerMethods == null) {
			controllerMethods = new HashMap<String, Set<ContributedMethod>>();
		}
		Set<ContributedMethod> methods = controllerMethods.get(methodName);
		if (methods == null) {
			methods = new LinkedHashSet<ContributedMethod>();
			controllerMethods.put(methodName, methods);
		}
		methods.add(new ContributedMethod(methodName, returnType,
				parameterTypes, Opcodes.ACC_PUBLIC, getName()));
	}

	public Map<String, ContributedProperty> getDomainProperties() {
		return domainProperties == null ? Collections.EMPTY_MAP
				: domainProperties;
	}

	public Map<String, ContributedProperty> getControllerProperties() {
		return controllerProperties == null ? Collections.EMPTY_MAP
				: controllerProperties;
	}

	public Map<String, Set<ContributedMethod>> getDomainMethods() {
		return domainMethods == null ? Collections.EMPTY_MAP : domainMethods;
	}

	public Map<String, Set<ContributedMethod>> getControllerMethods() {
		return controllerMethods == null ? Collections.EMPTY_MAP
				: controllerMethods;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PluginData [domainProperties=");
        builder.append(domainProperties);
        builder.append(", controllerProperties=");
        builder.append(controllerProperties);
        builder.append(", domainMethods=");
        builder.append(domainMethods);
        builder.append(", controllerMethods=");
        builder.append(controllerMethods);
        builder.append("]");
        return builder.toString();
    }
}
