/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsCompletionUtils.GrailsProposalProvider;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsCompletionUtils.ITextWidget;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;

public class CommandPromptProposalProviderTest extends GrailsTest {
	
	public class TextWidget implements ITextWidget {

		private String content = "";
		private int selectionStart = 0;
		private int selectionLen = 0;
		
		public void setText(String content) {
			Assert.isNotNull(content);
			this.content = content;
		}

		public String getText() {
			return content;
		}
		
		@Override
		public String toString() {
			return "TextWidget("+content+")";
		}

		public void setSelection(int start) {
			this.selectionStart = start;
			this.selectionLen = 0;
		}
		
		public int getSelectionStart() {
			return selectionStart;
		}

		public int getSelectionLength() {
			return selectionLen;
		}
		
	}
	
	private IProject project;
	private GrailsProposalProvider proposalProvider;
	private TextWidget textProvider;

	private void waitReady() throws Exception {
		input("cre"); //Need some input to test readyness of content assister
		new ACondition("Content Assist ready") {
			@Override
			public boolean test() throws Exception {
				return !notReady();
			}

			private boolean notReady() {
				IContentProposal[] proposals = proposalProvider.getProposals("cre", 3);
				// if not ready it will return a singe proposal saying "not ready" in its label
				if (proposals.length==1) {
					return proposals[0].getLabel().contains("not ready");
				}
				return false;
			}
		}.waitFor(5000); //5 seconds should be plenty... or the CA engine really sucks.
		input(""); //Set empty input as starting state for all the tests.
	}

	private String[] getContents(IContentProposal[] proposals) {
		String[] texts = new String[proposals.length];
		for (int i = 0; i < texts.length; i++) {
			texts[i] = proposals[i].getContent();
		}
		return texts;
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		project = ensureProject(CommandPromptProposalProviderTest.class.getName());
		textProvider = new TextWidget();
		proposalProvider = new GrailsProposalProvider(project, textProvider);
		waitReady();
	}
	
	/**
	 * Put input text into the test text widget
	 */
	private void input(String inputText) {
		textProvider.setText(inputText);
		textProvider.setSelection(inputText.length());
	}
	
	//////////// tests start here
	
	public void testCreateCommands() throws Exception {
		input("create");
		IContentProposal[] proposals = proposalProvider.getProposals(textProvider.getText(), textProvider.getSelectionStart());
		String[] proposalTexts = getContents(proposals);
		assertExpectedElements(proposalTexts, 
				"create-controller ",
				"create-domain-class ",
				"create-service "
		);
		for (String p : proposalTexts) {
			System.out.println(p);
		}
	}
	
	public void testGenerateCommands() throws Exception {
		input("generate");
		IContentProposal[] proposals = proposalProvider.getProposals(textProvider.getText(), textProvider.getSelectionStart());
		String[] proposalTexts = getContents(proposals);
		assertExpectedElements(proposalTexts, 
				"generate-controller ",
				"generate-views ",
				"generate-all "
		);
	}
	

}
