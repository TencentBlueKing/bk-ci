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

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.PipelineTaskService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.store.pojo.common.ATOM_POST_EXECUTE_TIP
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StartActionTaskContainerCmd(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineTaskService: PipelineTaskService
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(StartActionTaskContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: ContainerContext) {
        val actionType = commandContext.event.actionType
        when {
            ActionType.isStart(actionType) || ActionType.REFRESH == actionType -> {
                findStartTask(commandContext)?.sendTask(event = commandContext.event)
            }
            ActionType.isEnd(actionType) -> {
                findEndTask(commandContext)?.sendTask(event = commandContext.event)
            }
            else -> { // 未规定的类型，打回Stage处理
                commandContext.buildStatus = BuildStatus.UNKNOWN
                commandContext.latestSummary = "j(${commandContext.container.containerId}) unknown action: $actionType"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
        }
    }

    private fun findStartTask(containerContext: ContainerContext): PipelineBuildTask? {
        var toDoTask: PipelineBuildTask? = null
        var containerFinalStatus: BuildStatus = BuildStatus.SUCCEED
        var hasFailedTaskInSuccessContainer = false
        var startVMFail = false
        var breakFlag = false
        val source = containerContext.event.source
        val stageId = containerContext.event.stageId
        val buildId = containerContext.container.buildId
        val containerId = containerContext.container.containerId

        for (t in containerContext.containerTasks) {
            LOG.info("ENGINE|$buildId|$source|CONTAINER_TASK|$stageId|j($containerId)|" +
                "t(${t.taskId})|${t.taskName}|${t.status}")
            // 此处pause状态由构建机[PipelineVMBuildService.claim]认领任务遇到需要暂停任务时更新为PAUSE。
            if (t.status.isPause()) { // 若为暂停，则要确保拿到的任务为stopVM-关机或者空任务发送next stage任务
                toDoTask = t.pauseTaskFindNextTask(containerContext.containerTasks)
                containerFinalStatus = BuildStatus.PAUSE
            } else if (t.status.isRunning()) { // 容器中的任务要求串行执行，所以再次启动会直接当作成功结束返回。
                breakFlag = true
            } else if (t.status.isFailure()) {
                // 当前任务已经失败，并且没有设置失败时继续的， 将当前状态设置给容器最终状态
                if (!ControlUtils.continueWhenFailure(t.additionalOptions)) {
                    containerFinalStatus = t.status
                    startVMFail = startVMFail || TaskUtils.isStartVMTask(t) // 有一个true，就是失败
                } else {
                    hasFailedTaskInSuccessContainer = true
                }
            } else if (t.status.isReadyToRun()) {
                // 拿到按序号排列的第一个待执行的插件
                toDoTask = t.checkReadyToRunTask(
                    containerTasks = containerContext.containerTasks,
                    hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer,
                    containerFinalStatus = containerFinalStatus,
                    containerContext = containerContext,
                    startVMFail = startVMFail
                )
                if (toDoTask != null) {
                    containerFinalStatus = BuildStatus.RUNNING
                }
            }

            if (toDoTask != null || breakFlag) {
                break
            }
        }

        containerContext.buildStatus = containerFinalStatus
        LOG.info("ENGINE|$buildId|$source|CONTAINER_FIND_S_TASK|$stageId|j($containerId)|" +
            "${toDoTask?.taskId}|$containerFinalStatus|$startVMFail")
        if (breakFlag) { // 结束并跳过后续其他命令的执行
            containerContext.cmdFlowState = CmdFlowState.BREAK
        } else if (toDoTask == null) {
            containerContext.cmdFlowState = CmdFlowState.FINALLY
        }
        return toDoTask
    }

    private fun PipelineBuildTask.checkReadyToRunTask(
        containerTasks: List<PipelineBuildTask>,
        hasFailedTaskInSuccessContainer: Boolean,
        containerFinalStatus: BuildStatus,
        containerContext: ContainerContext,
        startVMFail: Boolean
    ): PipelineBuildTask? {
        val source = containerContext.event.source
        var toDoTask: PipelineBuildTask? = null
        when { // [post action] 包含对应的关机任务，优先开机失败startVMFail=true
            additionalOptions?.elementPostInfo != null -> { // 如果是[post task], elementPostInfo必不为空
                toDoTask = additionalOptions?.elementPostInfo?.checkPostAction(
                    containerTasks = containerTasks,
                    task = this,
                    startVMFail = startVMFail,
                    isContainerFailed = containerFinalStatus.isFailure(),
                    hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer
                )
                LOG.info("ENGINE|$buildId|$source|CONTAINER_POST_TASK|$stageId|j($containerId)|post=${toDoTask?.taskId}")
            }
            startVMFail -> { // 构建环境启动失败的，
                LOG.warn("ENGINE|$buildId|$source|CONTAINER_FAIL_VM|$stageId|j($containerId)|$taskId|$status")
                // 更新任务状态为跳过
                pipelineRuntimeService.updateTaskStatus(
                    buildId = buildId, taskId = taskId, userId = starter, buildStatus = BuildStatus.UNEXEC
                )
                // 更新编排模型状态
                pipelineBuildDetailService.taskEnd(buildId = buildId, taskId = taskId, buildStatus = BuildStatus.UNEXEC)
                // 打印构建日志
                buildLogPrinter.addYellowLine(executeCount = containerContext.executeCount, tag = taskId,
                    buildId = buildId, message = "Plugin [$taskName] unexec", jobId = containerHashId
                )
            }
            ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = additionalOptions,
                containerFinalStatus = containerFinalStatus,
                variables = containerContext.variables,
                hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer
            ) -> { // 检查条件跳过
                LOG.warn("ENGINE|$buildId|$source|CONTAINER_SKIP_TASK|$stageId|j($containerId)|$taskId")
                // 更新任务状态为跳过
                pipelineRuntimeService.updateTaskStatus(
                    buildId = buildId, taskId = taskId, userId = starter, buildStatus = BuildStatus.SKIP
                )
                // 更新编排模型状态
                pipelineBuildDetailService.taskEnd(buildId = buildId, taskId = taskId, buildStatus = BuildStatus.SKIP)
                // 打印构建日志
                buildLogPrinter.addYellowLine(executeCount = containerContext.executeCount, tag = taskId,
                    buildId = buildId, message = "Plugin [$taskName] was skipped", jobId = containerHashId
                )
            }
            else -> {
                toDoTask = this // 当前排队的任务晋级为下一个执行任务
            }
        }

        return toDoTask
    }

    /**
     * 从[containerTaskList]任务列表查找到暂停任务后的下一个待执行任务:
     * [PipelineBuildTask]的waitToDoTask可能会是null，或者是暂停状态下，将构建机环境停机的stopVM-xxx的任务
     */
    private fun PipelineBuildTask.pauseTaskFindNextTask(
        containerTaskList: Collection<PipelineBuildTask>
    ): PipelineBuildTask? {

        var waitToDoTask: PipelineBuildTask? = null

        val pipelineBuildTask = containerTaskList
            .filter { it.taskId.startsWith(VMUtils.getStopVmLabel()) } // 找构建环境关机任务
            .getOrNull(0) // 取第一个关机任务，如果没有返回空

        if (pipelineBuildTask?.status?.isFinish() == false) { // 如果未执行过，则取该任务作为后续执行任务
            waitToDoTask = pipelineBuildTask
            LOG.info("ENGINE|$buildId|findStartTask|PAUSE|$stageId|j($containerId)|$taskId|Next=${waitToDoTask.taskName}")

            pipelineTaskService.pauseBuild(
                buildId = buildId,
                taskId = taskId,
                stageId = stageId,
                containerId = containerId
            )
        }

        return waitToDoTask
    }

    fun ElementPostInfo.checkPostAction(
        containerTasks: List<PipelineBuildTask>,
        task: PipelineBuildTask,
        startVMFail: Boolean,
        isContainerFailed: Boolean,
        hasFailedTaskInSuccessContainer: Boolean
    ): PipelineBuildTask? {
        if (startVMFail) { // 构建机启动失败时，[post action]只适用于构建环境关机任务
            if (task.taskId != VMUtils.genStopVMTaskId(task.taskSeq)) {
                return null
            }
        }
        // 添加Post action标识打印
        addPostTipLog(task)
        // 查询父任务及是否允许当前的post action 任务执行标识
        val (parentTask, postExecuteFlag) = TaskUtils.getPostTaskAndExecuteFlag(
            taskList = containerTasks,
            task = task,
            isContainerFailed = isContainerFailed,
            hasFailedTaskInInSuccessContainer = hasFailedTaskInSuccessContainer
        )

        var waitToDoTask: PipelineBuildTask? = null
        if (parentTask != null && !postExecuteFlag) {
            val parentTaskSkipFlag = parentTask.status == BuildStatus.SKIP
            // 如果post任务的主体任务状态是SKIP，则该post任务的状态也应该置为SKIP
            val buildStatus = if (parentTaskSkipFlag) BuildStatus.SKIP else BuildStatus.UNEXEC
            val message = if (parentTaskSkipFlag) "Plugin [${task.taskName}] was skipped"
            else "Post action execution conditions (expectation: $postCondition), not executed"
            // 更新排队中的post任务的构建状态
            pipelineRuntimeService.updateTaskStatus(
                buildId = task.buildId, taskId = task.taskId, userId = task.starter, buildStatus = buildStatus
            )
            buildLogPrinter.addYellowLine(
                buildId = task.buildId,
                message = message,
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
        }
        if (parentTask != null && postExecuteFlag) {
            waitToDoTask = task
        }

        return waitToDoTask
    }

    private fun ElementPostInfo.addPostTipLog(task: PipelineBuildTask) {
        buildLogPrinter.addLine(
            buildId = task.buildId,
            message = MessageCodeUtil.getCodeMessage(
                messageCode = ATOM_POST_EXECUTE_TIP,
                params = arrayOf(
                    (parentElementJobIndex + 1).toString(),
                    parentElementName
                )
            ) ?: "",
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
    }

    private fun PipelineBuildTask.sendTask(event: PipelineBuildContainerEvent) {
        LOG.info("ENGINE|$buildId|CONTAINER_SEND|$stageId|j($containerId)|${event.actionType}|$taskId|$taskName")
        pipelineEventDispatcher.dispatch(
            PipelineBuildAtomTaskEvent(
                source = "CONTAINER_${event.actionType}",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = event.userId,
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                containerType = containerType,
                taskId = taskId,
                taskParam = taskParams,
                actionType = event.actionType
            )
        )
    }

    private fun findEndTask(containerContext: ContainerContext): PipelineBuildTask? {
        var toDoTask: PipelineBuildTask? = null
        var containerFinalStatus: BuildStatus = BuildStatus.FAILED
        var hasFailedTaskInSuccessContainer = false
        var startVMFail = false
        val event = containerContext.event
        val source = event.source
        val stageId = event.stageId
        val buildId = containerContext.container.buildId
        val containerId = containerContext.container.containerId

        for (t in containerContext.containerTasks) {
            LOG.info("ENGINE|$buildId|$source|CONTAINER_TASK|$stageId|j($containerId)|" +
                "t(${t.taskId})|${t.taskName}|${t.status}")
            when {
                t.status.isRunning() -> { // 运行中的设置为失败
                    containerFinalStatus = BuildStatus.FAILED
                    t.setUpFail(event, containerFinalStatus)
                    toDoTask = t
                }
                t.status.isFailure() -> {
                    if (!ControlUtils.continueWhenFailure(t.additionalOptions)) {
                        containerFinalStatus = t.status
                        startVMFail = startVMFail || TaskUtils.isStartVMTask(t)
                    } else {
                        hasFailedTaskInSuccessContainer = true
                    }
                }
                t.status.isReadyToRun() -> { // 直接置为未执行
                    toDoTask = t.checkReadyToRunTask(
                        containerTasks = containerContext.containerTasks,
                        hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer,
                        containerFinalStatus = containerFinalStatus,
                        containerContext = containerContext,
                        startVMFail = startVMFail
                    )
                    if (toDoTask != null) {
                        containerFinalStatus = BuildStatus.RUNNING
                    }
                }
                else -> containerFinalStatus = BuildStatus.FAILED
            }

            if (toDoTask != null) {
                break
            }
        }

        containerContext.buildStatus = containerFinalStatus
        if (toDoTask == null) {
            containerContext.cmdFlowState = CmdFlowState.FINALLY
        }
        LOG.info("ENGINE|$buildId|$source|CONTAINER_FIND_E_TASK|$stageId|j($containerId)|${toDoTask?.taskId}" +
            "|$containerFinalStatus|$startVMFail")
        return toDoTask
    }

    private fun PipelineBuildTask.setUpFail(event: PipelineBuildContainerEvent, containerFinalStatus: BuildStatus) {
        val message = event.reason
        buildLogPrinter.addRedLine(
            buildId = buildId, message = "Terminate Plugin[$taskName]: $message",
            tag = taskId, jobId = containerHashId, executeCount = executeCount ?: 1
        )
        if (event.timeout == true) {
            pipelineBuildDetailService.taskEnd(
                buildId = buildId,
                taskId = taskId,
                buildStatus = containerFinalStatus,
                canRetry = true,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_JOB_OUTTIME_LIMIT,
                errorMsg = message ?: "Job执行时间超过限制"
            )
            // Job超时错误存于startVM插件中
            pipelineRuntimeService.setTaskErrorInfo(
                buildId = buildId,
                taskId = VMUtils.genStartVMTaskId(containerId),
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_JOB_OUTTIME_LIMIT,
                errorMsg = message ?: "Job执行时间超过限制"
            )
        } else {
            pipelineBuildDetailService.taskEnd(
                buildId = buildId,
                taskId = taskId,
                buildStatus = containerFinalStatus,
                canRetry = true,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                errorMsg = message ?: "插件执行意外终止"
            )
        }
    }
}
