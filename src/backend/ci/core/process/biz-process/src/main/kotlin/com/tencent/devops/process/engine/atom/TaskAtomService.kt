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

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TaskAtomService @Autowired(required = false) constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val buildVariableService: BuildVariableService,
    private val jmxElements: JmxElements,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    @Autowired(required = false)
    private val measureService: MeasureService?
) {

    fun start(task: PipelineBuildTask): AtomResponse {
        val startTime = System.currentTimeMillis()
        val elementType = task.taskType

        jmxElements.execute(elementType)
        var atomResponse = AtomResponse(BuildStatus.FAILED)
        try {
            // 更新状态
            pipelineRuntimeService.updateTaskStatus(task.buildId, task.taskId, task.starter, BuildStatus.RUNNING)
            pipelineBuildDetailService.taskStart(task.buildId, task.taskId)
            val runVariables = buildVariableService.getAllVariable(task.buildId)

            atomResponse = if (task.isSkip(runVariables)) { // 跳过
                AtomResponse(BuildStatus.SKIP)
            } else {
                SpringContextUtil.getBean(IAtomTask::class.java, task.taskAtom).execute(task, runVariables)
            }
        } catch (t: BuildTaskException) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = t.errorType,
                errorCode = t.errorCode,
                errorMsg = "后台服务任务执行出错"
            )
            logger.warn("[${task.buildId}]|Fail to execute the task [${task.taskName}]", t)
        } catch (t: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_DAEMON_INTERRUPTED,
                errorMsg = "后台服务运行出错"
            )
            logger.warn("[${task.buildId}]|Fail to execute the task [${task.taskName}]", t)
        } finally {
            // 文本长度保护
            atomResponse.errorMsg = CommonUtils.interceptStringInLength(atomResponse.errorMsg, PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX)
            // 存储变量
            if (atomResponse.outputVars != null && atomResponse.outputVars!!.isNotEmpty()) {
                buildVariableService.batchUpdateVariable(
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    variables = atomResponse.outputVars!!
                )
            }
            // 循环的是还未结束，直接返回
            if (BuildStatus.isFinish(atomResponse.buildStatus)) {
                taskEnd(
                    status = atomResponse.buildStatus,
                    task = task,
                    startTime = startTime,
                    elementType = elementType,
                    errorType = atomResponse.errorType,
                    errorCode = atomResponse.errorCode,
                    errorMsg = atomResponse.errorMsg
                )
            }
            return atomResponse
        }
    }

    private fun taskEnd(
        status: BuildStatus,
        task: PipelineBuildTask,
        startTime: Long,
        elementType: String,
        errorType: ErrorType?,
        errorCode: Int?,
        errorMsg: String?
    ) {
        try {
            // 更新状态
            pipelineRuntimeService.updateTaskStatus(
                buildId = task.buildId,
                taskId = task.taskId,
                userId = task.starter,
                buildStatus = status,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )
            pipelineBuildDetailService.pipelineTaskEnd(
                buildId = task.buildId,
                taskId = task.taskId,
                buildStatus = status,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )
            measureService?.postTaskData(
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                taskId = task.taskId,
                atomCode = task.atomCode ?: task.taskParams["atomCode"] as String? ?: task.taskType,
                name = task.taskName,
                buildId = task.buildId,
                startTime = task.startTime?.timestampmilli() ?: startTime,
                status = status,
                type = elementType,
                executeCount = task.executeCount,
                errorType = errorType?.name,
                errorCode = errorCode,
                errorMsg = errorMsg,
                userId = task.starter
            )
            if (BuildStatus.isFailure(status)) {
                jmxElements.fail(elementType)
            }
        } catch (ignored: Throwable) {
            logger.error("Fail to post the task($task): ${ignored.message}")
        }
        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "task-end-${task.taskId}",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = task.starter,
                taskId = task.taskId,
                buildId = task.buildId,
                actionType = ActionType.END
            )
        )
        buildLogPrinter.stopLog(
            buildId = task.buildId,
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount
        )
    }

    fun tryFinish(task: PipelineBuildTask, force: Boolean): AtomResponse {
        val startTime = System.currentTimeMillis()
        val elementType = task.taskType
        var atomResponse = AtomResponse(BuildStatus.FAILED)

        try {
            val runVariables = buildVariableService.getAllVariable(task.buildId)
            val iAtomTask = SpringContextUtil.getBean(IAtomTask::class.java, task.taskAtom)
            atomResponse = iAtomTask.tryFinish(task, runVariables, force)

            log(atomResponse, task, force)
        } catch (t: BuildTaskException) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("[${task.buildId}]|Fail to execute the task[${task.taskName}]", t)
            atomResponse.errorType = ErrorType.SYSTEM
            atomResponse.errorMsg = t.message
        } catch (t: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("[${task.buildId}]|Fail to execute the task [${task.taskName}]", t)
            atomResponse.errorType = ErrorType.SYSTEM
            atomResponse.errorMsg = t.message
        } finally {
            // 存储变量
            if (atomResponse.outputVars != null && atomResponse.outputVars!!.isNotEmpty()) {
                buildVariableService.batchUpdateVariable(
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    variables = atomResponse.outputVars!!)
            }
            // 循环的是还未结束，直接返回
            if (BuildStatus.isFinish(atomResponse.buildStatus)) {
                taskEnd(
                    status = atomResponse.buildStatus,
                    task = task,
                    startTime = startTime,
                    elementType = elementType,
                    errorType = atomResponse.errorType,
                    errorCode = atomResponse.errorCode,
                    errorMsg = atomResponse.errorMsg
                )
            }
            return atomResponse
        }
    }

    private fun log(
        atomResponse: AtomResponse,
        task: PipelineBuildTask,
        force: Boolean
    ) {
        if (BuildStatus.isFinish(atomResponse.buildStatus)) {
            buildLogPrinter.addLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] ${atomResponse.buildStatus}!",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
        } else {
            if (force) {
                buildLogPrinter.addLine(
                    buildId = task.buildId,
                    message = "Try to Stop Task [${task.taskName}]...",
                    tag = task.taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
            }
        }
    }

    private fun PipelineBuildTask.isSkip(variables: Map<String, String>): Boolean {
        try {

            val skipValue = variables[SkipElementUtils.getSkipElementVariableName(taskId)]
            if (skipValue != null && (skipValue.toBoolean())) {
                logger.info(
                    "[$buildId]|isSkip|stage=$stageId|containerId=$containerId|taskId=$taskId| " +
                        "is skip of the build"
                )
                return true
            }
        } catch (ignored: Throwable) {
            logger.warn(
                "[$buildId]|isSkip|stage=$stageId|containerId=$containerId|taskId=$taskId| " +
                    "Fail to check if skip", ignored
            )
        }
        return false
    }

    fun runByVmTask(buildTask: PipelineBuildTask): Boolean {
        // 非官方内置的原子任务不在此运行
        if (buildTask.taskAtom.isBlank()) {
            return true
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskAtomService::class.java)
    }
}
