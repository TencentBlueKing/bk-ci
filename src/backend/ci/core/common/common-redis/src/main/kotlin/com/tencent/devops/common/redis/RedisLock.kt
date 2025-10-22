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

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.util.UUID
import java.util.concurrent.TimeUnit

open class RedisLock(
    private val redisOperation: RedisOperation,
    private val lockKey: String,
    private val expiredTimeInSeconds: Long,
    private val sleepTime: Long = 100L,
    private var lockValue: String = UUID.randomUUID().toString()
) : AutoCloseable {

    /**
     * 锁是否已经被占用
     */
    fun isLocked() = redisOperation.hasKey(lockKey)

    /**
     * 获取锁,直到成功才会返回
     */
    fun lock() {
        try {
            synchronized(getLocalLock()) {
                while (true) {
                    if (tryLockRemote()) {
                        break
                    }
                    Thread.sleep(sleepTime)
                }
            }
        } catch (e: Exception) {
            logger.error("lock error", e)
            unlock()
        }
    }

    /**
     * 尝试获取锁, 成功会返回true
     */
    fun tryLock(): Boolean {
        return try {
            tryLockRemote()
        } catch (e: Exception) {
            logger.error("try lock error", e)
            unlock()
            false
        }
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
        try {
            if (!unLockRemote()) {
                logger.warn("remote lock has changed , key: $lockKey , value: $lockValue")
                return false
            }
            return true
        } catch (e: Exception) {
            logger.error("unlock error", e)
            return unLockRemote() // try again
        }
    }

    fun <T> lockAround(action: () -> T): T {
        try {
            this.lock()
            return action()
        } finally {
            this.unlock()
        }
    }

    private fun tryLockRemote(): Boolean {
        return redisOperation.setNxEx(decorateKey(lockKey), lockValue, expiredTimeInSeconds)
    }

    private fun unLockRemote(): Boolean {
        return redisOperation.execute(
            DefaultRedisScript(unLockLua, Long::class.java),
            listOf(decorateKey(lockKey)),
            lockValue
        ) > 0
    }

    open fun decorateKey(key: String): String {
        return redisOperation.getKeyByRedisName(key)
    }

    private fun getLocalLock(): Any = localLock.get(lockKey)!!

    fun getLockValue() = lockValue

    fun setLockValue(lockValue: String) {
        this.lockValue = lockValue
    }

    override fun close() {
        unlock()
    }

    companion object {
        private val localLock = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(100000)
            .build<String/*lockKey*/, Any/*localLock*/> { Any() }
        private val unLockLua = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
        """.trimIndent()
        private val logger = LoggerFactory.getLogger(RedisLock::class.java)
    }
}
