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
import com.tencent.devops.process.engine.dao.PipelineBuildVarOverflowDao
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * 大变量按需加载器（**会话级**：随 [com.tencent.devops.process.pojo.BuildVariableSnapshot]
 * 一起创建，与流水线引擎一次"表达式求值会话"同寿命）。
 *
 * ## 与历史业务的兼容性
 *  - T_PIPELINE_BUILD_VAR.VALUE 是 varchar(4000)，**历史 build 的单变量最多 4K**，
 *    全部走 [com.tencent.devops.process.pojo.BuildVariableSnapshot].smallVars 直读路径，
 *    **根本不会触发 [load]**——所以这里的 [maxCacheBytes] / [maxBudgetBytes]
 *    对历史业务**完全无感**；
 *  - 只有"启用大变量后的新 build"才会走 loader 路径。
 *
 * ## 设计动机
 *  - 现网 process / engine 服务每个 Pod 16G 内存，10 Pod 部署。
 *  - 单变量硬上限 4M；若裸调用 [PipelineBuildVarOverflowDao.getValue]，
 *    一次表达式中多次访问同一大变量会产生 N+1 查询；
 *    异常/恶意脚本反复展开多个 4M 变量也容易触发 OOM。
 *
 * ## 实现要点
 *  - **本地缓存统一使用 [Caffeine]**：按字符数加权（weight = string.length），
 *    超过 [maxCacheBytes] 时 Caffeine 自动按 W-TinyLFU 算法淘汰冷数据；
 *  - **会话级总加载预算（硬上限）**：超过 [maxBudgetBytes] 时再次加载抛
 *    [BuildVarOverflowBudgetExceededException]，把潜在 OOM 转为"用户用法异常"。
 *    阈值可由 [com.tencent.devops.process.service.BuildVariableService] 注入
 *    （`pipeline.variables.lazyLoad.budgetMax` Spring 配置覆盖默认 32M）；
 *  - **单值过大不进缓存**：长度超过 [maxCacheBytes] 的单值跳过缓存写入，
 *    防止 Caffeine 接受后立即驱逐造成短暂"独占缓存"瞬时尖峰；
 *  - **线程安全**：Caffeine 内部保证；预算计数使用 [AtomicLong] 原子操作。
 *
 * ## 生命周期
 *  - 实例**不能**长期持有，正常生命周期 ≤ 一次 API/任务的表达式求值；
 *  - 实例被回收后，Caffeine 缓存随之随 GC 释放。
 */
class BuildVarOverflowLoader(
    private val overflowDao: PipelineBuildVarOverflowDao,
    private val dslContext: DSLContext,
    private val projectId: String,
    private val buildId: String,
    private val maxCacheBytes: Long = PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX.toLong(),
    private val maxBudgetBytes: Long = PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX.toLong()
) {

    private val cache: Cache<String, String> = Caffeine.newBuilder()
        .maximumWeight(maxCacheBytes)
        .weigher<String, String> { _, value -> value.length }
        .build()

    /** 会话内累计从 DB 加载到 JVM 的字符数（不论是否进缓存）。 */
    private val loadedBytes = AtomicLong(0L)

    /**
     * 加载指定 key 的真实值。
     *  - 命中缓存：直接返回；
     *  - 未命中：查询溢出表，必要时进入缓存；
     *  - 加上本次后累计 byte 超过 [maxBudgetBytes]：抛 [BuildVarOverflowBudgetExceededException]，
     *    上层流水线引擎应将其作为"用户脚本一次性引用了过多大变量"的明确错误反馈给用户。
     */
    @Throws(BuildVarOverflowBudgetExceededException::class)
    fun load(key: String): String? {
        val cached = cache.getIfPresent(key)
        if (cached != null) return cached

        val value = overflowDao.getValue(dslContext, projectId, buildId, key) ?: return null

        val len = value.length
        // 原子性预扣预算：先 add，越界则立即 revert，保证并发场景下严格不超阈值。
        val nextLoaded = loadedBytes.addAndGet(len.toLong())
        if (nextLoaded > maxBudgetBytes) {
            loadedBytes.addAndGet(-len.toLong())
            LOG.warn(
                "$buildId|VAR_OVERFLOW_BUDGET_EXCEEDED|key=$key|len=$len|" +
                    "loadedAfter=$nextLoaded|budget=$maxBudgetBytes"
            )
            throw BuildVarOverflowBudgetExceededException(
                buildId = buildId,
                key = key,
                triedLength = len,
                budget = maxBudgetBytes
            )
        }

        // 单值大于缓存总容量时跳过 put，避免 Caffeine 接受后立即驱逐造成短暂"独占缓存"。
        if (len <= maxCacheBytes) {
            cache.put(key, value)
        }
        return value
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildVarOverflowLoader::class.java)
    }
}

/**
 * 单次会话内累计加载的大变量字符数超过预算时抛出。
 *
 * 语义定位：**用户脚本一次性引用了过多大变量**，可能是误用 / 死循环。
 * 上层（流水线引擎、API 调用方）应将此异常以明确的错误信息回显给用户，
 * 并建议拆分大变量或减少同次表达式中的引用数量。
 *
 * 对历史 build **完全不会触发**：历史变量最大 4K，根本走不到 loader 路径。
 */
class BuildVarOverflowBudgetExceededException(
    val buildId: String,
    val key: String,
    val triedLength: Int,
    val budget: Long
) : RuntimeException(
    "Build[$buildId] tried to lazy-load variable[$key] of length $triedLength," +
        " but the per-session overflow budget ($budget chars) is exhausted." +
        " Reduce the number of large variables referenced in a single expression evaluation," +
        " or raise the limit via `pipeline.variables.lazyLoad.budgetMax`."
)
