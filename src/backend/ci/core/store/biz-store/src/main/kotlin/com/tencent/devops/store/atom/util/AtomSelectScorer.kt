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

package com.tencent.devops.store.atom.util

/**
 * 插件选型检索的召回与打分，只依赖契约选型字段，不访问数据库
 */
object AtomSelectScorer {

    const val MAX_QUERIES = 8
    private const val MIN_QUERY_LENGTH = 2
    private const val STRONG_SCORE = 3
    private const val MEDIUM_SCORE = 2
    private const val WEAK_SCORE = 1
    private const val SHORT_WORD_MAX_LENGTH = 3
    private val SEGMENT_SEPARATOR = Regex("[,，、]")
    private val WHITESPACE = Regex("\\s+")

    data class Entry(
        val atomCode: String,
        val summary: String?,
        // 已转小写、去空白
        val keywords: List<String>,
        val coverage: String?,
        val stale: Boolean
    ) {
        val lowerSummary: String? = summary?.lowercase()
    }

    data class Hit(
        val atomCode: String,
        val hitCount: Int,
        val score: Int,
        val matched: List<String>,
        // 至少一个检索词命中关键词（中档及以上），只命中简介时为 false
        val keywordHit: Boolean
    )

    fun parseKeywords(raw: String?): List<String> {
        return raw?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
    }

    /**
     * 拆分调用方传入的检索词：中英文逗号、顿号和空白都算分隔，去重，过短的丢弃，最多保留 [MAX_QUERIES] 个
     */
    fun parseQueries(raw: String?): List<String> {
        return parseSegments(raw)
            .flatMap { it.split(WHITESPACE) }
            .filter { it.length >= MIN_QUERY_LENGTH }
            .distinctBy { it.lowercase() }
            .take(MAX_QUERIES)
    }

    /**
     * 只按逗号、顿号拆分，保留词内空白，用于匹配带空格的插件名
     */
    fun parseSegments(raw: String?): List<String> {
        return raw?.split(SEGMENT_SEPARATOR)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    fun normalizeName(name: String): String = name.replace(WHITESPACE, "").lowercase()

    /**
     * 检索词与插件名去空白后完全相同（忽略大小写）的插件代码
     */
    fun matchNames(terms: Collection<String>, codesByName: Map<String, List<String>>): List<String> {
        return terms.flatMap { codesByName[normalizeName(it)] ?: emptyList() }.distinct()
    }

    fun score(entry: Entry, queries: List<String>): Hit {
        var total = 0
        var keywordHit = false
        val matched = mutableListOf<String>()
        queries.forEach { query ->
            val value = queryScore(entry, query.lowercase())
            if (value > 0) {
                total += value
                matched.add(query)
            }
            if (value >= MEDIUM_SCORE) {
                keywordHit = true
            }
        }
        return Hit(
            atomCode = entry.atomCode,
            hitCount = matched.size,
            score = total,
            matched = matched,
            keywordHit = keywordHit
        )
    }

    /**
     * 对全部契约打分，按关键词命中、命中词数、得分降序返回命中的条目
     */
    fun recall(entries: Collection<Entry>, queries: List<String>, maxSize: Int): List<Hit> {
        return entries.asSequence()
            .map { score(it, queries) }
            .filter { it.hitCount > 0 }
            .sortedWith(
                compareByDescending<Hit> { it.keywordHit }
                    .thenByDescending { it.hitCount }
                    .thenByDescending { it.score }
            )
            .take(maxSize)
            .toList()
    }

    private fun queryScore(entry: Entry, query: String): Int {
        var best = 0
        for (keyword in entry.keywords) {
            if (keyword == query) {
                return STRONG_SCORE
            }
            if (contains(keyword, query) || (keyword.length >= MIN_QUERY_LENGTH && contains(query, keyword))) {
                best = MEDIUM_SCORE
            }
        }
        if (best == 0 && entry.lowerSummary?.let { contains(it, query) } == true) {
            best = WEAK_SCORE
        }
        return best
    }

    /**
     * 短英文词按整词匹配，避免 ai 命中 rainbow、container 这类单词内部
     */
    private fun contains(text: String, part: String): Boolean {
        if (part.length > SHORT_WORD_MAX_LENGTH || !part.all { it.isAsciiLetterOrDigit() }) {
            return text.contains(part)
        }
        var index = text.indexOf(part)
        while (index >= 0) {
            val before = text.getOrNull(index - 1)
            val after = text.getOrNull(index + part.length)
            if (before?.isAsciiLetterOrDigit() != true && after?.isAsciiLetterOrDigit() != true) {
                return true
            }
            index = text.indexOf(part, index + 1)
        }
        return false
    }

    private fun Char.isAsciiLetterOrDigit() = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'
}
