/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *     Pivotal Software, Inc - bugfix STS-3820, open up for regression testing
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.inplace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.bindings.Trigger;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.contentassist.ContentProposalAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.ui.contentassist.ClassContentAssistCalculator;
import org.grails.ide.eclipse.ui.contentassist.IContentAssistContext;
import org.grails.ide.eclipse.ui.contentassist.IContentAssistProposalRecorder;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsCompletionUtils.ITextWidget;

/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @author Nieraj Singh
 * @since 2.2.0
 */
public abstract class GrailsCompletionUtils {

	public static class SWTTextWidget implements ITextWidget {

		private Text text;

		public SWTTextWidget(Text text) {
			this.text = text;
		}

		public void setText(String content) {
			text.setText(content);
		}

		public String getText() {
			return text.getText();
		}

		public void setSelection(int start) {
			text.setSelection(start);
		}

	}

	public interface ITextWidget {

		void setText(String content);

		String getText();

		/** 
		 * Sets position of the cursor (i.e. selection of length 0 at given position).
		 */
		void setSelection(int length);

	}

	public static String getScriptName(String name) {
		if (name == null)
			return null;

		if (name.endsWith(".groovy")) {
			name = name.substring(0, name.length() - 7);
		}
		String naturalName = getNaturalName(getShortName(name));
		return naturalName.replaceAll("\\s", "-").toLowerCase();
	}

	public static String getShortName(String className) {
		int i = className.lastIndexOf(".");
		if (i > -1) {
			className = className.substring(i + 1, className.length());
		}
		return className;
	}

	public static String getNaturalName(String name) {
		List<String> words = new ArrayList<String>();
		int i = 0;
		char[] chars = name.toCharArray();
		for (int j = 0; j < chars.length; j++) {
			char c = chars[j];
			String w;
			if (i >= words.size()) {
				w = "";
				words.add(i, w);
			}
			else {
				w = words.get(i);
			}

			if (Character.isLowerCase(c) || Character.isDigit(c)) {
				if (Character.isLowerCase(c) && w.length() == 0)
					c = Character.toUpperCase(c);
				else if (w.length() > 1 && Character.isUpperCase(w.charAt(w.length() - 1))) {
					w = "";
					words.add(++i, w);
				}

				words.set(i, w + c);
			}
			else if (Character.isUpperCase(c)) {
				if ((i == 0 && w.length() == 0) || Character.isUpperCase(w.charAt(w.length() - 1))) {
					words.set(i, w + c);
				}
				else {
					words.add(++i, String.valueOf(c));
				}
			}

		}

		StringBuilder buf = new StringBuilder();

		for (Iterator<String> j = words.iterator(); j.hasNext();) {
			String word = j.next();
			buf.append(word);
			if (j.hasNext())
				buf.append(' ');
		}
		return buf.toString();
	}

	public static ContentProposalAdapter addTypeFieldAssistToText(Text text, IProject project) {
		KeyStroke triggerKeys = getKeyBindingFor("org.eclipse.ui.edit.text.contentAssist.proposals");
		if (triggerKeys==null) {
			//There's no workable active keybinding for content assist, so we don't provide content assist. 
			return null;
		}
		if (project == null) {
			//Without an active/selected project, there's no context to compute proposals.
			return null;
		}

		int bits = SWT.TOP | SWT.LEFT;
		ControlDecoration controlDecoration = new ControlDecoration(text, bits);
		controlDecoration.setMarginWidth(0);
		controlDecoration.setShowHover(true);
		controlDecoration.setShowOnlyOnFocus(true);
		FieldDecoration contentProposalImage = FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		controlDecoration.setImage(contentProposalImage.getImage());

		// Create the proposal provider
		GrailsProposalProvider proposalProvider = new GrailsProposalProvider(project, text);
		TextContentAdapter textContentAdapter = new TextContentAdapter();
		ContentProposalAdapter adapter = new ContentProposalAdapter(text, textContentAdapter, proposalProvider,
				triggerKeys, null);
		ILabelProvider labelProvider = new LabelProvider();
		adapter.setLabelProvider(labelProvider);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		adapter.setFilterStyle(ContentProposalAdapter.FILTER_NONE);
		return adapter;
	}

	private static KeyStroke getKeyBindingFor(String commandId) {
		IBindingService bindingService= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence[] bindings = bindingService.getActiveBindingsFor(commandId);
		if (bindings==null || bindings.length==0) {
			return null;
		} else {
			if (bindings.length > 1) {
				GrailsCoreActivator.log(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Multiple bindings for 'Content assist', ignoring all except first one"));
			}
			TriggerSequence binding = bindings[0];
			Trigger[] triggers = binding.getTriggers();
			if (triggers.length==0) return null;
			if (triggers.length > 1) {
				GrailsCoreActivator.log(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Multiple triggers in sequence for 'Content assist'. Only one keyStroke is supported. Content assist in Grails Command prompt will not work"));
				return null;
			}
			Trigger trigger = triggers[0];
			if (trigger instanceof KeyStroke) {
				return (KeyStroke) trigger;
			} else {
				GrailsCoreActivator.log(new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Trigger for 'Content assist' isn't a KeyStroke"));
				return null;
			}				
		}
	}

	public static class GrailsProposalProvider implements IContentProposalProvider {

		/**
		 * Scheduling rule to ensure proposal gatherer jobs do not run concurrently.
		 */
		private static final ISchedulingRule jobRule = new ISchedulingRule() {
			public boolean isConflicting(ISchedulingRule other) {
				return this==other;
			}
			public boolean contains(ISchedulingRule other) {
				return this==other;
			}
		};
		private Job gatherProposalsJob = null;

		private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

		private static final List<String> ENVIRONMENTS = Arrays.asList(new String[] { "prod", "test", "dev" });

		private volatile List<String> proposals = null;

		private final ITextWidget text;
		private IProject project;

		public GrailsProposalProvider(final IProject project,
				Text text) {
			this(project, new SWTTextWidget(text));
		}

		public GrailsProposalProvider(IProject project, ITextWidget textWidget) {
			this.text = textWidget;
			this.setProject(project);
		}

		/**
		 * Update content assist when selected project changes.
		 */
		public void setProject(final IProject project) {
			this.proposals = null; // Shouldn't be used until initialized
			this.project = project;
			final IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
			final String baseDir = GrailsBuildSettingsHelper.getBaseDir(project);
			if (gatherProposalsJob!=null) gatherProposalsJob.cancel();
			gatherProposalsJob = new Job("Retrieving available scripts") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<String> proposals = new ArrayList<String>();
					
					String grailsScripts = "file:" + install.getHome() + "scripts/*.groovy";
					scanForScripts(grailsScripts, proposals);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					
					String projectScripts = "file:" + baseDir + "/scripts/*.groovy";
					scanForScripts(projectScripts, proposals);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					
					String userHome = System.getProperty("user.home");
					String globalScripts = "file:" + userHome + "/.grails/scripts/*.groovy";
					scanForScripts(globalScripts, proposals);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					DependencyData data = PerProjectDependencyDataCache.get(project);
					if (data!=null) {
						String pluginsDir = data.getPluginsDirectory();
						System.out.println("pluginsDir: "+pluginsDir);
						if (pluginsDir!=null) {
							String pluginScripts = "file:" + pluginsDir + "/**/scripts/*.groovy";
							System.out.println("looking for plugin scripts: "+pluginScripts);
							scanForScripts(pluginScripts, proposals);
						}
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					}

					GrailsProposalProvider.this.proposals = proposals;
					gatherProposalsJob=null;
					return Status.OK_STATUS;
				}

			};
			gatherProposalsJob.setSystem(true);
			gatherProposalsJob.setPriority(Job.INTERACTIVE);
			gatherProposalsJob.setRule(jobRule);
			gatherProposalsJob.schedule();
		}

		private void scanForScripts(String pattern, List<String> proposals) {
			try {
				Resource[] scripts = RESOLVER.getResources(pattern);
				for (Resource script : scripts) {
					String scriptName = getScriptName(script.getFilename());
					if (!isFiltered(scriptName)) {
						proposals.add(scriptName);
					}
				}
			}
			catch (Exception e) {
				// swallow exception as Spring can't really decide what to throw if dir does not exist
			}
		}
		
		private boolean isInitialized() {
			return proposals!=null;
		}

		protected boolean isFiltered(String scriptName) {
			return scriptName.startsWith("_") || scriptName.matches("create-app|create-plugin");
		}

		public IContentProposal[] getProposals(String contents, int position) {
			if (!isInitialized()) {
				return new IContentProposal[] { new GrailsContentProposal(" -- content assist not ready yet -- ", "",
						null, null) };
			}

			String prefix = contents.substring(0, position);
			// split out environments
			String[] prefixes = StringUtils.split(prefix, " ");
			String environment = "";
			if (prefixes != null && prefixes.length > 1) {
				String potentialEnvironment = prefixes[0];
				if (ENVIRONMENTS.contains(potentialEnvironment)) {
					prefix = prefixes[1];
					environment = potentialEnvironment + " ";

				}
			}

			List<IContentProposal> newProposals = new ArrayList<IContentProposal>();

			// do it again to check second parameter for class name
			prefixes = StringUtils.split(prefix, " ");
			if (prefixes != null && prefixes.length > 1) {
				String potentialClassName = prefixes[1];
				new ClassContentAssistCalculator().computeProposals(new GrailsContentAssistContext(project,
						potentialClassName), new GrailsContentAssistProposalRecorder(environment + prefixes[0],
						newProposals));
			}

			for (String proposal : proposals) {
				if (proposal.startsWith(prefix)) {
					newProposals.add(new GrailsContentProposal(proposal, environment + proposal + " ", null, null));
				}
			}
			if (!StringUtils.hasLength(environment)) {
				for (String proposal : ENVIRONMENTS) {
					if (proposal.startsWith(prefix)) {
						newProposals.add(new GrailsContentProposal(proposal, environment + proposal + " ", null, null));
					}
				}
			}

			
			// if only one proposal is found apply it immediately
			if (newProposals.size() == 1) {
				text.setText(newProposals.get(0).getContent());
				text.setSelection(text.getText().length());
				return new IContentProposal[0];
			}

			return newProposals.toArray(new IContentProposal[newProposals.size()]);
		}

	}

	private static class GrailsContentAssistProposalRecorder implements IContentAssistProposalRecorder {

		private final String prefix;

		private final List<IContentProposal> proposals;

		public GrailsContentAssistProposalRecorder(String prefix, List<IContentProposal> proposals) {
			this.prefix = prefix;
			this.proposals = proposals;
		}

		public void recordProposal(Image image, int relevance, String displayText, String replaceText) {
			proposals.add(new GrailsContentProposal(displayText, prefix + " " + replaceText, null, image));
		}

		public void recordProposal(Image image, int relevance, String displayText, String replaceText,
				Object proposedObject) {
			proposals.add(new GrailsContentProposal(displayText, prefix + " " + replaceText, null, image));
		}
	}

	private static class GrailsContentAssistContext implements IContentAssistContext {

		private final IProject project;

		private final String prefix;

		public GrailsContentAssistContext(IProject project, String prefix) {
			this.project = project;
			this.prefix = prefix;
		}

		public String getAttributeName() {
			// no op
			return null;
		}

		public Document getDocument() {
			// no op
			return null;
		}

		public IFile getFile() {
			return project.getFile(".project");
		}

		public String getMatchString() {
			return prefix;
		}

		public Node getNode() {
			// no op
			return null;
		}

		public Node getParentNode() {
			// no op
			return null;
		}

	}

	private static class GrailsContentProposal implements IContentProposal, Comparable<GrailsContentProposal> {

		private String fLabel;

		private String fContent;

		private String fDescription;

		private Image fImage;

		public GrailsContentProposal(String label, String content, String description, Image image) {
			fLabel = label;
			fContent = content;
			fDescription = description;
			fImage = image;
		}

		public String getContent() {
			return fContent;
		}

		public int getCursorPosition() {
			if (fContent != null) {
				return fContent.length();
			}
			return 0;
		}

		public String getDescription() {
			return fDescription;
		}

		public String getLabel() {
			return fLabel;
		}

		@SuppressWarnings("unused")
		public Image getImage() {
			return fImage;
		}

		public String toString() {
			return fLabel;
		}

		public int compareTo(GrailsContentProposal o) {
			return this.fContent.compareTo(o.fContent);
		}
	}
}
