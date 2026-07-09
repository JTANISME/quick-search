package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchAlgorithmTest {

    @Test
    fun deterministicMatchesRankBeforeFuzzyOnlyMatches() {
        val exactMatch = app("Settings", "settings", launchCount = 1)
        val fuzzyOnlyMatch = app("Settlings", "settlings", launchCount = 100)

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "settings",
                source = listOf(fuzzyOnlyMatch, exactMatch),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = true,
            )

        assertEquals(listOf(exactMatch, fuzzyOnlyMatch), matches)
    }

    @Test
    fun shortTypoQueriesDoNotReturnNoisyFuzzyMatches() {
        val matches =
            AppSearchAlgorithm.findMatches(
                query = "zz",
                source = listOf(app("Gmail", "gmail")),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun typoQueriesCanFindAppsWhenDeterministicMatchingMisses() {
        val settings = app("Settings", "settings")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "setings",
                source = listOf(settings),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(settings), matches)
    }

    @Test
    fun githubTypoQueriesReturnGithubResult() {
        val github = app("GitHub", "github")

        val deletedCharMatches =
            AppSearchAlgorithm.findMatches(
                query = "Githb",
                source = listOf(github),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )
        val substitutedCharMatches =
            AppSearchAlgorithm.findMatches(
                query = "Githbb",
                source = listOf(github),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(github), deletedCharMatches)
        assertEquals(listOf(github), substitutedCharMatches)
    }

    @Test
    fun punctuationSeparatedAppNameMatchesCompactPrefixQuery() {
        val fdroid = app("F-Droid", "fdroid")

        listOf("fdr", "fdro", "fdroi").forEach { query ->
            val matches =
                AppSearchAlgorithm.findMatches(
                    query = query,
                    source = listOf(fdroid),
                    limit = 10,
                    fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                    appNicknames = emptyMap(),
                    sortAppsByUsageEnabled = false,
                )

            assertEquals("Expected F-Droid to match $query", listOf(fdroid), matches)
        }
    }

    @Test
    fun typoEligibleCandidatesAreNotStarvedByUnrelatedApps() {
        val github = app("GitHub", "github")
        val unrelatedApps =
            (1..1_300).map { index ->
                app("Camera$index", "camera$index", launchCount = 100 + index)
            }

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "githb",
                source = unrelatedApps + github,
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = true,
            )

        assertTrue(matches.contains(github))
    }

    @Test
    fun chineseAppMatchesPinyinFullPrefix() {
        val wechat = app("微信", "wechat")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "wei",
                source = listOf(wechat),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(wechat), matches)
    }

    @Test
    fun chineseAppMatchesPinyinInitials() {
        val wechat = app("微信", "wechat")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "wx",
                source = listOf(wechat),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(wechat), matches)
    }

    @Test
    fun mixedChineseAndLatinAppMatchesPinyinQueries() {
        val qqMusic = app("QQ音乐", "qqmusic")

        val initialsMatches =
            AppSearchAlgorithm.findMatches(
                query = "qqyy",
                source = listOf(qqMusic),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )
        val fullPinyinMatches =
            AppSearchAlgorithm.findMatches(
                query = "yinyue",
                source = listOf(qqMusic),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(qqMusic), initialsMatches)
        assertEquals(listOf(qqMusic), fullPinyinMatches)
    }

    @Test
    fun chineseNicknameAlsoMatchesPinyin() {
        val browser = app("Browser", "browser")
        val nicknames = mapOf(browser.packageName to "浏览器")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "llq",
                source = listOf(browser),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = nicknames,
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(browser), matches)
    }

    private fun app(
        appName: String,
        packageSuffix: String,
        launchCount: Int = 0,
    ): AppInfo =
        AppInfo(
            appName = appName,
            packageName = "com.example.$packageSuffix",
            lastUsedTime = 0L,
            totalTimeInForeground = 0L,
            launchCount = launchCount,
            firstInstallTime = 0L,
            isSystemApp = false,
        )
}
