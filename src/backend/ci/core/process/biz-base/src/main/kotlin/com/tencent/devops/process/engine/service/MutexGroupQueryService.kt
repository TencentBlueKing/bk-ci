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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.pojo.MutexGroupTaskInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MutexGroupQueryService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineContainerService: PipelineContainerService,
    private val engineConfigService: EngineConfigService,
) {

    fun queryMutexGroupTasks(projectId: String, mutexGroupName: String): List<MutexGroupTaskInfo> {
        if (mutexGroupName.isBlank()) {
            throw ParamBlankException("Invalid mutexGroupName")
        }

        // 构建临时 MutexGroup 实例（仅用于生成各 Redis Key）
        val mutexGroup = MutexGroup(
            enable = true,
            mutexGroupName = mutexGroupName,
            queueEnable = true,
            timeout = 0,
            queue = 0,
        ).also { it.runtimeMutexGroup = mutexGroupName }

        val lockKey = mutexGroup.genMutexLockKey(projectId)
        val queueKey = mutexGroup.genMutexQueueKey(projectId)

        // 查询 Redis 锁持有者 + 排队队列
        val lockHolderId = redisOperation.get(lockKey)
        val queueEntries: Map<String, String> = redisOperation.hentries(queueKey)

        // 排队中的任务，按入队时间升序，与 MutexControl 抢锁时取最小入队时间的顺序一致
        val maxQueue = engineConfigService.getMutexMaxQueue()
        if (queueEntries.size > maxQueue) {
            logger.warn(
                "Mutex queue size exceeds limit|projectId=$projectId|group=$mutexGroupName|size=${queueEntries.size}",
            )
        }
        val sortedQueue = queueEntries.entries
            .filter { it.key != lockHolderId } // 去重，避免锁持有者同时出现在排队队列中的并发残留
            .sortedBy { it.value.toLongOrNull() ?: Long.MAX_VALUE }

        // 锁持有者排第一，其后为排队任务；扫描条数为 maxQueue 加脏数据缓冲，受 SCAN_LIMIT_MAX 封顶
        val candidates = mutableListOf<Pair<String, Boolean>>()
        if (!lockHolderId.isNullOrBlank()) {
            candidates.add(lockHolderId to true)
        }
        val scanLimit = (maxQueue + SCAN_DIRTY_TOLERANCE).coerceAtMost(SCAN_LIMIT_MAX)
        if (sortedQueue.size > scanLimit) {
            logger.warn(
                "Mutex queue scan truncated|projectId=$projectId|group=$mutexGroupName|" +
                    "queueSize=${sortedQueue.size}|scanLimit=$scanLimit",
            )
        }
        sortedQueue.take(scanLimit).forEach { (containerMutexId, _) ->
            candidates.add(containerMutexId to false)
        }
        val containers = listContainers(
            projectId = projectId,
            containerMutexIds = candidates.map { it.first },
        )

        // 组装结果，已结束的记录不占名额，避免 Redis 脏数据把存活的排队任务挤出结果
        val result = mutableListOf<MutexGroupTaskInfo>()
        var queued = 0
        for ((containerMutexId, isLockHolder) in candidates) {
            if (!isLockHolder && queued >= maxQueue) break
            buildTaskInfo(
                containerMutexId = containerMutexId,
                mutexGroup = mutexGroup,
                isLockHolder = isLockHolder,
                containers = containers,
            )?.let {
                result.add(it)
                if (!isLockHolder) queued++
            }
        }

        return result
    }

    /**
     * 批量查询 container，按 containerMutexId
     */
    private fun listContainers(
        projectId: String,
        containerMutexIds: Collection<String>,
    ): Map<String, PipelineBuildContainer> {
        val buildIds = mutableSetOf<String>()
        containerMutexIds.forEach { containerMutexId ->
            val parts = containerMutexId.split(DELIMITER)
            if (parts.size >= 2) {
                buildIds.add(parts[0])
            }
        }
        if (buildIds.isEmpty()) {
            return emptyMap()
        }
        val containers = try {
            pipelineContainerService.listByBuildIds(projectId = projectId, buildIds = buildIds)
        } catch (e: Throwable) {
            logger.warn("Failed to query containers|projectId=$projectId|buildIds=${buildIds.size}", e)
            throw e
        }
        val containerMap = mutableMapOf<String, PipelineBuildContainer>()
        containers.forEach { container ->
            containerMap["${container.buildId}$DELIMITER${container.containerId}"] = container
        }
        return containerMap
    }

    /**
     * 根据 containerMutexId（格式：buildId_containerId）构建任务信息
     */
    private fun buildTaskInfo(
        containerMutexId: String,
        mutexGroup: MutexGroup,
        isLockHolder: Boolean,
        containers: Map<String, PipelineBuildContainer>,
    ): MutexGroupTaskInfo? {
        val parts = containerMutexId.split(DELIMITER)
        if (parts.size < 2) {
            logger.warn("Invalid containerMutexId format: $containerMutexId, skip")
            return null
        }
        val buildId = parts[0]
        val container = containers[containerMutexId]

        // 与 MutexControl.cleanMutex 判定一致：container 不存在或已结束不再作为当前任务返回
        if (container == null || container.status.isFinish()) {
            return null
        }

        // 查询 Redis LinkTip 获取可读信息
        val linkTipValue = try {
            redisOperation.get(mutexGroup.genMutexLinkTipKey(containerMutexId))
        } catch (e: Throwable) {
            logger.warn("Failed to query linkTip: $containerMutexId", e)
            null
        }
        val (pipelineName, jobName) = parseLinkTip(linkTipValue)

        return MutexGroupTaskInfo(
            mutexGroupName = mutexGroup.fetchRuntimeMutexGroup(),
            buildId = buildId,
            pipelineId = container.pipelineId,
            pipelineName = pipelineName,
            jobName = jobName,
            jobId = container.jobId,
            containerId = container.containerId,
            status = container.status,
            isLockHolder = isLockHolder,
        )
    }

    /**
     * 解析 LinkTip 值中的流水线名与 Job 名，格式为 `{pipelineId}_Pipeline[{流水线名}]Job[{Job名}]`
     */
    private fun parseLinkTip(linkTipValue: String?): Pair<String?, String?> {
        if (linkTipValue.isNullOrBlank()) return null to null
        val firstUnderscore = linkTipValue.indexOf(DELIMITER)
        if (firstUnderscore < 0) return null to null
        val remaining = linkTipValue.substring(firstUnderscore + 1)
        val pipelinePrefix = "Pipeline["
        val jobSeparator = "]Job["
        if (!remaining.startsWith(pipelinePrefix)) return null to null
        val separatorIndex = remaining.indexOf(jobSeparator, pipelinePrefix.length)
        if (separatorIndex < 0) return null to null
        val pipelineName = remaining.substring(pipelinePrefix.length, separatorIndex)
        val jobName = remaining.substring(separatorIndex + jobSeparator.length).removeSuffix("]")
        return pipelineName to jobName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MutexGroupQueryService::class.java)
        private const val DELIMITER = "_"
        private const val SCAN_DIRTY_TOLERANCE = 150
        private const val SCAN_LIMIT_MAX = 500
    }
}
