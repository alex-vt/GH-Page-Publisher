package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class GitRepository() {

    fun clone(url: String, personalAccessToken: String, localRepoPath: String)

    fun fetch(personalAccessToken: String, localRepoPath: String)

    fun resetToRemote(isHard: Boolean, localRepoPath: String)

    fun clean(localRepoPath: String)

    fun commitAllChanges(commitMessage: String, isAmend: Boolean, localRepoPath: String)

    fun push(personalAccessToken: String, isForce: Boolean, localRepoPath: String)

}
