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
import javax.swing.JTextField;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a configuration dialog for the JIRA Actions plugin. This class provides a
 * graphical user interface to configure the JIRA base URL, which is essential for JIRA
 * ticket integration within the application.
 *
 * Responsibilities: - Display a form where users can configure the JIRA base URL. -
 * Validate and persist the entered configuration data. - Provide feedback on whether the
 * current configuration has been modified.
 *
 * Features: - Allows users to view and set the JIRA base URL used for constructing JIRA
 * ticket URLs. - Integrates with the {@link JiraActionsPluginSettings} to persist
 * configuration changes. - Displays a help message to guide users on the usage of the
 * JIRA base URL.
 *
 * Implements the IntelliJ IDEA {@code Configurable} interface to ensure compatibility
 * with the IDE's settings dialog architecture.
 *
 * @author Akmal Sarhan
 */
public class JiraConfigurationDialog implements Configurable {

	private JTextField jiraBaseUrlField;

	private JPanel mainPanel;

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

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		formPanel.add(new JLabel("JIRA Base URL:"), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		this.jiraBaseUrlField = new JTextField(JiraActionsPluginSettings.getInstance().getState().jiraBaseUrl, 30);
		formPanel.add(this.jiraBaseUrlField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		JLabel helpLabel = new JLabel(
				"<html><i>Enter your JIRA base URL (e.g. https://yourcompany.atlassian.net/browse/). Tickets will be opened as: baseUrl + ticketId</i></html>");
		helpLabel.setForeground(Color.GRAY);
		formPanel.add(helpLabel, gbc);

		this.mainPanel.add(formPanel, BorderLayout.NORTH);
		return this.mainPanel;
	}

	@Override
	public boolean isModified() {
		if (this.mainPanel == null) {
			return false;
		}
		return !Objects.equals(JiraActionsPluginSettings.getInstance().getState().jiraBaseUrl,
				this.jiraBaseUrlField.getText());
	}

	@Override
	public void apply() throws ConfigurationException {
		JiraActionsPluginSettings.getInstance().getState().jiraBaseUrl = this.jiraBaseUrlField.getText();

	}

}
