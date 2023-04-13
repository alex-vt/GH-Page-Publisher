/*
 * Copyright (C) 2008-2013, Google Inc.
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

package org.eclipse.jgit.merge;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.errors.NoMergeBaseException.MergeBaseFailureReason;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * Instance of a specific {@link MergeStrategy} for a single {@link Repository}.
 */
public abstract class Merger {
	/** The repository this merger operates on. */
	protected final Repository db;

	/** Reader to support {@link #walk} and other object loading. */
	protected ObjectReader reader;

	/** A RevWalk for computing merge bases, or listing incoming commits. */
	protected RevWalk walk;

	private ObjectInserter inserter;

	/** The original objects supplied in the merge; this can be any tree-ish. */
	protected RevObject[] sourceObjects;

	/** If {@link #sourceObjects}[i] is a commit, this is the commit. */
	protected RevCommit[] sourceCommits;

	/** The trees matching every entry in {@link #sourceObjects}. */
	protected RevTree[] sourceTrees;

	/**
	 * Create a new merge instance for a repository.
	 *
	 * @param local
	 *            the repository this merger will read and write data on.
	 */
	protected Merger(final Repository local) {
		db = local;
		inserter = db.newObjectInserter();
		reader = inserter.newReader();
		walk = new RevWalk(reader);
	}

	/**
	 * @return the repository this merger operates on.
	 */
	public Repository getRepository() {
		return db;
	}

	/** @return an object writer to create objects in {@link #getRepository()}. */
	public ObjectInserter getObjectInserter() {
		return inserter;
	}

	/**
	 * Set the inserter this merger will use to create objects.
	 * <p>
	 * If an inserter was already set on this instance (such as by a prior set,
	 * or a prior call to {@link #getObjectInserter()}), the prior inserter as
	 * well as the in-progress walk will be released.
	 *
	 * @param oi
	 *            the inserter instance to use. Must be associated with the
	 *            repository instance returned by {@link #getRepository()}.
	 */
	public void setObjectInserter(ObjectInserter oi) {
		walk.release();
		reader.release();
		inserter.release();
		inserter = oi;
		reader = oi.newReader();
		walk = new RevWalk(reader);
	}

	/**
	 * Merge together two or more tree-ish objects.
	 * <p>
	 * Any tree-ish may be supplied as inputs. Commits and/or tags pointing at
	 * trees or commits may be passed as input objects.
	 *
	 * @param tips
	 *            source trees to be combined together. The merge base is not
	 *            included in this set.
	 * @return true if the merge was completed without conflicts; false if the
	 *         merge strategy cannot handle this merge or there were conflicts
	 *         preventing it from automatically resolving all paths.
	 * @throws IncorrectObjectTypeException
	 *             one of the input objects is not a commit, but the strategy
	 *             requires it to be a commit.
	 * @throws IOException
	 *             one or more sources could not be read, or outputs could not
	 *             be written to the Repository.
	 */
	public boolean merge(final AnyObjectId... tips) throws IOException {
		return merge(true, tips);
	}

	/**
	 * Merge together two or more tree-ish objects.
	 * <p>
	 * Any tree-ish may be supplied as inputs. Commits and/or tags pointing at
	 * trees or commits may be passed as input objects.
	 *
	 * @since 3.5
	 * @param flush
	 *            whether to flush the underlying object inserter when finished to
	 *            store any content-merged blobs and virtual merged bases; if
	 *            false, callers are responsible for flushing.
	 * @param tips
	 *            source trees to be combined together. The merge base is not
	 *            included in this set.
	 * @return true if the merge was completed without conflicts; false if the
	 *         merge strategy cannot handle this merge or there were conflicts
	 *         preventing it from automatically resolving all paths.
	 * @throws IncorrectObjectTypeException
	 *             one of the input objects is not a commit, but the strategy
	 *             requires it to be a commit.
	 * @throws IOException
	 *             one or more sources could not be read, or outputs could not
	 *             be written to the Repository.
	 */
	public boolean merge(final boolean flush, final AnyObjectId... tips)
			throws IOException {
		sourceObjects = new RevObject[tips.length];
		for (int i = 0; i < tips.length; i++)
			sourceObjects[i] = walk.parseAny(tips[i]);

		sourceCommits = new RevCommit[sourceObjects.length];
		for (int i = 0; i < sourceObjects.length; i++) {
			try {
				sourceCommits[i] = walk.parseCommit(sourceObjects[i]);
			} catch (IncorrectObjectTypeException err) {
				sourceCommits[i] = null;
			}
		}

		sourceTrees = new RevTree[sourceObjects.length];
		for (int i = 0; i < sourceObjects.length; i++)
			sourceTrees[i] = walk.parseTree(sourceObjects[i]);

		try {
			boolean ok = mergeImpl();
			if (ok && flush)
				inserter.flush();
			return ok;
		} finally {
			if (flush)
				inserter.release();
			reader.release();
		}
	}

	/**
	 * @return the ID of the commit that was used as merge base for merging, or
	 *         null if no merge base was used or it was set manually
	 * @since 3.2
	 */
	public abstract ObjectId getBaseCommitId();

	/**
	 * Return the merge base of two commits.
	 * <p>
	 * May only be called after {@link #merge(RevCommit...)}.
	 *
	 * @param aIdx
	 *            index of the first commit in tips passed to
	 *            {@link #merge(RevCommit...)}.
	 * @param bIdx
	 *            index of the second commit in tips passed to
	 *            {@link #merge(RevCommit...)}.
	 * @return the merge base of two commits
	 * @throws IncorrectObjectTypeException
	 *             one of the input objects is not a commit.
	 * @throws IOException
	 *             objects are missing or multiple merge bases were found.
	 * @deprecated use {@link #getBaseCommitId()} instead, as that does not
	 *             require walking the commits again
	 */
	@Deprecated
	public RevCommit getBaseCommit(final int aIdx, final int bIdx)
			throws IncorrectObjectTypeException,
			IOException {
		if (sourceCommits[aIdx] == null)
			throw new IncorrectObjectTypeException(sourceObjects[aIdx],
					Constants.TYPE_COMMIT);
		if (sourceCommits[bIdx] == null)
			throw new IncorrectObjectTypeException(sourceObjects[bIdx],
					Constants.TYPE_COMMIT);
		return getBaseCommit(sourceCommits[aIdx], sourceCommits[bIdx]);
	}

	/**
	 * Return the merge base of two commits.
	 *
	 * @param a
	 *            the first commit in {@link #sourceObjects}.
	 * @param b
	 *            the second commit in {@link #sourceObjects}.
	 * @return the merge base of two commits
	 * @throws IncorrectObjectTypeException
	 *             one of the input objects is not a commit.
	 * @throws IOException
	 *             objects are missing or multiple merge bases were found.
	 * @since 3.0
	 */
	protected RevCommit getBaseCommit(RevCommit a, RevCommit b)
			throws IncorrectObjectTypeException, IOException {
		walk.reset();
		walk.setRevFilter(RevFilter.MERGE_BASE);
		walk.markStart(a);
		walk.markStart(b);
		final RevCommit base = walk.next();
		if (base == null)
			return null;
		final RevCommit base2 = walk.next();
		if (base2 != null) {
			throw new NoMergeBaseException(
					MergeBaseFailureReason.MULTIPLE_MERGE_BASES_NOT_SUPPORTED,
					MessageFormat.format(
					JGitText.get().multipleMergeBasesFor, a.name(), b.name(),
					base.name(), base2.name()));
		}
		return base;
	}

	/**
	 * Open an iterator over a tree.
	 *
	 * @param treeId
	 *            the tree to scan; must be a tree (not a treeish).
	 * @return an iterator for the tree.
	 * @throws IncorrectObjectTypeException
	 *             the input object is not a tree.
	 * @throws IOException
	 *             the tree object is not found or cannot be read.
	 */
	protected AbstractTreeIterator openTree(final AnyObjectId treeId)
			throws IncorrectObjectTypeException, IOException {
		return new CanonicalTreeParser(null, reader, treeId);
	}

	/**
	 * Execute the merge.
	 * <p>
	 * This method is called from {@link #merge(AnyObjectId[])} after the
	 * {@link #sourceObjects}, {@link #sourceCommits} and {@link #sourceTrees}
	 * have been populated.
	 *
	 * @return true if the merge was completed without conflicts; false if the
	 *         merge strategy cannot handle this merge or there were conflicts
	 *         preventing it from automatically resolving all paths.
	 * @throws IncorrectObjectTypeException
	 *             one of the input objects is not a commit, but the strategy
	 *             requires it to be a commit.
	 * @throws IOException
	 *             one or more sources could not be read, or outputs could not
	 *             be written to the Repository.
	 */
	protected abstract boolean mergeImpl() throws IOException;

	/**
	 * @return resulting tree, if {@link #merge(AnyObjectId[])} returned true.
	 */
	public abstract ObjectId getResultTreeId();
}
