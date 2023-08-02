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

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.VERSION
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BACKGROUND_SERVICE_RUNNING_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BACKGROUND_SERVICE_TASK_EXECUTION
import com.tencent.devops.process.engine.control.VmOperateTaskGenerator
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.UpdateTaskInfo
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class TaskAtomService @Autowired(required = false) constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineTaskService: PipelineTaskService,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val buildVariableService: BuildVariableService,
    private val jmxElements: JmxElements,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val client: Client
) {

    /**
     * 启动插件任务[task], 并返回[AtomResponse]
     */
    fun start(task: PipelineBuildTask): AtomResponse {
        val startTime = System.currentTimeMillis()
        jmxElements.execute(task.taskType)
        var atomResponse = AtomResponse(BuildStatus.FAILED)
        try {
            if (!VmOperateTaskGenerator.isVmAtom(task)) {
                dispatchBroadCastEvent(task, ActionType.START)
            }
            // 更新状态
            pipelineTaskService.updateTaskStatus(
                task = task,
                userId = task.starter,
                buildStatus = BuildStatus.RUNNING
            )
            // 插件状态变化-启动（排除VM控制的启动插件）
            if (!VMUtils.isVMTask(task.taskId)) taskBuildRecordService.taskStart(
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                buildId = task.buildId,
                taskId = task.taskId,
                executeCount = task.executeCount ?: 1
            )
            val runVariables = buildVariableService.getAllVariable(task.projectId, task.pipelineId, task.buildId)
            // 动态加载内置插件业务逻辑并执行
            atomResponse = SpringContextUtil.getBean(IAtomTask::class.java, task.taskAtom).execute(task, runVariables)
        } catch (t: BuildTaskException) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            atomResponse.errorType = t.errorType
            atomResponse.errorCode = t.errorCode
            atomResponse.errorMsg = MessageUtil.getMessageByLocale(
                ERROR_BACKGROUND_SERVICE_TASK_EXECUTION,
                I18nUtil.getDefaultLocaleLanguage()
            )
        } catch (ignored: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${ignored.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            atomResponse.errorType = ErrorType.SYSTEM
            atomResponse.errorCode = ErrorCode.SYSTEM_DAEMON_INTERRUPTED
            atomResponse.errorMsg = MessageUtil.getMessageByLocale(
                ERROR_BACKGROUND_SERVICE_RUNNING_ERROR,
                I18nUtil.getDefaultLocaleLanguage()
            )
            logger.warn("[${task.buildId}]|Fail to execute the task [${task.taskName}]", ignored)
        } finally {
            taskAfter(atomResponse, task, startTime)
        }
        return atomResponse
    }

    private fun dispatchBroadCastEvent(task: PipelineBuildTask, actionType: ActionType) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "task-${task.taskId}",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = task.starter,
                taskId = task.taskId,
                buildId = task.buildId,
                actionType = actionType
            )
        )
    }

    /**
     * 插件启动之后的处理，根据插件返回的结果[atomResponse]判断是否要结束任务[task]
     */
    private fun taskAfter(atomResponse: AtomResponse, task: PipelineBuildTask, startTime: Long) {
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
        if (atomResponse.buildStatus.isFinish()) {
            atomResponse.errorMsg = CommonUtils.interceptStringInLength(
                atomResponse.errorMsg, PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
            ) // 文本长度保护
            taskEnd(atomResponse = atomResponse, task = task, startTime = startTime)
        }
    }

    /**
     * 插件任务[task]结束时做的业务处理，启动时间[startTime]毫秒，
     */
    fun taskEnd(task: PipelineBuildTask, startTime: Long, atomResponse: AtomResponse) {
        try {
            // 更新状态
            pipelineTaskService.updateTaskStatus(
                task = task,
                userId = task.starter,
                buildStatus = atomResponse.buildStatus,
                errorType = atomResponse.errorType,
                errorCode = atomResponse.errorCode,
                errorMsg = atomResponse.errorMsg
            )
            // 系统控制类插件不涉及到Detail编排状态修改
            if (EnvControlTaskType.parse(task.taskType) == null) {
                val taskParams = task.taskParams
                val taskVersion = taskParams[VERSION].toString()
                val atomCode = taskParams[KEY_ATOM_CODE].toString()
                val taskType = task.taskType
                val marketAtomFlag = taskType == MarketBuildAtomElement.classType ||
                    taskType == MarketBuildLessAtomElement.classType
                val projectId = task.projectId
                val atomVersion = if (marketAtomFlag) {
                    if (taskVersion.contains("*")) {
                        // 市场插件如果选择最新版本号需将版本号转换为具体的版本号
                        client.get(ServiceAtomResource::class).getAtomRealVersion(projectId, atomCode, taskVersion).data
                    } else {
                        taskVersion
                    }
                } else {
                    INIT_VERSION
                }
                val endParam = TaskBuildEndParam(
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    containerId = task.containerId,
                    taskId = task.taskId,
                    executeCount = task.executeCount ?: 1,
                    buildStatus = atomResponse.buildStatus,
                    errorType = atomResponse.errorType,
                    errorCode = atomResponse.errorCode,
                    errorMsg = atomResponse.errorMsg,
                    atomVersion = atomVersion
                )
                val updateTaskStatusInfos = taskBuildRecordService.taskEnd(endParam)
                updateTaskStatusInfos.forEach { updateTaskStatusInfo ->
                    pipelineTaskService.updateTaskStatusInfo(
                        task = task,
                        updateTaskInfo = UpdateTaskInfo(
                            projectId = task.projectId,
                            buildId = task.buildId,
                            executeCount = task.executeCount ?: 1,
                            taskId = updateTaskStatusInfo.taskId,
                            taskStatus = updateTaskStatusInfo.buildStatus
                        )
                    )
                    if (!updateTaskStatusInfo.message.isNullOrBlank()) {
                        buildLogPrinter.addLine(
                            buildId = task.buildId,
                            message = updateTaskStatusInfo.message!!,
                            tag = updateTaskStatusInfo.taskId,
                            jobId = updateTaskStatusInfo.containerHashId,
                            executeCount = updateTaskStatusInfo.executeCount
                        )
                    }
                }
                measureService?.postTaskData(
                    task = task,
                    startTime = task.startTime?.timestampmilli() ?: startTime,
                    status = atomResponse.buildStatus,
                    type = task.taskType,
                    errorType = atomResponse.errorType?.name,
                    errorCode = atomResponse.errorCode,
                    errorMsg = atomResponse.errorMsg
                )
                if (atomResponse.buildStatus.isFailure()) {
                    jmxElements.fail(task.taskType)
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to post the task($task): ${ignored.message}")
        }

        if (!VmOperateTaskGenerator.isVmAtom(task)) {
            dispatchBroadCastEvent(task, ActionType.END)
        }

        buildLogPrinter.stopLog(
            buildId = task.buildId,
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount
        )
    }

    /**
     * 尝试结束插件任务[task], actionType为TERMINATE表示是强制结束，如果为其它值则表示尝试探测是否结束，但不会做强制结束
     */
    fun tryFinish(task: PipelineBuildTask, actionType: ActionType): AtomResponse {
        val startTime = System.currentTimeMillis()
        // #8879 被动终止的插件应该设为取消状态
        var atomResponse = if (actionType.isTerminate()) {
            AtomResponse(BuildStatus.CANCELED)
        } else {
            AtomResponse(BuildStatus.FAILED)
        }
        try {
            val runVariables = buildVariableService.getAllVariable(task.projectId, task.pipelineId, task.buildId)
            // 动态加载插件业务逻辑
            val iAtomTask = SpringContextUtil.getBean(IAtomTask::class.java, task.taskAtom)
            atomResponse = iAtomTask.tryFinish(task = task, runVariables = runVariables, actionType = actionType)
            val runCondition = task.additionalOptions?.runCondition
            val stopFlag = actionType == ActionType.END && runCondition != RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
            log(atomResponse, task, stopFlag)
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
        } catch (ignored: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] has exception: ${ignored.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("[${task.buildId}]|Fail to execute the task [${task.taskName}]", ignored)
            atomResponse.errorType = ErrorType.SYSTEM
            atomResponse.errorMsg = ignored.message
        } finally {
            taskAfter(atomResponse, task, startTime)
        }
        return atomResponse
    }

    private fun log(atomResponse: AtomResponse, task: PipelineBuildTask, stopFlag: Boolean) {
        if (atomResponse.buildStatus.isFinish()) {
            buildLogPrinter.addLine(
                buildId = task.buildId,
                message = "Task [${task.taskName}] ${atomResponse.buildStatus}!",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
        } else {
            if (stopFlag) {
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

    /**
     * 检测插件任务[buildTask]是否是在构建机上运行
     */
    fun runByVmTask(buildTask: PipelineBuildTask): Boolean {
        // 非官方内置的原子任务不在此运行
        return buildTask.taskAtom.isBlank()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskAtomService::class.java)
    }
}
