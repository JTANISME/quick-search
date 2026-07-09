package com.tk.quicksearch.search.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchPinyinTest {
    @Test
    fun formsForChineseAppNameIncludeFullPinyinAndInitials() {
        val forms = AppSearchPinyin.formsFor("微信")

        assertTrue(forms.contains("weixin"))
        assertTrue(forms.contains("wx"))
    }

    @Test
    fun formsForMixedNameIncludeLatinAndChineseParts() {
        val forms = AppSearchPinyin.formsFor("QQ音乐")

        assertTrue(forms.contains("qqyinyue"))
        assertTrue(forms.contains("qqyy"))
    }

    @Test
    fun formsForLatinOnlyNameAreEmpty() {
        assertTrue(AppSearchPinyin.formsFor("GitHub").isEmpty())
    }

    @Test
    fun aliasesForCombinesLatinInitialsAndPinyinForms() {
        val aliases = AppSearchInitials.aliasesFor("QQ音乐")

        assertTrue(aliases.contains("qqyinyue"))
        assertTrue(aliases.contains("qqyy"))
    }

    @Test
    fun browserNicknameProducesExpectedInitials() {
        val aliases = AppSearchInitials.aliasesFor("浏览器")

        assertEquals(2, aliases.size)
        assertTrue(aliases.contains("llq"))
        assertTrue(aliases.contains("liulanqi"))
    }
}
