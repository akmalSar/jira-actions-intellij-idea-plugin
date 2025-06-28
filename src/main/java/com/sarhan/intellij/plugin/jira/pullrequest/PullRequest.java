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

package com.sarhan.intellij.plugin.jira.pullrequest;

/**
 * Represents a pull request in a version control system. This class encapsulates the
 * details of a pull request such as its identifier, title, state, and URL.
 *
 * @author Akmal Sarhan
 */
class PullRequest {

	private final int id;

	private final String title;

	private final String state;

	private final String url;

	PullRequest(int id, String title, String state, String url) {
		this.id = id;
		this.title = title;
		this.state = state;
		this.url = url;
	}

	int getId() {
		return this.id;
	}

	String getTitle() {
		return this.title;
	}

	String getState() {
		return this.state;
	}

	String getUrl() {
		return this.url;
	}

	@Override
	public String toString() {
		return String.format("PR #%d: %s [%s]", this.id, this.title, this.state);
	}

}
