package com.tk.quicksearch.search.apps

import com.github.promeg.pinyinhelper.Pinyin
import java.util.Locale

private const val MAX_LATIN_RUN_FOR_FULL_INITIALS = 4
private const val PINYIN_CACHE_MAX_ENTRIES = 512

internal object AppSearchPinyin {
    private val cache =
        object : LinkedHashMap<String, PinyinForms>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PinyinForms>?): Boolean =
                size > PINYIN_CACHE_MAX_ENTRIES
        }

    data class PinyinForms(
        val full: String?,
        val initials: String?,
    )

    fun formsFor(text: String): List<String> {
        val forms = cachedForms(text)
        return listOfNotNull(forms.full, forms.initials)
    }

    private fun cachedForms(text: String): PinyinForms {
        synchronized(cache) {
            cache[text]?.let { return it }
            val computed = computeForms(text)
            cache[text] = computed
            return computed
        }
    }

    private fun computeForms(text: String): PinyinForms {
        if (text.isBlank() || !containsChinese(text)) {
            return PinyinForms(null, null)
        }

        val fullBuilder = StringBuilder()
        val initialsBuilder = StringBuilder()

        for (token in tokenize(text)) {
            when (token) {
                is SearchToken.Chinese -> {
                    val pinyin = Pinyin.toPinyin(token.char.toString(), "").lowercase(Locale.US)
                    if (pinyin.isBlank()) continue
                    fullBuilder.append(pinyin)
                    initialsBuilder.append(pinyin.first())
                }

                is SearchToken.Latin -> {
                    val lower = token.text.lowercase(Locale.US)
                    fullBuilder.append(lower)
                    if (lower.length <= MAX_LATIN_RUN_FOR_FULL_INITIALS) {
                        initialsBuilder.append(lower)
                    } else {
                        initialsBuilder.append(lower.first())
                    }
                }
            }
        }

        return PinyinForms(
            full = fullBuilder.toString().takeIf { it.length >= 2 },
            initials = initialsBuilder.toString().takeIf { it.length >= 2 },
        )
    }

    private fun containsChinese(text: String): Boolean = text.any(Pinyin::isChinese)

    private sealed interface SearchToken {
        data class Latin(
            val text: String,
        ) : SearchToken

        data class Chinese(
            val char: Char,
        ) : SearchToken
    }

    private fun tokenize(text: String): List<SearchToken> {
        val tokens = mutableListOf<SearchToken>()
        val latinBuffer = StringBuilder()

        fun flushLatin() {
            if (latinBuffer.isNotEmpty()) {
                tokens.add(SearchToken.Latin(latinBuffer.toString()))
                latinBuffer.clear()
            }
        }

        for (character in text) {
            when {
                Pinyin.isChinese(character) -> {
                    flushLatin()
                    tokens.add(SearchToken.Chinese(character))
                }

                character.isLetterOrDigit() -> latinBuffer.append(character)
                else -> flushLatin()
            }
        }
        flushLatin()
        return tokens
    }
}
