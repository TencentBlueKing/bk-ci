/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisCallback

class RedisLockByValue(
    private val redisOperation: RedisOperation,
    private val lockKey: String,
    private val lockValue: String,
    private val expiredTimeInSeconds: Long
) : AutoCloseable {
    companion object {
        /**
         * 将key 的值设为value ，当且仅当key 不存在，等效于 SETNX。
         */
        private const val NX = "NX"

        /**
         * seconds — 以秒为单位设置 key 的过期时间，等效于EXPIRE key seconds
         */
        private const val EX = "EX"

        /**
         * 调用set后的返回值
         */
        private const val OK = "OK"

        private val logger = LoggerFactory.getLogger(RedisLockByValue::class.java)

        private const val UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
    }

    /**
     * 一直等待到获取到锁
     *
     *
     */
    fun lock() {
        while (true) {
            // 如果存在且value一致，则返回true
            val redisValue = redisOperation.get(lockKey)
            if (redisValue != null && redisValue == lockValue) {
                return
            }
//            logger.info("Start to lock($lockKey) of value($lockValue) for $expiredTimeInSeconds sec")
            val result = set(lockKey, lockValue, expiredTimeInSeconds)
//            logger.info("Get the lock result($result)")
            val l = OK.equals(result, true)
            if (l) {
                return
            }
            Thread.sleep(100)
        }
    }

    /**
     * 尝试获取锁 立即返回
     *
     * @return 是否成功获得锁
     */
    fun tryLock(): Boolean {
        // 如果存在且value一致，则返回true
        val redisValue = redisOperation.get(lockKey)
        if (redisValue != null && redisValue == lockValue) {
            return true
        }
        // 不存在则添加 且设置过期时间（单位ms）
        logger.info("Start to lock($lockKey) of value($lockValue) for $expiredTimeInSeconds sec")
        val result = set(lockKey, lockValue, expiredTimeInSeconds)
        logger.info("Get the lock result($result)")
        return OK.equals(result, true)
    }

    /**
     * 重写redisTemplate的set方法
     * <p>
     * 命令 SET resource-name anystring NX EX max-lock-time 是一种在 Redis 中实现锁的简单方法。
     * <p>
     * 客户端执行以上的命令：
     * <p>
     * 如果服务器返回 OK ，那么这个客户端获得锁。
     * 如果服务器返回 NIL ，那么客户端获取锁失败，可以在稍后再重试。
     *
     * @param key 锁的Key
     * @param value 锁里面的值
     * @param seconds 过去时间（秒）
     * @return
     */
    private fun set(key: String, value: String, seconds: Long): String? {
        return redisOperation.execute(
            RedisCallback { connection ->

                val result =
                    when (val nativeConnection = connection.nativeConnection) {
                        is RedisAdvancedClusterAsyncCommands<*, *> -> {
                            (nativeConnection as RedisAdvancedClusterAsyncCommands<ByteArray, ByteArray>)
                                .statefulConnection.sync()
                                .set(
                                    key.toByteArray(), value.toByteArray(), SetArgs.Builder.nx().ex(seconds)
                                )
                        }
                        is RedisAsyncCommands<*, *> -> {
                            (nativeConnection as RedisAsyncCommands<ByteArray, ByteArray>).statefulConnection.sync()
                                .set(
                                    key.toByteArray(), value.toByteArray(), SetArgs.Builder.nx().ex(seconds)
                                )
                        }
                        else -> {
                            logger.warn("Unknown redis connection($nativeConnection)")
                            null
                        }
                    }
                result
            }
        )
    }

    /**
     * 解锁
     * <p>
     * 可以通过以下修改，让这个锁实现更健壮：
     * <p>
     * 不使用固定的字符串作为键的值，而是设置一个不可猜测（non-guessable）的长随机字符串，作为口令串（token）。
     * 不使用 DEL 命令来释放锁，而是发送一个 Lua 脚本，这个脚本只在客户端传入的值和键的口令串相匹配时，才对键进行删除。
     * 这两个改动可以防止持有过期锁的客户端误删现有锁的情况出现。
     */
    fun unlock(): Boolean {
        // 只有加锁成功并且锁还有效才去释放锁
        // 如果存在且value一致，则返回true
        val redisValue = redisOperation.get(lockKey)
        var result = false
        if (redisValue == null) {
            logger.info("It's already unlock")
            return true
        }
        if (redisValue == lockValue) {
            logger.info("It's owner lock.")
            result = true
        }
        if (redisValue != lockValue) {
            logger.info("It's not allowed to unlock")
            return false
        }
        if (result) {
            result = redisOperation.execute(
                RedisCallback { connection ->
                    val nativeConnection = connection.nativeConnection

                    val keys = arrayOf(lockKey.toByteArray())
                    val queryResult =
                        when (nativeConnection) {
                            is RedisAdvancedClusterAsyncCommands<*, *> -> {
                                (
                                        nativeConnection
                                                as RedisAdvancedClusterAsyncCommands<ByteArray, ByteArray>
                                        )
                                    .eval<Long>(
                                        UNLOCK_LUA,
                                        ScriptOutputType.INTEGER,
                                        keys,
                                        lockValue.toByteArray()
                                    ).get()
                            }
                            is RedisAsyncCommands<*, *> -> {
                                (nativeConnection as RedisAsyncCommands<ByteArray, ByteArray>).eval<Long>(
                                    UNLOCK_LUA,
                                    ScriptOutputType.INTEGER,
                                    keys,
                                    lockValue.toByteArray()
                                ).get()
                            }
                            else -> {
                                logger.warn("Unknown redis connection($nativeConnection)")
                                0L
                            }
                        }
                    queryResult == 1L
                }
            ) ?: false
        }
        return result
    }

    override fun close() {
        unlock()
    }
}
