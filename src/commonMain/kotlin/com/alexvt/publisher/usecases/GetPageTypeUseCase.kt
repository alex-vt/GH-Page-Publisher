package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class GetPageTypeUseCase {

    fun execute(title: String, isNew: Boolean): PageType {
        val titleLowCase = title.lowercase()
        return when {
            isNew -> PageType.NEW
            titleLowCase in listOf("readme.md", "index.md", "index.html") -> PageType.INDEX
            titleLowCase.endsWith(".md") -> PageType.MARKDOWN
            settingsExtensions.any { titleLowCase.endsWith(".$it") } -> PageType.OTHER_SUPPORTED
            title == "CNAME" -> PageType.OTHER_SUPPORTED
            else -> PageType.OTHER_UNSUPPORTED
        }
    }

    private val settingsExtensions = listOf(
        "html", "js", "css", "yml", "txt", "json", "gitignore", "properties"
    )

}
