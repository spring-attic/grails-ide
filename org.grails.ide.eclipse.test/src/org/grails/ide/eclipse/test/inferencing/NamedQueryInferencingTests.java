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
package org.grails.ide.eclipse.test.inferencing;

import junit.framework.Test;



/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Nov 28, 2011
 */
public class NamedQueryInferencingTests extends AbstractGrailsInferencingTests {
    private static final String NAMED_QUERY_PROXY_SEARCH = "org.codehaus.groovy.grails.orm.hibernate.cfg.NamedCriteriaProxy<Search>";
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
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "}";
        int start = contents.indexOf("firstQuery");
        int end = start + "firstQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.indexOf("secondQuery");
        end = start + "secondQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery2() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { firstQuery.secondQuery }\n" +
                "  }\n" +
                "}";
        int start = contents.lastIndexOf("firstQuery");
        int end = start + "firstQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("secondQuery");
        end = start + "secondQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery3() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { firstQuery.secondQuery }\n" +
                "}";
        int start = contents.lastIndexOf("firstQuery");
        int end = start + "firstQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("secondQuery");
        end = start + "secondQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery4() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.secondQuery }\n" +
                "}";
        int start = contents.lastIndexOf("firstQuery");
        int end = start + "firstQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("secondQuery");
        end = start + "secondQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQuery5() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.secondQuery.firstQuery.secondQuery.firstQuery.secondQuery.firstQuery.secondQuery }\n" +
                "}";
        int start = contents.lastIndexOf("firstQuery");
        int end = start + "firstQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        start = contents.lastIndexOf("secondQuery");
        end = start + "secondQuery".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
    public void testNamedQueryWithFinder1() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.list }\n" +
                "}";
        int start = contents.lastIndexOf("list");
        int end = start + "list".length();
        // Commented out see https://jira.grails.org/browse/GRAILS-8387
//      assertTypeInDomainClassNoPrefix(contents, start, end, LIST_OF_SEARCH);
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
    }
    public void testNamedQueryWithFinder1a() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.count }\n" +
                "}";
        int start = contents.lastIndexOf("count");
        int end = start + "count".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Integer");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
    }
    public void testNamedQueryWithFinder2() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.get }\n" +
                "}";
        int start = contents.lastIndexOf("get");
        int end = start + "get".length();
        assertTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
    }
    public void testNamedQueryWithFinder3() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.findWhere }\n" +
                "}";
        int start = contents.lastIndexOf("findWhere");
        int end = start + "findWhere".length();
        // Commented out see https://jira.grails.org/browse/GRAILS-8387
//      assertTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
      assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
    }
    public void testNamedQueryWithFinder4() throws Exception {
        String contents = 
                "class Search {\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.findAllWhere }\n" +
                "}";
        int start = contents.lastIndexOf("findAllWhere");
        int end = start + "findAllWhere".length();
        // Commented out see https://jira.grails.org/browse/GRAILS-8387
//      assertTypeInDomainClassNoPrefix(contents, start, end, LIST_OF_SEARCH);
        assertTypeInDomainClassNoPrefix(contents, start, end, "java.lang.Object");
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, NAMED_QUERY_PROXY_SEARCH);
    }
    public void testNamedQueryWithFinder5() throws Exception {
        String contents = 
                "class Search {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.findByName }\n" +
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
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { Search.firstQuery.findAllByNameBetween }\n" +
                "}";
        int start = contents.lastIndexOf("findAllByNameBetween");
        int end = start + "findAllByNameBetween".length();
      assertTypeInDomainClassNoPrefix(contents, start, end, LIST_OF_SEARCH);
        assertDeclarationTypeInDomainClassNoPrefix(contents, start, end, SEARCH);
    }
}
