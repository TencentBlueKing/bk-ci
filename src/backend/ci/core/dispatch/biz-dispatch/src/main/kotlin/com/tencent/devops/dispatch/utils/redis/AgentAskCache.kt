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
package com.tencent.devops.dispatch.utils.redis

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.thirdpartyagent.BuildJobType
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

/** 每台 agent 短暂记住最近一次成功的队列查空结果，ALL 查空也覆盖单一构建类型。 */
@Component
class AgentAskCache(
    // Lua、Hash 读取和失效操作必须使用相同的字段编码。
    @param:Qualifier("redisStringHashOperation") private val redis: RedisOperation,
    @Value("\${dispatch.agent.emptyQueueCacheSeconds:20}") private val buildTtlSeconds: Long
) {
    fun isBuildEmpty(agentId: String, type: BuildJobType): Boolean {
        if (type == BuildJobType.NONE || buildTtlSeconds <= 0) return false
        val marker = try {
            activeMarker(redis.hget(key(agentId), BUILD), System.currentTimeMillis())
        } catch (e: Exception) {
            logger.debug("Unable to read empty build queue for agent {}", agentId, e)
            return false
        }
        return marker == BuildJobType.ALL.name || marker == type.name
    }

    private fun activeMarker(value: String?, now: Long): String? {
        if (value == null) return null
        val separator = value.indexOf(':')
        if (separator < 0) return null // 查询中的 UUID
        val expiresAt = value.substring(0, separator).toLongOrNull() ?: return null
        return if (now < expiresAt) value.substring(separator + 1) else null
    }

    /** 入队、改派或超时回队会清除目标 agent 的构建标记，同时作废正在进行的查空确认。 */
    fun invalidateBuild(agentId: String) {
        try {
            redis.hdelete(key(agentId), BUILD)
        } catch (e: Exception) {
            logger.warn("Unable to invalidate empty build queue for agent {}", agentId, e)
        }
    }

    /** 查库前登记 UUID。只有同一轮查询成功返回空时才确认标记，Redis 异常时仍然执行领取查询。 */
    fun <T : Any> queryAndRememberEmpty(agentId: String, type: BuildJobType, load: () -> T?): T? {
        if (buildTtlSeconds <= 0 || type == BuildJobType.NONE) return load()
        val marker = type.name
        val key = key(agentId)
        val probe = UUID.randomUUID().toString()
        val ttl = TimeUnit.SECONDS.toMillis(buildTtlSeconds)
        val start = System.currentTimeMillis()
        val deadline = start + ttl.coerceAtMost(Long.MAX_VALUE - start)
        try {
            redis.execute(register, listOf(key), BUILD, probe, ttl.toString())
        } catch (e: Exception) {
            logger.debug("Unable to register ask cache probe for {}", agentId, e)
            return load()
        }
        val result = load()
        // 有效期从查询开始算。超出有效期的慢查询即使最后返回空，也不再登记命中。
        if (result == null && System.currentTimeMillis() < deadline) {
            try {
                redis.execute(confirm, listOf(key), BUILD, probe, "$deadline:$marker")
            } catch (e: Exception) {
                logger.debug("Unable to confirm ask cache marker for {}", agentId, e)
            }
        }
        return result
    }

    private fun key(agentId: String) = redis.getKeyByRedisName("dispatch:agent:askCache:$agentId")

    companion object {
        private const val BUILD = "build"
        private val logger = LoggerFactory.getLogger(AgentAskCache::class.java)

        // 只在写入时延长物理 TTL，轮询读取不会让空队列标记一直续期。
        private val register = DefaultRedisScript(
            """
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
            if redis.call('PTTL', KEYS[1]) < tonumber(ARGV[3]) then
                redis.call('PEXPIRE', KEYS[1], ARGV[3])
            end
            return 1
            """.trimIndent(), Long::class.javaObjectType
        )

        // 只比较本字段的 UUID。入队的 HDEL 或同字段的下一次查询都能阻止旧结果晚到回写。
        private val confirm = DefaultRedisScript(
            """
            if redis.call('HGET', KEYS[1], ARGV[1]) ~= ARGV[2] then return 0 end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])
            return 1
            """.trimIndent(), Long::class.javaObjectType
        )
    }
}
