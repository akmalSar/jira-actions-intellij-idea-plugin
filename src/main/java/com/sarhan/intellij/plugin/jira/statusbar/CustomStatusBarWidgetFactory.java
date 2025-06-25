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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating instances of {@link CustomStatusBarWidget}. This class
 * implements the {@link StatusBarWidgetFactory} interface to integrate the custom widget
 * into the IntelliJ IDEA status bar.
 *
 * The {@link CustomStatusBarWidget} provides a clickable icon that allows users to
 * navigate to a JIRA ticket associated with the current Git branch name.
 *
 * Responsibilities of this factory include: - Defining the widget factory's unique ID and
 * display name. - Checking the availability of the widget for a given project. -
 * Instantiating and disposing of widget instances. - Determining whether the widget can
 * be enabled on a specific {@link StatusBar}.
 *
 * @author Akmal Sarhan
 */
public class CustomStatusBarWidgetFactory implements StatusBarWidgetFactory {

	@Override
	@NotNull
	public String getId() {
		return "CustomStatusBarWidget";
	}

	@Override
	@Nls
	@NotNull
	public String getDisplayName() {
		return "Custom Widget";
	}

	@Override
	public boolean isAvailable(@NotNull Project project) {
		return true; // Show for all projects
	}

	@Override
	@NotNull
	public StatusBarWidget createWidget(@NotNull Project project) {
		return new CustomStatusBarWidget(project);
	}

	@Override
	public void disposeWidget(@NotNull StatusBarWidget widget) {
		widget.dispose();
	}

	@Override
	public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
		return true;
	}

}
