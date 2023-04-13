package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject
import java.nio.file.Paths

@AppScope
@Inject
class GhPagesRepository(
    private val cacheStorageRepository: CacheStorageRepository,
    private val fileAccessRepository: FileAccessRepository,
    private val gitRepository: GitRepository,
) {

    private fun cloneFreshFromRemote(settingsProfile: SettingsProfile): Result<Unit> {
        return try {
            val repoFolderPath =
                Paths.get(
                    cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName
                ).toString()
            fileAccessRepository.delete(repoFolderPath)
            gitRepository.clone(
                url = settingsProfile.githubPagesRepoUrl,
                personalAccessToken = settingsProfile.githubPersonalAccessToken,
                repoFolderPath,
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun fetchCleanFromRemote(settingsProfile: SettingsProfile): Result<Unit> {
        return try {
            val repoFolderPath =
                Paths.get(
                    cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName
                ).toString()
            gitRepository.fetch(
                personalAccessToken = settingsProfile.githubPersonalAccessToken,
                repoFolderPath,
            )
            gitRepository.resetToRemote(
                isHard = true,
                repoFolderPath,
            )
            gitRepository.clean(
                repoFolderPath,
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun getUpdatedRemoteRepo(settingsProfile: SettingsProfile): Result<Unit> {
        val isLocalCopyPresent =
            fileAccessRepository.isPresent(
                cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName, ".git",
            )
        return if (!isLocalCopyPresent) {
            cloneFreshFromRemote(settingsProfile)
        } else {
            fetchCleanFromRemote(settingsProfile)
        }
    }

    fun listAllRemote(
        settingsProfile: SettingsProfile,
    ): Result<List<GhPagesFile>> {
        with(getUpdatedRemoteRepo(settingsProfile)) {
            if (isFailure) return Result.failure(Exception(exceptionOrNull()))
        }
        return fileAccessRepository.listFiles(
            cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName,
        ).mapCatching { fileNames ->
            fileNames.map { fileName ->
                GhPagesFile(
                    ghPagesPath = fileName,
                    content = null
                )
            }
        }
    }

    fun getPage(
        ghPageFilePath: String,
        settingsProfile: SettingsProfile,
    ): Result<GhPagesFile> {
        return fileAccessRepository.readFile(
            cacheStorageRepository.getRootFolderPath(),
            settingsProfile.repoFolderName,
            ghPageFilePath,
        ).map {
            GhPagesFile(ghPageFilePath, content = it)
        }
    }

    private fun commitAndPush(settingsProfile: SettingsProfile): Result<Unit> {
        return try {
            val repoFolderPath =
                Paths.get(
                    cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName
                ).toString()
            gitRepository.commitAllChanges(
                commitMessage = settingsProfile.commitMessage,
                isAmend = settingsProfile.overwriteLastCommit,
                repoFolderPath,
            )
            gitRepository.push(
                personalAccessToken = settingsProfile.githubPersonalAccessToken,
                isForce = true,
                repoFolderPath,
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun publishToRemote(
        ghPagesFiles: List<GhPagesFile>,
        settingsProfile: SettingsProfile,
    ): Result<Unit> {
        with(fetchCleanFromRemote(settingsProfile)) {
            if (isFailure) return Result.failure(Exception(exceptionOrNull()))
        }
        ghPagesFiles.forEach { (path, content) ->
            fileAccessRepository.writeFile(
                content = content
                    ?: return Result.failure(IllegalStateException("No content of $path")),
                cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName, path
            )
        }
        return commitAndPush(settingsProfile)
    }

    fun deleteRemote(
        ghPageFilePaths: List<String>,
        settingsProfile: SettingsProfile,
    ): Result<Unit> {
        with(fetchCleanFromRemote(settingsProfile)) {
            if (isFailure) return Result.failure(Exception(exceptionOrNull()))
        }
        ghPageFilePaths.forEach { path ->
            fileAccessRepository.delete(
                cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName, path
            )
        }
        return commitAndPush(settingsProfile)
    }

}

data class GhPagesFile(
    val ghPagesPath: String,
    val content: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GhPagesFile
        if (ghPagesPath != other.ghPagesPath) return false
        if (!content.contentEquals(other.content)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ghPagesPath.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}