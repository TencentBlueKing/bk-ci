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

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.common.BS_CANCEL_BUILD_SOURCE
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.ContainerIdLock
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.utils.BuildUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.util.TaskUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "ComplexCondition", "TooManyFunctions")
@Service
class BuildCancelControl @Autowired constructor(
    private val mutexControl: MutexControl,
    private val redisOperation: RedisOperation,
    private val pipelineMQEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineBuildRecordService: PipelineBuildRecordService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val buildVariableService: BuildVariableService,
    private val buildLogPrinter: BuildLogPrinter,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val dependOnControl: DependOnControl
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildCancelControl::class.java)
    }

    @BkTimed
    fun handle(event: PipelineBuildCancelEvent) {
        val watcher = Watcher(id = "ENGINE|BuildCancel|${event.traceId}|${event.buildId}|${event.status}")
        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = event.buildId)
        try {
            watcher.start("lock")
            redisLock.lock()
            watcher.start("execute")
            execute(event)
        } catch (ignored: Exception) {
            LOG.error("ENGINE|${event.buildId}|{${event.source}}|build finish fail: $ignored", ignored)
        } finally {
            redisLock.unlock()
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    private fun execute(event: PipelineBuildCancelEvent): Boolean {
        val buildId = event.buildId
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId = event.projectId, buildId = buildId)
        // 已经结束的构建，不再受理，抛弃消息
        if (buildInfo == null || buildInfo.status.isFinish()) {
            LOG.info("[$$buildId|${event.source}|REPEAT_CANCEL_EVENT|${event.status}| abandon!")
            return false
        }

        val model = pipelineBuildDetailService.getBuildModel(projectId = event.projectId, buildId = buildId)
        return if (model != null) {
            LOG.info("ENGINE|${event.buildId}|${event.source}|CANCEL|status=${event.status}")
            if (event.actionType != ActionType.TERMINATE) {
                // 往redis中设置取消构建标识以防止重复提交
                setBuildCancelActionRedisFlag(buildId)
            }
            cancelAllPendingTask(event = event, model = model)
            if (event.actionType == ActionType.TERMINATE) {
                // 修改detail model
                pipelineBuildRecordService.buildCancel(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    buildStatus = event.status,
                    cancelUser = event.userId,
                    executeCount = buildInfo.executeCount ?: 1
                )
            }

            // 排队的则不再获取Pending Stage，防止Final Stage被执行
            val pendingStage: PipelineBuildStage? =
                if (buildInfo.status.isReadyToRun() || buildInfo.status.isNeverRun()) {
                    null
                } else {
                    pipelineStageService.getPendingStage(event.projectId, buildId)
                }

            if (pendingStage != null) {
                if (pendingStage.status.isPause()) { // 处于审核暂停的Stage需要走取消Stage逻辑
                    pipelineStageService.cancelStageBySystem(
                        userId = event.userId,
                        buildInfo = buildInfo,
                        buildStage = pendingStage,
                        timeout = false
                    )
                } else {
                    pendingStage.dispatchEvent(event)
                }
            } else {
                sendBuildFinishEvent(event)
            }

            measureService?.postCancelData(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = buildId,
                userId = event.userId
            )
            true
        } else {
            false
        }
    }

    private fun setBuildCancelActionRedisFlag(buildId: String) =
        redisOperation.set(
            key = BuildUtils.getCancelActionBuildKey(buildId),
            value = System.currentTimeMillis().toString(),
            expiredInSecond = Timeout.transMinuteTimeoutToSec(Timeout.MAX_MINUTES)
        )

    private fun sendBuildFinishEvent(event: PipelineBuildCancelEvent) {
        pipelineMQEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = BS_CANCEL_BUILD_SOURCE,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                userId = event.userId,
                buildId = event.buildId,
                status = event.status
            )
        )
    }

    fun PipelineBuildStage.dispatchEvent(event: PipelineBuildCancelEvent) {
        // #3138 buildCancel支持finallyStage
        pipelineMQEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = BS_CANCEL_BUILD_SOURCE,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = event.userId,
                buildId = buildId,
                stageId = stageId,
                actionType = ActionType.END
            )
        )
    }

    @Suppress("ALL")
    private fun cancelAllPendingTask(event: PipelineBuildCancelEvent, model: Model) {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        val variables: Map<String, String> by lazy {
            buildVariableService.getAllVariable(
                projectId,
                pipelineId,
                buildId
            )
        }
        val executeCount: Int by lazy { buildVariableService.getBuildExecuteCount(projectId, pipelineId, buildId) }
        val stages = model.stages
        stages.forEachIndexed nextStage@{ index, stage ->
            if (stage.status == null || index == 0) { // Trigger 和 未启动的忽略
                return@nextStage
            }

            if (event.actionType != ActionType.TERMINATE && stage.finally && index > 1) {
                // 当前stage为finallyStage且它前一个stage也已经运行过了或者还未运行业务逻辑，则finallyStage也能取消
                val preStageStatus = BuildStatus.parse(stages[index - 1].status)
                val preStageNoExecuteBusFlag = !preStageStatus.isFinish() && preStageStatus != BuildStatus.UNEXEC
                // 当触发器stage执行完成且业务stage还未执行，则不需要执行finallyStage
                if (getStageExecuteBusFlag(stages[0]) &&
                    (preStageNoExecuteBusFlag || !getStageExecuteBusFlag(stages[1]))
                ) {
                    return@nextStage
                }
            }

            stage.containers.forEach nextC@{ container ->
                if (container.status == null || BuildStatus.parse(container.status).isFinish()) { // 未启动的和已完成的忽略
                    return@nextC
                }
                val stageId = stage.id ?: ""
                cancelContainerPendingTask(
                    stageId = stageId,
                    event = event,
                    variables = variables,
                    container = container,
                    executeCount = executeCount
                )
                container.fetchGroupContainers()?.forEach matrix@{ c ->
                    if (c.status == null || BuildStatus.parse(c.status).isFinish()) { // 未启动的和已完成的忽略
                        return@matrix
                    }
                    cancelContainerPendingTask(
                        event = event,
                        stageId = stageId,
                        variables = variables,
                        container = c,
                        executeCount = executeCount
                    )
                }
            }
        }
    }

    private fun cancelContainerPendingTask(
        event: PipelineBuildCancelEvent,
        variables: Map<String, String>,
        stageId: String,
        container: Container,
        executeCount: Int
    ) {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        val containerId = container.id ?: return
        val containerIdLock = ContainerIdLock(redisOperation, buildId, containerId)
        try {
            containerIdLock.lock()
            val pipelineContainer = pipelineContainerService.getContainer(
                projectId = projectId,
                buildId = event.buildId,
                stageId = stageId,
                containerId = containerId
            ) ?: run {
                LOG.warn("ENGINE|$buildId|${event.source}|$stageId|j($containerId)|bad container")
                return
            }
            // 调整Container状态位
            val containerBuildStatus = BuildStatus.parse(container.status)
            // 取消构建,如果actionType不为TERMINATE那么当前运行的stage及当前stage下的job不能马上置为取消状态
            if (event.actionType == ActionType.TERMINATE ||
                containerBuildStatus != BuildStatus.RUNNING || // 运行中的返回Stage流程进行闭环处理
                dependOnControl.dependOnJobStatus(pipelineContainer) != BuildStatus.SUCCEED // 非运行中的判断是否有依赖
            ) {
                // 删除redis中取消构建操作标识
                redisOperation.delete(BuildUtils.getCancelActionBuildKey(buildId))
                redisOperation.delete(TaskUtils.getCancelTaskIdRedisKey(buildId, containerId, false))
                // 更新job状态
                val switchedStatus = BuildStatusSwitcher.jobStatusMaker.cancel(containerBuildStatus)
                pipelineContainerService.updateContainerStatus(
                    projectId = projectId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    startTime = null,
                    endTime = LocalDateTime.now(),
                    buildStatus = switchedStatus
                )
                containerBuildRecordService.updateContainerStatus(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    containerId = containerId,
                    buildStatus = switchedStatus,
                    executeCount = executeCount,
                    operation = "cancelContainerPendingTask#${container.containerId}"
                )
                // 释放互斥锁
                unlockMutexGroup(
                    variables = variables, container = container, pipelineId = event.pipelineId,
                    buildId = event.buildId, projectId = event.projectId, stageId = stageId
                )
                // 构建机关机
                if (container is VMBuildContainer) {
                    container.shutdown(event = event, executeCount = executeCount)
                } else if (container is NormalContainer) { // 非编译环境关机
                    container.shutdown(event = event, executeCount = executeCount)
                }
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "[$executeCount]|Job#${container.id} was cancel by ${event.userId}",
                    tag = VMUtils.genStartVMTaskId(container.id!!),
                    jobId = container.containerHashId,
                    executeCount = executeCount
                )
                buildLogPrinter.stopLog(
                    buildId = buildId,
                    tag = VMUtils.genStartVMTaskId(container.id!!),
                    jobId = container.containerHashId,
                    executeCount = executeCount
                )
            }
        } finally {
            containerIdLock.unlock()
        }
    }

    private fun getStageExecuteBusFlag(tmpStage: Stage): Boolean {
        val containers = tmpStage.containers
        val containerSize = containers.size - 1
        var flag = false
        for (i in 0..containerSize) {
            val container = containers[i]
            if (!container.status.isNullOrBlank() && container.status != BuildStatus.UNEXEC.name) {
                // stage下有容器执行过业务，跳出循环
                flag = true
                break
            }
        }
        return flag
    }

    private fun NormalContainer.shutdown(event: PipelineBuildCancelEvent, executeCount: Int) {
        pipelineMQEventDispatcher.dispatch(
            PipelineBuildLessShutdownDispatchEvent(
                source = "BuildCancelControl",
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                userId = event.userId,
                buildId = event.buildId,
                buildResult = true,
                vmSeqId = id,
                executeCount = executeCount
            )
        )
    }

    private fun VMBuildContainer.shutdown(event: PipelineBuildCancelEvent, executeCount: Int) {
        pipelineMQEventDispatcher.dispatch(
            PipelineAgentShutdownEvent(
                source = "BuildCancelControl",
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                userId = event.userId,
                buildId = event.buildId,
                buildResult = false, // #5046 取消不是成功
                vmSeqId = id,
                routeKeySuffix = dispatchType?.routeKeySuffix?.routeKeySuffix,
                executeCount = executeCount
            )
        )
    }

    private fun unlockMutexGroup(
        container: Container,
        buildId: String,
        pipelineId: String,
        projectId: String,
        stageId: String,
        variables: Map<String, String>
    ) {

        val mutexGroup = when (container) {
            is VMBuildContainer -> mutexControl.decorateMutexGroup(container.mutexGroup, variables)
            is NormalContainer -> mutexControl.decorateMutexGroup(container.mutexGroup, variables)
            else -> null
        }

        if (mutexGroup?.enable == true && !mutexGroup.mutexGroupName.isNullOrBlank()) {
            mutexControl.releaseContainerMutex(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerId = container.id!!,
                mutexGroup = mutexGroup,
                executeCount = container.executeCount
            )
        }
    }
}
