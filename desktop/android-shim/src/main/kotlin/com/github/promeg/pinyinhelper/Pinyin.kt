package com.github.promeg.pinyinhelper

// Desktop stand-in for TinyPinyin (its Android build does not resolve on desktop).
// shortcut: returns the character unchanged, so Chinese lyrics are not romanised yet; swap in a JVM pinyin library later.
object Pinyin {
    fun toPinyin(c: Char): String = c.toString()
}
