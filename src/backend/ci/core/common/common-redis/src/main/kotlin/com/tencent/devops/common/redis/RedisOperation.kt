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
 */

package com.tencent.devops.common.redis

import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import java.util.Date
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions", "UNUSED")
class RedisOperation(private val redisTemplate: RedisTemplate<String, String>, private val redisName: String? = null) {

    // max expire time is 30 days
    private val maxExpireTime = TimeUnit.DAYS.toSeconds(30)

    fun get(key: String, isDistinguishCluster: Boolean? = false): String? {
        return redisTemplate.opsForValue().get(getFinalKey(key, isDistinguishCluster))
    }

    fun getAndSet(
        key: String,
        defaultValue: String,
        expiredInSecond: Long? = null,
        isDistinguishCluster: Boolean? = false
    ): String? {
        val finalKey = getFinalKey(key, isDistinguishCluster)
        val value = redisTemplate.opsForValue().getAndSet(finalKey, defaultValue)
        if (value == null) {
            redisTemplate.expire(finalKey, expiredInSecond ?: maxExpireTime, TimeUnit.SECONDS)
        }
        return value
    }

    fun increment(key: String, incr: Long, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForValue().increment(getFinalKey(key, isDistinguishCluster), incr)
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
            redisTemplate.opsForValue().set(finalKey, value)
        } else {
            var timeout = expiredInSecond ?: maxExpireTime
            if (timeout <= 0) { // #5901 不合法值清理，设置默认为超时时间，防止出错。
                timeout = maxExpireTime
            }
            redisTemplate.opsForValue().set(finalKey, value, timeout, TimeUnit.SECONDS)
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
            redisTemplate.opsForValue().setIfAbsent(finalKey, value) ?: false
        } else {
            var timeout = expiredInSecond ?: maxExpireTime
            if (timeout <= 0) { // #5901 不合法值清理，设置默认为超时时间，防止出错。
                timeout = maxExpireTime
            }
            redisTemplate.opsForValue().setIfAbsent(finalKey, value, timeout, TimeUnit.SECONDS) ?: false
        }
    }

    fun delete(key: String, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.delete(getFinalKey(key, isDistinguishCluster))
    }

    fun delete(keys: Collection<String>, isDistinguishCluster: Boolean? = false) {
        val finalKeys = mutableListOf<String>()
        keys.forEach {
            finalKeys.add(getFinalKey(it, isDistinguishCluster))
        }
        redisTemplate.delete(finalKeys)
    }

    fun hasKey(key: String, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.hasKey(getFinalKey(key, isDistinguishCluster))
    }

    fun addSetValue(key: String, item: String, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.opsForSet().add(getFinalKey(key, isDistinguishCluster), item) == 1L
    }

    fun removeSetMember(key: String, item: String, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.opsForSet().remove(getFinalKey(key, isDistinguishCluster), item) == 1L
    }

    fun isMember(key: String, item: String, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.opsForSet().isMember(getFinalKey(key, isDistinguishCluster), item) ?: false
    }

    fun getSetMembers(key: String, isDistinguishCluster: Boolean? = false): Set<String>? {
        return redisTemplate.opsForSet().members(getFinalKey(key, isDistinguishCluster))
    }

    /**
     * @param key key
     * @param hashKey hash key
     * @param values values
     */
    fun hset(key: String, hashKey: String, values: String, isDistinguishCluster: Boolean? = false) {
        redisTemplate.opsForHash<String, String>().put(getFinalKey(key, isDistinguishCluster), hashKey, values)
    }

    fun hmset(key: String, map: Map<String, String>, isDistinguishCluster: Boolean? = false) {
        redisTemplate.opsForHash<String, String>().putAll(getFinalKey(key, isDistinguishCluster), map)
    }

    fun hIncrBy(key: String, hashKey: String, delta: Long, isDistinguishCluster: Boolean? = false): Long =
        redisTemplate.opsForHash<String, String>().increment(getFinalKey(key, isDistinguishCluster), hashKey, delta)

    fun hget(key: String, hashKey: String, isDistinguishCluster: Boolean? = false): String? {
        return redisTemplate.opsForHash<String, String>().get(getFinalKey(key, isDistinguishCluster), hashKey)
    }

    fun hdelete(key: String, hashKey: String, isDistinguishCluster: Boolean? = false) {
        redisTemplate.opsForHash<String, String>().delete(getFinalKey(key, isDistinguishCluster), hashKey)
    }

    fun hdelete(key: String, hashKeys: Collection<String>, isDistinguishCluster: Boolean? = false) {
        redisTemplate.opsForHash<String, String>().delete(getFinalKey(key, isDistinguishCluster), hashKeys)
    }

    fun hhaskey(key: String, hashKey: String, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.opsForHash<String, String>().hasKey(getFinalKey(key, isDistinguishCluster), hashKey)
    }

    fun hsize(key: String, isDistinguishCluster: Boolean? = false): Long {
        return redisTemplate.opsForHash<String, String>().size(getFinalKey(key, isDistinguishCluster))
    }

    fun hvalues(key: String, isDistinguishCluster: Boolean? = false): MutableList<String>? {
        return redisTemplate.opsForHash<String, String>().values(getFinalKey(key, isDistinguishCluster))
    }

    fun hkeys(key: String, isDistinguishCluster: Boolean? = false): MutableSet<String>? {
        return redisTemplate.opsForHash<String, String>().keys(getFinalKey(key, isDistinguishCluster))
    }

    fun hentries(key: String, isDistinguishCluster: Boolean? = false): MutableMap<String, String>? {
        return redisTemplate.opsForHash<String, String>().entries(getFinalKey(key, isDistinguishCluster))
    }

    fun sadd(key: String, vararg values: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForSet().add(getFinalKey(key, isDistinguishCluster), *values)
    }

    fun sremove(key: String, values: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForSet().remove(getFinalKey(key, isDistinguishCluster), values)
    }

    fun sscan(
        key: String,
        pattern: String,
        count: Long = 1000L,
        isDistinguishCluster: Boolean? = false
    ): Cursor<String>? {
        val options = ScanOptions.scanOptions().match(pattern).count(count).build()
        return redisTemplate.opsForSet().scan(getFinalKey(key, isDistinguishCluster), options)
    }

    fun zadd(key: String, values: String, score: Double, isDistinguishCluster: Boolean? = false): Boolean? {
        return redisTemplate.opsForZSet().add(getFinalKey(key, isDistinguishCluster), values, score)
    }

    fun zremove(key: String, values: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForZSet().remove(getFinalKey(key, isDistinguishCluster), values)
    }

    fun zsize(key: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForZSet().size(getFinalKey(key, isDistinguishCluster))
    }

    fun zsize(key: String, min: Double, max: Double, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForZSet().count(getFinalKey(key, isDistinguishCluster), min, max)
    }

    fun zrange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): Set<String>? {
        return redisTemplate.opsForZSet().range(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun zrank(key: String, values: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForZSet().rank(getFinalKey(key, isDistinguishCluster), values)
    }

    fun zrevrange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): Set<String>? {
        return redisTemplate.opsForZSet().reverseRange(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun zremoveRange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForZSet().removeRange(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun zremoveRangeByScore(key: String, min: Double, max: Double, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForZSet().removeRangeByScore(getFinalKey(key, isDistinguishCluster), min, max)
    }

    fun expireAt(key: String, date: Date, isDistinguishCluster: Boolean? = false): Boolean {
        return redisTemplate.expireAt(getFinalKey(key, isDistinguishCluster), date)
    }

    fun expire(key: String, expiredInSecond: Long, isDistinguishCluster: Boolean? = false) {
        redisTemplate.expire(getFinalKey(key, isDistinguishCluster), expiredInSecond, TimeUnit.SECONDS)
    }

    fun <T> execute(action: RedisCallback<T>): T? {
        return redisTemplate.execute(action)
    }

    fun listSize(key: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForList().size(getFinalKey(key, isDistinguishCluster))
    }

    fun listRange(key: String, start: Long, end: Long, isDistinguishCluster: Boolean? = false): List<String>? {
        return redisTemplate.opsForList().range(getFinalKey(key, isDistinguishCluster), start, end)
    }

    fun leftPush(key: String, value: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForList().leftPush(getFinalKey(key, isDistinguishCluster), value)
    }

    fun rightPush(key: String, value: String, isDistinguishCluster: Boolean? = false): Long? {
        return redisTemplate.opsForList().rightPush(getFinalKey(key, isDistinguishCluster), value)
    }

    fun rightPop(key: String, isDistinguishCluster: Boolean? = false): String? {
        return redisTemplate.opsForList().rightPop(getFinalKey(key, isDistinguishCluster))
    }

    fun trim(key: String, start: Long, end: Long) {
        redisTemplate.opsForList().trim(key, start, end)
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
}
