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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class JiraTicketsTableModel extends AbstractTableModel {

	private static final String[] COLUMN_NAMES = { "Key", "Summary", "Status", "Priority" };

	private List<JiraTicket> tickets = new ArrayList<>();

	public void updateTickets(List<JiraTicket> newTickets) {
		this.tickets = new ArrayList<>(newTickets);
		fireTableDataChanged();
	}

	public JiraTicket getTicketAt(int row) {
		return ((row >= 0) && (row < this.tickets.size())) ? this.tickets.get(row) : null;
	}

	@Override
	public int getRowCount() {
		return this.tickets.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		JiraTicket ticket = this.tickets.get(rowIndex);
		switch (columnIndex) {
			case 0:
				return ticket.getKey();
			case 1:
				return ticket.getSummary();
			case 2:
				return ticket.getStatus();
			case 3:
				return ticket.getPriority();
			default:
				return "";
		}
	}

}
