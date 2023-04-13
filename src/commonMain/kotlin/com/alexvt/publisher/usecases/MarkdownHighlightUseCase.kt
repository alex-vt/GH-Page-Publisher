package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class MarkdownHighlightUseCase {

    private val markdownHighlightRules = listOf(
        HighlightRule( // multiline code, see https://regexr.com/4h9sh
            ContentType.MULTILINE_CODE,
            matchRegexString = "(^|\\s)```(?:[^`])([\\s\\S]*?)[^`]```(\$|\\s)",
            color = 0xFF99DD88
        ),
        HighlightRule( // inline code
            ContentType.INLINE_CODE,
            matchRegexString = "`(?:)([^`\n\r]+?)`",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFF88DDAA
        ),
        HighlightRule( // link
            ContentType.LINK,
            matchRegexString = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
            ),
            color = 0xFF22CCFF, isUnderlined = true
        ),
        HighlightRule( // link
            ContentType.NAMED_LINK,
            matchRegexString = "\\[(.+)\\]\\((?:)([^\n\r]+?)\\)",
            groupIndex = 2,
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
            ),
            color = 0xFF22CCFF, isUnderlined = true
        ),
        HighlightRule( // italic
            ContentType.ITALIC,
            matchRegexString = "_(?:)([^_\n\r]+?)_",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
                ContentType.LINK, ContentType.NAMED_LINK,
            ),
            color = 0xFFAA88EE, isItalic = true
        ),
        HighlightRule( // bold
            ContentType.BOLD,
            matchRegexString = "\\*\\*(?:)([^\\*\n\r]+?)\\*\\*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
                ContentType.LINK, ContentType.NAMED_LINK,
            ),
            color = 0xFFDDAA55, isBold = true
        ),
        HighlightRule( // heading 1
            ContentType.HEADING_1,
            matchRegexString = "^#[^#\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFF7755, isBold = true
        ),
        HighlightRule( // heading 2
            ContentType.HEADING_2,
            matchRegexString = "^##[^#\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFF9977, isBold = true
        ),
        HighlightRule( // heading 3
            ContentType.HEADING_3,
            matchRegexString = "^###[^#\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFFBB99, isBold = true
        ),
        HighlightRule( // heading 4
            ContentType.HEADING_4,
            matchRegexString = "^####[^#\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFFCCBB
        ),
        HighlightRule( // heading 5
            ContentType.HEADING_5,
            matchRegexString = "^#####[^#\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFFDDCC
        ),
        HighlightRule( // heading 6
            ContentType.HEADING_6,
            matchRegexString = "^######[^#\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFFEEDD
        ),
        HighlightRule( // quote
            ContentType.QUOTE,
            matchRegexString = "^>[^\n\r]*",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFAAAA99
        ),
        HighlightRule( // bullet list
            ContentType.LIST,
            matchRegexString = "^(\\+|-|\\*)\\s",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFFF7777
        ),
        HighlightRule( // numbered list
            ContentType.LIST,
            matchRegexString = "^[0-9]+\\.\\s",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
                ContentType.LINK, ContentType.NAMED_LINK,
            ),
            color = 0xFFFF7777
        ),
        HighlightRule( // link text
            ContentType.LINK_TEXT,
            matchRegexString = "[^!]\\[(.+)\\]\\((?:)([^`\n\r]+?)\\)",
            groupIndex = 1,
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
                ContentType.LINK, ContentType.NAMED_LINK,
            ),
            color = 0xFF33EEEE
        ),
        HighlightRule( // image text
            ContentType.IMAGE_TEXT,
            matchRegexString = "!\\[(.+)\\]\\((?:)([^`\n\r]+?)\\)",
            groupIndex = 1,
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE, ContentType.INLINE_CODE,
                ContentType.LINK, ContentType.NAMED_LINK,
            ),
            color = 0xFFBBDD22
        ),
        HighlightRule( // table, see https://stackoverflow.com/a/54771485
            ContentType.TABLE,
            matchRegexString = "^(\\|[^\\n]+\\|\\r?\\n)((?:\\|:?[-]+:?)+\\|)(\\n(?:\\|[^\\n]+\\|\\r?\\n?)*)?\$",
            noOverlapWith = listOf(
                ContentType.MULTILINE_CODE,
            ),
            color = 0xFFEECCEE
        ),
    )

    /**
     * Highlighting rules are sorted by priority decreasing.
     * Some rules self-exclude from other rules' highlight ranges.
     * This prevents formatting, for example, links inside code, or text inside links.
     */
    fun execute(
        text: String,
        vararg contentTypes: ContentType = ContentType.values()
    ): List<ContentHighlight> {
        val highlightRanges: MutableList<Pair<HighlightRule, IntRange>> = mutableListOf()
        markdownHighlightRules.asSequence().filter { rule ->
            rule.contentType in contentTypes
        }.flatMap { highlightRule ->
            highlightRule.matchRegexString.toRegex(RegexOption.MULTILINE).findAll(text)
                .map { matchResult ->
                    matchResult.getGroupRange(highlightRule.groupIndex)
                }.map { matchRange ->
                    highlightRule to matchRange
                }
        }.filterNot { (highlightRule, matchRange) ->
            val previousRangesWithNoIntersection =
                highlightRanges.filter { (rule, _) ->
                    rule.contentType in highlightRule.noOverlapWith
                }.map { (_, range) ->
                    range
                }
            matchRange intersectsAnyOf previousRangesWithNoIntersection
        }.forEach { (highlightRule, matchRange) ->
            highlightRanges.add(highlightRule to matchRange)
        }
        return highlightRanges.map { (highlightRule, matchRange) ->
            ContentHighlight(
                contentType = highlightRule.contentType,
                position = matchRange,
                colorArgbInt = highlightRule.color.toInt(),
                isBold = highlightRule.isBold,
                isItalic = highlightRule.isItalic,
                isUnderlined = highlightRule.isUnderlined
            )
        }
    }

    private infix operator fun IntRange.plus(addedValue: Int): IntRange =
        (first + addedValue)..(last + addedValue)

    private infix fun IntRange.intersects(other: IntRange): Boolean =
        when {
            first >= other.last -> false
            last <= other.first -> false
            else -> true
        }

    private infix fun IntRange.intersectsAnyOf(ranges: List<IntRange>) =
        ranges.any { rangeFromList ->
            rangeFromList intersects this
        }

    private fun MatchResult.getGroupRange(groupIndex: Int): IntRange {
        val existingGroupIndex = groupIndex
            .coerceAtLeast(0)
            .coerceAtMost(groupValues.size - 1)
        val groupText = groupValues[existingGroupIndex]
        val groupTextOffset = value.indexOf(groupText).coerceAtLeast(0)
        val groupFirst = range.first + groupTextOffset
        val groupLast = groupFirst + groupText.length
        return groupFirst..groupLast
    }

    private data class HighlightRule(
        val contentType: ContentType,
        val matchRegexString: String,
        val groupIndex: Int = 0,
        val noOverlapWith: List<ContentType> = emptyList(),
        val color: Long,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderlined: Boolean = false
    )

}

data class ContentHighlight(
    val contentType: ContentType,
    val position: IntRange,
    val colorArgbInt: Int,
    val isBold: Boolean,
    val isItalic: Boolean,
    val isUnderlined: Boolean
)

enum class ContentType {
    MULTILINE_CODE,
    INLINE_CODE,
    LINK,
    NAMED_LINK,
    ITALIC,
    BOLD,
    HEADING_1,
    HEADING_2,
    HEADING_3,
    HEADING_4,
    HEADING_5,
    HEADING_6,
    QUOTE,
    LIST,
    LINK_TEXT,
    IMAGE_TEXT,
    TABLE,
}
