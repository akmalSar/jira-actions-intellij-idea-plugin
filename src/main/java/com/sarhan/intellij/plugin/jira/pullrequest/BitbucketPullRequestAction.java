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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vcs.actions.ShowAnnotateOperationsPopup;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.sarhan.intellij.plugin.jira.settings.JiraActionsPluginSettings;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import icons.TasksIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The BitbucketPullRequestAction class defines a custom action to retrieve and display
 * pull requests associated with a specific Git commit. It integrates with Bitbucket
 * Server to query information about pull requests based on a commit hash fetched from
 * annotated files.
 *
 * This action leverages the IntelliJ Platform's AnAction framework to provide
 * functionality within an integrated development environment, allowing users to trigger
 * the action through the IDE's interface.
 *
 * Key functionality includes: - Extracting the repository details from the Git remote. -
 * Making REST API calls to a configured Bitbucket Server endpoint to fetch pull requests.
 * - Parsing the API response to display a list of pull requests linked to the commit. -
 * Displaying appropriate messages or errors if no pull requests are found or if issues
 * occur.
 *
 * The action relies on the project's context, file annotations, and file state to
 * determine the line and commit hash for which pull request data is retrieved.
 *
 * @author Akmal Sarhan
 */
class BitbucketPullRequestAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(BitbucketPullRequestAction.class);

	// Pattern to match Bitbucket Server URLs
	private static final Pattern BITBUCKET_URL_PATTERN = Pattern
		.compile("^https?://([^/]+)/(?:scm/)?([^/]+)/([^/]+)(?:\\.git)?/?$");

	private final Project project;

	private final FileAnnotation annotation;

	private final VirtualFile file;

	BitbucketPullRequestAction(@NotNull Project project, @NotNull FileAnnotation annotation,
			@NotNull VirtualFile file) {
		super("Show Pull Requests for Commit", "Show pull requests for this commit from Bitbucket Server",
				TasksIcons.Bug);
		this.project = project;
		this.annotation = annotation;
		this.file = file;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Integer lineNumber = getLineNumberFromContext(e);
		if (lineNumber == null) {
			return;
		}

		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			try {
				GitRepository repository = GitUtil.getRepositoryManager(this.project).getRepositoryForFile(this.file);
				if (repository == null) {
					LOG.info("No Git repository found for file: " + this.file.getPath());
					return;
				}

				String bitbucketBaseUrl = getBitbucketServerUrl(repository);
				if (bitbucketBaseUrl == null) {
					LOG.info("No Bitbucket Server URL found for repository");
					return;
				}

				VcsRevisionNumber revision = this.annotation.getLineRevisionNumber(lineNumber);
				if (revision == null) {
					return;
				}

				String commitHash = revision.asString();

				// Get pull requests for this commit
				List<PullRequest> pullRequests = getPullRequestsForCommit(bitbucketBaseUrl, commitHash);

				SwingUtilities.invokeLater(() -> {
					if (pullRequests.isEmpty()) {
						showNoPullRequestsMessage();
					}
					else {
						showPullRequestsPopup(e, pullRequests);
					}
				});

			}
			catch (Exception ex) {
				LOG.warn("Failed to retrieve pull requests", ex);
				SwingUtilities
					.invokeLater(() -> showErrorMessage("Failed to retrieve pull requests: " + ex.getMessage()));
			}
		});
	}

	private List<PullRequest> getPullRequestsForCommit(@NotNull String bitbucketBaseUrl, @NotNull String commitHash) {
		List<PullRequest> pullRequests = new ArrayList<>();

		try {
			// Extract project and repo from URL
			Matcher matcher = BITBUCKET_URL_PATTERN.matcher(bitbucketBaseUrl);
			if (!matcher.matches()) {
				LOG.warn("Could not parse Bitbucket URL: " + bitbucketBaseUrl);
				return pullRequests;
			}

			String server = matcher.group(1);
			String project = matcher.group(2);
			String repo = matcher.group(3);

			// Build the REST API URL
			String apiUrl = String.format(
					"https://%s/rest/api/latest/projects/%s/repos/%s/commits/%s/pull-requests?start=0&limit=25", server,
					project.toUpperCase(), repo, commitHash);

			// Get the token from settings
			JiraActionsPluginSettings settings = JiraActionsPluginSettings.getInstance();
			String token = settings.getState().token; // Assuming this method exists

			if ((token == null) || token.trim().isEmpty()) {
				LOG.warn("No Bitbucket token configured");
				return pullRequests;
			}

			// Make the API request
			URL url = new URL(apiUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", "Bearer " + token);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();
			if (responseCode == 200) {
				// Read the response
				StringBuilder response = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						response.append(line);
					}
				}

				// Parse JSON response
				pullRequests = parsePullRequestsResponse(response.toString(), bitbucketBaseUrl);

			}
			else {
				LOG.warn("API request failed with response code: " + responseCode);
				// Try to read error response
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
					String line;
					StringBuilder errorResponse = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						errorResponse.append(line);
					}
					LOG.warn("Error response: " + errorResponse.toString());
				}
				catch (Exception exception) {
					// Ignore if we can't read error stream
				}
			}

		}
		catch (IOException ioException) {
			LOG.warn("Failed to make API request", ioException);
		}

		return pullRequests;
	}

	private List<PullRequest> parsePullRequestsResponse(@NotNull String jsonResponse,
			@NotNull String bitbucketBaseUrl) {
		List<PullRequest> pullRequests = new ArrayList<>();

		try {
			Gson gson = new Gson();
			JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
			JsonArray values = response.getAsJsonArray("values");

			if (values != null) {
				for (JsonElement element : values) {
					JsonObject pr = element.getAsJsonObject();

					int id = pr.get("id").getAsInt();
					String title = pr.get("title").getAsString();
					String state = pr.get("state").getAsString();

					// Build PR URL
					String prUrl = buildPullRequestUrl(bitbucketBaseUrl, id);

					pullRequests.add(new PullRequest(id, title, state, prUrl));
				}
			}

		}
		catch (Exception exception) {
			LOG.warn("Failed to parse pull requests response", exception);
		}

		return pullRequests;
	}

	private void showPullRequestsPopup(@NotNull AnActionEvent e, @NotNull List<PullRequest> pullRequests) {
		BaseListPopupStep<PullRequest> step = new BaseListPopupStep<PullRequest>("Pull Requests for Commit",
				pullRequests) {
			@Override
			public PopupStep onChosen(PullRequest selectedValue, boolean finalChoice) {
				if (finalChoice) {
					BrowserUtil.browse(selectedValue.getUrl());
				}
				return FINAL_CHOICE;
			}

			@Override
			public String getTextFor(PullRequest value) {
				return value.toString();
			}

			@Override
			public String getIndexedString(PullRequest value) {
				return value.getTitle();
			}
		};

		ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);

		// Show popup in the center of the screen
		popup.showCenteredInCurrentWindow(this.project);
	}

	private void showNoPullRequestsMessage() {
		JBPopupFactory.getInstance()
			.createMessage(
					"No pull requests found for this commit or no token configured in settings -> tools -> JIRA Actions")
			.showCenteredInCurrentWindow(this.project);
	}

	private void showErrorMessage(@NotNull String message) {
		JBPopupFactory.getInstance().createMessage(message).showCenteredInCurrentWindow(this.project);
	}

	private String getBitbucketServerUrl(@NotNull GitRepository repository) {
		try {
			String remoteUrl = repository.getRemotes()
				.stream()
				.filter((GitRemote remote) -> "origin".equals(remote.getName())
						|| remote.getName().toLowerCase().contains("bitbucket"))
				.findFirst()
				.map(GitRemote::getFirstUrl)
				.orElse(null);

			if (remoteUrl == null) {
				return null;
			}

			remoteUrl = remoteUrl.replaceAll("\\.git$", "");

			// Convert SSH URL to HTTPS if needed
			if (remoteUrl.startsWith("git@")) {
				remoteUrl = remoteUrl.replace("git@", "https://").replace(":", "/").replaceAll("\\.git$", "");
			}

			// Validate it's a Bitbucket Server URL
			Matcher matcher = BITBUCKET_URL_PATTERN.matcher(remoteUrl);
			if (matcher.matches()) {
				return remoteUrl;
			}

		}
		catch (Exception exception) {
			LOG.warn("Failed to get Bitbucket URL", exception);
		}

		return null;
	}

	private @Nullable Integer getLineNumberFromContext(@NotNull AnActionEvent e) {
		return ShowAnnotateOperationsPopup.getAnnotationLineNumber(e.getDataContext());
	}

	private @NotNull String buildPullRequestUrl(@NotNull String bitbucketBaseUrl, int prNumber) {
		Matcher matcher = BITBUCKET_URL_PATTERN.matcher(bitbucketBaseUrl);
		if (matcher.matches()) {
			String server = matcher.group(1);
			String project = matcher.group(2);
			String repo = matcher.group(3);

			return String.format("https://%s/projects/%s/repos/%s/pull-requests/%d", server, project.toUpperCase(),
					repo, prNumber);
		}

		return bitbucketBaseUrl + "/pull-requests/" + prNumber;
	}

}
