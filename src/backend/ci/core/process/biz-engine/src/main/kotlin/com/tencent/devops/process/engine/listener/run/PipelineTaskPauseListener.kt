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

package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.common.BS_MANUAL_STOP_PAUSE_ATOM
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineTaskPauseEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineTaskPauseService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("LongParameterList")
@Component
class PipelineTaskPauseListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskPauseService: PipelineTaskPauseService,
    private val buildVariableService: BuildVariableService,
    private val dslContext: DSLContext,
    private val buildLogPrinter: BuildLogPrinter
) : BaseListener<PipelineTaskPauseEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineTaskPauseEvent) {
        val taskRecord = pipelineTaskService.getBuildTask(event.projectId, event.buildId, event.taskId)
        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = event.buildId)
        try {
            redisLock.lock()
            // feat: 插件暂停继续需校验关机状态 #6317:没有关机，丢入延迟队列轮询
            if (!checkStopVm(taskRecord!!)) {
                // 重试次数超过阈值, 直接失败
                if (event.retryCount > RETRY_MAX_COUNT) {
                    logger.warn("Pause Continue retryCount fail. ${taskRecord.buildId}|${taskRecord.taskName}")
                    return
                }
                with(event) {
                    delayEvent(taskRecord)
                }
                return
            }

            if (event.actionType == ActionType.REFRESH) {
                taskContinue(taskRecord, event.userId)
            } else if (event.actionType == ActionType.END) {
                taskPauseCancel(task = taskRecord, userId = event.userId)
            }
            // #3400 减少重复DETAIL事件转发， Cancel与Continue之后插件任务执行都会刷新DETAIL
        } catch (ignored: Exception) {
            logger.warn("ENGINE|${event.buildId}|pause task execute fail,$ignored")
        } finally {
            redisLock.unlock()
        }
    }

    private fun taskContinue(task: PipelineBuildTask, userId: String) {
        continuePauseTask(current = task, userId = userId)

        var newElement: Element? = null
        val newElementRecord = pipelineTaskPauseService.getPauseTask(
            projectId = task.projectId,
            buildId = task.buildId,
            taskId = task.taskId
        )
        if (newElementRecord != null) {
            newElement = JsonUtil.to(newElementRecord.newValue, Element::class.java)
            newElement.executeCount = task.executeCount ?: 1
            // 修改插件运行设置
            pipelineTaskService.updateTaskParamWithElement(
                projectId = task.projectId,
                buildId = task.buildId,
                taskId = task.taskId,
                newElement = newElement
            )
            logger.info("update task param success | ${task.buildId}| ${task.taskId} ")
        }

        // 修改详情model
        taskBuildRecordService.taskPauseContinue(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId,
            executeCount = task.executeCount ?: 1,
            taskId = task.taskId,
            element = newElement
        )
        // issues_6210 添加继续操作操作人变量
        buildVariableService.saveVariable(
            dslContext = dslContext,
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            name = "${VALUE_KEY}_${task.taskId}",
            value = userId
        )

        // 触发引擎container事件，继续后续流程
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "pauseContinue",
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                stageId = task.stageId,
                pipelineId = task.pipelineId,
                buildId = task.buildId,
                userId = userId,
                projectId = task.projectId,
                actionType = ActionType.REFRESH,
                containerType = ""
            )
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId,
            message = "[${task.taskName}] processed. user: $userId, action: continue",
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
    }

    private fun taskPauseCancel(task: PipelineBuildTask, userId: String) {
        logger.info("${task.buildId}|task cancel|${task.taskId}|CANCELED")
        // 修改插件状态位运行
        pipelineTaskService.updateTaskStatus(task = task, userId = userId, buildStatus = BuildStatus.CANCELED)

        // 刷新detail内model
        taskBuildRecordService.taskPauseCancel(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId,
            taskId = task.taskId,
            executeCount = task.executeCount ?: 1,
            cancelUser = userId // fix me: 是否要直接更新取消人，暂时维护原有逻辑
        )

        buildLogPrinter.addYellowLine(
            buildId = task.buildId,
            message = "[${task.taskName}] processed. user: $userId, action: terminate",
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        val containerRecord = pipelineContainerService.getContainer(
            projectId = task.projectId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId
        )

        // 刷新stage状态
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = BS_MANUAL_STOP_PAUSE_ATOM,
                actionType = ActionType.END,
                pipelineId = task.pipelineId,
                projectId = task.projectId,
                userId = userId,
                buildId = task.buildId,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                stageId = task.stageId,
                containerType = containerRecord?.containerType ?: "vmBuild"
            ),
            PipelineBuildStatusBroadCastEvent(
                source = "pauseCancel-${task.containerId}-${task.buildId}",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = task.starter,
                buildId = task.buildId,
                taskId = null,
                stageId = null,
                actionType = ActionType.END
            )
        )
    }

    private fun continuePauseTask(current: PipelineBuildTask, userId: String) {
        logger.info("ENGINE|${current.buildId}]|PAUSE|${current.stageId}]|j(${current.containerId}|${current.taskId}")

        // 将启动和结束任务置为排队。用于启动构建机
        val taskRecords = pipelineTaskService.getAllBuildTask(current.projectId, current.buildId)
        val startAndEndTask = mutableListOf<PipelineBuildTask>()
        taskRecords.forEach { task ->
            if (task.containerId == current.containerId && task.stageId == current.stageId) {
                if (task.taskId == current.taskId) {
                    startAndEndTask.add(task)
                } else if (task.taskName.startsWith(VMUtils.getCleanVmLabel()) &&
                    task.taskId.startsWith(VMUtils.getStopVmLabel())) {
                    startAndEndTask.add(task)
                } else if (task.taskName.startsWith(VMUtils.getPrepareVmLabel()) &&
                    task.taskId.startsWith(VMUtils.getStartVmLabel())) {
                    startAndEndTask.add(task)
                } else if (task.taskName.startsWith(VMUtils.getWaitLabel()) &&
                    task.taskId.startsWith(VMUtils.getEndLabel())) {
                    startAndEndTask.add(task)
                }
            }
        }

        startAndEndTask.forEach {
            pipelineTaskService.updateTaskStatus(task = it, userId = userId, buildStatus = BuildStatus.QUEUE)
            logger.info("update|${current.buildId}|${it.taskId}|task status from ${it.status} to ${BuildStatus.QUEUE}")
        }

        // 修改容器状态位运行
        pipelineContainerService.updateContainerStatus(
            projectId = current.projectId,
            buildId = current.buildId,
            stageId = current.stageId,
            containerId = current.containerId,
            buildStatus = BuildStatus.QUEUE
        )
    }

    private fun checkStopVm(task: PipelineBuildTask): Boolean {
        val projectId = task.projectId
        val buildId = task.buildId
        val jobId = task.containerId
        // 查找job所属job对应的关机插件是否完成
        val jobTaskInfos = pipelineTaskService.listContainerBuildTasks(
            projectId = projectId,
            buildId = buildId,
            containerSeqId = jobId
        )
        jobTaskInfos.forEach {
            if (it.taskId.startsWith(VMUtils.getStopVmLabel())) {
                if (it.status.isFinish()) {
                    logger.info("PauseListener checkStopVm $projectId|$buildId|$jobId| success")
                    return true
                } else return@forEach
            }
        }
        logger.warn("PauseListener checkStopVm $projectId|$buildId|$jobId| fail")
        return false
    }

    private fun PipelineTaskPauseEvent.delayEvent(task: PipelineBuildTask) {
        // 默认延迟3秒再继续丢入队列
        val loopDelayMills =
            if (task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] != null) {
                task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS].toString().trim().toInt()
            } else {
                DELAY_SIZE
            }
        // 重试延迟时间随着次数添加延迟时间
        this.delayMills += loopDelayMills
        this.retryCount += 1
        logger.info("taskContinue delay${this.delayMills}. ${this.buildId}| ${this.containerId}| ${task.taskName}")
        pipelineEventDispatcher.dispatch(this)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTaskPauseListener::class.java)
        private const val VALUE_KEY = "BK_CI_OPERATOR"
        private const val DELAY_SIZE = 3000
        private const val RETRY_MAX_COUNT = 5
    }
}
