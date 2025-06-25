/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sarhan.intellij.plugin.jira.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * Provides persistent state management for Jira Actions plugin settings. This class is
 * responsible for storing and retrieving configuration settings specific to the Jira
 * integration within the application.
 *
 * The settings are serialized into an XML file and stored locally, allowing the plugin to
 * maintain user-specific preferences across application restarts.
 *
 * The primary settings managed include the JIRA base URL required for constructing and
 * interacting with JIRA APIs or links.
 *
 * This class is implemented as a service, meaning it follows the singleton pattern and is
 * accessible through IntelliJ's application service framework.
 *
 * Responsibilities: - Persist plugin settings to disk. - Retrieve plugin settings from
 * disk during application initialization. - Provide a centralized point of access for
 * settings to other plugin components.
 *
 * @author Akmal Sarhan
 */
@State(name = "JiraPluginSettings", storages = @Storage("jiraPluginSettings.xml"))
public final class JiraActionsPluginSettings implements PersistentStateComponent<JiraActionsPluginSettings.State> {

	private State myState = new State();

	public static JiraActionsPluginSettings getInstance() {
		return ApplicationManager.getApplication().getService(JiraActionsPluginSettings.class);
	}

	@Override
	public State getState() {
		return this.myState;
	}

	@Override
	public void loadState(State state) {
		this.myState = state;
	}

	public static class State {

		/**
		 * Stores the base URL for a JIRA instance used throughout the application. This
		 * URL is typically used to construct API endpoints or hypertext links for
		 * accessing specific resources within the JIRA system.
		 */
		public String jiraBaseUrl = "";

		public State() {
		}

	}

}
