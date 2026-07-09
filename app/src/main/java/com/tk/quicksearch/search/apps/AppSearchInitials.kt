package com.tk.quicksearch.search.apps

import com.github.promeg.pinyinhelper.Pinyin
import com.tk.quicksearch.search.models.AppInfo
import java.util.Locale

object AppSearchInitials {
    fun initialsFor(app: AppInfo): List<String> = aliasesFor(app.appName)

    fun aliasesFor(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        return buildList {
            computeLatinInitials(text)?.let(::add)
            addAll(AppSearchPinyin.formsFor(text))
        }.distinct()
    }

    private fun computeLatinInitials(appName: String): String? {
        if (appName.isBlank()) return null
        val source = appName.trim()
        val builder = StringBuilder()

        for (index in source.indices) {
            val current = source[index]
            if (!current.isLetterOrDigit() || Pinyin.isChinese(current)) continue

            val previous = source.getOrNull(index - 1)
            val startsWord = previous == null || !previous.isLetterOrDigit()
            val startsCamelWord =
                previous != null &&
                    previous.isLetter() &&
                    previous.isLowerCase() &&
                    current.isLetter() &&
                    current.isUpperCase()

            if (startsWord || startsCamelWord) {
                builder.append(current.lowercaseChar())
            }
        }

        val initials = builder.toString().lowercase(Locale.getDefault())
        return initials.takeIf { it.length >= 2 }
    }
}
