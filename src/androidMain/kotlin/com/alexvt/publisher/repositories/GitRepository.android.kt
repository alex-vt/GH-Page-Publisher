package com.alexvt.publisher.repositories

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

actual class GitRepository {

    actual fun clone(url: String, personalAccessToken: String, localRepoPath: String) {
        Git.cloneRepository()
            .setURI(url)
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    "PRIVATE-TOKEN", personalAccessToken
                )
            )
            .setDirectory(File(localRepoPath))
            .call()
    }

    actual fun fetch(personalAccessToken: String, localRepoPath: String) {
        Git.open(File(localRepoPath))
            .fetch()
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    "PRIVATE-TOKEN", personalAccessToken
                )
            )
            .setCheckFetchedObjects(true)
            .call()
    }

    actual fun resetToRemote(isHard: Boolean, localRepoPath: String) {
        val remoteMainRefPrefix = "refs/heads/"
        val remoteMainRef = Git.open(File(localRepoPath)).lsRemote().call().run {
            find { ref ->
                ref.name.startsWith(remoteMainRefPrefix)
            } ?: first()
        }
        Git.open(File(localRepoPath))
            .reset()
            .setMode(if (isHard) ResetCommand.ResetType.HARD else ResetCommand.ResetType.SOFT)
            .setRef("origin/${remoteMainRef.name.removePrefix(remoteMainRefPrefix)}")
            .call()
    }

    actual fun clean(localRepoPath: String) {
        Git.open(File(localRepoPath))
            .clean()
            .setCleanDirectories(true)
            .call()
    }

    actual fun commitAllChanges(commitMessage: String, isAmend: Boolean, localRepoPath: String) {
        Git.open(File(localRepoPath))
            .apply {
                add().addFilepattern(".").call()
                add().addFilepattern(".").setUpdate(true).call()
            }
            .commit()
            .setAmend(isAmend)
            .setMessage(commitMessage)
            .call()
    }

    actual fun push(personalAccessToken: String, isForce: Boolean, localRepoPath: String) {
        Git.open(File(localRepoPath))
            .push()
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    "PRIVATE-TOKEN", personalAccessToken
                )
            )
            .setForce(isForce)
            .call()
    }

}
