package org.grails.ide.api.impl;

import grails.util.BuildSettings;

import org.grails.ide.api.IBuildSettings;

public class BuildSettingsAdapter implements IBuildSettings {

	private BuildSettings buildSettings;

	public BuildSettingsAdapter(BuildSettings buildSettings) {
		this.buildSettings = buildSettings;
	}

}
