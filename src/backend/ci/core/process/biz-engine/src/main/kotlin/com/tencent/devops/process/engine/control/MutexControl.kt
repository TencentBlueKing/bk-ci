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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.enums.ContainerMutexStatus
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.PipelineContainerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
@Suppress("TooManyFunctions", "LongParameterList")
class MutexControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineUrlBean: PipelineUrlBean,
    private val pipelineContainerService: PipelineContainerService
) {

    companion object {
        private const val DELIMITERS = "_"
        private const val SECOND_TO_PRINT = 19
        private const val MUTEX_MAX_QUEUE = 10
        private val LOG = LoggerFactory.getLogger(MutexControl::class.java)
        private fun getMutexContainerId(buildId: String, containerId: String) = "${buildId}$DELIMITERS$containerId"
        private fun getBuildIdAndContainerId(mutexId: String): List<String> = mutexId.split(DELIMITERS)
    }

    internal fun decorateMutexGroup(mutexGroup: MutexGroup?, variables: Map<String, String>): MutexGroup? {
        if (mutexGroup == null || mutexGroup.inited == true) {
            return mutexGroup
        }
        // 超时时间限制，0表示排队不等待直接超时
        val timeOut = parseTimeoutVar(mutexGroup, variables)
        // 排队任务数量限制，0表示不排队
        val queue = mutexGroup.queue.coerceAtLeast(0).coerceAtMost(MUTEX_MAX_QUEUE)
        // 替换环境变量
        val mutexGroupName = if (!mutexGroup.mutexGroupName.isNullOrBlank()) {
            EnvUtils.parseEnv(mutexGroup.mutexGroupName, variables)
        } else {
            mutexGroup.mutexGroupName
        }

        mutexGroup.inited = true // 初始化过
        return if (
            mutexGroupName != mutexGroup.mutexGroupName ||
            timeOut != mutexGroup.timeout ||
            queue != mutexGroup.timeout
        ) {
            mutexGroup.copy(mutexGroupName = mutexGroupName, timeout = timeOut, queue = queue)
        } else {
            mutexGroup
        }
    }

    private fun parseTimeoutVar(mutexGroup: MutexGroup, variables: Map<String, String>) =
        (if (!mutexGroup.timeoutVar.isNullOrBlank()) {
            try {
                if (mutexGroup.timeoutVar?.startsWith("\${") == true) { //
                    EnvUtils.parseEnv(mutexGroup.timeoutVar!!, variables).toInt()
                } else {
                    mutexGroup.timeoutVar!!.toInt()
                }
            } catch (ignore: NumberFormatException) { // 解析失败，以timeout为准
                mutexGroup.timeout
            }
        } else {
            mutexGroup.timeout
        }).coerceAtLeast(0).coerceAtMost(Timeout.MAX_MINUTES)

    internal fun acquireMutex(mutexGroup: MutexGroup?, container: PipelineBuildContainer): ContainerMutexStatus {
        // 当互斥组为空为空或互斥组名称为空或互斥组没有启动的时候，不做互斥行为
        if (mutexGroup == null || mutexGroup.mutexGroupName.isNullOrBlank() || !mutexGroup.enable) {
            return ContainerMutexStatus.READY
        }
        // 每次都对Job互斥组的redis key和queue都进行清理
        cleanMutex(projectId = container.projectId, mutexGroup = mutexGroup)

        val lockResult = tryToLockMutex(mutexGroup = mutexGroup, container = container)
        return if (lockResult) {
            LOG.info("ENGINE|${container.buildId}|LOCK_SUCCESS|${container.stageId}|j(${container.containerId})")
            // 抢到锁则可以继续运行，并退出队列
            quitMutexQueue(
                projectId = container.projectId,
                buildId = container.buildId,
                containerId = container.containerId,
                mutexGroup = mutexGroup
            )
            ContainerMutexStatus.READY
        } else {
            // 首先判断队列的等待情况
            val queueResult = checkMutexQueue(mutexGroup = mutexGroup, container = container)
            if (ContainerMutexStatus.CANCELED == queueResult) {
                // 排队失败说明超时或者超出队列则取消运行，解锁并退出队列
                releaseContainerMutex(
                    projectId = container.projectId,
                    buildId = container.buildId,
                    stageId = container.stageId,
                    containerId = container.containerId,
                    mutexGroup = mutexGroup,
                    executeCount = container.executeCount
                )
            }
            queueResult
        }
    }

    /**
     * 解锁,分两步，
     * 1.解锁lock key;
     * 2.退出排队队列
     */
    internal fun releaseContainerMutex(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        mutexGroup: MutexGroup?,
        executeCount: Int?
    ) {
        if (mutexGroup != null && mutexGroup.enable) {
            val mutexGroupName = mutexGroup.mutexGroupName
            LOG.info("[$buildId]|RELEASE_MUTEX_LOCK|s($stageId)|j($containerId)|project=$projectId|[$mutexGroupName]")
            val containerMutexId = getMutexContainerId(buildId = buildId, containerId = containerId)
            val lockKey = mutexGroup.genMutexLockKey(projectId)
            val containerMutexLock = RedisLockByValue(redisOperation, lockKey, containerMutexId, 1)
            containerMutexLock.unlock()
            redisOperation.delete(mutexGroup.genMutexLinkTipKey(containerMutexId)) // #5454 删除tip
            quitMutexQueue(
                projectId = projectId,
                buildId = buildId,
                containerId = containerId,
                mutexGroup = mutexGroup
            )

            // 完善日志
            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = "释放互斥组锁(Release Lock) Mutex[$mutexGroupName]",
                tag = VMUtils.genStartVMTaskId(containerId),
                jobId = null,
                executeCount = executeCount ?: 1
            )
        }
    }

    /**
     * 尝试获取互斥锁，如果获取到则返回true,没有则进入队列排队
     */
    private fun tryToLockMutex(mutexGroup: MutexGroup, container: PipelineBuildContainer): Boolean {
        val containerMutexId = getMutexContainerId(container.buildId, containerId = container.containerId)
        val lockKey = mutexGroup.genMutexLockKey(container.projectId)
        val expireSec = getTimeoutSec(container)
        val containerMutexLock = RedisLockByValue(redisOperation, lockKey, containerMutexId, expireSec)
        // 获取到锁的containerId
        val lockedContainerMutexId = redisOperation.get(lockKey)

        if (lockedContainerMutexId != null) {
            // 当前锁不为null的时候
            return lockedContainerMutexId == containerMutexId
        }
        val queueKey = mutexGroup.genMutexQueueKey(container.projectId)
        // 获取队列中的开始时间，为空的时候则为当前时间
        val startTime = redisOperation.hget(queueKey, containerMutexId)?.toLong() ?: LocalDateTime.now().timestamp()
        var minTime: Long? = null
        val queueValues = redisOperation.hvalues(queueKey)
        if (queueValues != null && queueValues.size > 0) {
            val queueLongValues = queueValues.map { it.toLong() }
            minTime = queueLongValues.minOrNull()
        }

        val lockResult = if (minTime != null) {
            if (startTime == minTime) {
                // 最小值和container入队列时间一致的时候，可以开始抢锁
                containerMutexLock.tryLock()
            } else {
                // 不是最早的队列，则不能抢锁，直接返回
                false
            }
        } else {
            // 没有排队最小值的时候，则开始抢锁
            containerMutexLock.tryLock()
        }

        if (lockResult) {
            mutexGroup.linkTip?.let {
                redisOperation.set(mutexGroup.genMutexLinkTipKey(containerMutexId), mutexGroup.linkTip!!, expireSec)
            }
            logContainerMutex(container, mutexGroup, null, msg = "获得锁定(Matched) 锁定期(Exp): ${expireSec}s")
        }

        return lockResult
    }

    /**
     * 获取锁的过期时间以[container]Job的超时时间为准，并冗余2分钟，默认902分钟
     * 如果是以前的设置0的情况，则设置为最大超时时间
     */
    private fun getTimeoutSec(container: PipelineBuildContainer): Long {
        var tm = (container.controlOption?.jobControlOption?.timeout ?: Timeout.DEFAULT_TIMEOUT_MIN)
        // 兼容设置为0的情况（最大默认值）
        if (tm == 0) {
            tm = Timeout.MAX_MINUTES
        }
        return TimeUnit.MINUTES.toSeconds(tm + 2L) // 冗余2分钟
    }

    @Suppress("LongMethod")
    private fun checkMutexQueue(mutexGroup: MutexGroup, container: PipelineBuildContainer): ContainerMutexStatus {
        val lockKey = mutexGroup.genMutexLockKey(projectId = container.projectId)
        val lockedContainerMutexId = redisOperation.get(lockKey)
        // 当没有启用互斥组排队或者互斥组名字为空的时候，则直接排队失败
        if (!mutexGroup.queueEnable) {
            logContainerMutex(container, mutexGroup, lockedContainerMutexId, "未开启排队(Queue disabled)", isError = true)
            return ContainerMutexStatus.CANCELED
        }
        val containerMutexId = getMutexContainerId(buildId = container.buildId, containerId = container.containerId)
        val queueKey = mutexGroup.genMutexQueueKey(container.projectId)
        val exist = redisOperation.hhaskey(queueKey, containerMutexId)
        val queueSize = redisOperation.hsize(queueKey)
        // 也已经在队列中,判断是否已经超时
        return if (exist) {
            val startTime = redisOperation.hget(queueKey, containerMutexId)?.toLong() ?: LocalDateTime.now().timestamp()
            val currentTime = LocalDateTime.now().timestamp()
            val timeDiff = currentTime - startTime
            // 排队等待时间为0的时候，立即超时, 退出队列，并失败, 没有就继续在队列中,timeOut时间为分钟
            if (mutexGroup.timeout == 0 || timeDiff > TimeUnit.MINUTES.toSeconds(mutexGroup.timeout.toLong())) {
                val desc = "${
                    if (mutexGroup.timeoutVar.isNullOrBlank()) {
                        "[${mutexGroup.timeout} minutes]"
                    } else " timeoutVar[${mutexGroup.timeoutVar}] setup to [${mutexGroup.timeout} minutes]"
                } "
                logContainerMutex(
                    container = container, mutexGroup = mutexGroup, lockedContainerMutexId = lockedContainerMutexId,
                    msg = "排队超时(Queue timeout): $desc", isError = true
                )
                quitMutexQueue(
                    projectId = container.projectId,
                    buildId = container.buildId,
                    containerId = container.containerId,
                    mutexGroup = mutexGroup
                )
                ContainerMutexStatus.CANCELED
            } else {
                val timeDiffMod = timeDiff % TimeUnit.MINUTES.toSeconds(1L) // 余数 1分钟内
                // 在一分钟内的小于[SECOND_TO_PRINT]秒的才打印
                if (timeDiffMod <= SECOND_TO_PRINT) {
                    logContainerMutex(
                        container, mutexGroup, lockedContainerMutexId,
                        msg = "当前排队数(Queuing)[$queueSize], 已等待(Waiting)[$timeDiff seconds]"
                    )
                }
                ContainerMutexStatus.WAITING
            }
        } else { // todo此处存在并发问题，假设capacity只有1个, 两个并发都会同时满足queueSize = 0,导入入队，但问题不大，暂不解决
            // 排队队列为0的时候，不做排队
            // 还没有在队列中，则判断队列的数量,如果超过了则排队失败,没有则进入队列.
            if (mutexGroup.queue == 0 || queueSize >= mutexGroup.queue) {
                logContainerMutex(container, mutexGroup, lockedContainerMutexId, "队列满(Queue full)", isError = true)
                ContainerMutexStatus.CANCELED
            } else {
                logContainerMutex(
                    container, mutexGroup, lockedContainerMutexId,
                    msg = "当前排队数(Queuing)[${queueSize + 1}]. 入队等待(Enqueue)"
                )
                // 则进入队列,并返回成功
                enterMutexQueue(
                    projectId = container.projectId,
                    buildId = container.buildId,
                    containerId = container.containerId,
                    mutexGroup = mutexGroup
                )
                mutexGroup.linkTip?.let {
                    redisOperation.set(
                        key = mutexGroup.genMutexLinkTipKey(containerMutexId),
                        value = mutexGroup.linkTip!!,
                        expiredInSecond = getTimeoutSec(container)
                    )
                }
                ContainerMutexStatus.FIRST_LOG
            }
        }
    }

    private fun enterMutexQueue(projectId: String, buildId: String, containerId: String, mutexGroup: MutexGroup) {
        val containerMutexId = getMutexContainerId(buildId = buildId, containerId = containerId)
        val queueKey = mutexGroup.genMutexQueueKey(projectId)
        val currentTime = LocalDateTime.now().timestamp()
        redisOperation.hset(queueKey, containerMutexId, currentTime.toString())
    }

    private fun quitMutexQueue(projectId: String, buildId: String, containerId: String, mutexGroup: MutexGroup) {
        val containerMutexId = getMutexContainerId(buildId = buildId, containerId = containerId)
        val queueKey = mutexGroup.genMutexQueueKey(projectId)
        redisOperation.hdelete(queueKey, containerMutexId)
    }

    private fun logContainerMutex(
        container: PipelineBuildContainer,
        mutexGroup: MutexGroup,
        lockedContainerMutexId: String?,
        msg: String,
        isError: Boolean = false
    ) {

        val message = "Job#${container.containerId}|互斥组Mutex[${mutexGroup.mutexGroupName}]|" +
            if (!lockedContainerMutexId.isNullOrBlank()) {
                // #5454 拿出占用锁定的信息
                redisOperation.get(mutexGroup.genMutexLinkTipKey(lockedContainerMutexId))?.let { s ->
                    val endIndex = s.indexOf("_")
                    val pipelineId = s.substring(0, endIndex)
                    val linkTip = s.substring(endIndex + 1)
                    val cs = getBuildIdAndContainerId(lockedContainerMutexId)
                    val link = pipelineUrlBean.genBuildDetailUrl(
                        projectCode = container.projectId,
                        pipelineId = pipelineId,
                        buildId = cs[0],
                        position = null,
                        stageId = null,
                        needShortUrl = false
                    )
                    if (cs[0] != container.buildId) {
                        "锁定中(Running): $linkTip<a target='_blank' href='$link'>查看(Click)</a> | $msg"
                    } else {
                        "当前(Current): $linkTip| $msg"
                    }
                } ?: msg
            } else {
                msg
            }

        if (isError) {
            buildLogPrinter.addErrorLine(
                buildId = container.buildId,
                message = message,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = null,
                executeCount = container.executeCount
            )
        } else {
            buildLogPrinter.addYellowLine(
                buildId = container.buildId,
                message = message,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = null,
                executeCount = container.executeCount
            )
        }
    }

    /**
     * 清理互斥锁和队列
     */
    private fun cleanMutex(projectId: String, mutexGroup: MutexGroup) {
        val lockKey = mutexGroup.genMutexLockKey(projectId)
        // 当互斥锁的key为空的时候，直接返回
        val mutexLockId = if (lockKey.isBlank()) {
            ""
        } else {
            redisOperation.get(lockKey) ?: ""
        }

        if (mutexLockId.isNotBlank()) {
            val mutexIdList = mutexLockId.split(DELIMITERS)
            val buildId = mutexIdList[0]
            val containerId = mutexIdList[1]
            // container结束的时候，删除lock key
            val containerFinished = isContainerFinished(projectId, buildId, containerId)
            if (containerFinished) {
                LOG.warn("[MUTEX] CLEAN LOCK KEY|buildId=$buildId|container=$containerId|projectId=$projectId")
                val containerMutexLock = RedisLockByValue(redisOperation, lockKey, mutexLockId, 1)
                containerMutexLock.unlock()
            }
        }

        // 清理互斥队列
        val queueKey = mutexGroup.genMutexQueueKey(projectId)
        // 当互斥队列的key为空的时候，直接返回
        if (queueKey.isBlank()) {
            return
        }
        val queueMutexIdList = redisOperation.hkeys(queueKey)
        queueMutexIdList?.forEach { mutexId ->
            val mutexIdList = mutexId.split(DELIMITERS)
            val buildId = mutexIdList[0]
            val containerId = mutexIdList[1]
            // container结束的时候，删除queue中的key
            if (isContainerFinished(projectId, buildId, containerId)) {
                redisOperation.hdelete(queueKey, mutexId)
            }
        }
    }

    // 判断container是否已经结束
    private fun isContainerFinished(projectId: String, buildId: String?, containerId: String?): Boolean {
        if (buildId.isNullOrBlank() || containerId.isNullOrBlank()) {
            return false
        }
        val container = pipelineContainerService.getContainer(
            projectId = projectId,
            buildId = buildId,
            stageId = null,
            containerId = containerId
        )
        return container == null || container.status.isFinish()
    }
}
