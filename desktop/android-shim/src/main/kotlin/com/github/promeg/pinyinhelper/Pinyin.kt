package com.github.promeg.pinyinhelper

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

// Desktop replacement for TinyPinyin (its Android build does not resolve on desktop), backed by pinyin4j.
// Same contract as the original call: a Chinese character gives its pinyin without tones, anything else comes back unchanged.
// shortcut: characters with several readings use pinyin4j's first reading, as TinyPinyin also picks one without context.
object Pinyin {
    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.UPPERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    fun toPinyin(c: Char): String =
        runCatching { PinyinHelper.toHanyuPinyinStringArray(c, format)?.firstOrNull() }.getOrNull() ?: c.toString()
}
