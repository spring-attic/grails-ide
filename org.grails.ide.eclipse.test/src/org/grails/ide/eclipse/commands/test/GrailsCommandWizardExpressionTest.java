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
package org.grails.ide.eclipse.commands.test;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommandDescriptor;


/**
 * There is some ordering to the tests
 * The create-domain-class test must be called before the 
 * generate-* tests
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @created Sep 17, 2010
 */
public class GrailsCommandWizardExpressionTest extends
		AbstractGrailsCommandWizardHarnessTest {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        project = ensureProject("bart");
        //On build server, some tests are failing because mismatching grails version (default
        // install was modified by some other tests, since creating the bart project?)
        ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(project));
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        
        // delete some files that may be recreated by other  
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        try {
            project.getFile("grails-app/controllers/bart/StoreController.groovy").delete(true, null);
        } catch (Exception e) {
        }
        try {
            project.getFolder("grails-app/views/store").delete(true, null);
        } catch (Exception e) {
        }
        try {
            project.getFolder("test/unit/bart/StoreTests.groovy").delete(true, null);
        } catch (Exception e) {
        }
        try {
            project.getFolder("test/unit/bart/StoreControllerTests.groovy").delete(true, null);
        } catch (Exception e) {
        }
    }

	/**
	 * Basic test to make sure every registered command has a name and a
	 * non-empty list of parameter descriptors
	 * 
	 * @throws Exception
	 */
	public void testBasicCommandDefinition() throws Exception {
		Collection<IFrameworkCommandDescriptor> registeredDescriptors = GrailsCommandFactory
				.getAllCommands();
		for (IFrameworkCommandDescriptor descriptor : registeredDescriptors) {
			assertNotNull(descriptor.getName());
			assertNotNull(descriptor.getParameters());
		}
	}

	
	public void test_ADDPROXY_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.ADD_PROXY);
		assertCommandDefinition(instance, "add-proxy", 5);
		
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("Store");
        
        parameter = getParameter("host", instance);
        parameter.setValue("a");
        
        parameter = getParameter("port", instance);
        parameter.setValue("b");
        
        parameter = getParameter("username", instance);
        parameter.setValue("c");
        
        parameter = getParameter("password", instance);
        parameter.setValue("d");
        
        assertExpectedCommandString(instance, "add-proxy Store --host=a --port=b --username=c --password=d");
		assertCommandExecution(instance, project);
	}

	public void test_BUGREPORT_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.BUG_REPORT);
		assertCommandDefinition(instance, "bug-report", 0);
		assertExpectedCommandString(instance, "bug-report");
        assertCommandExecution(instance, project);
	}

	public void test_CLEANexpression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CLEAN);
		assertCommandDefinition(instance, "clean", 0);
		assertExpectedCommandString(instance, "clean");
		assertCommandExecution(instance, project);
	}

	public void test_CLEARPROXY_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CLEAR_PROXY);
		assertCommandDefinition(instance, "clear-proxy", 0);
		assertExpectedCommandString(instance, "clear-proxy");
        assertCommandExecution(instance, project);
	}

	public void test_COMPILE_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.COMPILE);
		assertCommandDefinition(instance, "compile", 0);
		assertExpectedCommandString(instance, "compile");
        assertCommandExecution(instance, project);
	}

	public void test_CREATECONTROLLER_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_CONTROLLER);
		assertCommandDefinition(instance, "create-controller", 1);
		ICommandParameter parameter = getParameter("name", instance);
		parameter.setValue("Store");
		assertExpectedCommandString(instance, "create-controller Store");
		assertCommandExecution(instance, project);
	}

	public void test_CREATEDOMAINCLASS_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_DOMAIN_CLASS);
		assertCommandDefinition(instance, "create-domain-class", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("Store");
        assertExpectedCommandString(instance, "create-domain-class Store");
		assertCommandExecution(instance, project);
	}

	
    public void test_CREATEDOMAINCLASS_expression_failure() throws Exception {
        IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_DOMAIN_CLASS);
        assertCommandDefinition(instance, "create-domain-class", 1);
        assertExpectedCommandString(instance, "create-domain-class");
        // should fail because no class name is set
        try {
            assertCommandExecution(instance, project, false);
            fail("Expecting an exception to be thrown");
        } catch (CoreException e) {
            
        }
    }

	public void test_CREATEFILTERS_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_FILTERS);
		assertCommandDefinition(instance, "create-filters", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("Store");
        assertExpectedCommandString(instance, "create-filters Store");
		assertCommandExecution(instance, project);
	}

	public void test_CREATESERVICE_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_SERVICE);
		assertCommandDefinition(instance, "create-service", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("Store");
        assertExpectedCommandString(instance, "create-service Store");
		assertCommandExecution(instance, project);
	}

	public void test_CREATETAGLIB_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_TAGLIB);
		assertCommandDefinition(instance, "create-tag-lib", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("Store");
        assertExpectedCommandString(instance, "create-tag-lib Store");
		assertCommandExecution(instance, project);
	}

	public void test_CREATEUNITTEST_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.CREATE_UNIT_TEST);
		assertCommandDefinition(instance, "create-unit-test", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("bart.Store");
        assertExpectedCommandString(instance, "create-unit-test bart.Store");
        
        // FIXADE this command is failing on 1.3.4.  Uncomment when we upgrade to 1.3.5 or above
        // see https://svn.cargo.codehaus.org/browse/GRAILS-6606
//		assertCommandExecution(instance, project);
	}

	public void test_GENERATEALL_expression() throws Exception {
	    project.getFile("grails-app/controllers/bart/StoreController.groovy").delete(true, null);
	    project.getFile("test/unit/bart/StoreControllerTests.groovy").delete(true, null);
	    project.getFolder("grails-app/views/store").delete(true, null);
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.GENERATE_ALL);
		assertCommandDefinition(instance, "generate-all", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("bart.Store");
        assertExpectedCommandString(instance, "generate-all bart.Store");
        // This command's execution will fail because the StoreController already exists, so it will
        // prompt user to overwrite it:
        assertCommandExecution(instance, project);
	}

	public void test_GENERATECONTROLLER_expression() throws Exception {
	    project.getFile("grails-app/controllers/bart/StoreController.groovy").delete(true, null);
	    project.getFile("test/unit/bart/StoreControllerTests.groovy").delete(true, null);
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.GENERATE_CONTROLLER);
		assertCommandDefinition(instance, "generate-controller", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("bart.Store");
        assertExpectedCommandString(instance, "generate-controller bart.Store");
		assertCommandExecution(instance, project);
	}

	public void test_GENERATEVIEWS_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.GENERATE_VIEWS);
		assertCommandDefinition(instance, "generate-views", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("bart.Store");
        assertExpectedCommandString(instance, "generate-views bart.Store");
		assertCommandExecution(instance, project);
	}

	public void test_INSTALLTEMPLATES_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.INSTALL_TEMPLATES);
		assertCommandDefinition(instance, "install-templates", 0);
		assertExpectedCommandString(instance, "install-templates");
        assertCommandExecution(instance, project);
	}

	public void test_LISTPLUGINS_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.LIST_PLUGINS);
		assertCommandDefinition(instance, "list-plugins", 2);
        // ignore non-mandatory fields
        assertExpectedCommandString(instance, "list-plugins");
		assertCommandExecution(instance, project);
	}

	public void test_PACKAGE_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.PACKAGE);
		assertCommandDefinition(instance, "package", 0);
		assertExpectedCommandString(instance, "package");
        assertCommandExecution(instance, project);
	}

	public void test_PACKAGEPLUGIN_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.PACKAGE_PLUGIN);
		assertCommandDefinition(instance, "package-plugin", 0);
		assertExpectedCommandString(instance, "package-plugin");
		IProject plugin = ensureProject("Bart-plugin", true);
		
        assertCommandExecution(instance, plugin);
	}

	public void test_PLUGININFO_expression() throws Exception {
//		if (!GrailsVersion.MOST_RECENT.equals(GrailsVersion.V_2_0_0_RC1)) {
			//Skip: http://jira.grails.org/browse/GRAILS-8200 (revisit for RC2)
			IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.PLUGIN_INFO);
			assertCommandDefinition(instance, "plugin-info", 1);
			ICommandParameter parameter = getParameter("name", instance);
			parameter.setValue("feeds");
			assertExpectedCommandString(instance, "plugin-info feeds");
			assertCommandExecution(instance, project);
//		}
	}

	public void test_REMOVEPROXY_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.REMOVE_PROXY);
		assertCommandDefinition(instance, "remove-proxy", 1);
        ICommandParameter parameter = getParameter("name", instance);
        parameter.setValue("Store");
        assertExpectedCommandString(instance, "remove-proxy Store");
		assertCommandExecution(instance, project);
	}

	public void test_SCHEMAEXPORT_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.SCHEMA_EXPORT);
		assertCommandDefinition(instance, "schema-export", 3);
		// ignore non-mandatory fields
		assertCommandExecution(instance, project);
	}

	public void test_SETVERSION_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.SET_VERSION);
		assertCommandDefinition(instance, "set-version", 1);
        ICommandParameter parameter = getParameter("number", instance);
        parameter.setValue("1.0.0");
        assertExpectedCommandString(instance, "set-version 1.0.0");
		assertCommandExecution(instance, project);
	}

	public void test_STATS_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.STATS);
		assertCommandDefinition(instance, "stats", 0);
		assertExpectedCommandString(instance, "stats");
        assertCommandExecution(instance, project);
	}

	public void test_UPGRADE_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.UPGRADE);
		assertCommandDefinition(instance, "upgrade", 0);
		assertExpectedCommandString(instance, "upgrade");
		
		// expecting user feedback, which we can't give yet
		// cannot execute
//        assertCommandExecution(instance, project);
	}

	public void test_WAR_expression() throws Exception {
		IFrameworkCommand instance = createCommandInstance(GrailsCommandFactory.WAR);
		assertCommandDefinition(instance, "war", 2);
		// ignore non-mandatory fields
        assertExpectedCommandString(instance, "war");
		assertCommandExecution(instance, project);
	}
}
