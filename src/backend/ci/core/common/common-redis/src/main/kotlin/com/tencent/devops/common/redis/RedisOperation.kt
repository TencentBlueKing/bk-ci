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

package com.tencent.devops.common.redis

import com.tencent.devops.common.redis.split.RedisSplitProperties
import io.micrometer.core.instrument.util.NamedThreadFactory
import java.util.Date
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.DefaultTypedTuple
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.script.RedisScript

@Suppress("TooManyFunctions", "UNUSED", "ComplexMethod")
class RedisOperation(
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val slaveRedisTemplate: RedisTemplate<String, String>? = null,
    private val splitMode: RedisSplitProperties.Mode,
    private val redisName: String? = null
) {

    // max expire time is 30 days
    private val maxExpireTime = TimeUnit.DAYS.toSeconds(30)

    private val slaveThreadPool = ThreadPoolExecutor(
        5,
        5,
        60L,
        TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue(1024),
        NamedThreadFactory("redis-double-write"),
        object : ThreadPoolExecutor.CallerRunsPolicy() {
            override fun rejectedExecution(r: Runnable, e: ThreadPoolExecutor) {
                logger.error("Redis slave thread pool is full, running task in caller thread")
            }
        }
    )

    fun get(key: String, isDistinguishCluster: Boolean? = false): String? {
        return masterRedisTemplate.opsForValue().get(getFinalKey(key, isDistinguishCluster))
    }

    fun getAndSet(
        key: String,
        defaultValue: String,
        expiredInSecond: Long? = null,
        isDistinguishCluster: Boolean? = false
    ): String? {
        val finalKey = getFinalKey(key, isDistinguishCluster)
        val value = masterRedisTemplate.opsForValue().getAndSet(finalKey, defaultValue)
        // 双写
        writeSlaveIfNeed {
            if (slaveRedisTemplate!!.opsForValue().getAndSet(finalKey, defaultValue) == null) {
                slaveRedisTemplate.opsForValue().getAndSet(finalKey, defaultValue)
            }
        }
        if (value == null) {
            masterRedisTemplate.expire(finalKey, expiredInSecond ?: maxExpireTime, TimeUnit.SECONDS)
        }
        // 双写
        writeSlaveIfNeed {
            if (slaveRedisTemplate!!.opsForValue().getAndSet(finalKey, defaultValue) == null) {
                slaveRedisTemplate.expire(finalKey, expiredInSecond ?: maxExpireTime, TimeUnit.SECONDS)
            }
        }
        return value
    }

    fun increment(key: String, incr: Long, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForValue().increment(getFinalKey(key, isDistinguishCluster), incr)
        }
        return masterRedisTemplate.opsForValue().increment(getFinalKey(key, isDistinguishCluster), incr)
    }

    fun set(
        key: String,
        value: String,
        expiredInSecond: Long? = null,
        expired: Boolean? = true,
        isDistinguishCluster: Boolean? = false
    ) {
        val finalKey = getFinalKey(key, isDistinguishCluster)
        return if (expired == false) {
            // 双写
            writeSlaveIfNeed {
                slaveRedisTemplate!!.opsForValue().set(finalKey, value)
            }
            masterRedisTemplate.opsForValue().set(finalKey, value)
        } else {
            var timeout = expiredInSecond ?: maxExpireTime
            if (timeout <= 0) { // #5901 不合法值清理，设置默认为超时时间，防止出错。
                timeout = maxExpireTime
            }
            // 双写
            writeSlaveIfNeed {
                slaveRedisTemplate!!.opsForValue().set(finalKey, value, timeout, TimeUnit.SECONDS)
            }
            masterRedisTemplate.opsForValue().set(finalKey, value, timeout, TimeUnit.SECONDS)
        }
    }

    fun setIfAbsent(
        key: String,
        value: String,
        expiredInSecond: Long? = null,
        expired: Boolean? = true,
        isDistinguishCluster: Boolean? = false
    ): Boolean {
        val finalKey = getFinalKey(key, isDistinguishCluster)
        return if (expired == false) {
            // 双写
            writeSlaveIfNeed {
                slaveRedisTemplate!!.opsForValue().setIfAbsent(finalKey, value)
            }
            masterRedisTemplate.opsForValue().setIfAbsent(finalKey, value) ?: false
        } else {
            var timeout = expiredInSecond ?: maxExpireTime
            if (timeout <= 0) { // #5901 不合法值清理，设置默认为超时时间，防止出错。
                timeout = maxExpireTime
            }
            // 双写
            writeSlaveIfNeed {
                slaveRedisTemplate!!.opsForValue().setIfAbsent(finalKey, value, timeout, TimeUnit.SECONDS)
            }
            masterRedisTemplate.opsForValue().setIfAbsent(finalKey, value, timeout, TimeUnit.SECONDS) ?: false
        }
    }

    fun delete(key: String, isDistinguishCluster: Boolean? = false): Boolean {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.delete(getFinalKey(key, isDistinguishCluster))
        }
        return masterRedisTemplate.delete(getFinalKey(key, isDistinguishCluster))
    }

    fun delete(keys: Collection<String>, isDistinguishCluster: Boolean? = false) {
        val finalKeys = mutableListOf<String>()
        keys.forEach {
            finalKeys.add(getFinalKey(it, isDistinguishCluster))
        }
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.delete(finalKeys)
        }
        masterRedisTemplate.delete(finalKeys)
    }

    fun hasKey(key: String, isDistinguishCluster: Boolean? = false): Boolean {
        return masterRedisTemplate.hasKey(getFinalKey(key, isDistinguishCluster))
    }

    fun addSetValue(key: String, item: String, isDistinguishCluster: Boolean? = false): Boolean {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForSet().add(getFinalKey(key, isDistinguishCluster), item)
        }
        return masterRedisTemplate.opsForSet().add(getFinalKey(key, isDistinguishCluster), item) == 1L
    }

    fun removeSetMember(key: String, item: String, isDistinguishCluster: Boolean? = false): Boolean {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForSet().remove(getFinalKey(key, isDistinguishCluster), item)
        }
        return masterRedisTemplate.opsForSet().remove(getFinalKey(key, isDistinguishCluster), item) == 1L
    }

    fun isMember(key: String, item: String, isDistinguishCluster: Boolean? = false): Boolean {
        return masterRedisTemplate.opsForSet().isMember(getFinalKey(key, isDistinguishCluster), item) ?: false
    }

    fun isMember(key: String, items: Array<String>, isDistinguishCluster: Boolean? = false): Map<String, Boolean> {
        val finalKey = getFinalKey(key, isDistinguishCluster)
        return items.associateWith {
            masterRedisTemplate.opsForSet().isMember(finalKey, it) ?: false
        }
    }

    fun getSetMembers(key: String, isDistinguishCluster: Boolean? = false): Set<String>? {
        return masterRedisTemplate.opsForSet().members(getFinalKey(key, isDistinguishCluster))
    }

    /**
     * @param key key
     * @param hashKey hash key
     * @param values values
     */
    fun hset(key: String, hashKey: String, values: String, isDistinguishCluster: Boolean? = false) {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForHash<String, String>()
                .put(getFinalKey(key, isDistinguishCluster), hashKey, values)
        }
        masterRedisTemplate.opsForHash<String, String>().put(getFinalKey(key, isDistinguishCluster), hashKey, values)
    }

    fun hmset(key: String, map: Map<String, String>, isDistinguishCluster: Boolean? = false) {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForHash<String, String>().putAll(getFinalKey(key, isDistinguishCluster), map)
        }
        masterRedisTemplate.opsForHash<String, String>().putAll(getFinalKey(key, isDistinguishCluster), map)
    }

    fun hIncrBy(key: String, hashKey: String, delta: Long, isDistinguishCluster: Boolean? = false): Long {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForHash<String, String>()
                .increment(getFinalKey(key, isDistinguishCluster), hashKey, delta)
        }
        return masterRedisTemplate.opsForHash<String, String>()
            .increment(getFinalKey(key, isDistinguishCluster), hashKey, delta)
    }

    fun hget(key: String, hashKey: String, isDistinguishCluster: Boolean? = false): String? {
        return masterRedisTemplate.opsForHash<String, String>().get(getFinalKey(key, isDistinguishCluster), hashKey)
    }

    fun hmGet(key: String, hashKeys: Collection<String>, isDistinguishCluster: Boolean? = false): List<String> {
        return masterRedisTemplate.opsForHash<String, String>()
            .multiGet(getFinalKey(key, isDistinguishCluster), hashKeys)
    }

    fun hdelete(key: String, hashKey: String, isDistinguishCluster: Boolean? = false) {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForHash<String, String>().delete(getFinalKey(key, isDistinguishCluster), hashKey)
        }
        masterRedisTemplate.opsForHash<String, String>().delete(getFinalKey(key, isDistinguishCluster), hashKey)
    }

    fun hdelete(key: String, hashKeys: Array<String>, isDistinguishCluster: Boolean? = false) {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForHash<String, String>().delete(getFinalKey(key, isDistinguishCluster), *hashKeys)
        }
        masterRedisTemplate.opsForHash<String, String>().delete(getFinalKey(key, isDistinguishCluster), *hashKeys)
    }

    fun hhaskey(key: String, hashKey: String, isDistinguishCluster: Boolean? = false): Boolean {
        return masterRedisTemplate.opsForHash<String, String>().hasKey(getFinalKey(key, isDistinguishCluster), hashKey)
    }

    fun hsize(key: String, isDistinguishCluster: Boolean? = false): Long {
        return masterRedisTemplate.opsForHash<String, String>().size(getFinalKey(key, isDistinguishCluster))
    }

    fun hvalues(key: String, isDistinguishCluster: Boolean? = false): MutableList<String> {
        return masterRedisTemplate.opsForHash<String, String>().values(getFinalKey(key, isDistinguishCluster))
    }

    fun hkeys(key: String, isDistinguishCluster: Boolean? = false): MutableSet<String> {
        return masterRedisTemplate.opsForHash<String, String>().keys(getFinalKey(key, isDistinguishCluster))
    }

    fun hentries(key: String, isDistinguishCluster: Boolean? = false): MutableMap<String, String> {
        return masterRedisTemplate.opsForHash<String, String>().entries(getFinalKey(key, isDistinguishCluster))
    }

    fun hscan(
        key: String,
        pattern: String = "*",
        count: Long = 1000L,
        isDistinguishCluster: Boolean? = false
    ): Cursor<MutableMap.MutableEntry<String, String>> {
        val options = ScanOptions.scanOptions().match(pattern).count(count).build()
        return masterRedisTemplate.opsForHash<String, String>().scan(getFinalKey(key, isDistinguishCluster), options)
    }

    fun sadd(key: String, vararg values: String, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForSet().add(getFinalKey(key, isDistinguishCluster), *values)
        }
        return masterRedisTemplate.opsForSet().add(getFinalKey(key, isDistinguishCluster), *values)
    }

    fun sremove(key: String, vararg values: String, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForSet().remove(getFinalKey(key, isDistinguishCluster), *values)
        }
        return masterRedisTemplate.opsForSet().remove(getFinalKey(key, isDistinguishCluster), *values)
    }

    fun sscan(
        key: String,
        pattern: String,
        count: Long = 1000L,
        isDistinguishCluster: Boolean? = false
    ): Cursor<String> {
        val options = ScanOptions.scanOptions().match(pattern).count(count).build()
        return masterRedisTemplate.opsForSet().scan(getFinalKey(key, isDistinguishCluster), options)
    }

    fun zadd(key: String, values: String, score: Double, isDistinguishCluster: Boolean? = false): Boolean? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForZSet().add(getFinalKey(key, isDistinguishCluster), values, score)
        }
        return masterRedisTemplate.opsForZSet().add(getFinalKey(key, isDistinguishCluster), values, score)
    }

    fun zaddTuples(
        key: String,
        values: Set<DefaultTypedTuple<String>>,
        isDistinguishCluster: Boolean? = false
    ): Long? {
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForZSet().add(getFinalKey(key, isDistinguishCluster), values)
        }
        return masterRedisTemplate.opsForZSet().add(getFinalKey(key, isDistinguishCluster), values)
    }

    /**
     * redis version >= 3.0
     */
    fun zaddIfAbsent(
        key: String,
        values: Set<DefaultTypedTuple<String>>,
        isDistinguishCluster: Boolean? = false
    ): Long? {
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForZSet().addIfAbsent(getFinalKey(key, isDistinguishCluster), values)
        }
        return masterRedisTemplate.opsForZSet().addIfAbsent(getFinalKey(key, isDistinguishCluster), values)
    }

    fun zremove(key: String, vararg values: String, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForZSet().remove(getFinalKey(key, isDistinguishCluster), *values)
        }
        return masterRedisTemplate.opsForZSet().remove(getFinalKey(key, isDistinguishCluster), *values)
    }

    fun zsize(key: String, isDistinguishCluster: Boolean? = false): Long? {
        return masterRedisTemplate.opsForZSet().size(getFinalKey(key, isDistinguishCluster))
    }

    fun zsize(key: String, min: Double, max: Double, isDistinguishCluster: Boolean? = false): Long? {
        return masterRedisTemplate.opsForZSet().count(getFinalKey(key, isDistinguishCluster), min, max)
    }

    fun zrange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): Set<String>? {
        return masterRedisTemplate.opsForZSet().range(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun zscore(key: String, item: String, isDistinguishCluster: Boolean? = false): Double? {
        return masterRedisTemplate.opsForZSet().score(getFinalKey(key, isDistinguishCluster), item)
    }

    fun zrank(key: String, values: String, isDistinguishCluster: Boolean? = false): Long? {
        return masterRedisTemplate.opsForZSet().rank(getFinalKey(key, isDistinguishCluster), values)
    }

    fun zrevrange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): Set<String>? {
        return masterRedisTemplate.opsForZSet().reverseRange(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun zremoveRange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForZSet().removeRange(getFinalKey(key, isDistinguishCluster), start, end)
        }
        return masterRedisTemplate.opsForZSet().removeRange(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun zremoveRangeByScore(key: String, min: Double, max: Double, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForZSet().removeRangeByScore(getFinalKey(key, isDistinguishCluster), min, max)
        }
        return masterRedisTemplate.opsForZSet().removeRangeByScore(getFinalKey(key, isDistinguishCluster), min, max)
    }

    fun expireAt(key: String, date: Date, isDistinguishCluster: Boolean? = false): Boolean {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.expireAt(getFinalKey(key, isDistinguishCluster), date)
        }
        return masterRedisTemplate.expireAt(getFinalKey(key, isDistinguishCluster), date)
    }

    fun expire(key: String, expiredInSecond: Long, isDistinguishCluster: Boolean? = false) {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.expire(getFinalKey(key, isDistinguishCluster), expiredInSecond, TimeUnit.SECONDS)
        }
        masterRedisTemplate.expire(getFinalKey(key, isDistinguishCluster), expiredInSecond, TimeUnit.SECONDS)
    }

    fun getExpire(key: String, isDistinguishCluster: Boolean? = false): Long {
        return masterRedisTemplate.getExpire(getFinalKey(key, isDistinguishCluster))
    }

    fun <T> execute(action: RedisCallback<T>): T? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.execute(action)
        }
        return masterRedisTemplate.execute(action)
    }

    fun <T> execute(script: RedisScript<T>, keys: List<String>, vararg args: Any?): T {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.execute(script, keys, *args)
        }
        return masterRedisTemplate.execute(script, keys, *args)
    }

    fun listSize(key: String, isDistinguishCluster: Boolean? = false): Long? {
        return masterRedisTemplate.opsForList().size(getFinalKey(key, isDistinguishCluster))
    }

    fun listRange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): List<String> {
        return masterRedisTemplate.opsForList().range(getFinalKey(key, isDistinguishCluster), start, end) ?: emptyList()
    }

    fun leftPush(key: String, value: String, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForList().leftPush(getFinalKey(key, isDistinguishCluster), value)
        }
        return masterRedisTemplate.opsForList().leftPush(getFinalKey(key, isDistinguishCluster), value)
    }

    fun rightPush(key: String, value: String, isDistinguishCluster: Boolean? = false): Long? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForList().rightPush(getFinalKey(key, isDistinguishCluster), value)
        }
        return masterRedisTemplate.opsForList().rightPush(getFinalKey(key, isDistinguishCluster), value)
    }

    fun rightPop(key: String, isDistinguishCluster: Boolean? = false): String? {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForList().rightPop(getFinalKey(key, isDistinguishCluster))
        }
        return masterRedisTemplate.opsForList().rightPop(getFinalKey(key, isDistinguishCluster))
    }

    fun trim(key: String, start: Long, end: Long) {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForList().trim(key, start, end)
        }
        masterRedisTemplate.opsForList().trim(key, start, end)
    }

    fun setNxEx(key: String, value: String, expiredInSecond: Long): Boolean {
        // 双写
        writeSlaveIfNeed {
            slaveRedisTemplate!!.opsForValue().setIfAbsent(key, value, expiredInSecond, TimeUnit.SECONDS)
        }
        return masterRedisTemplate.opsForValue().setIfAbsent(key, value, expiredInSecond, TimeUnit.SECONDS) ?: false
    }

    fun getRedisName(): String? {
        return redisName
    }

    fun getKeyByRedisName(key: String): String {
        val redisName = getRedisName()
        return if (!redisName.isNullOrBlank()) "$redisName:$key" else key
    }

    private fun getFinalKey(key: String, isDistinguishCluster: Boolean? = false): String {
        return if (isDistinguishCluster == true) {
            getKeyByRedisName(key)
        } else {
            key
        }
    }

    private fun <T> writeSlaveIfNeed(action: () -> T) {
        if (slaveRedisTemplate != null && splitMode == RedisSplitProperties.Mode.DOUBLE_WRITE_SLAVE) {
            // 使用 execute 避免创建 Future，并在满载时按 CallerRunsPolicy 进行背压
            slaveThreadPool.execute { action() }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RedisOperation::class.java)
    }
}
