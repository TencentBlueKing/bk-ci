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

package com.tencent.bkrepo.common.redis

import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import java.util.Date
import java.util.concurrent.TimeUnit

class RedisOperation(private val redisTemplate: RedisTemplate<String, String>) {

    // max expire time is 30 days
    private val maxExpireTime = TimeUnit.DAYS.toSeconds(30)

    fun get(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    fun getAndSet(key: String, defaultValue: String, expiredInSecond: Long? = null): String? {
        val value = redisTemplate.opsForValue().getAndSet(key, defaultValue)
        if (value == null) {
            redisTemplate.expire(key, expiredInSecond ?: maxExpireTime, TimeUnit.SECONDS)
        }
        return value
    }

    fun increment(key: String, incr: Long): Long? {
        return redisTemplate.opsForValue().increment(key, incr)
    }

    fun set(key: String, value: String, expiredInSecond: Long? = null, expired: Boolean? = true) {
        return if (expired == false) {
            redisTemplate.opsForValue().set(key, value)
        } else {
            redisTemplate.opsForValue().set(key, value, expiredInSecond ?: maxExpireTime, TimeUnit.SECONDS)
        }
    }

    fun delete(key: String) {
        redisTemplate.delete(key)
    }

    fun delete(keys: Collection<String>) {
        redisTemplate.delete(keys)
    }

    fun hasKey(key: String): Boolean {
        return redisTemplate.hasKey(key)
    }

    fun addSetValue(key: String, item: String) {
        redisTemplate.opsForSet().add(key, item)
    }

    fun removeSetMember(key: String, item: String) {
        redisTemplate.opsForSet().remove(key, item)
    }

    fun isMember(key: String, item: String): Boolean ? {
        return redisTemplate.opsForSet().isMember(key, item)
    }

    fun getSetMembers(key: String): Set<String>? {
        return redisTemplate.opsForSet().members(key)
    }

    /**
     * @param key key
     * @param hashKey hash key
     * @param values values
     */
    fun hset(key: String, hashKey: String, values: String) {
        redisTemplate.opsForHash<String, String>().put(key, hashKey, values)
    }

    fun hget(key: String, hashKey: String): String? {
        return redisTemplate.opsForHash<String, String>().get(key, hashKey)
    }

    fun hdelete(key: String, hashKey: String) {
        redisTemplate.opsForHash<String, String>().delete(key, hashKey)
    }

    fun hdelete(key: String, hashKeys: Collection<String>) {
        redisTemplate.opsForHash<String, String>().delete(key, hashKeys)
    }

    fun hhaskey(key: String, hashKey: String): Boolean {
        return redisTemplate.opsForHash<String, String>().hasKey(key, hashKey)
    }

    fun hsize(key: String): Long {
        return redisTemplate.opsForHash<String, String>().size(key)
    }

    fun hvalues(key: String): MutableList<String>? {
        return redisTemplate.opsForHash<String, String>().values(key)
    }

    fun hkeys(key: String): MutableSet<String>? {
        return redisTemplate.opsForHash<String, String>().keys(key)
    }

    fun hentries(key: String): MutableMap<String, String>? {
        return redisTemplate.opsForHash<String, String>().entries(key)
    }

    fun sadd(key: String, vararg values: String): Long? {
        return redisTemplate.opsForSet().add(key, *values)
    }

    fun sremove(key: String, values: String): Long? {
        return redisTemplate.opsForSet().remove(key, values)
    }

    fun sscan(key: String, pattern: String, count: Long = 1000L): Cursor<String>? {
        val options = ScanOptions.scanOptions().match(pattern).count(count).build()
        return redisTemplate.opsForSet().scan(key, options)
    }

    fun zadd(key: String, values: String, score: Double): Boolean? {
        return redisTemplate.opsForZSet().add(key, values, score)
    }

    fun zremove(key: String, values: String): Long ? {
        return redisTemplate.opsForZSet().remove(key, values)
    }

    fun zsize(key: String, min: Double, max: Double): Long ? {
        return redisTemplate.opsForZSet().count(key, min, max)
    }

    fun zremoveRangeByScore(key: String, min: Double, max: Double): Long? {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max)
    }

    fun expireAt(key: String, date: Date): Boolean {
        return redisTemplate.expireAt(key, date)
    }

    fun expire(key: String, expiredInSecond: Long) {
        redisTemplate.expire(key, expiredInSecond, TimeUnit.SECONDS)
    }

    fun <T> execute(action: RedisCallback<T>): T ? {
        return redisTemplate.execute(action)
    }
}
