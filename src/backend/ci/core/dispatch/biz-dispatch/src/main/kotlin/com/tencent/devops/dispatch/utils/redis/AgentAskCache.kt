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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** 每台 agent 缓存最近一次查空的构建类型。ALL 也覆盖 BINARY 和 DOCKER。 */
@Component
class AgentAskCache(
    private val redis: RedisOperation,
    @Value("\${dispatch.agent.emptyQueueCacheSeconds:20}") private val buildTtlSeconds: Long
) {
    fun isBuildEmpty(agentId: String, type: BuildJobType): Boolean {
        if (buildTtlSeconds <= 0 || type == BuildJobType.NONE) return false
        return try {
            // TODO: 各构建类型共用一个标记，agent 在 BINARY/DOCKER 之间交替询问时会互相覆盖、降低命中率。
            //  这类 agent 构建量大、队列多半不空，暂不按类型拆分。
            val marker = redis.get(key(agentId))
            val hit = marker == BuildJobType.ALL.name || marker == type.name
            if (hit) {
                logger.info("agentEmptyQueueCacheHit agentId={} buildType={}", agentId, type)
            }
            hit
        } catch (e: Exception) {
            logger.warn("Unable to read empty build queue for agent {}", agentId, e)
            false
        }
    }

    /**
     * 并发入队后晚到的查空结果可能重新写入标记，TTL 从本次写入起算。
     * 被挡住的任务最多晚 TTL 加一次 ask 间隔才被领到。
     */
    fun rememberEmpty(agentId: String, type: BuildJobType) {
        if (buildTtlSeconds <= 0 || type == BuildJobType.NONE) return
        try {
            redis.set(key(agentId), type.name, expiredInSecond = buildTtlSeconds)
        } catch (e: Exception) {
            logger.warn("Unable to remember empty build queue for agent {}", agentId, e)
        }
    }

    /**
     * 入队、改派或超时回队后清除空标记。本实例关闭缓存时也要清，其他实例仍可能持有空标记。
     *
     * 任务此时已经入库，Redis 失败只记录日志，不重试也不回滚：旧标记最多残留到过期，
     * 即该 agent 最多晚 TTL 加一次 ask 间隔才领到这个任务。
     */
    fun invalidateBuild(agentId: String) {
        try {
            redis.delete(key(agentId))
        } catch (e: Exception) {
            logger.warn("Unable to invalidate empty build queue for agent {}", agentId, e)
        }
    }

    // 队列表跨集群共享，缓存也按 agent 共享。
    private fun key(agentId: String) = "dispatch:agent:emptyBuildQueue:$agentId"

    companion object {
        private val logger = LoggerFactory.getLogger(AgentAskCache::class.java)
    }
}
