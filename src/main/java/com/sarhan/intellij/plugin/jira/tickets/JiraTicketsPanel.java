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

package com.sarhan.intellij.plugin.jira.tickets;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import com.sarhan.intellij.plugin.jira.settings.JiraActionsPluginSettings;

public class JiraTicketsPanel extends JPanel {

	private final Project project;

	private final JiraTicketsTableModel tableModel;

	private final JBTable table;

	private final JiraApiService jiraApiService;

	public JiraTicketsPanel(Project project) {
		super(new BorderLayout());
		this.project = project;
		this.tableModel = new JiraTicketsTableModel();
		this.table = new JBTable(this.tableModel);
		this.jiraApiService = ApplicationManager.getApplication().getService(JiraApiService.class);

		setupUI();
		setupTableClickListener();
		refreshTickets();
	}

	private void setupUI() {
		// Configure table
		this.table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.table.setShowGrid(true);
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		this.table.setAutoCreateRowSorter(true);

		this.table.getColumnModel().getColumn(0).setPreferredWidth(100); // Key
		this.table.getColumnModel().getColumn(1).setPreferredWidth(300); // Summary
		this.table.getColumnModel().getColumn(2).setPreferredWidth(100); // Status
		this.table.getColumnModel().getColumn(3).setPreferredWidth(80); // Priority

		JScrollPane scrollPane = new JScrollPane(this.table);
		add(scrollPane, BorderLayout.CENTER);

		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener((ActionEvent e) -> refreshTickets());

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.add(refreshButton);
		add(buttonPanel, BorderLayout.NORTH);
	}

	private void setupTableClickListener() {
		this.table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) { // Single click to open
					int row = JiraTicketsPanel.this.table.rowAtPoint(e.getPoint());
					if (row >= 0) {
						// Convert view row index to model row index (important for
						// sorting)
						int modelRow = JiraTicketsPanel.this.table.convertRowIndexToModel(row);
						JiraTicket ticket = JiraTicketsPanel.this.tableModel.getTicketAt(modelRow);
						if (ticket != null) {
							BrowserUtil.browse(ticket.getUrl());
						}
					}
				}
			}
		});
	}

	private void refreshTickets() {
		JiraConfiguration config = getJiraConfiguration();

		if (config != null) {
			this.jiraApiService.fetchAssignedTicketsAsync(config.getBaseUrl(), config.getApiToken())
				.thenAccept((List<JiraTicket> tickets) ->
				// Update UI on EDT
				SwingUtilities.invokeLater(() -> this.tableModel.updateTickets(tickets)))
				.exceptionally((Throwable throwable) -> {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
							"Error fetching JIRA tickets: " + throwable.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE));
					return null;
				});
		}
		else {
			JOptionPane.showMessageDialog(this, "JIRA configuration not found. Please configure your JIRA settings.",
					"Configuration Required", JOptionPane.WARNING_MESSAGE);
		}
	}

	private JiraConfiguration getJiraConfiguration() {
		JiraActionsPluginSettings.State state = JiraActionsPluginSettings.getInstance().getState();

		if ((state.jiraBaseUrl != null) && (state.jiraToken != null)) {
			return new JiraConfiguration(state.jiraBaseUrl, state.jiraToken);
		}
		return null;
	}

	// Configuration data class - adapt this to match your existing config structure
	public static class JiraConfiguration {

		private final String baseUrl;

		private final String apiToken;

		public JiraConfiguration(String baseUrl, String apiToken) {
			this.baseUrl = baseUrl;
			this.apiToken = apiToken;
		}

		public String getBaseUrl() {
			return this.baseUrl;
		}

		public String getApiToken() {
			return this.apiToken;
		}

	}

}
