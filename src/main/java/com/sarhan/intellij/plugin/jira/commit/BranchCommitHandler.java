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

package com.sarhan.intellij.plugin.jira.commit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A handler to manage commit messages based on the current branch name in a Git
 * repository. This class ensures that the branch name, particularly formatted to include
 * applicable JIRA tickets, is incorporated into commit messages if the name is not
 * already present or if the message is empty.
 *
 * This handler skips processing for certain predefined branches like "main" or "develop".
 * If the branch name matches a configurable JIRA ticket pattern, it is formatted and
 * appended to the commit message.
 *
 * Responsibilities: - Extract and format the branch name to identify and include JIRA
 * ticket identifiers. - Modify commit messages based on the branch context. - Skip
 * insertion for specific branches or already suitable commit messages.
 *
 * The handler uses IntelliJ's CheckinHandler mechanism to invoke logic at specific
 * moments in the commit workflow.
 *
 * @author Akmal Sarhan
 */
public class BranchCommitHandler extends CheckinHandler {

	private static final String JIRA_PATTERN = "([A-Z]+)-([0-9]+)";

	private static final Pattern pattern = Pattern.compile(JIRA_PATTERN);

	// Branches to skip (main branches that don't need to be in commit messages)
	private static final Set<String> SKIP_BRANCHES = new HashSet<>(Arrays.asList("main", "master", "develop", "dev"));

	private final CheckinProjectPanel panel;

	public BranchCommitHandler(@NotNull CheckinProjectPanel panel) {
		this.panel = panel;
	}

	@Nullable
	private static String extractJiraTicket(String prName) {
		Matcher matcher = pattern.matcher(prName);
		if (matcher.find()) {
			return matcher.group(); // Returns the full match (e.g., "XXX-7703")
		}
		return null; // No JIRA ticket found
	}

	@Override
	public void includedChangesChanged() {

		ApplicationManager.getApplication().runWriteAction(this::insertBranchNameIntoCommitMessage);

	}

	@Override
	@NotNull
	public ReturnResult beforeCheckin() {
		return ReturnResult.COMMIT;
	}

	private void insertBranchNameIntoCommitMessage() {
		Project project = this.panel.getProject();
		String branchName = StringUtils.defaultString(getCurrentBranchName(project));

		if ((branchName == null) || SKIP_BRANCHES.contains(branchName)) {
			return;
		}

		// Format branch name (remove common prefixes)
		String formattedBranchName = StringUtils.defaultString(formatBranchName(branchName));

		String currentText = StringUtils.defaultString(this.panel.getCommitMessage().trim());

		// Add branch name if message is empty OR if it doesn't contain the branch name

		if (currentText.isEmpty()) {
			// Always add branch name to empty messages
			this.panel.setCommitMessage(formattedBranchName + "\n\n");
		}
		else if (!currentText.contains(formattedBranchName) && !currentText.startsWith(branchName)) {
			// Add branch name if it's not already there in any form
			String newMessage = formattedBranchName + "\n\n" + currentText;
			this.panel.setCommitMessage(newMessage);
		}

	}

	private String formatBranchName(String branchName) {
		return extractJiraTicket(StringUtils.substringAfter(branchName, "/"));
	}

	@Nullable
	private String getCurrentBranchName(@NotNull Project project) {
		try {
			GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
			Collection<GitRepository> repositories = repositoryManager.getRepositories();

			if (!repositories.isEmpty()) {
				GitRepository repository = repositories.iterator().next();
				return repository.getCurrentBranchName();
			}
		}
		catch (Exception exception) {
			// Could add logging here if needed
			return null;
		}

		return null;
	}

}
