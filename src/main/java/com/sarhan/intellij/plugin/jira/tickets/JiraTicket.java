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

public class JiraTicket {

	private final String key;

	private final String summary;

	private final String status;

	private final String priority;

	private final String assignee;

	private final String url;

	public JiraTicket(String key, String summary, String status, String priority, String assignee, String url) {
		this.key = key;
		this.summary = summary;
		this.status = status;
		this.priority = priority;
		this.assignee = assignee;
		this.url = url;
	}

	public String getKey() {
		return this.key;
	}

	public String getSummary() {
		return this.summary;
	}

	public String getStatus() {
		return this.status;
	}

	public String getPriority() {
		return this.priority;
	}

	public String getAssignee() {
		return this.assignee;
	}

	public String getUrl() {
		return this.url;
	}

}
