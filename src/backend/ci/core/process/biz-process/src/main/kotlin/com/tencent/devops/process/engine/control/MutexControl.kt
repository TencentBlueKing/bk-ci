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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ContainerMutexStatus
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MutexControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun initMutexGroup(mutexGroup: MutexGroup?, variables: Map<String, String>): MutexGroup? {
        if (mutexGroup == null) {
            return null
        }
        // 超时时间为1-2880分钟
        val timeOut = when {
            mutexGroup.timeout > 10080 -> 10080
            mutexGroup.timeout < 0 -> 0
            else -> mutexGroup.timeout
        }
        // 排队任务数量为1-10
        val queue = when {
            mutexGroup.queue > 10 -> 10
            mutexGroup.queue < 0 -> 0
            else -> mutexGroup.queue
        }
        // 替换环境变量
        val mutexGroupName = if (mutexGroup.mutexGroupName != null) {
            EnvUtils.parseEnv(mutexGroup.mutexGroupName!!, variables)
        } else {
            null
        }
        return MutexGroup(
            enable = mutexGroup.enable,
            mutexGroupName = mutexGroupName,
            queueEnable = mutexGroup.queueEnable,
            timeout = timeOut,
            queue = queue
        )
    }

    internal fun checkContainerMutex(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        mutexGroup: MutexGroup?,
        container: PipelineBuildContainer
    ): ContainerMutexStatus {
        // 当互斥组为空为空或互斥组名称为空或互斥组没有启动的时候，不做互斥行为
        if (mutexGroup == null || mutexGroup.mutexGroupName.isNullOrBlank() || !mutexGroup.enable) {
            return ContainerMutexStatus.READY
        }

        // 每次都对Job互斥组的redis key和queue都进行清理
        cleanMutex(projectId, mutexGroup)

        val lockResult = tryToLockMutex(projectId, buildId, stageId, containerId, mutexGroup, container)
        return if (lockResult) {
            logger.info("[mutex] LOCK_SUCCESS |buildId=$buildId|stage=$stageId|container=$containerId|projectId=$projectId")
            // 抢到锁则可以继续运行，并退出队列
            quitMutexQueue(
                projectId = projectId,
                buildId = buildId,
                containerId = containerId,
                mutexGroup = mutexGroup
            )
            ContainerMutexStatus.READY
        } else {
            // 首先判断队列的等待情况
            val queueResult =
                checkForContainerMutexQueue(
                    projectId = projectId,
                    buildId = buildId,
                    containerId = containerId,
                    mutexGroup = mutexGroup,
                    container = container
                )
            if (queueResult) {
                // 排队成功则继续等待
                ContainerMutexStatus.WAITING
            } else {
                // 排队失败说明超时或者超出队列则取消运行，解锁并退出队列
                releaseContainerMutex(projectId, buildId, stageId, containerId, mutexGroup)
                ContainerMutexStatus.CANCELED
            }
        }
    }

    internal fun releaseContainerMutex(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        mutexGroup: MutexGroup?
    ) {
        logger.info("[mutex] RELEASE_MUTEX_LOCK |buildId=$buildId|stage=$stageId|container=$containerId|projectId=$projectId")
        if (mutexGroup != null) {
            unlockMutex(
                projectId = projectId,
                buildId = buildId,
                containerId = containerId,
                mutexGroup = mutexGroup
            )
        }
    }

    // 尝试获取互斥锁，如果获取到则返回true,没有则进入队列排队
    private fun tryToLockMutex(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        mutexGroup: MutexGroup,
        container: PipelineBuildContainer
    ): Boolean {
        val containerMutexId = getMutexContainerId(buildId, containerId)
        val lockKey = getMutexLockKey(projectId, mutexGroup)
        val queueKey = getMutexQueueKey(projectId, mutexGroup)
        val containerMutexLock = RedisLockByValue(redisOperation, lockKey, containerMutexId, 86400)
        // 获取到锁的containerId
        val lockedContainerMutexId = redisOperation.get(lockKey)

        if (lockedContainerMutexId != null) {
            // 当前锁不为null的时候
            logger.info("[mutex] RELEASE_LOCK |buildId=$buildId|stage=$stageId|container=$containerId|projectId=$projectId")
            return lockedContainerMutexId == containerMutexId
        }
        // 获取队列中的开始时间，为空的时候则为当前时间
        val startTime = redisOperation.hget(queueKey, containerMutexId)?.toLong() ?: LocalDateTime.now().timestamp()
        var minTime: Long? = null
        val queueValues = redisOperation.hvalues(queueKey)
        if (queueValues != null && queueValues.size > 0) {
            val queueLongValues = queueValues.map { it.toLong() }
            minTime = queueLongValues.min()
        }
        if (minTime != null) {
            val lockResult = if (startTime == minTime) {
                // 最小值和container入队列时间一致的时候，可以开始抢锁
                containerMutexLock.tryLock()
            } else {
                // 不是最早的队列，则不能抢锁，直接返回
                false
            }
            if (lockResult) {
                logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
                logContainerMutex(container, "[mutex] 已获取互斥锁(${mutexGroup.mutexGroupName})，准备运行该Job...")
            }
            return lockResult
        }
        // 没有排队最小值的时候，则开始抢锁
        val lockResult = containerMutexLock.tryLock()
        if (lockResult) {
            logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
            logContainerMutex(container, "[mutex] 已获取互斥锁(${mutexGroup.mutexGroupName})，准备运行该Job...")
        }
        return lockResult
    }

    // 解锁,分两步，第一步解锁lock key;第二部退出排队队列
    private fun unlockMutex(
        projectId: String,
        buildId: String,
        containerId: String,
        mutexGroup: MutexGroup
    ) {
        val containerMutexId = getMutexContainerId(buildId, containerId)
        val lockKey = getMutexLockKey(projectId, mutexGroup)
        val containerMutexLock = RedisLockByValue(redisOperation, lockKey, containerMutexId, 86400)
        containerMutexLock.unlock()
        quitMutexQueue(
            projectId = projectId,
            buildId = buildId,
            containerId = containerId,
            mutexGroup = mutexGroup
        )
    }

    private fun checkForContainerMutexQueue(
        projectId: String,
        buildId: String,
        containerId: String,
        mutexGroup: MutexGroup,
        container: PipelineBuildContainer
    ): Boolean {

        val lockKey = getMutexLockKey(projectId, mutexGroup)
        val lockedContainerMutexId = redisOperation.get(lockKey)
        // 当没有启动互斥组或者没有启动互斥组排队或者互斥组名字为空的时候，则直接排队失败
        if (!mutexGroup.enable || !mutexGroup.queueEnable || mutexGroup.mutexGroupName.isNullOrBlank()) {
            logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
            logContainerMutex(container, "[mutex] 已经有Job($lockedContainerMutexId)在运行，该Job取消...")
            return false
        }
        val containerMutexId = getMutexContainerId(buildId, containerId)
        val queueKey = getMutexQueueKey(projectId, mutexGroup)
        val exist = redisOperation.hhaskey(queueKey, containerMutexId)
        val queueSize = redisOperation.hsize(queueKey)

        // 也已经在队列中,判断是否已经超时
        if (exist) {
            val startTime = redisOperation.hget(queueKey, containerMutexId)?.toLong() ?: LocalDateTime.now().timestamp()
            val currentTime = LocalDateTime.now().timestamp()
            val timeDiff = currentTime - startTime
            // 排队等待时间为0的时候，立即超时
            // 超时就退出队列，并失败, 没有就继续在队列中,timeOut时间为分钟
            return if (mutexGroup.timeout == 0 || timeDiff > mutexGroup.timeout * 60) {
                logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
                logContainerMutex(container, "[mutex] 互斥组(${mutexGroup.mutexGroupName})超过了最长等待时间，正在运行的Job($lockedContainerMutexId)，该Job取消...")
                quitMutexQueue(
                    projectId = projectId,
                    buildId = buildId,
                    containerId = containerId,
                    mutexGroup = mutexGroup
                )
                false
            } else {
                val timeDiffMod = timeDiff % 60 // 余数，在19秒内的，应该只有一次运行，所以在一分钟内的小于19秒的才打印
                val timeDiffQuotient = timeDiff / 60 // 余数，在19秒内的，应该只有一次运行，所以在一分钟内的小于19秒的才打印
                val timeDiffDisplay = if (timeDiffQuotient > 0) "${timeDiffQuotient}分钟" else "${timeDiff}秒"
                var frontContainer = 1 // 排在该Container签名的数量
                redisOperation.hvalues(queueKey)?.map { it.toLong() }?.forEach {
                    if (it < startTime) {
                        frontContainer++
                    }
                }

                if (timeDiffMod <= 19) {
                    logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
                    logContainerMutex(container, "[mutex] 已等待互斥锁(${mutexGroup.mutexGroupName})$timeDiffDisplay，正在运行的Job($lockedContainerMutexId)，目前还有${frontContainer}个任务在排队...")
                }
                true
            }
        } else {
            // 排队队列为0的时候，不做排队
            // 还没有在队列中，则判断队列的数量,如果超过了则排队失败,没有则进入队列.
            return if (mutexGroup.queue == 0 || queueSize >= mutexGroup.queue) {
                logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
                logContainerMutex(container, "[mutex] 互斥组(${mutexGroup.mutexGroupName})队列已经超过最大任务数(${mutexGroup.queue})，正在运行的Job($lockedContainerMutexId)，该Job取消")
                false
            } else {
                logContainerMutex(container, "[mutex] Job(${buildId}_$containerId) 互斥判断")
                logContainerMutex(container, "[mutex] 进入互斥组(${mutexGroup.mutexGroupName})的排队队列，正在运行的Job($lockedContainerMutexId)，目前还有${queueSize + 1}个任务在排队...")
                // 则进入队列,并返回成功
                enterMutexQueue(
                    projectId = projectId,
                    buildId = buildId,
                    containerId = containerId,
                    mutexGroup = mutexGroup
                )
                true
            }
        }
    }

    private fun enterMutexQueue(projectId: String, buildId: String, containerId: String, mutexGroup: MutexGroup) {
        val containerMutexId = getMutexContainerId(buildId, containerId)
        val queueKey = getMutexQueueKey(projectId, mutexGroup)
        val currentTime = LocalDateTime.now().timestamp()
        redisOperation.hset(queueKey, containerMutexId, currentTime.toString())
    }

    private fun quitMutexQueue(
        projectId: String,
        buildId: String,
        containerId: String,
        mutexGroup: MutexGroup
    ) {
        val containerMutexId = getMutexContainerId(buildId, containerId)
        val queueKey = getMutexQueueKey(projectId, mutexGroup)
        redisOperation.hdelete(queueKey, containerMutexId)
    }

    private fun getMutexLockKey(projectId: String, mutexGroup: MutexGroup): String {
        val mutexGroupName = mutexGroup.mutexGroupName ?: ""
        return "lock:container:mutex:$projectId:$mutexGroupName:lock"
    }

    private fun getMutexQueueKey(projectId: String, mutexGroup: MutexGroup): String {
        val mutexGroupName = mutexGroup.mutexGroupName ?: ""
        return "lock:container:mutex:$projectId:$mutexGroupName:queue"
    }

    private fun getMutexContainerId(buildId: String, containerId: String): String {
        return "${buildId}_$containerId"
    }

    private fun logContainerMutex(container: PipelineBuildContainer, message: String) {
        buildLogPrinter.addYellowLine(
            buildId = container.buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(container.containerId),
            jobId = null,
            executeCount = container.executeCount
        )
    }

    // 清理互斥锁
    private fun cleanMutexLock(projectId: String, mutexGroup: MutexGroup) {
        val lockKey = getMutexLockKey(projectId, mutexGroup)
        // 当互斥锁的key为空的时候，直接返回
        if (lockKey.isBlank()) {
            return
        }
        val mutexId = redisOperation.get(lockKey)
        if (mutexId != null && mutexId.isNotBlank()) {
//            logger.info("[mutex] CHECK LOCK KEY |mutexId=$mutexId|projectId=$projectId")
            val mutexIdList = mutexId.split("_")
            val buildId = mutexIdList[0]
            val containerId = mutexIdList[1]
            // container结束的时候，删除lock key
            if (buildId.isNotBlank() && containerId.isNotBlank() && isContainerFinished(buildId, containerId)) {
                logger.warn("[mutex] CLEAN LOCK KEY|buildId=$buildId|container=$containerId|projectId=$projectId")
                val containerMutexLock = RedisLockByValue(redisOperation, lockKey, mutexId, 86400)
                containerMutexLock.unlock()
            }
        }
    }

    // 清理互斥队列
    private fun cleanMutexQueue(projectId: String, mutexGroup: MutexGroup) {
        val queueKey = getMutexQueueKey(projectId, mutexGroup)
        // 当互斥队列的key为空的时候，直接返回
        if (queueKey.isBlank()) {
            return
        }
        val queueMutexIdList = redisOperation.hkeys(queueKey)
        if (queueMutexIdList != null && queueMutexIdList.isNotEmpty()) {
            queueMutexIdList.forEach { mutexId ->
//                logger.info("[mutex] CHECK QUEUE KEY |mutexId=$mutexId|projectId=$projectId")
                val mutexIdList = mutexId.split("_")
                val buildId = mutexIdList[0]
                val containerId = mutexIdList[1]
                // container结束的时候，删除queue中的key
                if (buildId.isNotBlank() && containerId.isNotBlank() && isContainerFinished(buildId, containerId)) {
                    logger.warn("[mutex] CLEAN QUEUE KEY |mutexId=$mutexId|buildId=$buildId|container=$containerId|projectId=$projectId")
                    redisOperation.hdelete(queueKey, mutexId)
                }
            }
        }
    }

    // 清理互斥组
    private fun cleanMutex(projectId: String, mutexGroup: MutexGroup) {
        cleanMutexLock(projectId, mutexGroup)
        cleanMutexQueue(projectId, mutexGroup)
    }

    // 判断container是否已经结束
    private fun isContainerFinished(buildId: String, containerId: String): Boolean {
        val containerRecord = pipelineRuntimeService.getContainer(
            buildId = buildId,
            stageId = null,
            containerId = containerId
        )
        return if (containerRecord != null) {
            // container结束已经STAGE_SUCCESS都是结束
            BuildStatus.isFinish(containerRecord.status) || containerRecord.status == BuildStatus.STAGE_SUCCESS
        } else {
            true
        }
    }
}