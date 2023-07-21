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

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CONDITION_INVALID
import com.tencent.devops.process.constant.ProcessMessageCode.BK_UNEXECUTE_POSTACTION_TASK
import com.tencent.devops.process.constant.ProcessMessageCode.BK_UNEXECUTE_TASK
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.FastKillUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineTaskStatusInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.PipelineContextService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.store.pojo.common.ATOM_POST_EXECUTE_TIP
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("TooManyFunctions", "LongParameterList", "ComplexMethod")
@Service
class StartActionTaskContainerCmd(
    private val redisOperation: RedisOperation,
    private val pipelineTaskService: PipelineTaskService,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineContextService: PipelineContextService
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(StartActionTaskContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish() &&
            commandContext.container.matrixGroupFlag != true
    }

    override fun execute(commandContext: ContainerContext) {
        val actionType = commandContext.event.actionType
        commandContext.cmdFlowState = CmdFlowState.FINALLY
        when {
            actionType.isStartOrRefresh() || actionType.isEnd() -> {
                if (!actionType.isTerminate()) {
                    commandContext.buildStatus = BuildStatus.SUCCEED
                }
                val waitToDoTask = findTask(commandContext)
                if (waitToDoTask == null) { // 非fast kill的强制终止时到最后无任务，最终状态必定是FAILED
                    val fastKill = FastKillUtils.isFastKillCode(commandContext.event.errorCode)
                    if (!fastKill && actionType.isTerminate() && !commandContext.buildStatus.isFailure()) {
                        commandContext.buildStatus = BuildStatus.FAILED
                    } else {
                        setContextBuildStatus(commandContext)
                    }
                    commandContext.latestSummary += "| status=${commandContext.buildStatus}"
                } else {
                    doWaitToDoTaskBus(waitToDoTask, actionType, commandContext)
                    sendTask(event = commandContext.event, task = waitToDoTask)
                }
            }

            else -> { // 未规定的类型，打回Stage处理
                commandContext.buildStatus = BuildStatus.UNKNOWN
                commandContext.latestSummary = "j(${commandContext.container.containerId}) unknown action: $actionType"
            }
        }
    }

    private fun setContextBuildStatus(commandContext: ContainerContext) {
        val container = commandContext.container
        val runEvenCancelTaskIdKey = ContainerUtils.getContainerRunEvenCancelTaskKey(
            pipelineId = container.pipelineId,
            buildId = container.buildId,
            containerId = container.containerId
        )
        if (redisOperation.get(runEvenCancelTaskIdKey) != null) {
            /**
             * 当前的job如果包含runCondition为PRE_TASK_FAILED_EVEN_CANCEL的task，运行到最后如果状态为SUCCEED，
             * 则需要置为CANCELED
             */
            if (commandContext.buildStatus == BuildStatus.SUCCEED) {
                commandContext.buildStatus = BuildStatus.CANCELED
            }
            redisOperation.delete(runEvenCancelTaskIdKey)
        }
    }

    private fun doWaitToDoTaskBus(
        waitToDoTask: PipelineBuildTask,
        actionType: ActionType,
        commandContext: ContainerContext
    ) {
        // 当task的runCondition为PRE_TASK_FAILED_EVEN_CANCEL且task被用户取消时则写入标志到redis中
        val runCondition = waitToDoTask.additionalOptions?.runCondition
        val failedEvenCancelFlag = runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
        if (actionType == ActionType.END && failedEvenCancelFlag) {
            val container = commandContext.container
            val timeoutSec = Timeout.transMinuteTimeoutToSec(container.controlOption.jobControlOption.timeout)
            redisOperation.set(
                key = ContainerUtils.getContainerRunEvenCancelTaskKey(
                    pipelineId = waitToDoTask.pipelineId,
                    buildId = waitToDoTask.buildId,
                    containerId = waitToDoTask.containerId
                ),
                value = waitToDoTask.taskId,
                expiredInSecond = timeoutSec
            )
        }
    }

    /**
     * 寻找能下发的任务:
     * 如遇到[BuildStatus.isRunning]的任务，如果没有出现错误，则本次不做任何处理，返回null, 使用[CmdFlowState.BREAK]中断后续执行
     * 如遇到[BuildStatus.isPause]需要暂停任务，则查找下一个要执行的插件任务，比如关机插件任务，暂停需要关机
     * 如遇到[BuildStatus.isFailure]失败任务，检查是否开启了「失败继续」,未开启则会把容器标识为失败，继续寻找下一个可以执行的任务
     * 如遇到[BuildStatus.isReadyToRun]待执行任务，则检查是否可以执行，
     *  包括「是否条件跳过」「当前是否构建机启动失败」「Post Action检查」等等，通过方能成为待执行的任务
     */
    @Suppress("ComplexMethod", "NestedBlockDepth", "LongMethod")
    private fun findTask(containerContext: ContainerContext): PipelineBuildTask? {
        val fastKill = FastKillUtils.isFastKillCode(containerContext.event.errorCode)
        var toDoTask: PipelineBuildTask? = null
        var continueWhenFailure = false // 失败继续
        var needTerminate = isTerminate(containerContext) // 是否终止类型
        var breakFlag = false
        val containerTasks = containerContext.containerTasks
        val actionType = containerContext.event.actionType
        for ((index, t) in containerTasks.withIndex()) {
            // 此处pause状态由构建机[PipelineVMBuildService.claim]认领任务遇到需要暂停任务时更新为PAUSE。
            if (t.status.isPause()) { // 若为暂停，则要确保拿到的任务为stopVM-关机或者空任务发送next stage任务
                toDoTask = findNextTaskAfterPause(containerContext, currentTask = t)
                breakFlag = toDoTask == null
                if (!actionType.isStartOrRefresh()) {
                    needTerminate = true
                }
            } else if (t.status.isRunning()) { // 当前有运行中任务
                // 如果是要启动或者刷新, 当前已经有运行中任务，则需要break
                breakFlag = actionType.isStartOrRefresh()
                // 如果是要终止，则需要拿出当前任务进行终止
                toDoTask = findRunningTask(containerContext, currentTask = t)
            } else if (t.status.isFailure() || t.status.isCancel()) {
                needTerminate = needTerminate || TaskUtils.isStartVMTask(t) // #4301 构建机启动失败，就需要终止[P0]
                // 当前任务已经失败or取消，并且没有设置[失败继续]的， 设置给容器最终FAILED状态
                if (!ControlUtils.continueWhenFailure(t.additionalOptions)) {
                    containerContext.buildStatus = BuildStatusSwitcher.jobStatusMaker.forceFinish(t.status, fastKill)
                } else {
                    continueWhenFailure = true
                    if (needTerminate || t.status.isCancel() || t.status.isTerminate()) {
                        // #4301 强制终止的标志为失败，不管是不是设置了失败继续[P0]
                        containerContext.buildStatus = BuildStatusSwitcher.jobStatusMaker.forceFinish(t.status)
                    }
                }
            } else if (t.status.isReadyToRun()) {
                // 拿到按序号排列的第一个必须要执行的插件
                toDoTask = t.findNeedToRunTask(
                    index = index,
                    hasFailedTaskInSuccessContainer = continueWhenFailure,
                    containerContext = containerContext,
                    needTerminate = needTerminate,
                    contextMap = containerContext.variables.plus(
                        pipelineContextService.buildContext(
                            projectId = containerContext.container.projectId,
                            pipelineId = containerContext.container.pipelineId,
                            buildId = containerContext.container.buildId,
                            stageId = containerContext.container.stageId,
                            containerId = containerContext.container.containerId,
                            taskId = t.taskId,
                            variables = containerContext.variables
                        )
                    )
                )
            } else if (t.status == BuildStatus.SKIP && t.endTime == null) { // 手动跳过功能，暂时没有好的解决办法，可改进
                buildLogPrinter.addRedLine(
                    buildId = t.buildId,
                    message = "Plugin[${t.taskName}]: ${t.errorMsg ?: "unknown"}",
                    tag = t.taskId,
                    jobId = t.containerHashId,
                    executeCount = t.executeCount ?: 1
                )
                pipelineTaskService.updateTaskStatus(t, containerContext.event.userId, t.status)
            }

            if (toDoTask != null || breakFlag) {
                break
            }
        }

        LOG.info(
            "ENGINE|${containerContext.event.buildId}|${containerContext.event.source}|CONTAINER_FIND_TASK|" +
                "${containerContext.event.stageId}|j(${containerContext.event.containerId})|" +
                "${toDoTask?.taskId}|break=$breakFlag|needTerminate=$needTerminate"
        )

        if (!needTerminate && breakFlag) {
            // #3400 暂停场景下，Job超时引起的终止，以及FastKill 等对于要求终止的，未必有后续执行，需要结束而不是中断
            containerContext.latestSummary = "action=$actionType"
            containerContext.cmdFlowState = CmdFlowState.BREAK
        }
        return toDoTask
    }

    private fun isTerminate(containerContext: ContainerContext): Boolean {
        return containerContext.event.actionType.isTerminate() ||
            FastKillUtils.isTerminateCode(containerContext.event.errorCode)
    }

    private fun findRunningTask(
        containerContext: ContainerContext,
        currentTask: PipelineBuildTask
    ): PipelineBuildTask? {
        var toDoTask: PipelineBuildTask? = null
        when {
            containerContext.event.actionType.isTerminate() -> { // 终止命令，需要设置失败，并返回
                containerContext.buildStatus = BuildStatus.RUNNING
                toDoTask = currentTask // 将当前任务传给TaskControl做终止
                buildLogPrinter.addRedLine(
                    buildId = toDoTask.buildId,
                    message = "Terminate Plugin[${toDoTask.taskName}]: ${containerContext.event.reason ?: "unknown"}",
                    tag = toDoTask.taskId,
                    jobId = toDoTask.containerHashId,
                    executeCount = toDoTask.executeCount ?: 1
                )
            }

            containerContext.event.actionType.isEnd() -> { // 将当前正在运行的任务传给TaskControl做结束
                containerContext.buildStatus = BuildStatus.RUNNING
                toDoTask = currentTask
            }
        }
        return toDoTask
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun PipelineBuildTask.findNeedToRunTask(
        index: Int,
        hasFailedTaskInSuccessContainer: Boolean,
        containerContext: ContainerContext,
        needTerminate: Boolean,
        contextMap: Map<String, String>
    ): PipelineBuildTask? {
        val source = containerContext.event.source
        var toDoTask: PipelineBuildTask? = null
        if (containerContext.event.actionType == ActionType.END) {
            containerContext.buildStatus = BuildStatus.CANCELED
        }
        val containerTasks = containerContext.containerTasks
        val message = StringBuilder()
        val (needSkip, parseException) = try {
            val checkResult = checkAllSkipQuickly(
                startPos = index,
                containerContext = containerContext,
                contextMap = contextMap,
                hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer,
                message = message
            )
            Pair(checkResult, null)
        } catch (e: ExpressionParseException) {
            buildLogPrinter.addErrorLine(
                message = "[${e.kind}] expression(${e.expression}) of task condition is invalid: ${e.message}",
                buildId = buildId, jobId = containerHashId, tag = taskId, executeCount = executeCount ?: 1
            )
            LOG.warn("ENGINE|$buildId|$source|EXPRESSION_CHECK_FAILED|$stageId|j($containerId)|$taskId|$status", e)
            containerContext.latestSummary = containerContext.latestSummary.plus(
                " | conditionError=${e.kind}: ${e.message}"
            )
            Pair(false, e)
        } catch (ignore: Throwable) {
            buildLogPrinter.addErrorLine(
                message = "[EXPRESSION_ERROR] failed to parse condition(${additionalOptions?.customCondition}) " +
                    "with error: ${ignore.message}",
                buildId = buildId, jobId = containerHashId, tag = taskId, executeCount = executeCount ?: 1
            )
            LOG.error(
                "BKSystemErrorMonitor|findNeedToRunTask|$buildId|$source|" +
                    "EXPRESSION_CHECK_FAILED|$stageId|j($containerId)|$taskId|" +
                    "customCondition=${additionalOptions?.customCondition}",
                ignore
            )
            Pair(false, ignore)
        }
        when { // [post action] 包含对应的关机任务，优先开机失败startVMFail=true
            additionalOptions?.elementPostInfo != null -> { // 如果是[post task], elementPostInfo必不为空
                toDoTask = additionalOptions?.elementPostInfo?.findPostActionTask(
                    containerTasks = containerTasks,
                    currentTask = this,
                    isTerminate = needTerminate,
                    isContainerFailed = containerContext.buildStatus.isFailure(),
                    hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer
                )
                LOG.info("ENGINE|$buildId|$source|CONTAINER_POST_TASK|$stageId|j($containerId)|${toDoTask?.taskId}")
            }

            needTerminate -> { // 构建环境启动失败或者是启动成功但后续Agent挂掉导致心跳超时
                LOG.warn("ENGINE|$buildId|$source|TERM_NOT_EXEC|$stageId|j($containerId)|$taskId|$status")
                val taskStatus = if (status == BuildStatus.QUEUE_CACHE) { // 领取过程中被中断标志为取消
                    BuildStatus.CANCELED
                } else {
                    BuildStatus.UNEXEC
                }
                pipelineTaskService.updateTaskStatus(task = this, userId = starter, buildStatus = taskStatus)
                // 打印构建日志
                message.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_UNEXECUTE_TASK,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + "[$taskName] cause: ${containerContext.latestSummary}!"
                )
            }

            needSkip -> { // 检查条件跳过
                var taskStatus = BuildStatusSwitcher.readyToSkipWhen(containerContext.buildStatus)
                // 将第一个因为构建取消而被设置为UNEXEC状态的插件，重置为取消，作为后续Container状态状态的抓手 #5048
                if (containerContext.firstQueueTaskId == null && containerContext.buildStatus.isCancel()) {
                    taskStatus = BuildStatus.CANCELED
                    containerContext.firstQueueTaskId = this.taskId
                }
                LOG.warn("ENGINE|$buildId|$source|CONTAINER_SKIP_TASK|$stageId|j($containerId)|$taskId|$taskStatus")
                // 更新任务状态
                pipelineTaskService.updateTaskStatus(task = this, userId = starter, buildStatus = taskStatus)
                val endParam = TaskBuildEndParam(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    containerId = containerId,
                    taskId = taskId,
                    executeCount = executeCount ?: 1,
                    buildStatus = taskStatus
                )
                val updateTaskStatusInfos = taskBuildRecordService.taskEnd(endParam)
                refreshTaskStatus(updateTaskStatusInfos, index, containerTasks)
                message.insert(0, "[$taskName]").append(" | summary=${containerContext.latestSummary}")
            }

            parseException != null -> { // 如果执行条件判断是出错则直接将插件设为失败
                taskBuildRecordService.updateTaskStatus(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = taskId,
                    executeCount = executeCount ?: 1,
                    buildStatus = BuildStatus.FAILED,
                    operation = "taskConditionInvalid"
                )
                pipelineTaskService.updateTaskStatus(
                    task = this,
                    userId = starter,
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ProcessMessageCode.ERROR_CONDITION_EXPRESSION_PARSE.toInt(),
                    errorMsg = parseException.message
                )
                // 打印构建日志
                message.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_CONDITION_INVALID,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + "[$taskName] cause: ${containerContext.latestSummary}!"
                )
            }

            else -> {
                toDoTask = this // 当前排队的任务晋级为下一个执行任务
            }
        }

        if (message.isNotBlank()) {
            // #6366 增加日志明确展示跳过的原因
            // 打印构建日志--DEBUG级别日志，平时隐藏
            buildLogPrinter.addDebugLine(
                executeCount = containerContext.executeCount, tag = taskId,
                buildId = buildId, jobId = containerHashId, message = message.toString()
            )
        }

        if (toDoTask != null) {
            val msg = TaskUtils.parseTimeout(toDoTask, contextMap)
            buildLogPrinter.addDebugLine(
                buildId = toDoTask.buildId,
                message = msg,
                tag = toDoTask.taskId,
                jobId = toDoTask.containerHashId,
                executeCount = toDoTask.executeCount!!
            )
            // 进入预队列
            pipelineTaskService.updateTaskStatus(toDoTask, userId = starter, buildStatus = BuildStatus.QUEUE_CACHE)
            containerContext.buildStatus = BuildStatus.RUNNING
            containerContext.event.actionType = ActionType.START // 未开始的需要开始
        }
        return toDoTask
    }

    private fun PipelineBuildTask.checkAllSkipQuickly(
        startPos: Int,
        containerContext: ContainerContext,
        contextMap: Map<String, String>,
        hasFailedTaskInSuccessContainer: Boolean,
        message: StringBuilder
    ): Boolean {

        if (this.taskId != VMUtils.genStartVMTaskId(this.containerId)) { // 非开机插件,检查条件
            return ControlUtils.checkTaskSkip(
                buildId = buildId,
                additionalOptions = additionalOptions,
                containerFinalStatus = containerContext.buildStatus,
                variables = contextMap,
                hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer,
                message = message,
                asCodeEnabled = containerContext.pipelineAsCodeEnabled == true
            )
        }

        var skip = false
        var idx = startPos
        while (++idx < containerContext.containerTasks.size) {
            val it = containerContext.containerTasks[idx]
            if (!VMUtils.isVMTask(it.taskId)) {
                skip = ControlUtils.checkTaskSkip(
                    buildId = buildId,
                    additionalOptions = it.additionalOptions,
                    containerFinalStatus = containerContext.buildStatus,
                    variables = contextMap,
                    hasFailedTaskInSuccessContainer = hasFailedTaskInSuccessContainer,
                    message = message,
                    asCodeEnabled = containerContext.pipelineAsCodeEnabled == true
                )
                if (LOG.isDebugEnabled) {
                    LOG.debug("ENGINE|$buildId|CHECK_QUICK_SKIP|$stageId|j($containerId)|${it.taskName}|$skip")
                }
                if (!skip) { // 发现有未跳过的插件, 必须开机
                    break
                }
            }
        }
        return skip
    }

    private fun refreshTaskStatus(
        updateTaskStatusInfos: List<PipelineTaskStatusInfo>,
        index: Int,
        containerTasks: List<PipelineBuildTask>
    ) {
        if (updateTaskStatusInfos.isEmpty()) {
            return
        }
        // 更新task列表状态
        val endIndex = containerTasks.size - 1
        for (i in index..endIndex) {
            val task = containerTasks[i]
            for (updateTaskStatusInfo in updateTaskStatusInfos) {
                val taskId = updateTaskStatusInfo.taskId
                val buildStatus = updateTaskStatusInfo.buildStatus
                if (taskId == task.taskId) {
                    task.status = buildStatus
                    break
                }
            }
        }
    }

    /**
     * 根据当前处于暂停状态的[currentTask]任务之下，查找到暂停任务后的未执行过的stopVM-xxxx关机任务， 如果没找到，则返回null
     */
    private fun findNextTaskAfterPause(
        containerContext: ContainerContext,
        currentTask: PipelineBuildTask
    ): PipelineBuildTask? {

        var toDoTask: PipelineBuildTask? = null

        val pipelineBuildTask = containerContext.containerTasks
            .filter { it.taskId.startsWith(VMUtils.getStopVmLabel()) } // 找构建环境关机任务
            .getOrNull(0) // 取第一个关机任务，如果没有返回空

        if (pipelineBuildTask?.status?.isFinish() == false) { // 如果未执行过，则取该任务作为后续执行任务
            toDoTask = pipelineBuildTask
            LOG.info(
                "ENGINE|${currentTask.buildId}|findNextTaskAfterPause|PAUSE|${currentTask.stageId}|" +
                    "j(${currentTask.containerId})|${currentTask.taskId}|NextTask=${toDoTask.taskId}"
            )
            val endTask = containerContext.containerTasks
                .filter { it.taskId.startsWith(VMUtils.getEndLabel()) } // 获取end插件
                .getOrNull(0)
            // issues_5530 stop插件执行后会发送shutdown,无需构建机再跑endBuild逻辑,避免造成并发问题。
            // 若stop先到，endBuild未执行。则end插件就一直处于queue状态。导致暂停插件无法终止。
            if (endTask != null && endTask.status != BuildStatus.RUNNING) {
                pipelineTaskService.updateTaskStatus(
                    task = endTask,
                    buildStatus = BuildStatus.SUCCEED,
                    userId = endTask.starter
                )
            }
        }

        // 终止打印终止原因
        if (isTerminate(containerContext)) {
            buildLogPrinter.addRedLine(
                buildId = currentTask.buildId,
                message = "Terminate Plugin[${currentTask.taskName}]: ${containerContext.event.reason ?: "unknown"}",
                tag = currentTask.taskId,
                jobId = currentTask.containerHashId,
                executeCount = currentTask.executeCount ?: 1
            )
            containerContext.buildStatus = BuildStatus.CANCELED
        } else {
            containerContext.buildStatus = BuildStatus.PAUSE
            if (containerContext.event.actionType.isEnd()) {
                // #5244 若领到stop任务,碰到ActionType == end,需要变为刷新, 供TaskControl可以跑stopVm
                containerContext.event.actionType = ActionType.REFRESH
            }
        }

        return toDoTask
    }

    fun ElementPostInfo.findPostActionTask(
        containerTasks: List<PipelineBuildTask>,
        currentTask: PipelineBuildTask,
        isTerminate: Boolean, // 是否终止
        isContainerFailed: Boolean,
        hasFailedTaskInSuccessContainer: Boolean
    ): PipelineBuildTask? {
        // 添加Post action标识打印
        addPostTipLog(currentTask)
        // 终止情况下，[post action]只适用于stopVM-关机任务, 其他的设置为未执行,并打印日志
        // #5806 解决构建worker进程异常结束退出时，可能PostAction还会被startVM-xxxx任务捡起来，因此需要对其状态进行设置
        if (isTerminate && currentTask.taskId != VMUtils.genStopVMTaskId(currentTask.taskSeq)) {
            pipelineTaskService.updateTaskStatus(currentTask, currentTask.starter, BuildStatus.UNEXEC)
            buildLogPrinter.addYellowLine(
                buildId = currentTask.buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_UNEXECUTE_POSTACTION_TASK,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " [${currentTask.taskName}]",
                tag = currentTask.taskId,
                jobId = currentTask.containerHashId,
                executeCount = currentTask.executeCount ?: 1
            )
            return null
        }

        // 查询父任务及是否允许当前的post action 任务执行标识
        val (parentTask, postExecuteFlag) = TaskUtils.getPostTaskAndExecuteFlag(
            taskList = containerTasks,
            task = currentTask,
            isContainerFailed = isContainerFailed,
            hasFailedTaskInInSuccessContainer = hasFailedTaskInSuccessContainer
        )

        var waitToDoTask: PipelineBuildTask? = null
        if (parentTask != null && !postExecuteFlag) {
            val parentTaskSkipFlag = parentTask.status == BuildStatus.SKIP
            // 如果post任务的主体任务状态是SKIP，则该post任务的状态也应该置为SKIP
            val taskStatus = if (parentTaskSkipFlag) BuildStatus.SKIP else BuildStatus.UNEXEC
            val message = if (parentTaskSkipFlag) "Plugin [${currentTask.taskName}] was skipped"
            else "Post action execution conditions (expectation: $postCondition), not executed"
            // 更新排队中的post任务的构建状态
            pipelineTaskService.updateTaskStatus(currentTask, currentTask.starter, taskStatus)
            // 系统控制类插件不涉及到Detail编排状态修改
            if (EnvControlTaskType.parse(currentTask.taskType) == null) {
                taskBuildRecordService.updateTaskStatus(
                    projectId = currentTask.projectId,
                    pipelineId = currentTask.pipelineId,
                    buildId = currentTask.buildId,
                    stageId = currentTask.stageId,
                    containerId = currentTask.containerId,
                    taskId = currentTask.taskId,
                    executeCount = currentTask.executeCount ?: 1,
                    buildStatus = taskStatus,
                    operation = if (parentTaskSkipFlag) "taskSkip" else "taskUnExec"
                )
            }
            buildLogPrinter.addLine(
                buildId = currentTask.buildId,
                message = message,
                tag = currentTask.taskId,
                jobId = currentTask.containerHashId,
                executeCount = currentTask.executeCount ?: 1
            )
        }
        if (parentTask != null && postExecuteFlag) {
            waitToDoTask = currentTask
        }

        return waitToDoTask
    }

    private fun ElementPostInfo.addPostTipLog(task: PipelineBuildTask) {
        buildLogPrinter.addLine(
            buildId = task.buildId,
            message = I18nUtil.getCodeLanMessage(
                messageCode = ATOM_POST_EXECUTE_TIP,
                params = arrayOf((parentElementJobIndex + 1).toString(), parentElementName),
                language = I18nUtil.getLanguage()
            ),
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
    }

    private fun sendTask(event: PipelineBuildContainerEvent, task: PipelineBuildTask) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildAtomTaskEvent(
                source = "CONTAINER_${event.actionType}",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = event.userId,
                buildId = task.buildId,
                stageId = task.stageId,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                containerType = task.containerType,
                taskId = task.taskId,
                taskParam = task.taskParams,
                actionType = event.actionType,
                reason = event.reason,
                errorCode = event.errorCode,
                errorTypeName = event.errorTypeName,
                executeCount = task.executeCount ?: 1
            )
        )
    }
}
