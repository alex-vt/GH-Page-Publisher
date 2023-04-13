package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.streams.toList

@AppScope
@Inject
class FileAccessRepository() {

    fun listFiles(vararg absolutePathParts: String): Result<List<String>> =
        try {
            val folderPath = Paths.get("", *absolutePathParts)
            Files.walk(folderPath)
                .filter { it.isRegularFile() }
                .map { it.toAbsolutePath().toString().removePrefix(folderPath.toString()) }
                .toList()
                .let { Result.success(it) }
        } catch (t: Throwable) {
            Result.failure(t)
        }

    fun isPresent(vararg absolutePathParts: String): Boolean =
        Files.exists(Paths.get("", *absolutePathParts))

    fun readFile(vararg absolutePathParts: String): Result<ByteArray> {
        return try {
            Result.success(Files.readAllBytes(Paths.get("", *absolutePathParts)))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun writeFile(content: ByteArray, vararg absolutePathParts: String): Result<Unit> {
        val fileAbsolutePath = Paths.get("", *absolutePathParts)
        return try {
            Files.createDirectories(fileAbsolutePath.parent)
            Files.write(fileAbsolutePath, content)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun delete(vararg absolutePathParts: String): Result<Unit> {
        return try {
            Files.walk(Paths.get("", *absolutePathParts))
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.delete(path) }
                .let { Result.success(Unit) }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

}
