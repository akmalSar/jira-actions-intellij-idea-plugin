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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a specific {@link AnAction} for the annotation gutter in integration with
 * Bitbucket. This class implements {@link AnnotationGutterActionProvider} and is
 * responsible for creating actions associated with file annotations in the gutter,
 * enabling pull request workflows.
 *
 * @author Akmal Sarhan
 */
public class BitbucketAnnotationGutterActionProvider implements AnnotationGutterActionProvider {

	private static final Logger LOG = Logger.getInstance(BitbucketAnnotationGutterActionProvider.class);

	@Override
	@Nullable
	public AnAction createAction(@NotNull FileAnnotation annotation) {
		Project project = annotation.getProject();
		VirtualFile file = annotation.getFile();

		if ((project == null) || (file == null)) {
			return null;
		}

		return new BitbucketPullRequestAction(project, annotation, file);
	}

}
