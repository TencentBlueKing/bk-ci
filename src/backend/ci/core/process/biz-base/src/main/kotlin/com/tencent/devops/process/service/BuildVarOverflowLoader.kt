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

package com.tencent.devops.process.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory

/**
 * 服务端大变量按需加载器（**会话级**：与一次 API 调用 / 一次表达式求值同寿命，不要长期持有）。
 *
 *  - 按引用串中的内容摘要从制品库下载，并以摘要为键做 Caffeine 字符加权缓存，同值只下载一次；
 *  - 会话级总加载预算：按引用串声明的长度在**下载前**预扣，越界抛
 *    [BuildVarOverflowBudgetExceededException]，把潜在 OOM 转为明确的用法错误；
 *  - 历史构建的变量都 ≤ 4K，不会触发本加载器。
 */
class BuildVarOverflowLoader(
    private val storage: BuildVarOverflowStorage,
    private val userId: String?,
    private val projectId: String,
    private val pipelineId: String,
    private val buildId: String,
    private val maxCacheBytes: Long = PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX.toLong(),
    private val maxBudgetBytes: Long = PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX.toLong()
) {

    private val cache: Cache<String, String> = Caffeine.newBuilder()
        .maximumWeight(maxCacheBytes)
        .weigher<String, String> { _, value -> value.length }
        .build()

    private val loadedBytes = AtomicLong(0L)

    /**
     * 按引用串加载真实值；非引用串 / 无法解析 / 下载失败返回 null，由调用方决定是否回退为引用串。
     */
    @Throws(BuildVarOverflowBudgetExceededException::class)
    fun load(reference: String): String? {
        val ref = BuildVarOverflowUtils.parseReference(reference) ?: return null
        cache.getIfPresent(ref.digest)?.let { return it }

        val len = ref.length.toLong()
        val nextLoaded = loadedBytes.addAndGet(len)
        if (nextLoaded > maxBudgetBytes) {
            loadedBytes.addAndGet(-len)
            LOG.warn("$buildId|VAR_OVERFLOW_BUDGET_EXCEEDED|len=$len|loadedAfter=$nextLoaded|budget=$maxBudgetBytes")
            throw BuildVarOverflowBudgetExceededException(
                buildId = buildId,
                key = reference,
                triedLength = ref.length,
                budget = maxBudgetBytes
            )
        }
        val value = storage.download(userId, projectId, pipelineId, buildId, reference)
        if (value == null) {
            loadedBytes.addAndGet(-len)
            return null
        }
        if (len <= maxCacheBytes) {
            cache.put(ref.digest, value)
        }
        return value
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildVarOverflowLoader::class.java)
    }
}

/**
 * 单次会话内累计加载的大变量字符数超过预算时抛出：用户一次性引用了过多大变量。
 */
class BuildVarOverflowBudgetExceededException(
    val buildId: String,
    val key: String,
    val triedLength: Int,
    val budget: Long
) : RuntimeException(
    "Build[$buildId] tried to lazy-load variable[$key] of length $triedLength," +
        " but the per-session large variable budget ($budget chars) is exhausted." +
        " Reduce the number of large variables referenced in a single expression evaluation," +
        " or raise the limit via `pipeline.variables.lazyLoad.budgetMax`."
)
