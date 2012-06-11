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
package com.springsource.sts.grails.junit.runner;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;
import org.eclipse.jdt.internal.junit4.runner.JUnit4Identifier;
import org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runners.Suite;

/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GrailsJUnit4TestSuiteReference extends JUnit4TestReference {

	private String phaseName;
	private Description desc = null;

	public GrailsJUnit4TestSuiteReference(Suite suite, String phaseName) {
		super(Request.runner(suite), null);
		this.phaseName = phaseName;
	}

	public int countTestCases() {
		return fRunner.testCount();
	}

	public void sendTree(final IVisitsTestTrees notified) {
		Description desc = getDescription();
		sendDescriptionTree(notified, desc);
	}

	public Description getDescription() {
		if (desc!=null) return desc;
		Description grailsDesc = fRunner.getDescription();
		//Grails didn't set a displayName on this one so we have to fix it and provide a nice name
		desc = Description.createSuiteDescription(phaseName);
		for (Description child : grailsDesc.getChildren()) { 
			desc.addChild(child);
		}
		return desc;
	}

	private void sendDescriptionTree(final IVisitsTestTrees notified, Description description) {
		if (description.isTest()) {
			notified.visitTreeEntry(new JUnit4Identifier(description), false, 1);
		} else {
			notified.visitTreeEntry(new JUnit4Identifier(description), true, description.getChildren().size());
			for (Description child : description.getChildren()) {
				sendDescriptionTree(notified, child);
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof JUnit4TestReference))
			return false;

		JUnit4TestReference ref= (JUnit4TestReference) obj;
		return (ref.getIdentifier().equals(getIdentifier()));
	}

	@Override
	public int hashCode() {
		return fRunner.hashCode();
	}

	public ITestIdentifier getIdentifier() {
		return new JUnit4Identifier(getDescription());
	}
}
