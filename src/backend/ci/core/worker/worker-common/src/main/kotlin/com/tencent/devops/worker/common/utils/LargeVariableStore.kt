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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.BuildVarOverflowUtils.LargeVarRef
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.variable.LargeVariableSDKApi
import com.tencent.devops.worker.common.api.variable.LargeVariableScope
import com.tencent.devops.worker.common.logger.LoggerService
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * 构建机侧大变量读写：上传插件输出的大变量、按引用串下载真实值。
 *
 * - 多个值并行上传 / 并行下载，并发度 [MAX_PARALLEL]；
 * - 引用串携带内容摘要，本地按摘要做有界 LRU 缓存，同一 Job 内重复引用只下载一次；
 * - 所有方法不抛异常，失败由调用方按"保留引用串 / 丢弃变量"降级。
 */
object LargeVariableStore {

    private val logger = LoggerFactory.getLogger(LargeVariableStore::class.java)

    private const val CACHE_MAX_CHARS = 8L * 1024 * 1024
    private const val MAX_PARALLEL = 8
    private const val UPLOAD_ATTEMPTS = 3
    private const val TASK_TIMEOUT_SECONDS = 300L

    private val api by lazy { ApiFactory.create(LargeVariableSDKApi::class) }

    @Volatile
    private var limitReady = false

    /**
     * 把构建机上的单值上限写进当前进程。读同名环境变量、JVM 参数，再读 `.agent.properties`。
     * 读不到或文件不存在时保持默认值。
     */
    fun ensureLargeValueLimit() {
        if (limitReady) return
        synchronized(this) {
            if (limitReady) return
            val raw = runCatching {
                AgentEnv.getEnvProp(BuildVarOverflowUtils.LARGE_VALUE_MAX_BYTES_CONFIG)
            }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                ?: runCatching {
                    AgentEnv.getProperty(BuildVarOverflowUtils.LARGE_VALUE_MAX_BYTES_CONFIG)
                }.getOrNull()
            val maxBytes = raw?.trim()?.toIntOrNull()?.takeIf { it > 0 }
                ?: PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES
            BuildVarOverflowUtils.applyLargeValueMaxBytes(maxBytes)
            limitReady = true
        }
    }

    private val executor: ExecutorService by lazy {
        Executors.newFixedThreadPool(MAX_PARALLEL) { r ->
            Thread(r, "large-variable").apply { isDaemon = true }
        }
    }

    private val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {}
    private var cachedChars = 0L

    @Volatile
    private var supported: Boolean? = null

    /** 制品库是否支持存放大变量；查询失败视为不支持且下次重新查询。 */
    fun isSupported(): Boolean {
        supported?.let { return it }
        return try {
            api.isSupported().also { supported = it }
        } catch (ignore: Throwable) {
            logger.warn("LARGE_VAR_REALM_FAIL|${ignore.message}")
            false
        }
    }

    /** 当前构建的读写归属；构建上下文未就绪时为 null。 */
    fun currentScope(): LargeVariableScope? {
        val buildVariables = LoggerService.buildVariables ?: return null
        return LargeVariableScope(
            projectId = buildVariables.projectId,
            pipelineId = buildVariables.pipelineId,
            buildId = buildVariables.buildId,
            userId = buildVariables.variables[PIPELINE_START_USER_ID].orEmpty(),
            vmBuildEnv = TaskUtil.isVmBuildEnv(buildVariables.containerType),
            pipelineName = buildVariables.variables[PIPELINE_NAME],
            buildNum = buildVariables.variables[PIPELINE_BUILD_NUM]
        )
    }

    /**
     * 并行上传 [values]（key -> 原值），文件归入执行次数 [executeCount] 的目录。
     * @return key -> 引用串；上传失败的 key 不在结果中
     */
    fun store(scope: LargeVariableScope, executeCount: Int, values: Map<String, String>): Map<String, String> {
        ensureLargeValueLimit()
        if (values.isEmpty()) return emptyMap()
        val count = executeCount.coerceAtLeast(1)
        return runParallel(values.mapValues { (_, value) -> Callable<String?> { uploadOne(scope, count, value) } })
    }

    /** 按引用串加载真实值；不是合法引用或加载失败返回 null。 */
    fun load(scope: LargeVariableScope, reference: String): String? {
        ensureLargeValueLimit()
        val ref = BuildVarOverflowUtils.parseReference(reference) ?: return null
        return load(scope, ref)
    }

    private fun load(scope: LargeVariableScope, ref: LargeVarRef): String? {
        cacheGet(ref.digest)?.let { return it }
        val bytes = try {
            api.download(scope, ref)
        } catch (ignore: Throwable) {
            logger.warn("LARGE_VAR_DOWNLOAD_FAIL|digest=${ref.digest}|${ignore.message}")
            null
        } ?: return null
        return verifyAndCache(ref, bytes)
    }

    /**
     * 并行预取一批引用到本地缓存，随后的 [load] 直接命中缓存。
     *
     * 单个引用无需预取，直接由 [load] 按需加载（两者都是一次下载，预取反而多一次线程切换）；
     * 预取总量不超过缓存容量，超出部分预取了也会被淘汰，留给 [load]。
     *
     * 失败的引用不做处理：[load] 会在真正用到时重试，仍失败再按保留引用串降级。
     */
    fun prefetch(scope: LargeVariableScope, references: Collection<String>) {
        ensureLargeValueLimit()
        var budget = CACHE_MAX_CHARS
        val missing = references.asSequence()
            .mapNotNull { BuildVarOverflowUtils.parseReference(it) }
            .distinctBy { it.digest }
            .filter { cacheGet(it.digest) == null }
            .takeWhile { ref ->
                budget -= ref.length
                budget >= 0
            }
            .toList()
        if (missing.size <= 1) return
        runParallel(missing.associate { ref -> ref.digest to Callable<String?> { load(scope, ref) } })
    }

    private fun uploadOne(scope: LargeVariableScope, executeCount: Int, value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val ref = LargeVarRef(
            length = value.length,
            executeCount = executeCount,
            digest = BuildVarOverflowUtils.sha256Hex(bytes)
        )
        var attempt = 1
        while (true) {
            try {
                api.upload(scope, ref, bytes)
                break
            } catch (e: Throwable) {
                if (attempt >= UPLOAD_ATTEMPTS) throw e
                logger.warn("LARGE_VAR_UPLOAD_RETRY|digest=${ref.digest}|attempt=$attempt|${e.message}")
                attempt++
            }
        }
        cachePut(ref.digest, value)
        return BuildVarOverflowUtils.toReference(ref)
    }

    private fun verifyAndCache(ref: LargeVarRef, bytes: ByteArray): String? {
        val value = BuildVarOverflowUtils.decodeVerified(ref, bytes)
        if (value == null) {
            logger.warn("LARGE_VAR_DIGEST_MISMATCH|digest=${ref.digest}|size=${bytes.size}")
            return null
        }
        cachePut(ref.digest, value)
        return value
    }

    /**
     * 失败 / 超时的任务结果为 null 并被过滤。
     *
     * 单任务也必须走线程池：直接在当前线程 call() 会绕过 [TASK_TIMEOUT_SECONDS]，
     * 制品库卡住时整个插件会被无限期挂起，而"只有一个大变量"恰恰是最常见的场景。
     */
    private fun <K> runParallel(tasks: Map<K, Callable<String?>>): Map<K, String> {
        if (tasks.isEmpty()) return emptyMap()
        val futures = tasks.mapValues { (_, task) -> executor.submit(task) }
        val result = LinkedHashMap<K, String>(tasks.size)
        futures.forEach { (key, future) ->
            runCatching { future.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
                .onFailure {
                    future.cancel(true)
                    logger.warn("LARGE_VAR_TASK_FAIL|key=$key|${it.message}")
                }
                .getOrNull()?.let { result[key] = it }
        }
        return result
    }

    @Synchronized
    private fun cacheGet(digest: String): String? = cache[digest]

    @Synchronized
    private fun cachePut(digest: String, value: String) {
        if (value.length > CACHE_MAX_CHARS || cache.containsKey(digest)) return
        cache[digest] = value
        cachedChars += value.length
        val iterator = cache.entries.iterator()
        while (cachedChars > CACHE_MAX_CHARS && iterator.hasNext()) {
            cachedChars -= iterator.next().value.length
            iterator.remove()
        }
    }
}
