/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.jgit.api;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.FetchResult;

/**
 * The Pull command
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-pull.html"
 *      >Git documentation about Pull</a>
 */
public class PullCommand extends TransportCommand<PullCommand, PullResult> {

	private final static String DOT = "."; //$NON-NLS-1$

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	private PullRebaseMode pullRebaseMode = null;

	private String remote;

	private String remoteBranchName;

	private MergeStrategy strategy = MergeStrategy.RECURSIVE;

	private enum PullRebaseMode implements Config.ConfigEnum {
		REBASE_PRESERVE("preserve", true, true), //$NON-NLS-1$
		REBASE("true", true, false), //$NON-NLS-1$
		NO_REBASE("false", false, false); //$NON-NLS-1$

		private final String configValue;

		private final boolean rebase;

		private final boolean preserveMerges;

		PullRebaseMode(String configValue, boolean rebase,
				boolean preserveMerges) {
			this.configValue = configValue;
			this.rebase = rebase;
			this.preserveMerges = preserveMerges;
		}

		public String toConfigValue() {
			return configValue;
		}

		public boolean matchConfigValue(String in) {
			return in.equals(configValue);
		}
	}

	/**
	 * @param repo
	 */
	protected PullCommand(Repository repo) {
		super(repo);
	}

	/**
	 * @param monitor
	 *            a progress monitor
	 * @return this instance
	 */
	public PullCommand setProgressMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	/**
	 * Set if rebase should be used after fetching. If set to true, rebase is
	 * used instead of merge. This is equivalent to --rebase on the command
	 * line.
	 * <p>
	 * If set to false, merge is used after fetching, overriding the
	 * configuration file. This is equivalent to --no-rebase on the command
	 * line.
	 * <p>
	 * This setting overrides the settings in the configuration file. By
	 * default, the setting in the repository configuration file is used.
	 * <p>
	 * A branch can be configured to use rebase by default. See
	 * branch.[name].rebase and branch.autosetuprebase.
	 *
	 * @param useRebase
	 * @return {@code this}
	 */
	public PullCommand setRebase(boolean useRebase) {
		checkCallable();
		pullRebaseMode = useRebase ? PullRebaseMode.REBASE : PullRebaseMode.NO_REBASE;
		return this;
	}

	/**
	 * Executes the {@code Pull} command with all the options and parameters
	 * collected by the setter methods (e.g.
	 * {@link #setProgressMonitor(ProgressMonitor)}) of this class. Each
	 * instance of this class should only be used for one invocation of the
	 * command. Don't call this method twice on an instance.
	 *
	 * @return the result of the pull
	 * @throws WrongRepositoryStateException
	 * @throws InvalidConfigurationException
	 * @throws DetachedHeadException
	 * @throws InvalidRemoteException
	 * @throws CanceledException
	 * @throws RefNotFoundException
	 * @throws NoHeadException
	 * @throws org.eclipse.jgit.api.errors.TransportException
	 * @throws GitAPIException
	 */
	public PullResult call() throws GitAPIException,
			WrongRepositoryStateException, InvalidConfigurationException,
			DetachedHeadException, InvalidRemoteException, CanceledException,
			RefNotFoundException, NoHeadException,
			org.eclipse.jgit.api.errors.TransportException {
		checkCallable();

		monitor.beginTask(JGitText.get().pullTaskName, 2);

		String branchName;
		try {
			String fullBranch = repo.getFullBranch();
			if (fullBranch == null)
				throw new NoHeadException(
						JGitText.get().pullOnRepoWithoutHEADCurrentlyNotSupported);
			if (!fullBranch.startsWith(Constants.R_HEADS)) {
				// we can not pull if HEAD is detached and branch is not
				// specified explicitly
				throw new DetachedHeadException();
			}
			branchName = fullBranch.substring(Constants.R_HEADS.length());
		} catch (IOException e) {
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfPullCommand,
					e);
		}

		if (!repo.getRepositoryState().equals(RepositoryState.SAFE))
			throw new WrongRepositoryStateException(MessageFormat.format(
					JGitText.get().cannotPullOnARepoWithState, repo
							.getRepositoryState().name()));

		Config repoConfig = repo.getConfig();
		if (remote == null) {
			// get the configured remote for the currently checked out branch
			// stored in configuration key branch.<branch name>.remote
			remote = repoConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_REMOTE);
		}
		if (remote == null)
			// fall back to default remote
			remote = Constants.DEFAULT_REMOTE_NAME;

		if (remoteBranchName == null)
			// get the name of the branch in the remote repository
			// stored in configuration key branch.<branch name>.merge
			remoteBranchName = repoConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_MERGE);

		// determines whether rebase should be used after fetching
		if (pullRebaseMode == null) {
			pullRebaseMode = getRebaseMode(branchName, repoConfig);
		}

		if (remoteBranchName == null)
			remoteBranchName = branchName;

		final boolean isRemote = !remote.equals("."); //$NON-NLS-1$
		String remoteUri;
		FetchResult fetchRes;
		if (isRemote) {
			remoteUri = repoConfig.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, remote,
					ConfigConstants.CONFIG_KEY_URL);
			if (remoteUri == null) {
				String missingKey = ConfigConstants.CONFIG_REMOTE_SECTION + DOT
						+ remote + DOT + ConfigConstants.CONFIG_KEY_URL;
				throw new InvalidConfigurationException(MessageFormat.format(
						JGitText.get().missingConfigurationForKey, missingKey));
			}

			if (monitor.isCancelled())
				throw new CanceledException(MessageFormat.format(
						JGitText.get().operationCanceled,
						JGitText.get().pullTaskName));

			FetchCommand fetch = new FetchCommand(repo);
			fetch.setRemote(remote);
			fetch.setProgressMonitor(monitor);
			configure(fetch);

			fetchRes = fetch.call();
		} else {
			// we can skip the fetch altogether
			remoteUri = "local repository";
			fetchRes = null;
		}

		monitor.update(1);

		if (monitor.isCancelled())
			throw new CanceledException(MessageFormat.format(
					JGitText.get().operationCanceled,
					JGitText.get().pullTaskName));

		// we check the updates to see which of the updated branches
		// corresponds
		// to the remote branch name
		AnyObjectId commitToMerge;
		if (isRemote) {
			Ref r = null;
			if (fetchRes != null) {
				r = fetchRes.getAdvertisedRef(remoteBranchName);
				if (r == null)
					r = fetchRes.getAdvertisedRef(Constants.R_HEADS
							+ remoteBranchName);
			}
			if (r == null)
				throw new JGitInternalException(MessageFormat.format(JGitText
						.get().couldNotGetAdvertisedRef, remoteBranchName));
			else
				commitToMerge = r.getObjectId();
		} else {
			try {
				commitToMerge = repo.resolve(remoteBranchName);
				if (commitToMerge == null)
					throw new RefNotFoundException(MessageFormat.format(
							JGitText.get().refNotResolved, remoteBranchName));
			} catch (IOException e) {
				throw new JGitInternalException(
						JGitText.get().exceptionCaughtDuringExecutionOfPullCommand,
						e);
			}
		}

		String upstreamName = "branch \'"
				+ Repository.shortenRefName(remoteBranchName) + "\' of "
				+ remoteUri;

		PullResult result;
		if (pullRebaseMode.rebase) {
			RebaseCommand rebase = new RebaseCommand(repo);
			RebaseResult rebaseRes = rebase.setUpstream(commitToMerge)
					.setUpstreamName(upstreamName).setProgressMonitor(monitor)
					.setOperation(Operation.BEGIN).setStrategy(strategy)
					.setPreserveMerges(pullRebaseMode.preserveMerges)
					.call();
			result = new PullResult(fetchRes, remote, rebaseRes);
		} else {
			MergeCommand merge = new MergeCommand(repo);
			merge.include(upstreamName, commitToMerge);
			merge.setStrategy(strategy);
			MergeResult mergeRes = merge.call();
			monitor.update(1);
			result = new PullResult(fetchRes, remote, mergeRes);
		}
		monitor.endTask();
		return result;
	}

	/**
	 * The remote (uri or name) to be used for the pull operation. If no remote
	 * is set, the branch's configuration will be used. If the branch
	 * configuration is missing the default value of
	 * <code>Constants.DEFAULT_REMOTE_NAME</code> will be used.
	 *
	 * @see Constants#DEFAULT_REMOTE_NAME
	 * @param remote
	 * @return {@code this}
	 * @since 3.3
	 */
	public PullCommand setRemote(String remote) {
		checkCallable();
		this.remote = remote;
		return this;
	}

	/**
	 * The remote branch name to be used for the pull operation. If no
	 * remoteBranchName is set, the branch's configuration will be used. If the
	 * branch configuration is missing the remote branch with the same name as
	 * the current branch is used.
	 *
	 * @param remoteBranchName
	 * @return {@code this}
	 * @since 3.3
	 */
	public PullCommand setRemoteBranchName(String remoteBranchName) {
		checkCallable();
		this.remoteBranchName = remoteBranchName;
		return this;
	}

	/**
	 * @return the remote used for the pull operation if it was set explicitly
	 * @since 3.3
	 */
	public String getRemote() {
		return remote;
	}

	/**
	 * @return the remote branch name used for the pull operation if it was set
	 *         explicitly
	 * @since 3.3
	 */
	public String getRemoteBranchName() {
		return remoteBranchName;
	}

	/**
	 * @param strategy
	 *            The merge strategy to use during this pull operation.
	 * @return {@code this}
	 * @since 3.4
	 */
	public PullCommand setStrategy(MergeStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	private static PullRebaseMode getRebaseMode(String branchName, Config config) {
		PullRebaseMode mode = config.getEnum(PullRebaseMode.values(),
				ConfigConstants.CONFIG_PULL_SECTION, null,
				ConfigConstants.CONFIG_KEY_REBASE, PullRebaseMode.NO_REBASE);
		mode = config.getEnum(PullRebaseMode.values(),
				ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_REBASE, mode);
		return mode;
	}
}
