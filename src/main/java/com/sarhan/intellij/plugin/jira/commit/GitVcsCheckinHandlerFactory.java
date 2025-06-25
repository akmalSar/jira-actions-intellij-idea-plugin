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

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import git4idea.GitVcs;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating a Git-specific {@link CheckinHandler}. This factory is
 * responsible for providing a handler to manage certain aspects of the commit behavior in
 * the Git version control system.
 *
 * The created handler, specifically the {@link BranchCommitHandler}, is designed to
 * process commit messages based on the current branch context, ensuring that
 * branch-specific details such as JIRA ticket identifiers are incorporated into the
 * commit messages when appropriate.
 *
 * This factory is registered with IntelliJ's version control system (VCS) infrastructure
 * via the {@link VcsCheckinHandlerFactory} mechanism. It ensures that the check-in
 * handler logic is invoked at the relevant stages of the Git commit process.
 *
 * Responsibilities: - Provide integration between Git VCS and IntelliJ's commit workflow.
 * - Instantiate and supply the {@link BranchCommitHandler} for handling branch-aware
 * commit logic.
 *
 * @author Akmal Sarhan
 */
public class GitVcsCheckinHandlerFactory extends VcsCheckinHandlerFactory {

	public GitVcsCheckinHandlerFactory() {
		super(GitVcs.getKey());
	}

	@Override
	@NotNull
	protected CheckinHandler createVcsHandler(@NotNull CheckinProjectPanel panel,
			@NotNull CommitContext commitContext) {
		return new BranchCommitHandler(panel);
	}

}
