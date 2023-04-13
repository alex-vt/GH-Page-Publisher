package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.*
import me.tatarka.inject.annotations.Inject
import java.nio.charset.Charset

@AppScope
@Inject
class PublishPageUseCase(
    private val ghPagesRepository: GhPagesRepository,
    private val newPageRepository: NewPageRepository,
    private val settingsRepository: SettingsRepository,
    private val fileAccessRepository: FileAccessRepository,
    private val logRepository: LogRepository,
    private val cacheStorageRepository: CacheStorageRepository,
    private val markdownHighlightUseCase: MarkdownHighlightUseCase,
    private val getPageTypeUseCase: GetPageTypeUseCase,
) {

    /**
     * The page is scanned for local links. For each, the corresponding file is added as attachment.
     * The resulting changes are the page and its attachment files.
     */
    fun execute(
        ghPagesPath: String,
        pageText: String,
        isNew: Boolean,
        isAlternativeProfile: Boolean
    ): Result<Unit> {
        val settingsProfile = with(settingsRepository.readSettings()) {
            if (isAlternativeProfile) alternativeProfile else mainProfile
        }
        val localLinkHighlights =
            markdownHighlightUseCase.execute(pageText, ContentType.NAMED_LINK)
                .filterNot { contentHighlight ->
                    getLink(pageText, contentHighlight).contains("://")
                }.sortedByDescending { contentHighlight ->
                    contentHighlight.position.first
                }
        val ghPagesAttachmentFiles = localLinkHighlights.filter { contentHighlight ->
            fileAccessRepository.isPresent(
                settingsProfile.linkedFilesFolderPath,
                getLink(pageText, contentHighlight)
            )
        }.map { contentHighlight ->
            val localLink = getLink(pageText, contentHighlight)
            GhPagesFile(
                ghPagesPath = localLink,
                content = fileAccessRepository
                    .readFile(settingsProfile.linkedFilesFolderPath, localLink).getOrNull()
            )
        }.filterNot { it.content == null }
        val fullPageText = with(settingsProfile) {
            val headers = pageHeader.toListOfNotBlankWithCondition { isNew }
            val content = listOf(pageText)
            val footers = pageFooter.toListOfNotBlankWithCondition { isNew }
            (headers + content + footers).joinToString(separator = "\n\n")
        }
        val result = ghPagesRepository.publishToRemote(
            ghPagesFiles = listOf(
                GhPagesFile(
                    ghPagesPath,
                    content = fullPageText.toByteArray(Charset.defaultCharset())
                )
            ) + getIndexPagesWithNewPageListed(
                ghPagesPath, pageText, isNew, settingsProfile,
            ) + ghPagesAttachmentFiles,
            settingsProfile
        ).apply {
            if (isSuccess && isNew) {
                newPageRepository.set("")
            }
        }
        result.exceptionOrNull()?.let {
            logRepository.log(
                "Failed to publish page with ${ghPagesAttachmentFiles.size} files", it
            )
        }
        return result
    }

    private fun String.toListOfNotBlankWithCondition(extraCondition: () -> Boolean): List<String> =
        if (isBlank() || !extraCondition()) emptyList() else listOf(this)

    private fun getLink(text: String, contentHighlight: ContentHighlight): String =
        text.slice(contentHighlight.position).dropLast(1) // removing trailing character on link

    /**
     * When page is new and settings profile specifies where in index page it should be listed,
     * list the index page updated accordingly
     */
    private fun getIndexPagesWithNewPageListed(
        ghPagesPath: String,
        pageText: String,
        isNew: Boolean,
        settingsProfile: SettingsProfile,
    ): List<GhPagesFile> {
        if (!isNew) return emptyList() // todo update existing
        val indexPageFiles = fileAccessRepository.listFiles(
            cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName,
        ).getOrNull()?.filter { fileName ->
            getPageTypeUseCase.execute(
                title = fileName.substringAfterLast('/'),
                isNew = false,
            ) == PageType.INDEX
        }?.mapNotNull { indexPageFileName ->
            fileAccessRepository.readFile(
                cacheStorageRepository.getRootFolderPath(), settingsProfile.repoFolderName,
                indexPageFileName,
            ).getOrNull()?.let { fileBytes ->
                GhPagesFile(
                    ghPagesPath = indexPageFileName.substringAfterLast('/'),
                    content = fileBytes,
                )
            }
        } ?: return emptyList()
        val pageTitle = pageText.trim().substringBefore('\n').takeIf { it.isNotBlank() }
            ?: return emptyList() // todo warn on empty page
        val mainListingHeader = "${settingsProfile.mainListingHeaderName}\n"
        return indexPageFiles.filter { indexPageFile ->
            indexPageFile.getText().contains(mainListingHeader)
        }.map { indexPageFile ->
            val originalText = indexPageFile.getText()
            val indexAfterMainListingHeader =
                originalText.indexOf(mainListingHeader) + mainListingHeader.length
            val pageLinkMarkdown = "\n[$pageTitle]($ghPagesPath)\n"
            val updatedText = originalText.take(indexAfterMainListingHeader) + pageLinkMarkdown +
                    originalText.drop(indexAfterMainListingHeader)
            indexPageFile.copy(
                content = updatedText.toByteArray(Charset.defaultCharset())
            )
        }
    }

    private fun GhPagesFile.getText(): String =
        content?.toString(Charset.defaultCharset()) ?: ""

}
