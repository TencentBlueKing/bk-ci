/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
 *
 */

package com.tencent.devops.metrics.service.builds

import com.google.common.collect.MapMaker
import com.google.common.hash.Hashing
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.pojo.po.MetricsUserPO
import groovy.util.ObservableMap
import java.beans.PropertyChangeEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class MetricsCacheService @Autowired constructor(
    private val metricsHeartBeatService: MetricsHeartBeatService,
    @Qualifier("redisStringHashOperation")
    private val redisHashOperation: RedisOperation
) {

    private val cache = ObservableMap(
        MapMaker()
            .concurrencyLevel(10)
            .makeMap<String, MetricsUserPO>()
    )

    lateinit var addFunction: (key: String, value: MetricsUserPO) -> Unit

    lateinit var removeFunction: (key: String, value: MetricsUserPO) -> Unit

    lateinit var updateFunction: (key: String, oldValue: MetricsUserPO, newValue: MetricsUserPO) -> Unit

    companion object {
        fun podKey(key: String) = "build_metrics:pod:$key"
        fun updateKey() = "build_metrics:update"
        val logger: Logger = LoggerFactory.getLogger(MetricsCacheService::class.java)
    }

    fun init() {
        cache.addPropertyChangeListener { change: PropertyChangeEvent ->
            when {
                change is ObservableMap.PropertyAddedEvent && this::addFunction.isInitialized -> {
                    addFunction(change.propertyName, change.newValue as MetricsUserPO)
                }

                change is ObservableMap.PropertyUpdatedEvent && this::updateFunction.isInitialized -> {
                    updateFunction(
                        change.propertyName,
                        change.oldValue as MetricsUserPO,
                        change.newValue as MetricsUserPO
                    )
                }

                change is ObservableMap.PropertyRemovedEvent && this::removeFunction.isInitialized -> {
                    removeFunction(change.propertyName, change.oldValue as MetricsUserPO)
                }
            }
        }
        Thread(CacheUpdateProcess(metricsHeartBeatService.getPodName(), cache, redisHashOperation)).start()
        metricsHeartBeatService.init()
    }

    fun removeCache(key: String) {
        cache.remove(key)
        redisHashOperation.hdelete(podKey(metricsHeartBeatService.getPodName()), key)
    }

    fun buildCacheStart(
        buildId: String,
        executeCount: Int,
        data: MetricsUserPO
    ): String {
        val key = hash("$buildId-$executeCount")
        redisHashOperation.hset(podKey(metricsHeartBeatService.getPodName()), key, data.toString())
        cache[key] = data
        return key
    }

    fun buildCacheEnd(
        buildId: String,
        executeCount: Int,
        data: MetricsUserPO
    ): String {
        val key = hash("$buildId-$executeCount")
        cacheEnd(key, data)
        return key
    }

    fun jobCacheStart(
        buildId: String,
        jobId: String,
        executeCount: Int,
        data: MetricsUserPO
    ): String {
        val key = hash("$buildId-$jobId-$executeCount")
        redisHashOperation.hset(podKey(metricsHeartBeatService.getPodName()), key, data.toString())
        cache[key] = data
        return key
    }

    fun jobCacheEnd(
        buildId: String,
        jobId: String,
        executeCount: Int,
        data: MetricsUserPO
    ): String {
        val key = hash("$buildId-$jobId-$executeCount")
        cacheEnd(key, data)
        return key
    }

    fun stepCacheStart(
        buildId: String,
        stepId: String,
        executeCount: Int,
        data: MetricsUserPO
    ): String {
        val key = hash("$buildId-$stepId-$executeCount")
        redisHashOperation.hset(podKey(metricsHeartBeatService.getPodName()), key, data.toString())
        cache[key] = data
        return key
    }

    fun stepCacheEnd(
        buildId: String,
        stepId: String,
        executeCount: Int,
        data: MetricsUserPO
    ): String {
        val key = hash("$buildId-$stepId-$executeCount")
        cacheEnd(key, data)
        return key
    }

    private fun hash(input: String): String {
        return Hashing.murmur3_128().hashBytes(input.toByteArray()).toString()
    }

    private fun cacheEnd(key: String, data: MetricsUserPO) {
        val cacheKey = cache[key] as MetricsUserPO?
        if (cacheKey != null) {
            cache[key] = data.apply { startTime = cacheKey.startTime }
            redisHashOperation.hset(podKey(metricsHeartBeatService.getPodName()), key, data.toString())
        } else {
            redisHashOperation.hset(updateKey(), key, data.toString())
        }
    }

    private class CacheUpdateProcess(
        private val podHashKey: String,
        private val cache: ObservableMap,
        private val redisOperation: RedisOperation
    ) : Runnable {

        companion object {
            const val SLEEP = 5000L
        }

        override fun run() {
            logger.info("CacheUpdateProcess begin")
            while (true) {
                kotlin.runCatching { executeUpdate() }.onFailure {
                    logger.error("metrics execute update process find a error|${it.message}", it)
                }
                kotlin.runCatching { executeAdd() }.onFailure {
                    logger.error("metrics execute add process find a error|${it.message}", it)
                }
                Thread.sleep(SLEEP)
            }
        }

        private fun executeUpdate() {
            val update = redisOperation.hkeys(updateKey()) ?: return
            val snapshot = cache.keys.toList()
            update.parallelStream().forEach { ready ->
                if (ready in snapshot) {
                    // 如果key存在，说明状态维护在当前实例
                    val load = MetricsUserPO.load(redisOperation.hget(updateKey(), ready)) ?: return@forEach
                    redisOperation.hdelete(updateKey(), ready)
                    cache[ready] = load.apply { startTime = (cache[ready] as MetricsUserPO?)?.startTime ?: startTime }
                }
            }
        }

        private fun executeAdd() {
            val add = redisOperation.hkeys(podKey(podHashKey)) ?: return
            val snapshot = cache.keys.toList()
            logger.info("executeAdd local size=${snapshot.size}|${add.size}")
            add.parallelStream().forEach { ready ->
                if (ready !in snapshot) {
                    // 如果key不存在，说明是需要新增的
                    val load = MetricsUserPO.load(redisOperation.hget(podKey(podHashKey), ready)) ?: return@forEach
                    cache[ready] = load
                }
            }
            snapshot.parallelStream().forEach { already ->
                if (already !in add) {
                    cache.remove(already)
                }
            }
        }
    }
}
