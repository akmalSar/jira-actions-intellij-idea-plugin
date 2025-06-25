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

package com.sarhan.intellij.plugin.jira.statusbar;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.sarhan.intellij.plugin.jira.settings.JiraActionsPluginSettings;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import icons.TasksIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CustomStatusBarWidget is an implementation of both StatusBarWidget and
 * StatusBarWidget.IconPresentation. This widget displays an icon on the IntelliJ IDEA
 * status bar, allowing users to navigate to a JIRA ticket associated with the current Git
 * branch name.
 *
 * The core functionality includes: - Extracting JIRA ticket identifiers from the current
 * Git branch name based on a predefined pattern. - Opening the corresponding JIRA ticket
 * in the browser using the base URL from plugin settings. - Displaying notifications to
 * indicate the success or failure of operations.
 *
 * @author Akmal Sarhan
 */
public class CustomStatusBarWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {

	private static final String WIDGET_ID = "CustomStatusBarWidget";

	// Pattern to extract JIRA ticket from branch name
	// Matches patterns like: feature/ABC-123-description, bugfix/XYZ-456, ABC-789
	private static final Pattern JIRA_TICKET_PATTERN = Pattern
		.compile("(?:feature/|bugfix/|hotfix/|release/)?([A-Z]+-\\d+)", Pattern.CASE_INSENSITIVE);

	private final Project project;

	// Remove static initialization - lazy load instead
	private NotificationGroup notificationGroup;

	private StatusBar statusBar;

	public CustomStatusBarWidget(@NotNull Project project) {
		this.project = project;
	}

	// Lazy initialization of notification group
	private NotificationGroup getNotificationGroup() {
		if (this.notificationGroup == null) {
			this.notificationGroup = NotificationGroup.balloonGroup("JIRA Ticket Plugin");
		}
		return this.notificationGroup;
	}

	@Override
	@NotNull
	public String ID() {
		return WIDGET_ID;
	}

	@Override
	public void install(@NotNull StatusBar statusBar) {
		this.statusBar = statusBar;
	}

	@Override
	public void dispose() {
		this.statusBar = null;
	}

	@Override
	@Nullable
	public WidgetPresentation getPresentation() {
		return this;
	}

	@Override
	@NotNull
	public Icon getIcon() {
		return TasksIcons.Bug;
	}

	@Override
	@NotNull
	public String getTooltipText() {
		return "go to JIRA ticket";
	}

	@Override
	@Nullable
	public Consumer<MouseEvent> getClickConsumer() {
		return this::consume;
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
			// Handle silently
		}

		return null;
	}

	@Nullable
	private String extractJiraTicketFromBranch(@Nullable String branchName) {
		if (branchName == null) {
			return null;
		}

		Matcher matcher = JIRA_TICKET_PATTERN.matcher(branchName);
		if (matcher.find()) {
			return matcher.group(1).toUpperCase();
		}

		return null;
	}

	private void performCustomAction(Project project) {
		if (project == null) {
			return;
		}

		String branchName = getCurrentBranchName(project);
		if (branchName == null) {
			showNotification(project, "No Git repository found or not on any branch", NotificationType.WARNING);
			return;
		}

		String jiraTicket = extractJiraTicketFromBranch(branchName);
		if (jiraTicket == null) {
			showNotification(project, "No JIRA ticket found in branch: " + branchName, NotificationType.WARNING);
			return;
		}
		JiraActionsPluginSettings.State settings = JiraActionsPluginSettings.getInstance().getState();
		if (StringUtils.isBlank(settings.jiraBaseUrl)
				|| "https://yourcompany.atlassian.net/browse/".equals(settings.jiraBaseUrl)) {
			showNotification(project, "Please configure JIRA URL in Settings: Tools -> Jira Actions",
					NotificationType.ERROR);
			return;
		}
		String jiraUrl = "%s%s%s".formatted(settings.jiraBaseUrl, jiraTicket, "?devStatusDetailDialog=pullrequest");
		BrowserUtil.browse(jiraUrl);

		showNotification(project, "Opening JIRA ticket: %s".formatted(jiraTicket), NotificationType.INFORMATION);

	}

	private void showNotification(@NotNull Project project, @NotNull String message, @NotNull NotificationType type) {
		// Use the lazy-loaded notification group
		Notification notification = getNotificationGroup().createNotification(message, type);
		notification.notify(project);
	}

	private void consume(MouseEvent mouseEvent) {
		performCustomAction(this.project);
	}

}
