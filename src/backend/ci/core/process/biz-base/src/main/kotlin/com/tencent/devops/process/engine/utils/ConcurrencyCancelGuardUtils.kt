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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.process.engine.pojo.ConcurrencyGroupBuild
import org.slf4j.LoggerFactory

/**
 * 并发组 cancel-in-progress 的当前构建上下文。
 *
 * @param retryBuildNum 仅重试时有值。为空表示新触发，只排除自己、不比较构建号。
 */
data class ConcurrencyCancelContext(
    val pipelineId: String,
    val buildId: String,
    val retryBuildNum: Int? = null
) {
    companion object {
        /**
         * [currentBuildNum] 只在 [isRetry] 为 true 时求值，避免新触发多一次查库。
         */
        fun of(
            pipelineId: String,
            buildId: String,
            isRetry: Boolean,
            currentBuildNum: () -> Int?
        ) = ConcurrencyCancelContext(
            pipelineId = pipelineId,
            buildId = buildId,
            retryBuildNum = if (isRetry) currentBuildNum() else null
        )
    }
}

/**
 * 并发组 cancel-in-progress 取消目标过滤，只做内存判定。
 *
 * #13450 重试会复用当前 buildId，查询未完成构建时可能把"自己"查出来误杀；
 * 重试旧构建时也不能把同流水线中构建号更大的"最新构建"取消掉。
 * 新触发（非重试）仍取消组内其他未完成构建，保持历史逻辑。
 *
 */
object ConcurrencyCancelGuardUtils {

    private val logger = LoggerFactory.getLogger(ConcurrencyCancelGuardUtils::class.java)

    /**
     * 从候选列表中去掉不应取消的构建，返回真正要 cancel 的目标。
     * @param candidateBuilds 候选待取消的构建集合
     * @param currentContext 当前触发/重试构建的上下文，包含构建号（仅重试时有值）等信息
     * @return 过滤掉「不应取消」的构建后，最终需要执行 cancel 的目标列表
     */
    fun filterTargets(
        candidateBuilds: Collection<ConcurrencyGroupBuild>,
        currentContext: ConcurrencyCancelContext
    ): List<ConcurrencyGroupBuild> {
        return candidateBuilds.filterNot { candidate ->
            val skip = shouldSkip(currentContext, candidate)
            if (skip) {
                logger.info(
                    "[${candidate.pipelineId}]|[${candidate.buildId}]|skip concurrency cancel|" +
                        "current=${currentContext.buildId}|retryBuildNum=${currentContext.retryBuildNum}"
                )
            }
            skip
        }
    }

    /**
     * 判断是否应跳过取消。
     * @param currentContext 当前触发/重试构建的上下文，包含构建号（仅重试时有值）等信息
     * @param candidate 候选待取消的构建
     * @return true 表示跳过取消（保留该构建），false 表示可以取消。
     */
    private fun shouldSkip(
        currentContext: ConcurrencyCancelContext,
        candidate: ConcurrencyGroupBuild
    ): Boolean {
        // 重试复用同一 buildId，绝不能把当前这次启动/重试取消掉
        if (candidate.buildId == currentContext.buildId) {
            return true
        }
        // 新触发：retryBuildNum 为空，组内其他未完成构建仍按历史逻辑取消
        val currentNum = currentContext.retryBuildNum ?: return false
        // 跨流水线并发组的 buildNum 彼此不可比，保持原有取消行为
        if (candidate.pipelineId != currentContext.pipelineId) {
            return false
        }
        // 同流水线重试：构建号更大的是更新的那次，不能误杀
        return candidate.buildNum > currentNum
    }
}
