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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.pojo.MutexGroupTaskInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MutexGroupQueryService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineContainerService: PipelineContainerService
) {

    fun queryMutexGroupTasks(projectId: String, mutexGroupName: String): List<MutexGroupTaskInfo> {
        if (mutexGroupName.isBlank()) {
            return emptyList()
        }

        // 构建临时 MutexGroup 实例（仅用于生成各 Redis Key）
        val mutexGroup = MutexGroup(
            enable = true,
            mutexGroupName = mutexGroupName,
            queueEnable = true,
            timeout = 0,
            queue = 0
        ).also { it.runtimeMutexGroup = mutexGroupName }

        val lockKey = mutexGroup.genMutexLockKey(projectId)
        val queueKey = mutexGroup.genMutexQueueKey(projectId)

        // 查询 Redis 锁持有者 + 排队队列
        val lockHolderId = redisOperation.get(lockKey)
        val queueEntries: Map<String, String> = redisOperation.hentries(queueKey)

        // 组装结果
        val result = mutableListOf<MutexGroupTaskInfo>()

        // 锁持有者排第一
        if (!lockHolderId.isNullOrBlank()) {
            buildTaskInfo(projectId, lockHolderId, mutexGroup, isLockHolder = true)?.let { result.add(it) }
        }

        // 排队中的任务
        for ((containerMutexId, _) in queueEntries) {
            if (containerMutexId == lockHolderId) continue // 去重，避免锁持有者同时出现在排队队列中的并发残留
            buildTaskInfo(projectId, containerMutexId, mutexGroup, isLockHolder = false)?.let { result.add(it) }
        }

        return result
    }

    /**
     * 根据 containerMutexId（格式：buildId_containerId）构建任务信息
     */
    private fun buildTaskInfo(
        projectId: String,
        containerMutexId: String,
        mutexGroup: MutexGroup,
        isLockHolder: Boolean
    ): MutexGroupTaskInfo? {
        val parts = containerMutexId.split(DELIMITER)
        if (parts.size < 2) {
            logger.warn("Invalid containerMutexId format: $containerMutexId, skip")
            return null
        }
        val buildId = parts[0]
        val containerId = parts[1]

        // 查询 MySQL 获取 container 状态
        val container = try {
            pipelineContainerService.getContainer(
                projectId = projectId,
                buildId = buildId,
                stageId = null,
                containerId = containerId
            )
        } catch (e: Exception) {
            logger.warn("Failed to query container: projectId=$projectId, buildId=$buildId, containerId=$containerId", e)
            null
        }

        val status = container?.status ?: BuildStatus.UNKNOWN
        val pipelineIdFromDb = container?.pipelineId

        // 查询 Redis LinkTip 获取可读信息
        val linkTipValue = try {
            redisOperation.get(mutexGroup.genMutexLinkTipKey(containerMutexId))
        } catch (e: Exception) {
            logger.warn("Failed to query linkTip: $containerMutexId", e)
            null
        }
        val (linkPipelineId, pipelineName, jobName) = parseLinkTip(linkTipValue)

        return MutexGroupTaskInfo(
            mutexGroupName = mutexGroup.fetchRuntimeMutexGroup(),
            buildId = buildId,
            pipelineId = linkPipelineId ?: pipelineIdFromDb,
            pipelineName = pipelineName,
            jobName = jobName,
            status = status,
            isLockHolder = isLockHolder
        )
    }

    /**
     * 解析 LinkTip 值，
     * 与 MutexControl.logContainerMutex 中的解析模式一致
     */
    private fun parseLinkTip(linkTipValue: String?): Triple<String?, String?, String?> {
        if (linkTipValue.isNullOrBlank()) return Triple(null, null, null)
        val firstUnderscore = linkTipValue.indexOf(DELIMITER)
        if (firstUnderscore < 0) return Triple(null, null, null)
        val pipelineId = linkTipValue.substring(0, firstUnderscore)
        val remaining = linkTipValue.substring(firstUnderscore + 1)
        val pipelineName = extractBracketValue(remaining, "Pipeline[", "]")
        val jobName = extractBracketValue(remaining, "Job[", "]")
        return Triple(pipelineId, pipelineName, jobName)
    }

    /**
     * 从字符串中提取指定括号标记内的值
     */
    private fun extractBracketValue(source: String, prefix: String, suffix: String): String? {
        val startIndex = source.indexOf(prefix)
        if (startIndex < 0) return null
        val valueStart = startIndex + prefix.length
        val endIndex = source.indexOf(suffix, valueStart)
        if (endIndex < 0) return null
        return source.substring(valueStart, endIndex)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MutexGroupQueryService::class.java)
        private const val DELIMITER = "_"
    }
}
