/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.util

import java.nio.charset.Charset
import java.text.Collator

/**
 * 按名称首字母排序：英文取首字母，中文取拼音首字母，再交错到 a-z。
 * 例如 啊 与 a 同组，吧 与 b 同组。
 */
object ProjectNamePinyinHelper {

    private val gb2312 = Charset.forName("GB2312")

    // GB2312 区位码区间对应拼音首字母，不含 i/u/v（普通话声母没有这三项）
    private val gbRanges = intArrayOf(
        1601, 1637, 1833, 2078, 2274, 2302, 2433, 2594, 2787,
        3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027, 4086,
        4390, 4558, 4684, 4925, 5249, 5590
    )
    private val initials = charArrayOf(
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
        't', 'w', 'x', 'y', 'z'
    )

    fun compare(left: String, right: String, collator: Collator): Int {
        val keyCompare = sortKey(left).compareTo(sortKey(right))
        if (keyCompare != 0) {
            return keyCompare
        }
        return collator.compare(left, right)
    }

    fun firstLetter(name: String): Char? {
        val ch = firstSignificantChar(name) ?: return null
        if (ch in 'a'..'z' || ch in 'A'..'Z') {
            return ch.lowercaseChar()
        }
        return pinyinInitial(ch)
    }

    private fun sortKey(name: String): SortKey {
        val ch = firstSignificantChar(name) ?: return SortKey(isAlpha = false, letter = Char.MIN_VALUE)
        val letter = firstLetter(name)
        return if (letter != null) {
            SortKey(isAlpha = true, letter = letter)
        } else {
            SortKey(isAlpha = false, letter = ch.lowercaseChar())
        }
    }

    private fun firstSignificantChar(name: String): Char? {
        return name.firstOrNull { !it.isWhitespace() }
    }

    private fun pinyinInitial(ch: Char): Char? {
        val bytes = ch.toString().toByteArray(gb2312)
        if (bytes.size < 2) {
            return null
        }
        val sector = ((bytes[0].toInt() and 0xFF) - 160) * 100 +
            ((bytes[1].toInt() and 0xFF) - 160)
        if (sector < gbRanges.first()) {
            return null
        }
        for (index in gbRanges.indices.reversed()) {
            if (sector >= gbRanges[index]) {
                return initials[index]
            }
        }
        return null
    }

    private data class SortKey(
        val isAlpha: Boolean,
        val letter: Char
    ) : Comparable<SortKey> {
        override fun compareTo(other: SortKey): Int {
            if (isAlpha != other.isAlpha) {
                return if (isAlpha) 1 else -1
            }
            return letter.compareTo(other.letter)
        }
    }
}
