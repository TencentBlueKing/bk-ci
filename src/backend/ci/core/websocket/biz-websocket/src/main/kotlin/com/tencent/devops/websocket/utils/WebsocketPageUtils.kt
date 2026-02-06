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

package com.tencent.devops.websocket.utils

/**
 * 将前端/推送传入的页面 URL 规范化为 WebSocket 注册与推送使用的 key。
 * 约定：process 侧 buildPage() 产出的 key 须与本方法对“同一逻辑页”的规范化结果一致，
 * 否则 changePage 注册的 key 与 push 查找的 key 不一致，会导致收不到推送。
 */
object WebsocketPageUtils {

    private const val CREATIVE_STREAM_PREFIX = "/console/creative-stream"

    class PathNormalizer {
        companion object {
            // 顺序敏感：更具体的规则（如 creative-stream）放前面，避免被通用规则误截断
            private val NORMALIZATION_RULES = listOf(
                // creative-stream：列表详情页 .../detail/1/execution-record 只保留 .../detail 作为注册 key
                NormalizationRule(
                    pattern = Regex("""^(.*/flow/[^/]+/detail)/[^/?#]+/execution-record.*$"""),
                    transform = { it.groupValues[1] },
                    condition = { p -> p.startsWith(CREATIVE_STREAM_PREFIX) }
                ),
                // pipeline：history 相关
                NormalizationRule(
                    pattern = Regex("""^(.+?)/history/history/(.+)$"""),
                    transform = { match -> "${match.groupValues[1]}/history" },
                    condition = { !it.endsWith("/history") }
                ),
                NormalizationRule(
                    pattern = Regex("""^(.+?)/draftDebug/(.+)$"""),
                    transform = { match -> "${match.groupValues[1]}/history" }
                ),
                // 通用 list：.../list/xxx 或 .../list?xxx 只保留 .../list（含 creative-stream list）
                NormalizationRule(
                    pattern = Regex("""^(.+?)/list/(.+)$"""),
                    transform = { match -> "${match.groupValues[1]}/list" },
                    condition = { !it.endsWith("/list") }
                ),
                // 其他：如 .../detail/xxx/execute-detail（非 creative-stream 的 execute-detail 形式）
                NormalizationRule(
                    pattern = Regex("""^(.+?)/detail/(.+)/execute-detail$"""),
                    transform = { match -> "${match.groupValues[1]}/detail" }
                )
            )
        }

        fun normalize(page: String): String {
            for (rule in NORMALIZATION_RULES) {
                val match = rule.pattern.matchEntire(page)
                if (match != null && rule.condition?.invoke(page) != false) {
                    return rule.transform(match)
                }
            }
            return page
        }

        private data class NormalizationRule(
            val pattern: Regex,
            val transform: (MatchResult) -> String,
            val condition: ((String) -> Boolean)? = null
        )
    }

    fun buildNormalPage(page: String): String {
        return PathNormalizer().normalize(page)
    }
}
