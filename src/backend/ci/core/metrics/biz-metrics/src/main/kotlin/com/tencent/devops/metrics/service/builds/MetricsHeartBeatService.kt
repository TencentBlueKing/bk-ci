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
 *
 */

package com.tencent.devops.metrics.service.builds

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.metrics.config.MetricsUserConfig
import com.tencent.devops.metrics.pojo.po.MetricsUserPO
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["metrics.user.enable"], havingValue = "true", matchIfMissing = false)
class MetricsHeartBeatService @Autowired constructor(
    @Qualifier("redisStringHashOperation")
    private val redisHashOperation: RedisOperation,
    private val metricsUserConfig: MetricsUserConfig,
    private val bkTag: BkTag
) {
    private val podName: String = System.getenv("POD_NAME") ?: UUID.randomUUID().toString()

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsHeartBeatService::class.java)
    }

    fun heartBeatKey() = "build_metrics:${bkTag.getLocalTag()}:heart_beat"
    fun podKey(key: String) = "build_metrics:${bkTag.getLocalTag()}:pod:$key"
    fun updateKey() = "build_metrics:${bkTag.getLocalTag()}:update"
    fun bufferKey() = "build_metrics:${bkTag.getLocalTag()}:buffer"
    fun redisLockKey() = "metrics_heart_beat_manager_process_redis_lock_${bkTag.getLocalTag()}"

    fun init() {
        Thread(HeartBeatProcess(heartBeatKey(), podName, redisHashOperation)).start()
        Thread(
            HeartBeatManagerProcess(
                redisLockKey = redisLockKey(),
                updateKey = updateKey(),
                bufferKey = bufferKey(),
                heartBeatKey = heartBeatKey(),
                podKey = ::podKey,
                redisOperation = redisHashOperation,
                maxLocalCacheSize = metricsUserConfig.localCacheMaxSize
            )
        ).start()
    }

    fun getPodName(): String = podName

    private class HeartBeatProcess(
        private val heartBeatKey: String,
        private val podHashKey: String,
        private val redisOperation: RedisOperation
    ) : Runnable {

        companion object {
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
                heartBeatKey,
                podHashKey,
                LocalDateTime.now().toInstant(ZoneOffset.ofHours(8)).epochSecond.toString()
            )
        }
    }

    private class HeartBeatManagerProcess(
        private val redisLockKey: String,
        private val updateKey: String,
        private val bufferKey: String,
        private val heartBeatKey: String,
        private val podKey: (String) -> String,
        private val redisOperation: RedisOperation,
        private val maxLocalCacheSize: Long
    ) : Runnable {

        companion object {
            const val SLEEP = 15000L
            const val CHUNKED = 100
        }

        override fun run() {
            logger.info("HeartBeatManagerProcess begin")
            while (true) {
                val redisLock = RedisLock(redisOperation, redisLockKey, 60L)
                try {
                    val lockSuccess = redisLock.tryLock()
                    if (lockSuccess) {
                        logger.info("HeartBeatManagerProcess get lock.")
                        heartBeatCheck()
                        invalidUpdateCheck()
                        bufferCheck()
                    }
                } catch (e: Throwable) {
                    logger.error("HeartBeatManagerProcess failed ${e.message}", e)
                } finally {
                    Thread.sleep(SLEEP)
                    redisLock.unlock()
                }
            }
        }

        /**
         * 检查失效的更新数据并进行清理。
         *
         * 该方法会从Redis中获取更新数据的键列表，并根据更新数据的结束时间判断是否失效。
         * 如果更新数据的结束时间早于1小时前，则将其标记为失效数据。
         *
         * 失效的更新数据将被删除。
         *
         * @return 无
         */
        private fun invalidUpdateCheck() {
            val updateKeys = redisOperation.hkeys(updateKey)?.ifEmpty { null } ?: return
            val limit = LocalDateTime.now().plusMinutes(-10)
            val needDelete = mutableListOf<String>()
            updateKeys.chunked(CHUNKED).forEach { keys ->
                val updateValues = redisOperation.hmGet(updateKey, keys)
                    ?: return@forEach
                keys.forEachIndexed { index, key ->
                    val load = MetricsUserPO.load(updateValues[index]) ?: return@forEachIndexed
                    if (load.endTime!! < limit) {
                        needDelete.add(key)
                    }
                }
            }
            if (needDelete.isNotEmpty()) {
                redisOperation.hdelete(updateKey, needDelete.toTypedArray())
            }
        }

        /**
         * 检查缓冲区并将指标数据分配到可用的Pod。
         *
         * 判断是否需要检测buffer，如果buffer大小为0，则直接返回。
         * 获取可用的Pod列表，如果列表为空，则直接返回。
         * 检测可用空间并计算每个Pod的可用空间大小。
         * 循环处理直到没有更多可用空间或者buffer大小小于一个单位。
         * 选择可用空间最大的宿主Pod。
         * 如果该Pod的可用空间减去一个单位的大小小于等于0，则表示该Pod已无法容纳更多指标，结束循环。
         * 获取一个单位的指标数据进行处理。
         * 将指标数据存储到目标Pod中。
         * 删除buffer中对应的数据。
         * 如果buffer大小小于一个单位，则结束循环。
         * 刷新可用空间。
         */
        private fun bufferCheck() {
            // 判断是否需要检测buffer
            val buffSize = redisOperation.hsize(bufferKey)
            if (buffSize == 0L) return
            // 获取可用pod
            val livePods = redisOperation.hkeys(heartBeatKey) ?: return
            // 检测可用空间
            val availablePodsSizeMap = livePods.associateWith { podKey ->
                maxLocalCacheSize - redisOperation.hsize(podKey(podKey))
            }.toMutableMap()
            while (true) {
                // 选择最佳宿主Pod
                val targetPod = availablePodsSizeMap.maxByOrNull { it.value } ?: break
                if (targetPod.value - CHUNKED <= 0) {
                    // 目前已没有pod能容纳下更多指标了
                    break
                }
                // 获取一个单位进行处理
                val cursor = redisOperation.hscan(bufferKey, count = CHUNKED.toLong())
                val loadedKeys = mutableListOf<String>()
                while (cursor.hasNext()) {
                    val keyValue = cursor.next()
                    redisOperation.hset(podKey(targetPod.key), keyValue.key, keyValue.value)
                    loadedKeys.add(keyValue.key)
                }
                // 删除buffer对应数据
                redisOperation.hdelete(bufferKey, loadedKeys.toTypedArray())
                // 再次检测buffer大小，如果小于一个单位则break
                if (redisOperation.hsize(bufferKey) < CHUNKED) break
                // 刷新可用空间
                availablePodsSizeMap[targetPod.key] = redisOperation.hsize(podKey(targetPod.key))
            }
        }

        /**
         * 检查心跳状态并处理失效的Pod。
         *
         * 该方法会从Redis中获取心跳信息，并根据最后在线时间判断Pod的状态。
         * 如果Pod的最后在线时间早于1分钟前，则将其标记为失效Pod。
         * 如果Pod的最后在线时间在1分钟内，则将其标记为正常Pod。
         *
         * 失效的Pod将被移除，并且与之相关的指标缓存也会被删除。
         *
         * @return 无
         */
        private fun heartBeatCheck() {
            val heartBeats = redisOperation.hentries(heartBeatKey) ?: return
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
                    redisOperation.hdelete(heartBeatKey, losePod)
                    redisOperation.delete(podKey(losePod))
                }
            }
        }

        /**
         * 在失效Pod之后处理相关操作。
         *
         * 该方法会根据失效Pod的名称和正常Pod的列表，将失效Pod的指标数据转移给正常Pod。
         *
         * 首先，方法会从Redis中获取失效Pod的指标键列表。如果列表为空，则表示失效Pod已被处理，直接返回true。
         *
         * 然后，方法会将失效Pod的指标数据按照近似负载均衡的方式分配给正常Pod。分配的方式是将失效Pod的指标键值对
         * 逐个转移到正常Pod中，并删除失效Pod中对应的键值对。
         *
         * 最后，方法会再次校验失效Pod的指标键列表，如果为空，则表示数据转移成功，返回true；否则返回false。
         *
         * @param losePod 失效Pod的名称
         * @param live 正常Pod的列表
         * @return 数据转移是否成功的布尔值。如果失效Pod的指标键列表为空，则返回true；否则返回false。
         */
        private fun afterLosePod(losePod: String, live: List<String>): Boolean {
            val losePodKeys = redisOperation.hkeys(podKey(losePod))?.ifEmpty { null } ?: return true
            // 分块处理，优化性能。
            losePodKeys.chunked(CHUNKED).forEachIndexed { index, keys ->
                val losePodValues = redisOperation.hmGet(podKey(losePod), keys)
                    ?: return@forEachIndexed
                // 分配近似达到负载均衡的效果
                redisOperation.hmset(
                    podKey(live[index % live.size]),
                    keys.zip(losePodValues).toMap()
                )
                redisOperation.hdelete(podKey(losePod), keys.toTypedArray())
            }
            // 双重校验数据一致性
            val check = redisOperation.hkeys(podKey(losePod))
            return check.isNullOrEmpty()
        }
    }
}
