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
package org.grails.ide.eclipse.editor.groovy.contentassist;

import groovyjarjarasm.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.eclipse.codeassist.ProposalUtils;
import org.codehaus.groovy.eclipse.codeassist.processors.IProposalProvider;
import org.codehaus.groovy.eclipse.codeassist.proposals.GroovyFieldProposal;
import org.codehaus.groovy.eclipse.codeassist.proposals.GroovyMethodProposal;
import org.codehaus.groovy.eclipse.codeassist.proposals.GroovyPropertyProposal;
import org.codehaus.groovy.eclipse.codeassist.proposals.IGroovyProposal;
import org.codehaus.groovy.eclipse.codeassist.requestor.ContentAssistContext;
import org.codehaus.groovy.eclipse.codeassist.requestor.ContentAssistLocation;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.model.ContributedMethod;
import org.grails.ide.eclipse.core.model.ContributedProperty;
import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.DomainClass;
import org.grails.ide.eclipse.editor.groovy.elements.DomainClass.NamedQueryClassNode;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;
import org.grails.ide.eclipse.editor.groovy.elements.TagLibClass;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectServiceCache;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 */
public class GrailsProposalProvider implements IProposalProvider {

    /**
     * 
     */
    private static final String GRAILS = "Grails"; //$NON-NLS-1$
    private static final int GRAILS_PROPOSALS_RELEVANCE_MULTIPLIER = 10;

    /**
     * If we are in a domain class, then provide all the special fields that can
     * be added.
     * Also, if in a relevant grails element, contribute the service names
     */
    public List<String> getNewFieldProposals(ContentAssistContext context) {
        GrailsProject proj = GrailsWorkspaceCore.get().getGrailsProjectFor(context.unit);
        if (proj == null) {
            // not in a grails project
            return Collections.emptyList();
        }
        IGrailsElement elt = proj.getGrailsElement(context.unit);
        List<String> unimplemented;
        if (elt instanceof DomainClass) {
            DomainClass domainClass = (DomainClass) elt;
            unimplemented = domainClass
                    .getUnimplementedStaticFields();
            
            // if the prefix is "static", then we return all proposals
            if (!"static".startsWith(context.completionExpression)) {
                for (Iterator<String> unimplementedIter = unimplemented.iterator(); unimplementedIter
                .hasNext();) {
                    String field = unimplementedIter.next();
                    if (!field.startsWith(context.completionExpression)) {
                        unimplementedIter.remove();
                    }
                }
            }
        } else{
            unimplemented = new ArrayList<String>();
        }
        
        if (supportsServiceInjection(elt)) {
            PerProjectServiceCache cache = GrailsCore.get().connect(context.unit.getJavaProject().getProject(), PerProjectServiceCache.class);
            if (cache != null && context.containingDeclaration instanceof ClassNode) {
                ClassNode containingClass = (ClassNode) context.containingDeclaration;
                for (String serviceName : cache.getAllServices().keySet()) {
                    if ((
                            "def".startsWith(context.completionExpression) || 
                            serviceName.startsWith(context.completionExpression))
                            && containingClass.getField(serviceName) == null) {
                        unimplemented.add("NONSTATIC " + serviceName); //$NON-NLS-1$
                    }
                }
            }
        }
        
        return unimplemented;
    }

    /**
     * @param elt
     * @return
     */
    protected boolean supportsServiceInjection(IGrailsElement elt) {
        switch (elt.getKind()) {
            case DOMAIN_CLASS:
            case SERVICE_CLASS:
            case CONTROLLER_CLASS:
            case TAGLIB_CLASS:
            case BUILD_CONFIG:
            case INTEGRATION_TEST:
                return true;
            default:
                return false;
        }
    }

    /**
     * None, really
     */
    public List<MethodNode> getNewMethodProposals(ContentAssistContext context) {
        return null;
    }

    /**
     * Depending on the kind of the type of the completion expression (eg-
     * controller, domain, etc), add the appropriate possibilities. Delegate to
     * the appropriate Grails element
     */
    public List<IGroovyProposal> getStatementAndExpressionProposals(
            ContentAssistContext context, ClassNode completionType,
            boolean isStatic, Set<ClassNode> categories) {
        GrailsProject proj = GrailsWorkspaceCore.get()
                .getGrailsProjectFor(context.unit);
        if (proj == null) {
            // not in a grails project
            return Collections.emptyList();
        }
        List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>(10);

        IGrailsElement grailsElement = proj.getGrailsElement(context.unit);

        // Proposals here depend on the compilation unit of the context, not the
        // type.
        switch (grailsElement.getKind()) {
            case DOMAIN_CLASS:
                DomainClass domainClass = (DomainClass) grailsElement;
                // create the special mapping proposals if inside the mapping
                // field closure
                if (domainClass.isMappingField(context.containingDeclaration)) {
                    proposals.addAll(createMappingProposals(context,
                            domainClass));
                } else if (domainClass.isConstraintsField(context.containingDeclaration)) {
                    proposals.addAll(createConstraintsProposals(context,
                            domainClass));
                }
                break;

            default:
                // either not yet handling this kind, or this kind has no
                // special completions
                break;
        }

        // now create the methods that are not lexically specific, but rely on
        // the kind of the type being completed
        
        if (completionType instanceof NamedQueryClassNode) {
            // completing off of a namedQuery of a domain class
            DomainClass domainClass = ((NamedQueryClassNode) completionType).getDomainClass();
            ClassNode declaringType = domainClass.getGroovyClass();
            if (declaringType != null) {
                // finder prefixes
                proposals.addAll(findFinderProposals(context, domainClass, declaringType));
                
                // dynamic finders
                proposals.addAll(findDynamicFinderProposals(context, domainClass));

                // named queries
                proposals.addAll(findNamedQueryProposals(context, domainClass, declaringType));
            }
            
            // no other proposals are valid
            return proposals;
        }
        
        IGrailsElement completionGrailsElement;
        if (completionType == null || completionType.equals(context.getEnclosingGroovyType())) {
            completionGrailsElement = grailsElement;
        } else {
            completionGrailsElement = proj.getGrailsElement(completionType);
        }
        switch (completionGrailsElement.getKind()) {
            case DOMAIN_CLASS:
                DomainClass domainClass = (DomainClass) completionGrailsElement;
                proposals.addAll(createDomainProposals(context, domainClass,
                        isStatic));
                break;

            case CONTROLLER_CLASS:
                if (!isStatic) {
                    ControllerClass controllerClass = (ControllerClass) completionGrailsElement;
                    proposals.addAll(createControllerProposals(context,
                            controllerClass));
                }
                break;
            case TAGLIB_CLASS:
                TagLibClass tagLibClass = (TagLibClass) completionGrailsElement;
                proposals.addAll(createTagLibProposals(context, tagLibClass));
            default:
                // either not yet handling this kind, it is an invalid kind, or
                // this kind has no special completions
                break;
        }

        return proposals;
    }

    /**
     * @param context
     * @param controllerClass
     * @return
     */
    private List<IGroovyProposal> createControllerProposals(
            ContentAssistContext context, ControllerClass controllerClass) {
        if (context.location == ContentAssistLocation.STATEMENT
                || context.location == ContentAssistLocation.EXPRESSION
                || context.location == ContentAssistLocation.SCRIPT) {
            Map<String, ClassNode> references = controllerClass
                    .getExtraControllerReferences();
            List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>(
                    references.size());
            ClassNode declaringType = controllerClass.getGroovyClass();
            if (declaringType != null) {
                for (Map.Entry<String, ClassNode> entry : references.entrySet()) {
                    if (entry.getKey().startsWith(context.completionExpression)) {
                        if (controllerClass.isSpecialMethodReference(entry.getKey())) {
                            proposals.add(new GroovyMethodProposal(createNoArgMethodNode(
                                    entry.getKey(), context, declaringType, entry
                                            .getValue(), Opcodes.ACC_PUBLIC), GRAILS));
                        } else {
                            proposals.add(new GroovyFieldProposal(createFieldNode(
                                    entry.getKey(), context, declaringType, entry
                                            .getValue(), Opcodes.ACC_PUBLIC),
                                    GRAILS_PROPOSALS_RELEVANCE_MULTIPLIER, GRAILS));
                        }
                    }
                }
                // now add all the contributed methods and properties
                Map<String, ContributedProperty> contribProps = controllerClass.getAllContributedProperties();
                for (Map.Entry<String, ContributedProperty> contribProp : contribProps.entrySet()) {
                    String contribName = contribProp.getKey();
                    if (contribName.startsWith(context.completionExpression)) {
                        proposals.add(new GroovyPropertyProposal(contribProp.getValue().createMockProperty(declaringType), contribProp.getValue().getContributedBy()));
                    }
                }
                Map<String, Set<ContributedMethod>> contribMethods = controllerClass.getAllContributedMethods();
                for (Entry<String, Set<ContributedMethod>> contribMethodEntry : contribMethods.entrySet()) {
                    String contribName = contribMethodEntry.getKey();
                    if (contribName.startsWith(context.completionExpression)) {
                        for (ContributedMethod contribMethod : contribMethodEntry.getValue()) {
                            proposals.add(new GroovyMethodProposal(contribMethod.createMockMethod(declaringType), contribMethod.getContributedBy()));
                        }
                    }
                } 
                return proposals;
            }  // if (declaringType != null) {
        }
        return Collections.EMPTY_LIST;
    }

    private List<IGroovyProposal> createTagLibProposals(ContentAssistContext context, TagLibClass tagLibClass) {
        Map<String, ClassNode> tagLibMembers = tagLibClass
                .getTagLibMembers();
        List<IGroovyProposal> proposals = new LinkedList<IGroovyProposal>();
        for (Entry<String, ClassNode> entry : tagLibMembers
                .entrySet()) {
            if (entry.getKey().startsWith(context.completionExpression)) {
                ClassNode declaringType = tagLibClass.getGroovyClass();
                ClassNode type = entry.getValue();
                if (type == null) {
                    type = declaringType;
                }
                proposals.add(new GroovyFieldProposal(
                        createFieldNode(entry.getKey(), context,
                                declaringType, type,
                                Opcodes.ACC_PUBLIC),
                                GRAILS_PROPOSALS_RELEVANCE_MULTIPLIER, GRAILS));
            }
        }
        return proposals;
    }

    /**
     * Only valid within the <code>mapping</code> field in the domain class
     * 
     * @param context
     * @param domainClass
     * @return
     */
    private List<IGroovyProposal> createMappingProposals(
            ContentAssistContext context, DomainClass domainClass) {
        if (context.location == ContentAssistLocation.STATEMENT) {

            String[] mappingsFields = DomainClass.getMappingsFields();
            ClassNode enclosingGroovyType = context.getEnclosingGroovyType();
            List<IGroovyProposal> extraProposals = new ArrayList<IGroovyProposal>(
                    mappingsFields.length);
            for (String mappingFieldName : mappingsFields) {
                if (mappingFieldName.startsWith(context.completionExpression)) {
                    extraProposals.add(new GroovyFieldProposal(createFieldNode(
                            mappingFieldName, context, enclosingGroovyType,
                            Opcodes.ACC_STATIC), GRAILS_PROPOSALS_RELEVANCE_MULTIPLIER, GRAILS));
                }
            }
            return extraProposals;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Only valid within the <code>constraints</code> field in the domain class
     * 
     * @param context
     * @param domainClass
     * @return
     */
    private List<IGroovyProposal> createConstraintsProposals(
            ContentAssistContext context, DomainClass domainClass) {
        if (context.location == ContentAssistLocation.STATEMENT) {
            
            String[] contstraintsFields = DomainClass.getContstraintsFields();
            ClassNode enclosingGroovyType = context.getEnclosingGroovyType();
            List<IGroovyProposal> extraProposals = new ArrayList<IGroovyProposal>(
                    contstraintsFields.length);
            for (String constraintsFieldName : contstraintsFields) {
                if (constraintsFieldName.startsWith(context.completionExpression)) {
                    extraProposals.add(new GroovyFieldProposal(createFieldNode(
                            constraintsFieldName, context, enclosingGroovyType,
                            Opcodes.ACC_STATIC), GRAILS_PROPOSALS_RELEVANCE_MULTIPLIER, GRAILS));
                }
            }
            
            List<PropertyNode> props = domainClass.getDomainProperties();
            for (PropertyNode prop : props) {
                if (prop.getName().startsWith(context.completionExpression)) {
                    extraProposals.add(new GroovyPropertyProposal(prop, GRAILS));
                }
            }
            return extraProposals;
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    /**
     * @param context
     * @param domainClass
     * @return
     */
    private List<IGroovyProposal> createDomainProposals(
            ContentAssistContext context, DomainClass domainClass,
            boolean isStatic) {
        List<IGroovyProposal> proposals = new LinkedList<IGroovyProposal>();
        if (context.location == ContentAssistLocation.STATEMENT
                || context.location == ContentAssistLocation.EXPRESSION
                || context.location == ContentAssistLocation.SCRIPT) {
            ClassNode declaringType = domainClass.getGroovyClass();
    
            if (declaringType != null) {
                // static: name -> (returnType, declaringType)
                proposals.addAll(findStaticDomainProposals(context, domainClass, declaringType));
                proposals.addAll(findFinderProposals(context, domainClass, declaringType));
                proposals.addAll(findNamedQueryProposals(context, domainClass, declaringType));
                proposals.addAll(findDynamicFinderProposals(context, domainClass));
    
                if (!isStatic) {
                    proposals.addAll(findNonStaticDomainProposals(context, domainClass, declaringType));
                    
                    // now add all the contributed methods and properties
                    // only used in 1.3.7 and earlier
                    proposals.addAll(findContributedPropertiesProposals(context, domainClass, declaringType));
                }
            }
        }
        return proposals;
    }

    private List<IGroovyProposal>  findStaticDomainProposals(ContentAssistContext context,
            DomainClass domainClass, ClassNode declaringType) {
        Map<String, ClassNode[]> staticMembers = domainClass
                .getStaticMembers();
        List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>(3);
        for (Entry<String, ClassNode[]> entry : staticMembers.entrySet()) {
            if (ProposalUtils.looselyMatches(context.completionExpression, entry.getKey())) {
                ClassNode returnType = entry.getValue()[0];  // 0 is the return 1 is the declaring
                if (returnType == null) {
                    returnType = declaringType;
                }
                ClassNode inferredDeclaring = entry.getValue()[1];
                if (inferredDeclaring == null) {
                    inferredDeclaring = declaringType;
                }
                proposals.add(new GroovyMethodProposal(
                        createNoArgMethodNode(entry.getKey(), context,
                                inferredDeclaring, returnType,
                                Opcodes.ACC_PUBLIC & Opcodes.ACC_STATIC), GRAILS));
            }
        }
        return proposals;
    }

    private List<IGroovyProposal> findContributedPropertiesProposals(ContentAssistContext context,
            DomainClass domainClass, ClassNode declaringType) {
        Map<String, ContributedProperty> contribProps = domainClass.getAllContributedProperties();
        List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>(2);
        for (Map.Entry<String, ContributedProperty> contribProp : contribProps.entrySet()) {
            String contribName = contribProp.getKey();
            if (ProposalUtils.looselyMatches(context.completionExpression, contribName)) {
                proposals.add(new GroovyPropertyProposal(contribProp.getValue().createMockProperty(declaringType), contribProp.getValue().getContributedBy()));
            }
        }
        
        Map<String, Set<ContributedMethod>> contribMethods = domainClass.getAllContributedMethods();
        for (Entry<String, Set<ContributedMethod>> contribMethodEntry : contribMethods.entrySet()) {
            String contribName = contribMethodEntry.getKey();
            if (ProposalUtils.looselyMatches(context.completionExpression, contribName)) {
                for (ContributedMethod contribMethod : contribMethodEntry.getValue()) {
                    proposals.add(new GroovyMethodProposal(contribMethod.createMockMethod(declaringType), contribMethod.getContributedBy()));
                }
            }
        }
        return proposals;
    }

    private List<IGroovyProposal> findNonStaticDomainProposals(ContentAssistContext context,
            DomainClass domainClass,
            ClassNode declaringType) {
        Map<String, ClassNode> nonstaticMembers = domainClass
                .getNonstaticMembers();
        List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>(3);
        for (Entry<String, ClassNode> entry : nonstaticMembers
                .entrySet()) {
            if (ProposalUtils.looselyMatches(context.completionExpression, entry.getKey())) {
                ClassNode type = entry.getValue();
                if (type == null) {
                    type = declaringType;
                }
                if (DomainClass.isFieldReference(entry.getKey())) {
                    proposals.add(new GroovyFieldProposal(
                            createFieldNode(entry.getKey(), context,
                                    declaringType, type,
                                    Opcodes.ACC_PUBLIC),
                            GRAILS_PROPOSALS_RELEVANCE_MULTIPLIER, GRAILS));
                } else {
                    proposals.add(new GroovyMethodProposal(
                            createNoArgMethodNode(entry.getKey(),
                                    context, declaringType, type,
                                    Opcodes.ACC_PUBLIC), GRAILS));
                }
            }
        }
        return proposals;
    }

    private List<IGroovyProposal> findDynamicFinderProposals(ContentAssistContext context,
            DomainClass domainClass) {
        List<AnnotatedNode> finders = domainClass.getFinderValidator().findProposals(context.completionExpression);
        List<IGroovyProposal> finderProposals = new ArrayList<IGroovyProposal>(finders.size());
        for (AnnotatedNode finder : finders) {
            if (finder instanceof FieldNode) {
                finderProposals.add(new GroovyFieldProposal((FieldNode) finder, "GORM")); //$NON-NLS-1$
            } else {
                finderProposals.add(new GroovyMethodProposal((MethodNode) finder, "GORM")); //$NON-NLS-1$
            }
        }
        return finderProposals;
    }

    private List<IGroovyProposal> findNamedQueryProposals(
            ContentAssistContext context, DomainClass domainClass,
            ClassNode declaringType) {
        String[] namedQueries = domainClass.getNamedQueries();
        List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>(2);
        for (String namedQuery : namedQueries) {
            if (ProposalUtils.looselyMatches(context.completionExpression, namedQuery)) {
                proposals.add(new GroovyFieldProposal(domainClass.createNamedCriteria(declaringType, namedQuery)));
            }
        }
        return proposals;
    }

    private List<IGroovyProposal> findFinderProposals(ContentAssistContext context,
            DomainClass domainClass,
            ClassNode declaringType) {
        List<IGroovyProposal> proposals = new ArrayList<IGroovyProposal>();
        Map<String, ClassNode> dynamicFinderMembers = domainClass
                .getDynamicFinderMembers();
        for (Entry<String, ClassNode> entry : dynamicFinderMembers.entrySet()) {
            if (ProposalUtils.looselyMatches(context.completionExpression, entry.getKey())) {
                ClassNode returnType = entry.getValue();
                if (returnType == null) {
                    returnType = declaringType;
                }
                proposals.add(new GroovyFieldProposal(
                        createFieldNode(entry.getKey(), context,
                                declaringType, returnType,
                                Opcodes.ACC_PUBLIC & Opcodes.ACC_STATIC), GRAILS));
            }
        }
        return proposals;
    }

    private FieldNode createFieldNode(String fieldName,
            ContentAssistContext context, ClassNode declaringType, int flags) {
        return createFieldNode(fieldName, context, declaringType,
                VariableScope.VOID_CLASS_NODE, flags);
    }

    private FieldNode createFieldNode(String fieldName,
            ContentAssistContext context, ClassNode declaringType,
            ClassNode returnType, int flags) {
        FieldNode newField = new FieldNode(fieldName, flags, returnType,
                declaringType, null);
        newField.setDeclaringClass(declaringType);
        return newField;
    }

    private MethodNode createNoArgMethodNode(String methodName,
            ContentAssistContext context, ClassNode declaringType,
            ClassNode returnType, int flags) {
        MethodNode newMethod = new MethodNode(methodName, flags, returnType,
                new Parameter[0], new ClassNode[0], new BlockStatement());
        newMethod.setDeclaringClass(declaringType);
        return newMethod;
    
    }
}
