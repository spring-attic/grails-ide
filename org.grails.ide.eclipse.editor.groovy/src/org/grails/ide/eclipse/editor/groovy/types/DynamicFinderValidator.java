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
package org.grails.ide.eclipse.editor.groovy.types;

import groovyjarjarasm.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.eclipse.codeassist.ProposalUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.editor.groovy.elements.DomainClass;

/**
 * @author Andrew Eisenberg
 * @since 2.3.0
 */
public class DynamicFinderValidator {
    
    private static final ClassNode[] NO_EXCEPTIONS = new ClassNode[0];
    private static final BlockStatement EMPTY_BLOCK = new BlockStatement();


    enum CompleteKind { COMPARATOR, COMPARATOR_OPERATOR, NONE, OPERATOR, PROP }
    private static final String[] FINDER_COMPARATORS = new String[] { 
        "Between", "GreaterThan", "GreaterThanEquals", "Ilike", "InList", "IsNull", "IsNotNull", "LessThan", "LessThanEquals", "Like", "Not", "NotEqual" };
    private static final String COMPARATORS = concat(FINDER_COMPARATORS);
    
    private static final String[] FINDER_OPERATORS = new String[] { "And", "Or" };
    private static final String[] FINDER_OPERATORS_LCASE = new String[] { "and", "or" };
    private static final String OPERATORS = concat(FINDER_OPERATORS); 
    

    private static final Map<String, ClassNode[]> COMPARATOR_ARGUMENT_MAP = new HashMap<String, ClassNode[]>();
    static {
        COMPARATOR_ARGUMENT_MAP.put("Between", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE, VariableScope.OBJECT_CLASS_NODE });
        COMPARATOR_ARGUMENT_MAP.put("GreaterThan", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });  // should be Comparable
        COMPARATOR_ARGUMENT_MAP.put("GreaterThanEquals", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });  // should be Comparable
        COMPARATOR_ARGUMENT_MAP.put("Ilike", new ClassNode[] { VariableScope.STRING_CLASS_NODE });
        COMPARATOR_ARGUMENT_MAP.put("InList", new ClassNode[] { VariableScope.LIST_CLASS_NODE });
        COMPARATOR_ARGUMENT_MAP.put("IsNull", new ClassNode[] { });
        COMPARATOR_ARGUMENT_MAP.put("IsNotNull", new ClassNode[] { });
        COMPARATOR_ARGUMENT_MAP.put("LessThan", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });  // should be Comparable
        COMPARATOR_ARGUMENT_MAP.put("LessThanEquals", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });  // should be Comparable
        COMPARATOR_ARGUMENT_MAP.put("Like", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });
        COMPARATOR_ARGUMENT_MAP.put("Not", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });
        COMPARATOR_ARGUMENT_MAP.put("NotEqual", new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });
        COMPARATOR_ARGUMENT_MAP.put(null, new ClassNode[] { VariableScope.OBJECT_CLASS_NODE });
    }
    
    private static final Map<String,String> COMPARATOR_LCASE_MAP = new HashMap<String, String>();
    static {
        for (String comparator : FINDER_COMPARATORS) {
            COMPARATOR_LCASE_MAP.put(comparator.toLowerCase(), comparator);
        }
    }
    
    /**
     * Finder prefixes.  Grails 1.3.X
     */
    private static final String[] FINDER_PREFIXES_1_3_X = new String[] { "countBy", "findBy", "findAllBy", "listOrderBy" }; 
    private static final Map<String,String> FINDER_PREFIXES_MAP_1_3_X = new HashMap<String, String>();
    static {
        FINDER_PREFIXES_MAP_1_3_X.put("countby", "countBy");
        FINDER_PREFIXES_MAP_1_3_X.put("findby", "findBy");
        FINDER_PREFIXES_MAP_1_3_X.put("findallby", "findAllBy");
        FINDER_PREFIXES_MAP_1_3_X.put("listorderby", "listOrderBy");
    }
    /**
     * Finder prefixes text for pattern.  Grails 1.3.X
     */
    private static final String PREFIXES_1_3_X = "^" + concat(FINDER_PREFIXES_1_3_X);
    /**
     * Pattern for checking if a finder starts with a prefix.  Grails 1.3.X
     */
    private static final Pattern STARTS_WITH_PATTERN_1_3_X = Pattern.compile(PREFIXES_1_3_X);
    
    /**
     * Finder prefixes.  Grails 2.0.X
     */
    private static final String[] FINDER_PREFIXES_2_0_X = new String[] { "countBy", "findBy", "findAllBy", "listOrderBy", "findOrCreateBy", "findOrSaveBy"}; 
    private static final Map<String,String> FINDER_PREFIXES_MAP_2_0_X = new HashMap<String, String>();
    static {
        FINDER_PREFIXES_MAP_2_0_X.put("countby", "countBy");
        FINDER_PREFIXES_MAP_2_0_X.put("findby", "findBy");
        FINDER_PREFIXES_MAP_2_0_X.put("findallby", "findAllBy");
        FINDER_PREFIXES_MAP_2_0_X.put("listorderby", "listOrderBy");
        FINDER_PREFIXES_MAP_2_0_X.put("findorcreateby", "findOrCreateBy");
        FINDER_PREFIXES_MAP_2_0_X.put("findorsaveby", "findOrSaveBy");
    }
    /**
     * Finder prefixes text for pattern.  Grails 2.0.X
     */
    private static final String PREFIXES_2_0_X = "^" + concat(FINDER_PREFIXES_2_0_X);
    /**
     * Pattern for checking if a finder starts with a prefix.  Grails 2.0.X
     */
    private static final Pattern STARTS_WITH_PATTERN_2_0_X = Pattern.compile(PREFIXES_2_0_X);
    
    private static String concat(String[] ss) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < ss.length; i++) {
            if (i > 0) sb.append("|");
            sb.append(ss[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    private final DomainClass domain;

    private Set<String> domainProperties;
    private Map<String,String> lcaseDomainPropertiesMap;


    /**
     * caches pre-calculated findernames
     */
    private Map<String, Boolean> finderNameCache;


    /**
     * This pattern determines what kind of content assist is available
     */
    private Pattern splitFinderPattern;


    /**
     * Regex describing a valid finder
     */
    protected Pattern validFinderPattern;
    
    private String prefixes;
    private Pattern startsWithPattern;
    private Map<String, String> startsWithMap;
    
    
    protected DynamicFinderValidator(boolean use200, DomainClass domain) {
        this.domain = domain;
        this.finderNameCache = new HashMap<String, Boolean>();
        if (use200) {
            prefixes = PREFIXES_2_0_X;
            startsWithPattern = STARTS_WITH_PATTERN_2_0_X;
            startsWithMap = FINDER_PREFIXES_MAP_2_0_X;
        } else {
            prefixes = PREFIXES_1_3_X;
            startsWithPattern = STARTS_WITH_PATTERN_1_3_X;
            startsWithMap = FINDER_PREFIXES_MAP_1_3_X;
        }
        domainProperties = generateDomainProperties(domain);
        lcaseDomainPropertiesMap = lcase(domainProperties);
                
        String concatProperties = concatPropertyNames(domainProperties);
        validFinderPattern = Pattern.compile(createValidFinderPatternString(concatProperties));
        splitFinderPattern = Pattern.compile(createSplitFinder(concatProperties), Pattern.CASE_INSENSITIVE);
    }

    private Map<String,String> lcase(Set<String> props) {
        Map<String,String> lcaseMap = new HashMap<String,String>(props.size()*2);
        for (String prop : props) {
            lcaseMap.put(prop.toLowerCase(), prop);
        }
        return lcaseMap;
    }
    
    /**
     * For testing only
     */
    protected DynamicFinderValidator(boolean use200, Set<String> domainProperties) {
        this.domain = null;
        this.domainProperties = ensureCapitalized(domainProperties);
        this.lcaseDomainPropertiesMap = lcase(this.domainProperties);
        this.finderNameCache = new HashMap<String, Boolean>();
        if (use200) {
            prefixes = PREFIXES_2_0_X;
            startsWithPattern = STARTS_WITH_PATTERN_2_0_X;
            startsWithMap = FINDER_PREFIXES_MAP_2_0_X;
        } else {
            prefixes = PREFIXES_1_3_X;
            startsWithPattern = STARTS_WITH_PATTERN_1_3_X;
            startsWithMap = FINDER_PREFIXES_MAP_1_3_X;
        }
        String concatProperties = concatPropertyNames(domainProperties);
        validFinderPattern = Pattern.compile(createValidFinderPatternString(concatProperties));
        splitFinderPattern = Pattern.compile(createSplitFinder(concatProperties), Pattern.CASE_INSENSITIVE & Pattern.UNICODE_CASE);
    }
    
    /**
     * For testing only
     */
    protected DynamicFinderValidator(boolean use200, String ... domainPropertiesArr) {
        this.domain = null;
        Set<String> domainProperties = new HashSet<String>(Arrays.asList(domainPropertiesArr));
        this.domainProperties = ensureCapitalized(domainProperties);
        this.lcaseDomainPropertiesMap = lcase(this.domainProperties);
        this.finderNameCache = new HashMap<String, Boolean>();
        if (use200) {
            prefixes = PREFIXES_2_0_X;
            startsWithPattern = STARTS_WITH_PATTERN_2_0_X;
            startsWithMap = FINDER_PREFIXES_MAP_2_0_X;
        } else {
            prefixes = PREFIXES_1_3_X;
            startsWithPattern = STARTS_WITH_PATTERN_1_3_X;
            startsWithMap = FINDER_PREFIXES_MAP_1_3_X;
        }
        String concatProperties = concatPropertyNames(domainProperties);
        validFinderPattern = Pattern.compile(createValidFinderPatternString(concatProperties));
        splitFinderPattern = Pattern.compile(createSplitFinder(concatProperties), Pattern.CASE_INSENSITIVE & Pattern.UNICODE_CASE);
    }
    
    /**
     * Creates a valid declaration based on the name passed in
     * @param finderName the name of the dynamic finder
     * @return A field declaration corresponding to the dynamic finder
     * return a field and not a method so that components can be built up more easily
     */
    public FieldNode createFieldDeclaration(String finderName) {
        ClassNode declaring = domain != null ? domain.getGroovyClass() : null;
        if (declaring == null) {
            declaring = VariableScope.OBJECT_CLASS_NODE;
        }
        FieldNode field = new FieldNode(finderName, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, 
                createReturnType(finderName), declaring, null);
        field.setDeclaringClass(declaring);
        return field;
    }
    
    /**
     * Provide a list of content assist proposals for the given finder name
     * @param finderName
     * @return List of method proposals to complete the potential finder name, 
     * or empty list if none are available
     */
    public List<AnnotatedNode> findProposals(String finderName) {
        if (finderName == null || finderName.equals("")) {
            return Collections.emptyList();
        }
        Matcher m = splitFinderPattern.matcher(finderName);
        if (!m.matches()) {
            return Collections.emptyList();
        }
        
        // number of groups = 1 MAX_COUNT * 3 - 1 + 1
        // first is prefix, then triplets of component, comparator, operator
        // the last triplet cannot have an operator.  Then there is the remaining
        String prefix = m.group(1);
        String remaining = m.group((MAX_COMPONENTS * 3) + 2 - 1);  // everything else that is not part of a known group.
        
        String[] props = new String[MAX_COMPONENTS];
        String[] comparators = new String[MAX_COMPONENTS];
        String[] operators = new String[MAX_COMPONENTS];
        for (int i = 0, groupNum = 2; i < MAX_COMPONENTS; i++, groupNum += 3) {
            props[i] = m.group(groupNum);
            comparators[i] = m.group(groupNum+1);
            if (i < MAX_COMPONENTS -1) {
                operators[i] = m.group(groupNum+2);
            }
        }
        
        if (remaining == null) remaining = "";
        String lcaseRemaining = remaining.toLowerCase();
        
        // should do better here and check all props
        if (props[0] != null && props[0].equals(props[1])) {
            // can't have the same component twice
            return Collections.emptyList();
        }
        
        CompleteKind kind = CompleteKind.NONE;
        for (int i = 0; i < MAX_COMPONENTS; i++) {
            if (props[i] == null) {
                kind = CompleteKind.PROP;
                break;
            }
            if (i < MAX_COMPONENTS - 1) {
                if (comparators[i] == null && operators[i] == null) {
                    kind = CompleteKind.COMPARATOR_OPERATOR;
                    break;
                }
                if (operators[i] == null) {
                    kind = CompleteKind.OPERATOR;    
                    break;
                }
            } else {
                // the last component can have no operator
                if (comparators[i] == null) {
                    kind = CompleteKind.COMPARATOR;
                    break;
                }
            }
        }
        // at this point, kind should not be NONE
        
        // check that all operators are the same
        if (operators[0] != null) {
            for (int i = 1; i < operators.length; i++) {
                if (operators[i] != null && !operators[0].equals(operators[i])) {
                    return Collections.emptyList();
                }
            }
        }
        
        String caseCorrectedExisting = correctCase(prefix, props, comparators, operators);
        List<AnnotatedNode> proposedFinderMethods = null;
        String existingOperator;
        switch(kind) {
            case PROP:
                Set<String> lcaseProps = new HashSet<String>(MAX_COMPONENTS, 1);
                for (int i = 0; i < props.length; i++) {
                    if (props[i] != null) {
                        lcaseProps.add(props[i].toLowerCase());
                    }
                }
                // propose all property names, except for the one that is already used
                proposedFinderMethods = new ArrayList<AnnotatedNode>(lcaseDomainPropertiesMap.size()*2);
                for (Entry<String, String> entry : lcaseDomainPropertiesMap.entrySet()) {
                    String lcasePropName = entry.getKey();
                    String propName = entry.getValue();
                    if (ProposalUtils.looselyMatches(lcaseRemaining, lcasePropName) && !lcaseProps.contains(lcasePropName)) {
                        proposedFinderMethods.add(createFieldDeclaration(caseCorrectedExisting + propName));
                        // also add all of methods with the approproate arguments
                        proposedFinderMethods.add(createMethodDeclaration(caseCorrectedExisting + propName, props, comparators, propName, null));
                    }
                }
                break;
            case COMPARATOR_OPERATOR:
                proposedFinderMethods = new ArrayList<AnnotatedNode>(FINDER_COMPARATORS.length*2 + FINDER_OPERATORS.length);

                for (Entry<String, String> entry : COMPARATOR_LCASE_MAP.entrySet()) {
                    String lcaseComparator = entry.getKey();
                    if (ProposalUtils.looselyMatches(lcaseRemaining, lcaseComparator)) {
                        String comparator = entry.getValue();
                        proposedFinderMethods.add(createFieldDeclaration(caseCorrectedExisting + comparator));
                        // also add all as methods with appropriate arguments
                        proposedFinderMethods.add(createMethodDeclaration(caseCorrectedExisting + comparator, props, comparators, null, comparator));
                    }
                }
                // fall-through
            case OPERATOR:
                // propose operator(s)
                
                // only propose if there are any remaining domain properties to propose
                int propsLen = 0;
                for (int i = 0; i < props.length; i++) {
                    if (props[i] == null) {
                        break;
                    }
                    propsLen++;
                }
                if (proposedFinderMethods == null) {
                    proposedFinderMethods = new ArrayList<AnnotatedNode>(FINDER_OPERATORS.length);
                }
                if (propsLen < domainProperties.size()) {
                    // there is at least one more proeprty to propose
                    existingOperator = operators[0];
                    for (int i = 0; i < FINDER_OPERATORS.length; i++) {
                        if (existingOperator == null || FINDER_OPERATORS[i].equalsIgnoreCase(existingOperator)) {
                            // only look at the operator if it is the same as the first operator
                            // ie- all operators must be the same.
                            if (FINDER_OPERATORS_LCASE[i].startsWith(lcaseRemaining)) {
                                proposedFinderMethods.add(createFieldDeclaration(caseCorrectedExisting + FINDER_OPERATORS[i]));
                            }
                        }
                    }
                }
                break;
            case COMPARATOR:
                // propose all comparators
                proposedFinderMethods = new ArrayList<AnnotatedNode>(FINDER_COMPARATORS.length*2);
                for (Entry<String, String> entry : COMPARATOR_LCASE_MAP.entrySet()) {
                    String lcaseComparator = entry.getKey();
                    if (ProposalUtils.looselyMatches(lcaseRemaining, lcaseComparator)) {
                        String comparator = entry.getValue();
                        proposedFinderMethods.add(createFieldDeclaration(caseCorrectedExisting + comparator));
                        // also add all as methods with appropriate arguments
                        proposedFinderMethods.add(createMethodDeclaration(caseCorrectedExisting + comparator, props, comparators, null, comparator));
                    }
                }
                break;
            case NONE:
            default:
                proposedFinderMethods = Collections.emptyList();
        }
        
        return proposedFinderMethods;
    }

    private String correctCase(String prefix, 
            String[] props, String[] comparators, String[] operators) {
        
        String realPrefix = startsWithMap.get(prefix.toLowerCase());
        realPrefix = realPrefix == null ? prefix : realPrefix;
        
        String[] realProps = new String[props.length];
        String[] realComparators = new String[comparators.length];
        String[] realOperators = new String[comparators.length];
        for (int i = 0; i < props.length; i++) {
            if (props[i] != null) {
                realProps[i] = lcaseDomainPropertiesMap.get(props[i].toLowerCase());
                realProps[i] = realProps[i] == null ? props[i] : realProps[i];
            } else {
                realProps[i] = "";
            }
            
            if (comparators[i] != null) {
                realComparators[i] = COMPARATOR_LCASE_MAP.get(comparators[i].toLowerCase());
                realComparators[i] = realComparators[i] == null ? comparators[i] : realComparators[i];
            } else {
                realComparators[i] = "";
            }
            
            if (i < operators.length) {
                if (operators[i] != null) {
                    realOperators[i] = operators[i].equalsIgnoreCase("Or") ? "Or" : "And";
                } else {
                    realOperators[i] = "";
                }
            }
        }
    
        StringBuilder sb = new StringBuilder();
        sb.append(realPrefix);
        for (int i = 0; i < props.length; i++) {
            sb.append(realProps[i]);
            sb.append(realComparators[i]);
            if (i < operators.length) {
                sb.append(realOperators[i]);
            }
        }

        return sb.toString();
    }

    private AnnotatedNode createMethodDeclaration(String finderName,
            String[] props, String[] comparators, String newPropName, String newComparator) {
        // need to determine number of parameters
        
        // first figure out how many properties we need to look at
        int numArgs = -1;
        if (newPropName != null) {
            for (int i = 0; i < props.length; i++) {
                if (props[i] == null) {
                    props[i] = newPropName;
                    numArgs = i+1;
                    break;
                }
            }
        }
        if (newComparator != null) {
            for (int i = 0; i < comparators.length; i++) {
                if (comparators[i] == null) {
                    comparators[i] = newComparator;
                    numArgs = i+1;
                    break;
                }
            }
        }

        List<Parameter> params = new ArrayList<Parameter>(2);
        // now go through each component and comparator.  
        // determine the kind of parameters they require
        for (int i = 0; i < numArgs; i++) {
            ClassNode[] classNodes = COMPARATOR_ARGUMENT_MAP.get(comparators[i]);
            if (classNodes.length > 0) {
                String uncapitalized = uncapitalize(props[i]);
                params.add(new Parameter(classNodes[0], uncapitalized));
                if (classNodes.length == 2) {
                    params.add(new Parameter(classNodes[1], uncapitalized + "1"));
                }
            }
        }
        
        MethodNode method = new MethodNode(finderName, Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, createReturnType(finderName), 
                params.toArray(new Parameter[params.size()]), NO_EXCEPTIONS, EMPTY_BLOCK);
        if (domain != null) {
            method.setDeclaringClass(domain.getGroovyClass());
        } else {
            method.setDeclaringClass(VariableScope.OBJECT_CLASS_NODE);
        }
        return method;
    }

    /**
     * @param finderName
     * @return true iff this is a complete finder name
     */
    public boolean isValidFinderName(String finderName) {
        if (finderName == null) {
            return false;
        }
        if (finderNameCache.containsKey(finderName)) {
            return finderNameCache.get(finderName);
        }
        Matcher matcher = validFinderPattern.matcher(finderName);
        boolean matches = matcher.matches();
        if (matches) {
            String[] props = new String[MAX_COMPONENTS];
            // still more work to do.  must check that the field references are all unique
            for (int i = 0, groupNum = 2; i < MAX_COMPONENTS; i++, groupNum += 3) {
                props[i] = matcher.group(groupNum);
            }
            
            Set<String> existing = new HashSet<String>(5, 1);
            for (String prop : props) {
                if (existing.contains(prop)) {
                    matches = false;
                    break;
                }
                if (prop == null) {
                    break;
                }
                existing.add(prop);
            }
            
            if (matches) {
                // still more work to do...need to check that all operators are equal
                // one less oeprator than components
                String op = matcher.group(4);
                // start with 1 because we don't need to check the first one against itself
                // also, use MAX_COMPONENTS-1 since there are always one fewer operators than components
                for (int i = 1, groupNum = 7; i < MAX_COMPONENTS-1; i++, groupNum += 3) {
                    String nextOp = matcher.group(groupNum);
                    if (nextOp == null) {
                        break;
                    }
                    if (!op.equals(nextOp)) {
                        matches = false;
                        break;
                    }
                }
            }
        }
        
        finderNameCache.put(finderName, matches);
        return matches;
    }

    /**
     * Provide a fail fast way to determine if a name might be a dynamic finder
     * 
     * @param finderName
     * @return true iff the start of the finderName is a valid prefix
     * for a dynamic finder.
     */
    public boolean startsWithDynamicFinder(String finderName) {
        Matcher m = startsWithPattern.matcher(finderName);
        return m.lookingAt();
    }
    
    private String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        StringBuffer buf = new StringBuffer(str.length());
        buf.append(Character.toUpperCase(str.charAt(0)));
        buf.append(str.substring(1));
        return buf.toString();
    }
    
    private String uncapitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        StringBuffer buf = new StringBuffer(str.length());
        buf.append(Character.toLowerCase(str.charAt(0)));
        buf.append(str.substring(1));
        return buf.toString();
    }
    
    private String concatPropertyNames(Set<String> props) {
        if (props == null || props.size() == 0) {
            return "( NO_PROPERTIES )";  // if there are no properties, then there can mever be a match
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int i = props.size();
        for (Iterator<String> propIter = props.iterator(); propIter.hasNext(); ) {
            sb.append(capitalize(propIter.next()));
            if (--i > 0) sb.append("|");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * @param finderName
     * @return
     */
    private ClassNode createReturnType(String finderName) {
        if (finderName.startsWith("countBy")) {
            return VariableScope.INTEGER_CLASS_NODE;
        } else {
            ClassNode groovyClass = domain != null ? domain.getGroovyClass() : null;
            if (groovyClass == null) {
                groovyClass = VariableScope.OBJECT_CLASS_NODE;
            }
            if (finderName.startsWith("findAllBy") || finderName.startsWith("listOrderBy")) {
                ClassNode list = VariableScope.clonedList();
                ClassNode thisClass = groovyClass;
                list.getGenericsTypes()[0].setType(thisClass);
                list.getGenericsTypes()[0].setName(thisClass.getName());
                list.getGenericsTypes()[0].setUpperBounds(null);
                return list;
            } else {
                return groovyClass;
            }
        }
    }
    
    private static final int MAX_COMPONENTS = 5;
    
    private String createValidFinderPatternString(String propNameRegx) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefixes);
        sb.append(propNameRegx);
        // comparator is optional
        sb.append(COMPARATORS + "?");
        // then there is an optional operator with one more property name and an optional comparator
        // repeat the same for the remaingin components
        for (int i = 1; i < MAX_COMPONENTS; i++) {
            sb.append("(?:" + OPERATORS + propNameRegx + COMPARATORS + "?)?");
        }
        sb.append("$");
        return sb.toString();
    }

    /**
     * Almost the same as the valid finder patter, except allow for anything at the end.
     * Also allow for case-insensitive matching
     * @param propNameRegx
     * @return
     */
    private String createSplitFinder(String propNameRegx) {
      StringBuilder sb = new StringBuilder();
      sb.append(prefixes);
      // allow for an option prop name in case we are completing on the first property name
      sb.append(propNameRegx + "?");
      // comparator is optional
      sb.append(COMPARATORS + "?");
      // then there is an optional operator with one more property name and an optional comparator
      // repeat the same for the remaingin components
      for (int i = 1; i < MAX_COMPONENTS; i++) {
          // the property name is optional in case we are completing on the property
          sb.append("(?:" + OPERATORS + propNameRegx + "?" + COMPARATORS + "?)?");
      }
      sb.append("(.*)$");
      return sb.toString();
    }

    private Set<String> ensureCapitalized(Set<String> domainProperties) {
        Set<String> newSet = new HashSet<String>();
        for (String prop : domainProperties) {
            newSet.add(capitalize(prop));
        }
        return newSet;
    }
    
    private Set<String> generateDomainProperties(DomainClass domain) {
        Assert.isNotNull(domain, "Domain class should not be null");
        List<PropertyNode> domainPropertiesNodes = domain.getDomainProperties();
        Set<String> properties = new HashSet<String>(domainPropertiesNodes.size()*2);
        for (PropertyNode property : domainPropertiesNodes) {
            properties.add(capitalize(property.getName()));
        }
        return properties;
    }


    protected List<String> getFinderComponents(String finderName) {
        Matcher matcher = validFinderPattern.matcher(finderName);
        if (! matcher.matches()) {
            return null;
        } else {
            List<String> matches = new ArrayList<String>(matcher.groupCount());
            for (int i = 1; i <= matcher.groupCount(); i++) {
                matches.add(matcher.group(i));
            }
            return matches;
        }
    }

    // for testing
    public static void main(String[] args) {
        testFinder("findByFooOrBaz", true);
        // should have proposals
        testFinder("findBy", false);
        testFinder("findByB", false);
        testFinder("findByFooO", false);
        testFinder("findByFooL", false);
        testFinder("findByFooLikeAndBazL", false);
        testFinder("findByFooLikeA", false);

        testFinder("findByFoo", true);
        testFinder("findByFooLike", true);
        testFinder("findByFooLikeAnd", false);
        testFinder("findByFooLikeAndBaz", true);
        testFinder("findByFooLikeAndBazLike", true);
        testFinder("findByFooLikeAndBazLikeAndBarLikeAndBop", true);
        testFinder("findByFooLikeAndBazLikeAndBarLikeAndBopBetween", true);
        testFinder("findByFooLikeAndBazLikeAndBarLikeAndBarBetween", false);  
        testFinder("findByFooLikeAndBazLikeAndBarLikeOrBazBetween", false);
        testFinder("findByFooLikeAndBazLikeAndBarLikeAndBBetween", false);
        
    }

    private static void testFinder(String finderName, boolean expectMatch) {
        DynamicFinderValidator validator = new DynamicFinderValidator(false, "foo", "bar", "baz", "bop");
        boolean isValid = validator.isValidFinderName(finderName);
        List<AnnotatedNode> proposals = validator.findProposals(finderName);
        StringBuilder sb = new StringBuilder();
        sb.append((isValid ? " VALID   " : " INVALID ") + finderName + " proposals: ");
        for (AnnotatedNode anode : proposals) {
            if (anode instanceof FieldNode) {
                sb.append(((FieldNode) anode).getName() + "   ");
            } else if (anode instanceof MethodNode) {
                sb.append(((MethodNode) anode).getName() + "()   ");
            }
        }
        if (isValid != expectMatch) {
            System.err.println("Unexpected.  Should have " + (expectMatch ? "matched " : "no match ") + finderName);
        }
        System.out.println(sb);
    }
}
