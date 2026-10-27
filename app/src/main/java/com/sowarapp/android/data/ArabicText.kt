package com.sowarapp.android.data

/**
 * Makes Arabic search forgiving: "جمعه" finds "جمعة", "اصنع" finds "إصنع",
 * and diacritics (تشكيل) are ignored.
 */
object ArabicText {

    private val diacritics = Regex("[\\u064B-\\u0652\\u0670\\u0640]") // tashkeel + tatweel

    fun normalize(text: String): String =
        text.trim()
            .replace(diacritics, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .lowercase()
}
