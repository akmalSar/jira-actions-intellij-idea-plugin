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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.actions.ShowAnnotateOperationsPopup;
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BitbucketAnnotationGutterActionProvider implements AnnotationGutterActionProvider {

	private static final Logger LOG = Logger.getInstance(BitbucketAnnotationGutterActionProvider.class);

	// Pattern to match Bitbucket Server URLs
	private static final Pattern BITBUCKET_URL_PATTERN = Pattern
		.compile("^https?://([^/]+)/(?:scm/)?([^/]+)/([^/]+)(?:\\.git)?/?$");

	// Pattern to extract pull request info from commit messages
	private static final Pattern PR_PATTERN = Pattern.compile(
			"(?i)(?:merged?\\s+(?:pull\\s+request\\s+|pr\\s+)?#?(\\d+)|pull\\s+request\\s+#?(\\d+)|pr\\s+#?(\\d+))");

	@Override
	@Nullable
	public AnAction createAction(@NotNull FileAnnotation annotation) {

		Project project = annotation.getProject();
		VirtualFile file = annotation.getFile();

		if ((project == null) || (file == null)) {
			return null;
		}

		// Check if this is a Git repository
		GitRepository repository = GitUtil.getRepositoryManager(project).getRepositoryForFile(file);
		if (repository == null) {
			return null;
		}

		// Get Bitbucket Server URL from remote
		String bitbucketUrl = getBitbucketServerUrl(repository);
		if (bitbucketUrl == null) {
			return null;
		}

		return new BitbucketPullRequestAction(project, repository, annotation, bitbucketUrl);
	}

	private @Nullable String getBitbucketServerUrl(@NotNull GitRepository repository) {
		try {
			// Try to get the origin remote URL
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

			// Convert SSH URL to HTTPS if needed
			if (remoteUrl.startsWith("git@")) {
				// Convert git@server:project/repo.git to https://server/project/repo
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

	private static class BitbucketPullRequestAction extends AnAction {

		private final Project project;

		private final GitRepository repository;

		private final FileAnnotation annotation;

		private final String bitbucketBaseUrl;

		BitbucketPullRequestAction(@NotNull Project project, @NotNull GitRepository repository,
				@NotNull FileAnnotation annotation, @NotNull String bitbucketBaseUrl) {
			super("Open Pull Request in Bitbucket", "Open the pull request for this commit in Bitbucket Server", null);
			this.project = project;
			this.repository = repository;
			this.annotation = annotation;
			this.bitbucketBaseUrl = bitbucketBaseUrl;
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			// Get the current line from the action event context
			Integer lineNumber = getLineNumberFromContext(e);
			if (lineNumber == null) {
				return;
			}

			ApplicationManager.getApplication().executeOnPooledThread(() -> {
				try {

					VcsRevisionNumber revision = this.annotation.getLineRevisionNumber(lineNumber);
					if (revision == null) {
						return;
					}

					String commitHash = revision.asString();
					String commitMessage = getCommitMessage(commitHash);

					if (commitMessage != null) {
						Integer prNumber = extractPullRequestNumber(commitMessage);
						if (prNumber != null) {
							String prUrl = buildPullRequestUrl(prNumber);
							SwingUtilities.invokeLater(() -> BrowserUtil.browse(prUrl));
							return;
						}
					}

					// Fallback: try to find PR by commit hash
					findPullRequestByCommit(commitHash);

				}
				catch (Exception ex) {
					LOG.warn("Failed to open pull request", ex);
				}
			});
		}

		private @Nullable Integer getLineNumberFromContext(@NotNull AnActionEvent e) {

			return ShowAnnotateOperationsPopup.getAnnotationLineNumber(e.getDataContext());

		}

		private @Nullable String getCommitMessage(@NotNull String commitHash) {
			try {
				GitLineHandler handler = new GitLineHandler(this.project, this.repository.getRoot(), GitCommand.LOG);
				handler.addParameters("--format=%B", "-n", "1", commitHash);

				String result = Git.getInstance().runCommand(handler).getOutputOrThrow();
				return result.trim();

			}
			catch (VcsException vcsException) {
				LOG.warn("Failed to get commit message for " + commitHash, vcsException);
				return null;
			}
		}

		private @Nullable Integer extractPullRequestNumber(@NotNull String commitMessage) {
			Matcher matcher = PR_PATTERN.matcher(commitMessage);
			if (matcher.find()) {
				for (int i = 1; i <= matcher.groupCount(); i++) {
					String group = matcher.group(i);
					if (group != null) {
						try {
							return Integer.parseInt(group);
						}
						catch (NumberFormatException numberFormatException) {
							// Continue to next group
						}
					}
				}
			}
			return null;
		}

		private @NotNull String buildPullRequestUrl(int prNumber) {
			// Extract project and repo from URL
			Matcher matcher = BITBUCKET_URL_PATTERN.matcher(this.bitbucketBaseUrl);
			if (matcher.matches()) {
				String server = matcher.group(1);
				String project = matcher.group(2);
				String repo = matcher.group(3);

				return String.format("https://%s/projects/%s/repos/%s/pull-requests/%d", server, project.toUpperCase(),
						repo, prNumber);
			}

			// Fallback
			return this.bitbucketBaseUrl + "/pull-requests/" + prNumber;
		}

		private void findPullRequestByCommit(@NotNull String commitHash) {
			// This would require Bitbucket REST API integration
			// For now, open the commit view
			Matcher matcher = BITBUCKET_URL_PATTERN.matcher(this.bitbucketBaseUrl);
			if (matcher.matches()) {
				String server = matcher.group(1);
				String project = matcher.group(2);
				String repo = matcher.group(3);

				String commitUrl = String.format("https://%s/projects/%s/repos/%s/commits/%s", server,
						project.toUpperCase(), repo, commitHash);

				SwingUtilities.invokeLater(() -> BrowserUtil.browse(commitUrl));
			}
		}

	}

}
