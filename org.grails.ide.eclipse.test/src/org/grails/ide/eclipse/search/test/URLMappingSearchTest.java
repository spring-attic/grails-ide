/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.search.test;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.search.action.ControllerActionSearch;
import org.grails.ide.eclipse.search.controller.ControllerTypeSearch;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;

public class URLMappingSearchTest extends AbstractGrailsSearchParticipantTest {

	private static final boolean SIMPLE_TEMPLATE = true;
	/* DONE:
       "/product"(controller:"product") 
       "/showPeople" {
           controller = 'person' 
       }
       name personList: "/showPeople" {
           controller = 'person'
       }
       "/product"(controller:"product", action:"list") 
     * "/help"(controller:"site",view:"help") 
     * name personList: "/showPeople" {
     *     controller = 'person'  // link support to the controller
     *     action = 'list'  // link support to the action
     * }
       name personList: "/showPeople" {
           controller = 'person'
           action = 'list'
       }
     * "/showPeople" {
     *     controller = 'person' 
     *     action = 'list'
     * }
     * "/product/$id"(controller:"product"){
     *    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
     *  }    
     *  TODO: 
     *  "403"(view: "/errors/forbidden" 
     */	
	
	protected IProject project;
	protected IJavaProject javaProject;
	protected GrailsProject grailsProject;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		GrailsProjectVersionFixer.globalAskToConvertToGrailsProjectAnswer = true;
		project = ensureProject(TEST_PROJECT_NAME);
		javaProject = JavaCore.create(project);
		grailsProject = GrailsWorkspaceCore.get().create(project);
	}

	public void testComplexAction() throws Exception {
		String targetControllerName = "booger"; //The name of the controller who's action is being renamed
		String targetActionName = "off";
		
		String snippet = "\"/finish\"(controller:\"booger\") {" +
				"            action = [GET:'showoff', PUT:'off', DELETE:'off', POST:'save']\n" +
				"    }";
		String expectSnippet = "\"/finish\"(controller:\"booger\") {" +
				"            action = [GET:'showoff', PUT:'###', DELETE:'###', POST:'save']\n" +
				"    }";
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}
	
	public void testSimpleAction() throws Exception {
		String targetControllerName = "booger"; //The name of the controller who's action is being renamed
		String targetActionName = "off";
		
		String snippet =       "\"/finish\"(controller:\"booger\", action: \"off\")";
		String expectSnippet = "\"/finish\"(controller:\"booger\", action: \"###\")";
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}
	
	public void testClosureAction() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		String targetActionName = "list";
		
		String snippet = "\"/showPeople\" {\n" + 
				"            controller = 'person' \n" + 
				"            action = 'list'\n" + 
				"        }\n";
		String expectSnippet = "\"/showPeople\" {\n" + 
				"            controller = 'person' \n" + 
				"            action = '####'\n" + 
				"        }\n";
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}
	
	public void testMixedAction() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		String targetActionName = "list";
		
		String snippet = "\"/showPeople\"(controller: 'person') {\n" + 
				"            action = 'list'\n" + 
				"        }\n";
		String expectSnippet = "\"/showPeople\"(controller: 'person') {\n" + 
				"            action = '####'\n" + 
				"        }\n";;
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}
	
	public void testNonMatchingControllerAction() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		String targetActionName = "off";
		
		String snippet =       "\"/finish\"(controller:\"booger\", action: \"off\")";
		String expectSnippet = "\"/finish\"(controller:\"booger\", action: \"off\")";
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}
	
	public void testMissingControllerAction() throws Exception {
		//If controller is missing, the code is really not valid, but it shouldn't cause a crash or inadvertent match.
		String targetControllerName = "person"; 
		String targetActionName = "off";
		
		String snippet =       "\"/finish\"(action: \"off\")";
		String expectSnippet = "\"/finish\"(action: \"off\")";
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}

	public void testNamedActionMapping() throws Exception {
		String targetControllerName = "person";
		String targetActionName = "list";
		
		String snippet =       
				"       name personList: \"/showPeople\" {\n" + 
				"           controller = 'person'\n" + 
				"           action = 'list'\n" + 
				"       }";
		String expectSnippet =
				"       name personList: \"/showPeople\" {\n" + 
				"           controller = 'person'\n" + 
				"           action = '####'\n" + 
				"       }";
		
		doTestActionSnippet(targetControllerName, targetActionName,  snippet, expectSnippet);
	}
	
	public void testNamedMapping() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet =       
				"       name personList: \"/showPeople\" {\n" + 
				"           controller = 'person'\n" + 
				"           action = 'list'\n" + 
				"       }";
		String expectSnippet =
				"       name personList: \"/showPeople\" {\n" + 
				"           controller = '######'\n" + 
				"           action = 'list'\n" + 
				"       }";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testSimple() throws Exception {
		String targetControllerName = "booger"; //The name of the controller who's action is being renamed
		
		String snippet =       "\"/finish\"(controller:\"booger\", action: \"off\")";
		String expectSnippet = "\"/finish\"(controller:\"######\", action: \"off\")";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testSingleQuotes() throws Exception {
		String targetControllerName = "booger"; //The name of the controller who's action is being renamed
		
		String snippet =       "";
		String expectSnippet = "\"/finish\"(controller:\'######\', action: \"off\")";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}

	public void testMixedArgs1() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet = 
				"\"/showPeople\"(controller: 'person') {\n" + 
				"           action = 'show' \n" + 
				"       }\n";
		String expectSnippet = 
				"\"/showPeople\"(controller: '######') {\n" + 
				"           action = 'show' \n" + 
				"       }\n";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testMixedArgs2() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet = 
				"\"/showPeople\"(action: 'show') {\n" + 
				"           controller = 'person'\n" + 
				"       }\n";
		String expectSnippet = 
				"\"/showPeople\"(action: 'show') {\n" + 
				"           controller = '######'\n" + 
				"       }\n";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testMixedArgs3() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet = 
				"\"/showPeople\"() {\n" + 
				"           controller = 'person'\n" + 
				"       }\n";
		String expectSnippet = 
				"\"/showPeople\"() {\n" + 
				"           controller = '######'\n" + 
				"       }\n";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testControllerInClosureArg() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet = "\"/showPeople\" {\n" + 
				"           controller = 'person' \n" + 
				"       }\n";
		String expectSnippet ="\"/showPeople\" {\n" + 
				"           controller = '######' \n" + 
				"       }\n";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testFunkyInClosureArg() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet = "\"/showPeople\" {\n" + 
				"           controller = 'per'+'son' \n" + 
				"       }\n";
		String expectSnippet ="\"/showPeople\" {\n" + 
				"           controller = 'per'+'son' \n" + 
				"       }\n";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	
	public void testFunkyNamedArg() throws Exception {
		String targetControllerName = "person"; //The name of the controller who's action is being renamed
		
		String snippet = "\"/showPeople\"(controller: 'per'+'son')";
		String expectSnippet = "\"/showPeople\"(controller: 'per'+'son')";
		
		doTestSnippet(targetControllerName, snippet, expectSnippet);
	}
	

	private void doTestSnippet(String targetControllerName, String snippet, String expectSnippet) throws IOException, CoreException {
		String urlMappingsPath = "grails-app/conf/UrlMappings.groovy";
		String template = 
				SIMPLE_TEMPLATE ?
					"class UrlMappings {\n" + 
					"\n" + 
					"	static mappings = {\n" + 
					"		***\n" + 
					"	}\n" + 
					"}"
				:
					"class UrlMappings {\n" + 
					"\n" + 
					"	static mappings = {\n" + 
					"		\"/$controller/$action?/$id?\"{\n" + 
					"			constraints {\n" + 
					"				// apply constraints here\n" + 
					"			}\n" + 
					"		}\n" + 
					"		***\n" + 
					"		\"/\"(view:\"/index\")\n" + 
					"		\"500\"(view:'/error')\n" + 
					"	}\n" + 
					"}";
		tmpReplaceResource(project, urlMappingsPath, template.replace("***", snippet));
		
		ControllerTypeSearch search = new ControllerTypeSearch(grailsProject, targetControllerName);
		assertMatches(search, determineExpectedMatches(field(javaProject, null, "UrlMappings", "mappings"), 
				template, expectSnippet));
	}

	private void doTestActionSnippet(String targetControllerName, String targetActionName, String snippet, String expectSnippet) throws IOException, CoreException {
		String urlMappingsPath = "grails-app/conf/UrlMappings.groovy";
		String template = 
				SIMPLE_TEMPLATE ?
					"class UrlMappings {\n" + 
					"\n" + 
					"	static mappings = {\n" + 
					"		***\n" + 
					"	}\n" + 
					"}"
				:
					"class UrlMappings {\n" + 
					"\n" + 
					"	static mappings = {\n" + 
					"		\"/$controller/$action?/$id?\"{\n" + 
					"			constraints {\n" + 
					"				// apply constraints here\n" + 
					"			}\n" + 
					"		}\n" + 
					"		***\n" + 
					"		\"/\"(view:\"/index\")\n" + 
					"		\"500\"(view:'/error')\n" + 
					"	}\n" + 
					"}";
		tmpReplaceResource(project, urlMappingsPath, template.replace("***", snippet));
		
		ControllerActionSearch search = new ControllerActionSearch(grailsProject, 
				GrailsNameUtils.getClassName(targetControllerName, ControllerClass.CONTROLLER),
				targetActionName);
		assertMatches(search, determineExpectedMatches(field(javaProject, null, "UrlMappings", "mappings"), 
				template, expectSnippet));
	}
	
}
