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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the configuration dialog for JIRA integration within a plugin. It provides a
 * user interface for configuring the JIRA base URL and the access token needed for API
 * interactions.
 *
 * The configuration dialog component is integrated with the IntelliJ platform's settings
 * system, allowing users to persist their preferences for JIRA-related settings, such as
 * the base URL and token.
 *
 * Responsibilities: - Displays a form for configuring the JIRA base URL and access token.
 * - Persists the updated settings to the plugin's state. - Tracks modifications to the
 * input fields and applies them when saving. - Provides reset functionality to restore
 * the fields to their persisted state.
 *
 * Methods: - getDisplayName(): Returns the name displayed in the settings menu. -
 * createComponent(): Initializes and provides the main configuration component. -
 * isModified(): Determines if any of the settings fields have been modified. - apply():
 * Saves the modified settings to persistent storage. - reset(): Restores the components
 * to reflect the last saved settings. - disposeUIResources(): Disposes of any UI
 * resources when the dialog is closed.
 *
 * Implements: - Configurable: Allows the dialog to be used in the IntelliJ settings
 * framework.
 *
 * @author Akmal Sarhan
 */

public class JiraConfigurationDialog implements Configurable {

	private JTextField jiraBaseUrlField;

	private JPanel mainPanel;

	private JPasswordField bitbucketTokenField;

	private JPasswordField jiraTokenField;

	@Override
	@Nls(capitalization = Nls.Capitalization.Title)
	public String getDisplayName() {
		return "JIRA Integration";
	}

	@Override
	@Nullable
	public JComponent createComponent() {
		this.mainPanel = new JPanel(new BorderLayout());

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// JIRA Base URL Label
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		formPanel.add(new JLabel("JIRA Base URL:"), gbc);

		// JIRA Base URL Field
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		this.jiraBaseUrlField = new JTextField(JiraActionsPluginSettings.getInstance().getState().jiraBaseUrl, 30);
		formPanel.add(this.jiraBaseUrlField, gbc);

		// Help text for URL
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		JLabel helpLabel = new JLabel(
				"<html><i>Enter your JIRA base URL (e.g. https://yourcompany.atlassian.net/browse/). Tickets will be opened as: baseUrl + ticketId</i></html>");
		helpLabel.setForeground(Color.GRAY);
		formPanel.add(helpLabel, gbc);

		// JIRA Access Token Label
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(15, 5, 5, 5); // Extra top margin to separate from
		// username section
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		formPanel.add(new JLabel("JIRA Access Token:"), gbc);

		// JIRA Access Token Field
		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(15, 5, 5, 5);
		this.jiraTokenField = new JPasswordField(30);
		// Load existing token if available
		String existingJiraToken = JiraActionsPluginSettings.getInstance().getState().jiraToken;
		if (existingJiraToken != null) {
			this.jiraTokenField.setText(existingJiraToken);
		}
		formPanel.add(this.jiraTokenField, gbc);

		// Help text for JIRA token
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		JLabel jiraTokenHelpLabel = new JLabel("<html><i>Enter your JIRA API token for authentication</i></html>");
		jiraTokenHelpLabel.setForeground(Color.GRAY);
		formPanel.add(jiraTokenHelpLabel, gbc);

		// Bitbucket Access Token Label
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(15, 5, 5, 5); // Extra top margin to separate from
		// JIRA token section
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		formPanel.add(new JLabel("Bitbucket Access Token:"), gbc);

		// Bitbucket Access Token Field
		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(15, 5, 5, 5);
		this.bitbucketTokenField = new JPasswordField(30);
		// Load existing token if available
		String existingBitbucketToken = JiraActionsPluginSettings.getInstance().getState().bitbucketToken;
		if (existingBitbucketToken != null) {
			this.bitbucketTokenField.setText(existingBitbucketToken);
		}
		formPanel.add(this.bitbucketTokenField, gbc);

		// Help text for Bitbucket token
		gbc.gridx = 0;
		gbc.gridy = 7;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		JLabel bitbucketTokenHelpLabel = new JLabel(
				"<html><i>Enter your Bitbucket API token for authentication</i></html>");
		bitbucketTokenHelpLabel.setForeground(Color.GRAY);
		formPanel.add(bitbucketTokenHelpLabel, gbc);

		this.mainPanel.add(formPanel, BorderLayout.NORTH);
		return this.mainPanel;
	}

	@Override
	public boolean isModified() {
		if (this.mainPanel == null) {
			return false;
		}

		JiraActionsPluginSettings.State state = JiraActionsPluginSettings.getInstance().getState();

		// Check if URL is modified
		boolean urlModified = !Objects.equals(state.jiraBaseUrl, this.jiraBaseUrlField.getText());

		// Check if JIRA token is modified
		boolean jiraTokenModified = !Objects.equals(state.jiraToken, String.valueOf(this.jiraTokenField.getPassword()));

		// Check if Bitbucket token is modified
		boolean bitbucketTokenModified = !Objects.equals(state.bitbucketToken,
				String.valueOf(this.bitbucketTokenField.getPassword()));

		return urlModified || jiraTokenModified || bitbucketTokenModified;
	}

	@Override
	public void apply() throws ConfigurationException {
		JiraActionsPluginSettings.State state = JiraActionsPluginSettings.getInstance().getState();
		state.jiraBaseUrl = this.jiraBaseUrlField.getText();
		state.jiraToken = String.valueOf(this.jiraTokenField.getPassword());
		state.bitbucketToken = String.valueOf(this.bitbucketTokenField.getPassword());

		// Ensure the state is persisted
		JiraActionsPluginSettings.getInstance().loadState(state);
	}

	@Override
	public void reset() {
		if (this.mainPanel != null) {
			JiraActionsPluginSettings.State state = JiraActionsPluginSettings.getInstance().getState();
			this.jiraBaseUrlField.setText(state.jiraBaseUrl);
			this.jiraTokenField.setText((state.jiraToken != null) ? state.jiraToken : "");
			this.bitbucketTokenField.setText((state.bitbucketToken != null) ? state.bitbucketToken : "");
		}
	}

	@Override
	public void disposeUIResources() {
		this.mainPanel = null;
		this.jiraBaseUrlField = null;
		this.jiraTokenField = null;
		this.bitbucketTokenField = null;
	}

}
