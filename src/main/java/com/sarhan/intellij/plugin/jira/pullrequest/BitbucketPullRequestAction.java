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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
import com.intellij.vcs.log.VcsLogDataKeys;
import com.sarhan.intellij.plugin.jira.settings.JiraActionsPluginSettings;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import icons.TasksIcons;
import org.jetbrains.annotations.NotNull;

/**
 * The BitbucketPullRequestAction class defines a custom action to retrieve and display
 * pull requests associated with a specific Git commit. It integrates with Bitbucket
 * Server to query information about pull requests based on a commit hash fetched from
 * annotated files.
 * <p>
 * This action leverages the IntelliJ Platform's AnAction framework to provide
 * functionality within an integrated development environment, allowing users to trigger
 * the action through the IDE's interface.
 * <p>
 * Key functionality includes: - Extracting the repository details from the Git remote. -
 * Making REST API calls to a configured Bitbucket Server endpoint to fetch pull requests.
 * - Parsing the API response to display a list of pull requests linked to the commit. -
 * Displaying appropriate messages or errors if no pull requests are found or if issues
 * occur.
 * <p>
 * The action relies on the project's context, file annotations, and file state to
 * determine the line and commit hash for which pull request data is retrieved.
 *
 * @author Akmal Sarhan
 */
public class BitbucketPullRequestAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(BitbucketPullRequestAction.class);

	// Constants
	private static final Pattern BITBUCKET_URL_PATTERN = Pattern
		.compile("^https?://([^/]+)/(?:scm/)?([^/]+)/([^/]+)(?:\\.git)?/?$");

	private static final String API_PATH_TEMPLATE = "https://%s/rest/api/latest/projects/%s/repos/%s/commits/%s/pull-requests?start=0&limit=25";

	private static final String PR_URL_TEMPLATE = "https://%s/projects/%s/repos/%s/pull-requests/%d";

	private static final int HTTP_OK = 200;

	private static final String BEARER_PREFIX = "Bearer ";

	// Dependencies
	private final Project project;

	private final FileAnnotation annotation;

	private final VirtualFile file;

	// Default constructor for action registration
	public BitbucketPullRequestAction() {
		super("Show Pull Requests for Commit", "Show pull requests for this commit from Bitbucket Server",
				TasksIcons.Bug);
		this.project = null;
		this.annotation = null;
		this.file = null;
		logConstructorWarning();
	}

	// Main constructor
	public BitbucketPullRequestAction(@NotNull Project project, @NotNull FileAnnotation annotation,
			@NotNull VirtualFile file) {
		super("Show Pull Requests for Commit", "Show pull requests for this commit from Bitbucket Server",
				TasksIcons.Bug);
		this.project = project;
		this.annotation = annotation;
		this.file = file;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			try {
				handleActionPerformed(event);
			}
			catch (Exception ex) {
				LOG.warn("Failed to retrieve pull requests", ex);
				SwingUtilities
					.invokeLater(() -> showErrorMessage(event, "Failed to retrieve pull requests: " + ex.getMessage()));
			}
		});
	}

	private void handleActionPerformed(@NotNull AnActionEvent event) {
		String commitHash = getCommitHash(event);
		if (commitHash == null) {
			LOG.debug("No commit hash found for the current context");
			return;
		}

		GitRepository repository = findGitRepository(event);
		if (repository == null) {
			LOG.info("No Git repository found");
			return;
		}

		String bitbucketBaseUrl = getBitbucketServerUrl(repository);
		if (bitbucketBaseUrl == null) {
			LOG.info("No Bitbucket Server URL found for repository");
			return;
		}

		List<PullRequest> pullRequests = getPullRequestsForCommit(bitbucketBaseUrl, commitHash);

		SwingUtilities.invokeLater(() -> {
			if (pullRequests.isEmpty()) {
				showNoPullRequestsMessage(event);
			}
			else {
				showPullRequestsPopup(event, pullRequests);
			}
		});
	}

	private String getCommitHash(@NotNull AnActionEvent event) {
		Integer lineNumber = getLineNumberFromContext(event);

		// Try to get commit from VCS log selection first
		String vcsLogCommit = getCommitFromVcsLog(event, lineNumber);
		if (vcsLogCommit != null) {
			return vcsLogCommit;
		}

		// Fall back to annotation if available
		return getCommitFromAnnotation(lineNumber);
	}

	private String getCommitFromVcsLog(@NotNull AnActionEvent event, Integer lineNumber) {
		if ((lineNumber != null) && (lineNumber >= 0)) {
			return null; // Use annotation for positive line numbers
		}

		var commitSelection = event.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION);
		if ((commitSelection != null) && !commitSelection.getCachedFullDetails().isEmpty()) {
			return commitSelection.getCachedFullDetails().get(0).getId().asString();
		}

		return null;
	}

	private String getCommitFromAnnotation(Integer lineNumber) {
		if ((lineNumber == null) || (lineNumber < 0) || (this.annotation == null)) {
			return null;
		}

		VcsRevisionNumber revision = this.annotation.getLineRevisionNumber(lineNumber);
		return (revision != null) ? revision.asString() : null;
	}

	private GitRepository findGitRepository(@NotNull AnActionEvent event) {
		if (this.file != null) {
			return GitUtil.getRepositoryManager(this.project).getRepositoryForFile(this.file);
		}

		List<GitRepository> repositories = GitUtil.getRepositoryManager(event.getProject()).getRepositories();
		return repositories.isEmpty() ? null : repositories.get(0);
	}

	private List<PullRequest> getPullRequestsForCommit(@NotNull String bitbucketBaseUrl, @NotNull String commitHash) {
		try {
			BitbucketUrlComponents urlComponents = parseBitbucketUrl(bitbucketBaseUrl);
			if (urlComponents == null) {
				LOG.warn("Could not parse Bitbucket URL: " + bitbucketBaseUrl);
				return Collections.emptyList();
			}

			String apiUrl = buildApiUrl(urlComponents, commitHash);
			String authToken = getAuthToken();

			if ((authToken == null) || authToken.trim().isEmpty()) {
				LOG.warn("No Bitbucket token configured");
				return Collections.emptyList();
			}

			return fetchPullRequests(apiUrl, authToken, bitbucketBaseUrl);

		}
		catch (Exception ex) {
			LOG.warn("Failed to get pull requests for commit: " + commitHash, ex);
			return Collections.emptyList();
		}
	}

	private BitbucketUrlComponents parseBitbucketUrl(@NotNull String bitbucketBaseUrl) {
		Matcher matcher = BITBUCKET_URL_PATTERN.matcher(bitbucketBaseUrl);
		if (!matcher.matches()) {
			return null;
		}

		return new BitbucketUrlComponents(matcher.group(1), // server
				matcher.group(2), // project
				matcher.group(3) // repo
		);
	}

	private String buildApiUrl(@NotNull BitbucketUrlComponents components, @NotNull String commitHash) {
		return String.format(API_PATH_TEMPLATE, components.server(), components.project().toUpperCase(),
				components.repo(), commitHash);
	}

	private String getAuthToken() {
		JiraActionsPluginSettings settings = JiraActionsPluginSettings.getInstance();
		return settings.getState().token;
	}

	private List<PullRequest> fetchPullRequests(@NotNull String apiUrl, @NotNull String authToken,
			@NotNull String bitbucketBaseUrl) throws IOException {
		HttpURLConnection connection = createConnection(apiUrl, authToken);

		try {
			int responseCode = connection.getResponseCode();
			if (responseCode == HTTP_OK) {
				String response = readResponse(connection.getInputStream());
				return parsePullRequestsResponse(response, bitbucketBaseUrl);
			}
			else {
				logApiError(connection, responseCode);
				return Collections.emptyList();
			}
		}
		finally {
			connection.disconnect();
		}
	}

	private HttpURLConnection createConnection(@NotNull String apiUrl, @NotNull String authToken) throws IOException {
		URL url = new URL(apiUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", BEARER_PREFIX + authToken);
		connection.setRequestProperty("Accept", "application/json");
		connection.setConnectTimeout(10000); // 10 seconds
		connection.setReadTimeout(30000); // 30 seconds
		return connection;
	}

	private String readResponse(@NotNull InputStream inputStream) throws IOException {
		StringBuilder response = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
		}
		return response.toString();
	}

	private void logApiError(@NotNull HttpURLConnection connection, int responseCode) {
		LOG.warn("API request failed with response code: " + responseCode);
		try (InputStream errorStream = connection.getErrorStream()) {
			if (errorStream != null) {
				String errorResponse = readResponse(errorStream);
				LOG.warn("Error response: " + errorResponse);
			}
		}
		catch (IOException ex) {
			LOG.debug("Could not read error response", ex);
		}
	}

	private List<PullRequest> parsePullRequestsResponse(@NotNull String jsonResponse,
			@NotNull String bitbucketBaseUrl) {
		try {
			Gson gson = new Gson();
			JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
			JsonArray values = response.getAsJsonArray("values");

			if (values == null) {
				return Collections.emptyList();
			}

			List<PullRequest> pullRequests = new ArrayList<>();
			for (JsonElement element : values) {
				PullRequest pr = parseSinglePullRequest(element.getAsJsonObject(), bitbucketBaseUrl);
				if (pr != null) {
					pullRequests.add(pr);
				}
			}
			return pullRequests;

		}
		catch (Exception ex) {
			LOG.warn("Failed to parse pull requests response", ex);
			return Collections.emptyList();
		}
	}

	private PullRequest parseSinglePullRequest(@NotNull JsonObject prJson, @NotNull String bitbucketBaseUrl) {
		try {
			int id = prJson.get("id").getAsInt();
			String title = prJson.get("title").getAsString();
			String state = prJson.get("state").getAsString();
			String prUrl = buildPullRequestUrl(bitbucketBaseUrl, id);

			return new PullRequest(id, title, state, prUrl);
		}
		catch (Exception ex) {
			LOG.warn("Failed to parse individual pull request", ex);
			return null;
		}
	}

	private void showPullRequestsPopup(@NotNull AnActionEvent event, @NotNull List<PullRequest> pullRequests) {
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
		popup.showCenteredInCurrentWindow(event.getProject());
	}

	private void showNoPullRequestsMessage(@NotNull AnActionEvent event) {
		JBPopupFactory.getInstance()
			.createMessage("No pull requests found for this commit or no token configured in "
					+ "settings -> tools -> JIRA Actions")
			.showCenteredInCurrentWindow(event.getProject());
	}

	private void showErrorMessage(@NotNull AnActionEvent event, @NotNull String message) {
		JBPopupFactory.getInstance().createMessage(message).showCenteredInCurrentWindow(event.getProject());
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

			String normalizedUrl = normalizeRemoteUrl(remoteUrl);
			return isValidBitbucketUrl(normalizedUrl) ? normalizedUrl : null;

		}
		catch (Exception ex) {
			LOG.warn("Failed to get Bitbucket URL", ex);
			return null;
		}
	}

	private String normalizeRemoteUrl(@NotNull String remoteUrl) {
		String normalized = remoteUrl.replaceAll("\\.git$", "");

		// Convert SSH URL to HTTPS if needed
		if (normalized.startsWith("git@")) {
			normalized = normalized.replace("git@", "https://").replace(":", "/").replaceAll("\\.git$", "");
		}

		return normalized;
	}

	private boolean isValidBitbucketUrl(@NotNull String url) {
		return BITBUCKET_URL_PATTERN.matcher(url).matches();
	}

	private Integer getLineNumberFromContext(@NotNull AnActionEvent event) {
		return ShowAnnotateOperationsPopup.getAnnotationLineNumber(event.getDataContext());
	}

	private String buildPullRequestUrl(@NotNull String bitbucketBaseUrl, int prNumber) {
		BitbucketUrlComponents components = parseBitbucketUrl(bitbucketBaseUrl);
		if (components != null) {
			return String.format(PR_URL_TEMPLATE, components.server(), components.project().toUpperCase(),
					components.repo(), prNumber);
		}
		return bitbucketBaseUrl + "/pull-requests/" + prNumber;
	}

	private void logConstructorWarning() {
		LOG.warn("BitbucketPullRequestAction created with no project, annotation, or file");
		LOG.warn("This should not happen and may cause unexpected behavior");
		LOG.warn("Please report this issue: https://github.com/sarhan-sarhan/intellij-jira-actions-plugin");
	}

	// Helper record for URL components
	private record BitbucketUrlComponents(String server, String project, String repo) {
	}

}
