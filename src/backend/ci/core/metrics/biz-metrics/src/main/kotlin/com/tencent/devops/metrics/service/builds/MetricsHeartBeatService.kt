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

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.pojo.po.MetricsUserPO
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class MetricsHeartBeatService @Autowired constructor(
    @Qualifier("redisStringHashOperation")
    private val redisHashOperation: RedisOperation
) {
    private val podName: String = System.getenv("POD_NAME") ?: UUID.randomUUID().toString()

    companion object {
        private fun heartBeatKey() = "build_metrics:heart_beat"
        private val logger = LoggerFactory.getLogger(MetricsHeartBeatService::class.java)
    }

    fun init() {
        Thread(HeartBeatProcess(podName, redisHashOperation)).start()
        Thread(HeartBeatManagerProcess(redisHashOperation)).start()
    }

    fun getPodName(): String = podName

    private class HeartBeatProcess(
        private val podHashKey: String,
        private val redisOperation: RedisOperation
    ) : Runnable {

        companion object {
            private const val EXPIRED_SECOND = 60L
            const val SLEEP = 5000L
        }

        override fun run() {
            logger.info("HeartBeatProcess run")
            while (true) {
                kotlin.runCatching { execute() }.onFailure {
                    logger.error("metrics heart beat error|${it.message}", it)
                }
                Thread.sleep(SLEEP)
            }
        }

        private fun execute() {
            redisOperation.hset(
                heartBeatKey(),
                podHashKey,
                LocalDateTime.now().toInstant(ZoneOffset.ofHours(8)).epochSecond.toString()
            )
        }
    }

    private class HeartBeatManagerProcess(
        private val redisOperation: RedisOperation
    ) : Runnable {

        companion object {
            private const val REDIS_LOCK_KEY = "metrics_heart_beat_manager_process_redis_lock"
            const val SLEEP = 15000L
            const val CHUNKED = 100
        }

        override fun run() {
            logger.info("HeartBeatManagerProcess begin")
            while (true) {
                val redisLock = RedisLock(redisOperation, REDIS_LOCK_KEY, 60L)
                try {
                    val lockSuccess = redisLock.tryLock()
                    if (lockSuccess) {
                        logger.info("HeartBeatManagerProcess get lock.")
                        heartBeatCheck()
                        invalidUpdateCheck()
                    }
                } catch (e: Throwable) {
                    logger.error("HeartBeatManagerProcess failed ${e.message}", e)
                } finally {
                    Thread.sleep(SLEEP)
                    redisLock.unlock()
                }
            }
        }

        private fun invalidUpdateCheck() {
            val updateKey = redisOperation.hkeys(MetricsCacheService.updateKey())?.ifEmpty { null } ?: return
            val limit = LocalDateTime.now().plusHours(-1)
            val needDelete = mutableListOf<String>()
            updateKey.chunked(CHUNKED).forEach { keys ->
                val updateValues = redisOperation.hmGet(MetricsCacheService.updateKey(), keys)
                    ?: return@forEach
                keys.forEachIndexed { index, key ->
                    val load = MetricsUserPO.load(updateValues[index]) ?: return@forEachIndexed
                    if (load.endTime!! < limit) {
                        needDelete.add(key)
                    }
                }
            }
            if (needDelete.isNotEmpty()) {
                redisOperation.hdelete(MetricsCacheService.updateKey(), needDelete.toTypedArray())
            }
        }

        private fun heartBeatCheck() {
            val heartBeats = redisOperation.hentries(heartBeatKey()) ?: return
            val limit = LocalDateTime.now().plusMinutes(-1).toInstant(ZoneOffset.ofHours(8)).epochSecond
            val lose = mutableListOf<String>()
            val live = mutableListOf<String>()
            heartBeats.toList().parallelStream().forEach { (podName, lastOnlineTime) ->
                if (NumberUtils.toLong(lastOnlineTime) < limit) {
                    lose.add(podName)
                } else {
                    live.add(podName)
                }
            }
            logger.info("heartBeatCheck start check lose=$lose|live=$live")
            if (live.isEmpty()) {
                return
            }
            lose.forEach { losePod ->
                if (afterLosePod(losePod, live)) {
                    redisOperation.hdelete(heartBeatKey(), losePod)
                    redisOperation.delete(MetricsCacheService.podKey(losePod))
                }
            }
        }

        private fun afterLosePod(losePod: String, live: List<String>): Boolean {
            val losePodKeys = redisOperation.hkeys(MetricsCacheService.podKey(losePod))?.ifEmpty { null } ?: return true
            // 分块处理，优化性能。
            losePodKeys.chunked(CHUNKED).forEachIndexed { index, keys ->
                val losePodValues = redisOperation.hmGet(MetricsCacheService.podKey(losePod), keys)
                    ?: return@forEachIndexed
                // 分配近似达到负载均衡的效果
                redisOperation.hmset(
                    MetricsCacheService.podKey(live[index % live.size]),
                    keys.zip(losePodValues).toMap()
                )
                redisOperation.hdelete(MetricsCacheService.podKey(losePod), keys.toTypedArray())
            }
            // 双重校验数据一致性
            val check = redisOperation.hkeys(MetricsCacheService.podKey(losePod))
            return check.isNullOrEmpty()
        }
    }
}
