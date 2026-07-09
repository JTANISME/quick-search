package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.fuzzy.FuzzySearchPerformanceLogger
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.SearchQueryContext
import java.util.Locale

object AppSearchAlgorithm {
    fun findMatches(
        query: String,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        sortAppsByUsageEnabled: Boolean,
    ): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return findMatches(
            queryContext = SearchQueryContext.fromRawQuery(query),
            source = source,
            limit = limit,
            fuzzySearchStrategy = fuzzySearchStrategy,
            appNicknames = appNicknames,
            sortAppsByUsageEnabled = sortAppsByUsageEnabled,
        )
    }

    fun findMatches(
        queryContext: SearchQueryContext,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        sortAppsByUsageEnabled: Boolean,
    ): List<AppInfo> {
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        val canUseFuzzySearch = fuzzySearchStrategy.canUseFuzzySearch(queryContext.normalizedQuery)
        val fuzzyCandidateLimit =
            if (canUseFuzzySearch) {
                fuzzySearchStrategy.fuzzyCandidateLimitFor(queryContext.normalizedQuery)
            } else {
                0
            }
        var fuzzyCandidatesScored = 0

        val searchBlock = {
            source
                .asSequence()
                .mapNotNull { app ->
                    calculateAppMatch(
                        app = app,
                        queryContext = queryContext,
                        fuzzySearchStrategy = fuzzySearchStrategy,
                        appNicknames = appNicknames,
                        canScoreFuzzyCandidate = {
                            if (fuzzyCandidatesScored >= fuzzyCandidateLimit) {
                                false
                            } else {
                                fuzzyCandidatesScored += 1
                                true
                            }
                        },
                    )
                }.sortedWith(createAppComparator(sortAppsByUsageEnabled))
                .map { it.app }
                .take(limit)
                .toList()
        }

        if (!canUseFuzzySearch) return searchBlock()

        return FuzzySearchPerformanceLogger.measure(
            section = SearchSection.APPS,
            query = queryContext.normalizedQuery,
            candidateCount = minOf(source.size, fuzzyCandidateLimit),
            block = searchBlock,
        )
    }

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean,
    )

    private fun calculateAppMatch(
        app: AppInfo,
        queryContext: SearchQueryContext,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        canScoreFuzzyCandidate: () -> Boolean,
    ): AppMatch? {
        val nickname = appNicknames[app.packageName]
        val aliases = searchAliasesFor(app.appName, nickname)
        val priority = AppSearchPolicy.matchPriority(app.appName, nickname, queryContext, aliases)
        if (AppSearchPolicy.hasMatch(priority)) {
            if (
                !AppSearchPolicy.areAllQueryTokensCovered(
                    queryContext,
                    app.appName,
                    nickname,
                    aliases,
                    fuzzySearchStrategy,
                )
            ) {
                return null
            }
            return AppMatch(app, priority, 0, false)
        }

        if (
            !fuzzySearchStrategy.isTypoEligibleCandidate(
                query = queryContext.normalizedQuery,
                appName = app.appName,
                nickname = nickname,
                initials = aliases,
            )
        ) {
            return null
        }

        if (!canScoreFuzzyCandidate()) return null

        val match =
            fuzzySearchStrategy.computeMatch(
                query = queryContext.normalizedQuery,
                app = app,
                nickname = appNicknames[app.packageName],
                initials = aliases,
            )

        return match?.let {
            if (
                !AppSearchPolicy.areAllQueryTokensCovered(
                    queryContext,
                    app.appName,
                    nickname,
                    aliases,
                    fuzzySearchStrategy,
                )
            ) {
                return null
            }
            AppMatch(app, it.priority, it.score, true)
        }
    }

    private fun createAppComparator(sortAppsByUsageEnabled: Boolean): Comparator<AppMatch> {
        return Comparator { first, second ->
            if (first.isFuzzy != second.isFuzzy) {
                return@Comparator if (first.isFuzzy) 1 else -1
            }

            if (!first.isFuzzy) {
                val priorityCompare = first.priority.compareTo(second.priority)
                if (priorityCompare != 0) {
                    return@Comparator priorityCompare
                }
                return@Comparator compareByUsageOrName(first.app, second.app, sortAppsByUsageEnabled)
            }

            val fuzzyCompare = second.fuzzyScore.compareTo(first.fuzzyScore)
            if (fuzzyCompare != 0) {
                return@Comparator fuzzyCompare
            }
            compareByUsageOrName(first.app, second.app, sortAppsByUsageEnabled)
        }
    }

    private fun compareByUsageOrName(
        first: AppInfo,
        second: AppInfo,
        sortAppsByUsageEnabled: Boolean,
    ): Int =
        if (sortAppsByUsageEnabled) {
            compareValuesBy(
                second,
                first,
                AppInfo::lastUsedTime,
                AppInfo::launchCount,
            ).takeIf { it != 0 }
                ?: first.appName
                    .lowercase(Locale.getDefault())
                    .compareTo(second.appName.lowercase(Locale.getDefault()))
        } else {
            first.appName
                .lowercase(Locale.getDefault())
                .compareTo(second.appName.lowercase(Locale.getDefault()))
        }
}

private fun searchAliasesFor(
    appName: String,
    nickname: String?,
): List<String> =
    buildList {
        addAll(AppSearchInitials.aliasesFor(appName))
        nickname?.let { addAll(AppSearchInitials.aliasesFor(it)) }
    }.distinct()
