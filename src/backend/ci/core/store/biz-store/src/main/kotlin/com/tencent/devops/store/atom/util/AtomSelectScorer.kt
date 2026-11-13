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
        val matched: List<String>
    )

    fun parseKeywords(raw: String?): List<String> {
        return raw?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
    }

    /**
     * 拆分调用方传入的检索词：英文逗号分隔，去空白、去重，过短的丢弃，最多保留 [MAX_QUERIES] 个
     */
    fun parseQueries(raw: String?): List<String> {
        return raw?.split(",")
            ?.map { it.trim() }
            ?.filter { it.length >= MIN_QUERY_LENGTH }
            ?.distinctBy { it.lowercase() }
            ?.take(MAX_QUERIES)
            ?: emptyList()
    }

    fun score(entry: Entry, queries: List<String>): Hit {
        var total = 0
        val matched = mutableListOf<String>()
        queries.forEach { query ->
            val value = queryScore(entry, query.lowercase())
            if (value > 0) {
                total += value
                matched.add(query)
            }
        }
        return Hit(atomCode = entry.atomCode, hitCount = matched.size, score = total, matched = matched)
    }

    /**
     * 对全部契约打分，按命中词数、得分降序返回命中的条目
     */
    fun recall(entries: Collection<Entry>, queries: List<String>, maxSize: Int): List<Hit> {
        return entries.asSequence()
            .map { score(it, queries) }
            .filter { it.hitCount > 0 }
            .sortedWith(compareByDescending<Hit> { it.hitCount }.thenByDescending { it.score })
            .take(maxSize)
            .toList()
    }

    private fun queryScore(entry: Entry, query: String): Int {
        var best = 0
        for (keyword in entry.keywords) {
            if (keyword == query) {
                return STRONG_SCORE
            }
            if (keyword.contains(query) || (keyword.length >= MIN_QUERY_LENGTH && query.contains(keyword))) {
                best = MEDIUM_SCORE
            }
        }
        if (best == 0 && entry.lowerSummary?.contains(query) == true) {
            best = WEAK_SCORE
        }
        return best
    }
}
