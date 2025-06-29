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

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.sarhan.intellij.plugin.jira.HttpUtils;

@Service
public final class JiraApiService {

	private static final Logger LOG = Logger.getInstance(JiraApiService.class);

	private final HttpClient httpClient;

	private final Gson gson;

	public JiraApiService() {
		this.httpClient = HttpClient.newBuilder().build();
		this.gson = new Gson();
	}

	public CompletableFuture<List<JiraTicket>> fetchAssignedTicketsAsync(String jiraBaseUrl, String apiToken) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String jql = "assignee = currentUser() AND statusCategory !=  Done ORDER BY priority DESC";
				String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);
				String url = jiraBaseUrl.replace("/browse/", "") + "/rest/api/2/search?jql=" + encodedJql
						+ "&fields=key,summary,status,priority,assignee";

				String doneGet = HttpUtils.doGet(url, apiToken).orElse(null);

				return (doneGet != null) ? parseTicketsFromResponse(doneGet, jiraBaseUrl) : Collections.emptyList();

			}
			catch (Exception exception) {
				LOG.error("Error fetching JIRA tickets", exception);
				return new ArrayList<>();
			}
		});
	}

	private List<JiraTicket> parseTicketsFromResponse(String responseBody, String baseUrl) {
		List<JiraTicket> tickets = new ArrayList<>();
		try {
			JsonObject jsonResponse = this.gson.fromJson(responseBody, JsonObject.class);
			JsonArray issues = jsonResponse.getAsJsonArray("issues");

			for (JsonElement issueElement : issues) {
				JsonObject issue = issueElement.getAsJsonObject();
				JsonObject fields = issue.getAsJsonObject("fields");
				String key = issue.get("key").getAsString();

				String summary = getStringValue(fields, "summary", "No Summary");
				String status = getNestedStringValue(fields, "status", "name", "Unknown");
				String priority = getNestedStringValue(fields, "priority", "name", "None");
				String assignee = getNestedStringValue(fields, "assignee", "displayName", "Unassigned");
				String url = baseUrl + key;

				tickets.add(new JiraTicket(key, summary, status, priority, assignee, url));
			}
		}
		catch (Exception exception) {
			LOG.error("Error parsing JIRA response", exception);
		}
		return tickets;
	}

	private String getStringValue(JsonObject obj, String field, String defaultValue) {
		JsonElement element = obj.get(field);
		return ((element != null) && !element.isJsonNull()) ? element.getAsString() : defaultValue;
	}

	private String getNestedStringValue(JsonObject obj, String field, String nestedField, String defaultValue) {
		JsonElement element = obj.get(field);
		if ((element != null) && !element.isJsonNull() && element.isJsonObject()) {
			JsonObject nestedObj = element.getAsJsonObject();
			JsonElement nestedElement = nestedObj.get(nestedField);
			if ((nestedElement != null) && !nestedElement.isJsonNull()) {
				return nestedElement.getAsString();
			}
		}
		return defaultValue;
	}

}
