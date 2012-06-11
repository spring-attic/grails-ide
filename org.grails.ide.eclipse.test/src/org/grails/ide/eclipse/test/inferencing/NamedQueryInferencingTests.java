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
package org.grails.ide.eclipse.test.inferencing;

import junit.framework.Test;



/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Nov 28, 2011
 */
public class NamedQueryInferencingTests extends AbstractGrailsInferencingTests {
    private static final String NAMED_QUERY_PROXY = "org.codehaus.groovy.grails.orm.hibernate.cfg.NamedCriteriaProxy";
    private static final String SEARCH = "Search";
    private static final String LIST_OF_SEARCH = "java.util.List<Search>";
    public static Test suite() {
        return buildTestSuite(NamedQueryInferencingTests.class);
    }

    public NamedQueryInferencingTests(String name) {
        super(name);
    }
    
    public void testNamedQuery1() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "}";
        int start = contents.indexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.indexOf("second");
        end = start + "second".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery2() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { first.second }\n" +
                "  }\n" +
                "}";
        int start = contents.lastIndexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("second");
        end = start + "second".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery3() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { first.second }\n" +
                "}";
        int start = contents.lastIndexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("second");
        end = start + "second".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery4() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.second }\n" +
                "}";
        int start = contents.lastIndexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("second");
        end = start + "second".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery5() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.second.first.second.first.second.first.second }\n" +
                "}";
        int start = contents.lastIndexOf("first");
        int end = start + "first".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("second");
        end = start + "second".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQueryWithFinder1() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.list }\n" +
                "}";
        int start = contents.lastIndexOf("list");
        int end = start + "list".length();
        // Commented out see http://jira.grails.org/browse/GRAILS-8387
//      assertTypeInDomainClassNoPrefix(contents, start, end, LIST_OF_SEARCH);
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
    }
    public void testNamedQueryWithFinder1a() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.count }\n" +
                "}";
        int start = contents.lastIndexOf("count");
        int end = start + "count".length();
        // Commented out see http://jira.grails.org/browse/GRAILS-8387
//        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Integer");
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
    }
    public void testNamedQueryWithFinder2() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.get }\n" +
                "}";
        int start = contents.lastIndexOf("get");
        int end = start + "get".length();
        // Commented out see http://jira.grails.org/browse/GRAILS-8387
//        assertTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
    }
    public void testNamedQueryWithFinder3() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.findWhere }\n" +
                "}";
        int start = contents.lastIndexOf("findWhere");
        int end = start + "findWhere".length();
        // Commented out see http://jira.grails.org/browse/GRAILS-8387
//      assertTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
      assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
    }
    public void testNamedQueryWithFinder4() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.findAllWhere }\n" +
                "}";
        int start = contents.lastIndexOf("findAllWhere");
        int end = start + "findAllWhere".length();
        // Commented out see http://jira.grails.org/browse/GRAILS-8387
//      assertTypeInDomainClassNoPrefix(contents, start, end, LIST_OF_SEARCH);
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY);
    }
    public void testNamedQueryWithFinder5() throws Exception {
        String contents = 
                "class Search {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.findByName }\n" +
                "}";
        int start = contents.lastIndexOf("findByName");
        int end = start + "findByName".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQueryWithFinder6() throws Exception {
        String contents = 
                "class Search {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    first { }\n" +
                "    second { }\n" +
                "  }\n" +
                "  def x = { Search.first.findAllByNameBetween }\n" +
                "}";
        int start = contents.lastIndexOf("findAllByNameBetween");
        int end = start + "findAllByNameBetween".length();
      assertTypeInDomainClassNoPrefix(contents, start, end, LIST_OF_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
}
