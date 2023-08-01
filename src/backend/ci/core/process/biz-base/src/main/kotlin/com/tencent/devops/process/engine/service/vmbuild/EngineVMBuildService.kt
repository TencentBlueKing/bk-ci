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

package com.tencent.devops.process.engine.service.vmbuild

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.AtomRuntimeUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.engine.api.pojo.HeartBeatInfo
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CONTINUE_WHEN_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PROCESSING_CURRENT_REPORTED_TASK_PLEASE_WAIT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_VM_START_ALREADY
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.Timeout.transMinuteTimeoutToMills
import com.tencent.devops.process.engine.common.Timeout.transMinuteTimeoutToSec
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.BuildingHeartBeatUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.lock.ContainerIdLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.UpdateTaskInfo
import com.tencent.devops.process.engine.pojo.builds.CompleteTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import com.tencent.devops.process.engine.service.PipelineBuildTaskService
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.PipelineContextService
import com.tencent.devops.process.service.PipelineTaskPauseService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_REMARK
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.process.utils.PIPELINE_VMSEQ_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.ws.rs.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress(
    "LongMethod",
    "LongParameterList",
    "ReturnCount",
    "TooManyFunctions",
    "MagicNumber",
    "LargeClass"
)
@Service
class EngineVMBuildService @Autowired(required = false) constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val buildVariableService: BuildVariableService,
    private val pipelineContextService: PipelineContextService,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineTaskPauseService: PipelineTaskPauseService,
    private val jmxElements: JmxElements,
    private val buildExtService: PipelineBuildExtService,
    private val client: Client,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val pipelineBuildTaskService: PipelineBuildTaskService,
    private val buildingHeartBeatUtils: BuildingHeartBeatUtils,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(EngineVMBuildService::class.java)
        const val ENV_CONTEXT_KEY_PREFIX = "envs."

        // 任务结束上报Key
        private fun completeTaskKey(buildId: String, vmSeqId: String) = "build:$buildId:job:$vmSeqId:ending_task"
    }

    /**
     * 构建机报告启动完毕
     */
    fun buildVMStarted(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        retryCount: Int
    ): BuildVariables {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            containerIdLock.lock()
            return handleStartUpVMBus(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                vmName = vmName,
                retryCount = retryCount
            )
        } finally {
            containerIdLock.unlock()
        }
    }

    @Suppress("ThrowsCount", "ComplexMethod")
    private fun handleStartUpVMBus(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        retryCount: Int
    ): BuildVariables {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw NotFoundException("Fail to find build: buildId($buildId)")
        Preconditions.checkNotNull(buildInfo, NotFoundException("Pipeline build ($buildId) is not exist"))
        LOG.info("ENGINE|$buildId|BUILD_VM_START|j($vmSeqId)|vmName($vmName)")
        // var表中获取环境变量，并对老版本变量进行兼容
        val pipelineId = buildInfo.pipelineId
        val variables = buildVariableService.getAllVariable(projectId, buildInfo.pipelineId, buildId)
        val variablesWithType = buildVariableService.getAllVariableWithType(projectId, buildId).toMutableList()
        val model = containerBuildDetailService.getBuildModel(projectId, buildId)
        val asCodeSettings = pipelineAsCodeService.getPipelineAsCodeSettings(projectId, buildInfo.pipelineId)
        Preconditions.checkNotNull(model, NotFoundException("Build Model ($buildId) is not exist"))
        var vmId = 1

        model!!.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach nextContainer@{
                val c = it.getContainerById(vmSeqId)
                if (c != null) {
                    val container = pipelineContainerService.getContainer(
                        projectId = buildInfo.projectId,
                        buildId = buildId,
                        stageId = s.id,
                        containerId = vmSeqId
                    ) ?: throw NotFoundException("j($vmSeqId)|vmName($vmName) is not exist")
                    // 如果取消等操作已经发出关机消息了，则不允许构建机认领任务
                    if (container.status.isFinish()) {
                        throw OperationException("vmName($vmName) has been shutdown")
                    }
                    // #3769 如果是已经启动完成并且不是网络故障重试的(retryCount>0), 都属于构建机的重复无效启动请求,要抛异常拒绝
                    Preconditions.checkTrue(
                        condition = !BuildStatus.parse(c.startVMStatus).isFinish() || retryCount > 0,
                        exception = OperationException(
                            I18nUtil.getCodeLanMessage(messageCode = BK_VM_START_ALREADY) + " ${c.startVMStatus}"
                        )
                    )
                    val containerAppResource = client.get(ServiceContainerAppResource::class)

                    // #4518 填充构建机环境变量、构建上下文、获取超时时间
                    val (containerEnv, context, timeoutMills) = when (c) {
                        is VMBuildContainer -> {
                            val envList = mutableListOf<BuildEnv>()
                            val tm = transMinuteTimeoutToMills(container.controlOption.jobControlOption.timeout)
                            val contextMap = variables.plus(
                                pipelineContextService.buildContext(
                                    projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                                    stageId = s.id!!, containerId = c.id!!, taskId = null,
                                    variables = variables, model = model
                                )
                            ).toMutableMap()
                            fillContainerContext(contextMap, c.customBuildEnv, c.matrixContext, asCodeSettings?.enable)
                            val asCodeEnabled = asCodeSettings?.enable == true
                            val contextPair = if (asCodeEnabled) {
                                EnvReplacementParser.getCustomExecutionContextByMap(contextMap)
                            } else null
                            c.buildEnv?.forEach { env ->
                                containerAppResource.getBuildEnv(
                                    name = env.key,
                                    version = EnvReplacementParser.parse(
                                        value = env.value,
                                        contextMap = contextMap,
                                        onlyExpression = asCodeEnabled,
                                        contextPair = contextPair
                                    ),
                                    os = c.baseOS.name.lowercase()
                                ).data?.let { self -> envList.add(self) }
                            }

                            // 设置Job环境变量customBuildEnv到variablesWithType和variables中
                            // TODO 此处应收敛到variablesWithType或variables的其中一个
                            c.customBuildEnv?.map { (t, u) ->
                                val value = EnvReplacementParser.parse(
                                    value = u,
                                    contextMap = contextMap,
                                    onlyExpression = asCodeEnabled,
                                    contextPair = contextPair
                                )
                                contextMap[t] = value
                                BuildParameters(
                                    key = t,
                                    value = value,
                                    valueType = BuildFormPropertyType.STRING,
                                    readOnly = true
                                )
                            }?.let { self -> variablesWithType.addAll(self) }

                            Triple(envList, contextMap, tm)
                        }
                        is NormalContainer -> {
                            val tm = transMinuteTimeoutToMills(container.controlOption.jobControlOption.timeout)
                            val contextMap = pipelineContextService.getAllBuildContext(variables).toMutableMap()
                            fillContainerContext(contextMap, null, c.matrixContext, asCodeSettings?.enable)
                            Triple(mutableListOf(), contextMap, tm)
                        }
                        else -> throw OperationException("vmName($vmName) is an illegal container type: $c")
                    }
                    buildingHeartBeatUtils.addHeartBeat(buildId, vmSeqId, System.currentTimeMillis())
                    // # 2365 将心跳监听事件 构建机主动上报成功状态时才触发
                    buildingHeartBeatUtils.dispatchHeartbeatEvent(buildInfo = buildInfo, containerId = vmSeqId)
                    setStartUpVMStatus(
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        buildStatus = BuildStatus.SUCCEED
                    )
                    // #5109 启动时先清空任务结束上报Key
                    redisOperation.delete(key = completeTaskKey(buildId = buildId, vmSeqId = vmSeqId))
                    return BuildVariables(
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        vmName = vmName,
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        variables = context,
                        buildEnvs = containerEnv,
                        containerId = c.id!!,
                        containerHashId = c.containerHashId ?: "",
                        jobId = c.jobId,
                        variablesWithType = variablesWithType,
                        timeoutMills = timeoutMills,
                        containerType = c.getClassType(),
                        pipelineAsCodeSettings = asCodeSettings
                    )
                }
                vmId++
            }
        }
        LOG.info("ENGINE|$buildId|BUILD_VM_START|j($vmSeqId)|$vmName|Not Found VMContainer")
        throw NotFoundException("Fail to find the vm build container: j($vmSeqId) vmName($vmName)")
    }

    /**
     * 对[customBuildEnv]的占位符进行替换，
     * 再追加env.前缀的构建机容器的上下文[context]，
     * 同时追加构[matrixContext]建矩阵上下文
     */
    private fun fillContainerContext(
        context: MutableMap<String, String>,
        customBuildEnv: Map<String, String>?,
        matrixContext: Map<String, String>?,
        asCodeEnabled: Boolean?
    ) {
        customBuildEnv?.let {
            context.putAll(
                customBuildEnv.map {
                    "$ENV_CONTEXT_KEY_PREFIX${it.key}" to EnvReplacementParser.parse(it.value, context, asCodeEnabled)
                }.toMap()
            )
        }

        if (matrixContext?.isNotEmpty() == true) context.putAll(matrixContext)
    }

    fun setStartUpVMStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
    ): Boolean {
        // 针VM启动不是在第一个的情况，第一个可能是人工审核插件（避免占用VM）
        // agent上报状态需要判断根据ID来获取真正的启动VM的任务，否则兼容处理取第一个插件的状态（正常情况）
        var startUpVMTask = pipelineTaskService.getBuildTask(projectId, buildId, VMUtils.genStartVMTaskId(vmSeqId))

        if (startUpVMTask == null) {
            val buildTasks = pipelineTaskService.listContainerBuildTasks(projectId, buildId, vmSeqId)
            if (buildTasks.isNotEmpty()) {
                startUpVMTask = buildTasks[0]
            }
        }

        LOG.info("ENGINE|$buildId|SETUP_VM_STATUS|j($vmSeqId)|${startUpVMTask?.taskId}|status=$buildStatus")
        if (startUpVMTask == null) {
            return false
        }
        val finalBuildStatus = getFinalBuildStatus(buildStatus, buildId, vmSeqId, startUpVMTask)

        // 如果是完成状态，则更新构建机启动插件的状态
        if (finalBuildStatus.isFinish()) {
            pipelineTaskService.updateTaskStatus(
                task = startUpVMTask,
                userId = startUpVMTask.starter,
                buildStatus = finalBuildStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )

            // #2043 上报启动构建机状态时，重新刷新开始时间，以防止调度的耗时占用了Job的超时时间
            if (!startUpVMTask.status.isFinish()) { // #2043 构建机当前启动状态是未结束状态，才进行刷新开始时间
                pipelineContainerService.updateContainerStatus(
                    projectId = projectId,
                    buildId = buildId,
                    stageId = startUpVMTask.stageId,
                    containerId = startUpVMTask.containerId,
                    startTime = LocalDateTime.now(),
                    endTime = null,
                    buildStatus = BuildStatus.RUNNING
                )
                containerBuildRecordService.containerStarted(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    containerId = vmSeqId,
                    executeCount = startUpVMTask.executeCount ?: 1,
                    containerBuildStatus = finalBuildStatus
                )
            }
        }

        // 失败的话就发终止事件
        var message: String?
        val actionType = when {
            finalBuildStatus.isTimeout() -> {
                message = "Job Timeout"
                ActionType.TERMINATE
            }
            finalBuildStatus.isFailure() -> {
                message = "Agent failed to start!"
                ActionType.TERMINATE
            }
            finalBuildStatus.isCancel() -> {
                message = "Job Cancel"
                ActionType.TERMINATE
            }
            else -> {
                message = "Agent startup!"
                ActionType.START
            }
        }

        // #1613 完善日志
        errorType?.let { message = "$message \nerrorType: ${errorType.getI18n(I18nUtil.getDefaultLocaleLanguage())}" }
        errorCode?.let { message = "$message \nerrorCode: $errorCode" }
        errorMsg?.let { message = "$message \nerrorMsg: $errorMsg" }

        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "container_startup_$finalBuildStatus",
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = startUpVMTask.starter,
                stageId = startUpVMTask.stageId,
                containerId = startUpVMTask.containerId,
                containerHashId = startUpVMTask.containerHashId,
                containerType = startUpVMTask.containerType,
                actionType = actionType,
                reason = message
            )
        )
        return true
    }

    private fun getFinalBuildStatus(
        buildStatus: BuildStatus,
        buildId: String,
        vmSeqId: String,
        startUpVMTask: PipelineBuildTask
    ): BuildStatus {
        val finalBuildStatus = if (buildStatus.isFinish()) {
            buildStatus
        } else {
            val cancelTaskSetKey = TaskUtils.getCancelTaskIdRedisKey(buildId, vmSeqId, false)
            val cancelFlag = redisOperation.isMember(cancelTaskSetKey, startUpVMTask.taskId)
            val runCondition = startUpVMTask.additionalOptions?.runCondition
            val failedEvenCancelFlag = runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
            // 判断开机插件是否被取消
            if (!failedEvenCancelFlag && cancelFlag) {
                BuildStatus.CANCELED
            } else {
                buildStatus
            }
        }
        return finalBuildStatus
    }

    /**
     * 构建机请求执行任务
     */
    fun buildClaimTask(projectId: String, buildId: String, vmSeqId: String, vmName: String): BuildTask {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            containerIdLock.lock()
            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            if (buildInfo == null || buildInfo.status.isFinish()) {
                LOG.info("ENGINE|$buildId|BC_END|$projectId|j($vmSeqId|$vmName|buildInfo ${buildInfo?.status}")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            val container = pipelineContainerService.getContainer(
                projectId = projectId,
                buildId = buildId,
                stageId = null,
                containerId = vmSeqId
            )
            if (container == null || container.status.isFinish()) {
                LOG.info("ENGINE|$buildId|BC_END|$projectId|j($vmSeqId)|container ${container?.status}")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }

            val allTasks = pipelineTaskService.listContainerBuildTasks(
                projectId = projectId,
                buildId = buildId,
                containerSeqId = vmSeqId,
                buildStatusSet = setOf(BuildStatus.QUEUE_CACHE, BuildStatus.RUNNING)
            )

            val task = allTasks.firstOrNull()
                ?: return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)

            return claim(
                task = task, buildId = buildId, userId = task.starter, vmSeqId = vmSeqId,
                asCodeEnabled = pipelineAsCodeService.asCodeEnabled(task.projectId, task.pipelineId) == true
            )
        } finally {
            containerIdLock.unlock()
        }
    }

    private fun claim(
        task: PipelineBuildTask,
        buildId: String,
        userId: String,
        vmSeqId: String,
        asCodeEnabled: Boolean
    ): BuildTask {
        LOG.info("ENGINE|$buildId|BC_ING|${task.projectId}|j($vmSeqId)|[${task.taskId}-${task.taskName}]")
        return when {
            task.status == BuildStatus.QUEUE -> { // 初始化状态，表明任务还未准备好
                LOG.info("ENGINE|$buildId|BC_QUEUE|${task.projectId}|j($vmSeqId)|${task.taskId}|${task.taskName}")
                BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
            }
            task.taskAtom.isNotBlank() -> { // 排除非构建机的插件任务 继续等待直到它完成
                LOG.info("ENGINE|$buildId|BC_NOT_VM|${task.projectId}|j($vmSeqId)|${task.taskId}|${task.taskName}")
                BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
            }
            task.taskId == VMUtils.genEndPointTaskId(task.taskSeq) -> { // 全部完成了
                pipelineRuntimeService.claimBuildTask(task, userId) // 刷新一下这个结束的任务节点时间
                BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            pipelineTaskService.isNeedPause(taskId = task.taskId, buildId = task.buildId, taskRecord = task) -> {
                // 如果插件配置了前置暂停, 暂停期间关闭当前构建机，节约资源。
                pipelineTaskService.executePause(taskId = task.taskId, buildId = task.buildId, taskRecord = task)
                LOG.info("ENGINE|$buildId|BC_PAUSE|${task.projectId}|j($vmSeqId)|${task.taskId}|${task.taskAtom}")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStatusBroadCastEvent(
                        source = "TaskPause-${task.containerId}-${task.buildId}",
                        projectId = task.projectId,
                        pipelineId = task.pipelineId,
                        userId = task.starter,
                        buildId = task.buildId,
                        taskId = task.taskId,
                        stageId = task.stageId,
                        actionType = ActionType.REFRESH
                    )
                )
                BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            } // #5109 如果因平台异常原因导致上一个任务还处于运行中，结束处理不及时 ，需要等待
            task.status.isRunning() && task.taskId == redisOperation.get(completeTaskKey(buildId, vmSeqId)) -> {
                LOG.info("ENGINE|$buildId|BC_RUNNING|${task.projectId}|j($vmSeqId)|${task.taskId}|${task.status}")
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = MessageUtil.getMessageByLocale(
                        BK_PROCESSING_CURRENT_REPORTED_TASK_PLEASE_WAIT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = task.taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
            }
            else -> {
                val allVariable = buildVariableService.getAllVariable(task.projectId, task.pipelineId, buildId)
                // 构造扩展变量
                val extMap = buildExtService.buildExt(task, allVariable)
                val buildVariable = mutableMapOf(
                    PIPELINE_VMSEQ_ID to vmSeqId,
                    PIPELINE_ELEMENT_ID to task.taskId
                )

                PipelineVarUtil.fillOldVar(buildVariable)
                buildVariable.putAll(allVariable)
                buildVariable.putAll(extMap)

                // 如果状态未改变，则做认领任务动作
                if (!task.status.isRunning()) {
                    pipelineRuntimeService.claimBuildTask(task, userId)
                    taskBuildRecordService.taskStart(
                        projectId = task.projectId,
                        pipelineId = task.pipelineId,
                        buildId = buildId,
                        taskId = task.taskId,
                        executeCount = task.executeCount ?: 1
                    )
                    jmxElements.execute(task.taskType)
                }
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStatusBroadCastEvent(
                        source = "vm-build-claim($vmSeqId)", projectId = task.projectId, pipelineId = task.pipelineId,
                        userId = task.starter, buildId = buildId, taskId = task.taskId, actionType = ActionType.START
                    )
                )
                val signToken = UUIDUtil.generate()
                // 标记正在运行的atom_code
                if (!task.atomCode.isNullOrBlank()) {
                    AtomRuntimeUtil.setRunningAtomValue(
                        redisOperation = redisOperation,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        atomCode = task.atomCode!!,
                        signToken = signToken,
                        expiredInSecond = transMinuteTimeoutToSec(task.additionalOptions?.timeout?.toInt())
                    )
                }
                BuildTask(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    status = BuildTaskStatus.DO,
                    taskId = task.taskId,
                    elementId = task.taskId,
                    elementName = task.taskName,
                    stepId = task.stepId,
                    type = task.taskType,
                    params = task.taskParams.map {
                        // 在pipeline as code模式下，此处直接保持原文传给worker
                        val obj = if (asCodeEnabled) {
                            it.value
                        } else ObjectReplaceEnvVarUtil.replaceEnvVar(
                            it.value, buildVariable
                        )
                        it.key to JsonUtil.toJson(obj, formatted = false)
                    }.filter {
                        !it.first.startsWith("@type")
                    }.toMap(),
                    buildVariable = buildVariable,
                    containerType = task.containerType,
                    signToken = signToken
                )
            }
        }
    }

    /**
     * 构建机完成任务请求
     */
    fun buildCompleteTask(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        result: BuildTaskResult
    ) {
        val tCompleteTaskKey = completeTaskKey(buildId, vmSeqId)
        // #5109 提前做重复请求检测, 当弱网络或平台故障降级处理之前的请求较慢时，key仍然存在，需要拒绝客户端的重试产生的请求
        if (redisOperation.get(key = tCompleteTaskKey) == result.taskId) {
            LOG.warn("ENGINE|$buildId|BCT_DUPLICATE|$projectId|job#$vmSeqId|${result.taskId}")
            return
        }

        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: run {
            LOG.warn("ENGINE|BCT_NULL_BUILD|$buildId|$vmName|buildInfo is null")
            return // 数据为空是平台异常，正常情况不应该出现
        }
        // #5109 提前判断，防止异常数据流入，后续各类Redis锁定出现无必要的额外开启。
        if (buildInfo.isFinish()) {
            LOG.warn("ENGINE|BCT_END_BUILD|$buildId|$projectId|j($vmSeqId)|${result.taskId}|${buildInfo.status}")
            return
        }

        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            // 加锁防止和引擎并发改task状态的情况
            containerIdLock.lock()
            // 锁定先 长过期保证降级
            redisOperation.set(tCompleteTaskKey, result.taskId, expiredInSecond = TimeUnit.HOURS.toSeconds(5))
            // #7984 拿task信息放在锁内，避免被引擎修改后此处拿的旧值
            val buildTask = pipelineTaskService.getBuildTask(projectId, buildId, result.taskId)
            val taskStatus = buildTask?.status
            if (taskStatus == null) {
                // 当上报的任务不存在，则直接返回
                LOG.warn("ENGINE|$buildId|BCT_INVALID_TASK|$projectId|$vmName|${result.taskId}|")
                return
            }

            if (taskStatus.isFinish()) {
                LOG.warn("ENGINE|BCT_END_TASK|$buildId|$projectId|j($vmSeqId)|${result.taskId}|$taskStatus")
                return
            }
            executeCompleteTaskBus(
                result = result,
                buildInfo = buildInfo,
                vmSeqId = vmSeqId,
                runCondition = buildTask.additionalOptions?.runCondition
            )
        } finally {
            redisOperation.delete(key = tCompleteTaskKey)
            AtomRuntimeUtil.deleteRunningAtom(redisOperation = redisOperation, buildId = buildId, vmSeqId = vmSeqId)
            containerIdLock.unlock()
        }
    }

    private fun executeCompleteTaskBus(
        result: BuildTaskResult,
        buildInfo: BuildInfo,
        vmSeqId: String,
        runCondition: RunCondition? = null
    ) {
        val projectId = buildInfo.projectId
        val buildId = buildInfo.buildId
        // 只要buildResult不为空，都写入到环境变量里面
        if (result.buildResult.isNotEmpty()) {
            LOG.info("ENGINE|$buildId|BCT_ADD_VAR|$projectId| vars=${result.buildResult.size}")
            try {
                buildVariableService.batchUpdateVariable(
                    projectId = projectId,
                    pipelineId = buildInfo.pipelineId, buildId = buildId, variables = result.buildResult
                )
                LOG.info("ENGINE|$buildId|BCT_ADD_VAR_DONE|$projectId")
                writeRemark(result.buildResult, projectId, buildInfo.pipelineId, buildId, buildInfo.startUser)
            } catch (ignored: Exception) { // 防止因为变量字符过长而失败。
                LOG.warn("ENGINE|$buildId|BCT_ADD_VAR_FAIL|$projectId| save var fail: ${ignored.message}", ignored)
            }
        }
        val errorType = ErrorType.getErrorType(result.errorType)
        val buildStatus = getCompleteTaskBuildStatus(
            result = result,
            buildInfo = buildInfo,
            vmSeqId = vmSeqId,
            runCondition = runCondition
        )
        val endParam = TaskBuildEndParam(
            projectId = buildInfo.projectId,
            pipelineId = buildInfo.pipelineId,
            buildId = buildId,
            containerId = vmSeqId,
            taskId = result.elementId,
            executeCount = buildInfo.executeCount ?: 1,
            buildStatus = buildStatus,
            errorType = errorType,
            errorCode = result.errorCode,
            errorMsg = result.message,
            atomVersion = result.elementVersion
        )
        val updateTaskStatusInfos = taskBuildRecordService.taskEnd(endParam)
        updateTaskStatusInfos.forEach { updateTaskStatusInfo ->
            pipelineTaskService.updateTaskStatusInfo(
                task = null,
                updateTaskInfo = UpdateTaskInfo(
                    projectId = projectId,
                    buildId = buildId,
                    taskId = updateTaskStatusInfo.taskId,
                    executeCount = updateTaskStatusInfo.executeCount,
                    taskStatus = updateTaskStatusInfo.buildStatus
                )
            )
            if (!updateTaskStatusInfo.message.isNullOrBlank()) {
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = updateTaskStatusInfo.message,
                    tag = updateTaskStatusInfo.taskId,
                    jobId = updateTaskStatusInfo.containerHashId,
                    executeCount = updateTaskStatusInfo.executeCount
                )
            }
        }
        // 重置前置暂停插件暂停状态位
        pipelineTaskPauseService.pauseTaskFinishExecute(buildId, result.taskId)
        val task = pipelineRuntimeService.completeClaimBuildTask(
            completeTask = CompleteTask(
                projectId = projectId, buildId = buildId, taskId = result.taskId,
                userId = buildInfo.startUser, buildStatus = buildStatus,
                errorType = errorType, errorCode = result.errorCode, errorMsg = result.message,
                platformCode = result.platformCode, platformErrorCode = result.platformErrorCode
            )
        )

        task?.let {
            pipelineBuildTaskService.finishTask(
                buildTask = task,
                buildStatus = buildStatus,
                actionType = ActionType.REFRESH,
                source = "completeClaimBuildTask",
                sendEventFlag = false
            )
        }

        if (buildStatus.isFailure()) {
            // #1613 可能为空，需要先对预置
            task?.errorCode = result.errorCode
            task?.errorType = errorType
            task?.errorMsg = result.message
            logTaskFailed(task, errorType)
            // 打印出失败继续的日志
            if (ControlUtils.continueWhenFailure(task?.additionalOptions)) {
                buildLogPrinter.addRedLine(
                    buildId = task!!.buildId,
                    message = "Plugin[${task.taskName}]: " + I18nUtil.getCodeLanMessage(
                        messageCode = BK_CONTINUE_WHEN_ERROR,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = task.taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
            }
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "task-end-${result.taskId}", projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId, userId = buildInfo.startUser,
                buildId = buildId, taskId = result.taskId, actionType = ActionType.END
            )
        )
        // 发送度量数据
        sendElementData(projectId = projectId, buildId = buildId, result = result)

        LOG.info(
            "ENGINE|$buildId|BCT_DONE|$projectId|j($vmSeqId)|${result.taskId}|$buildStatus|" +
                "type=$errorType|code=${result.errorCode}|msg=${result.message}]"
        )
        buildLogPrinter.stopLog(buildId = buildId, tag = result.elementId, jobId = task?.containerHashId)
    }

    private fun getCompleteTaskBuildStatus(
        result: BuildTaskResult,
        buildInfo: BuildInfo,
        vmSeqId: String,
        runCondition: RunCondition? = null
    ): BuildStatus {
        val buildId = buildInfo.buildId
        val taskId = result.taskId
        val cancelTaskSetKey = TaskUtils.getCancelTaskIdRedisKey(buildId, vmSeqId, false)
        val cancelFlag = redisOperation.isMember(cancelTaskSetKey, taskId)
        val failedEvenCancelFlag = runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
        if (cancelFlag && failedEvenCancelFlag) {
            redisOperation.set(
                key = ContainerUtils.getContainerRunEvenCancelTaskKey(
                    pipelineId = buildInfo.pipelineId,
                    buildId = buildInfo.buildId,
                    containerId = vmSeqId
                ),
                value = taskId,
                expiredInSecond = TimeUnit.DAYS.toSeconds(Timeout.MAX_JOB_RUN_DAYS)
            )
        }
        return when {
            !failedEvenCancelFlag && cancelFlag -> {
                // 如果该任务运行时用户点击了取消则将任务的构建状态置为取消状态
                LOG.warn("ENGINE|$buildId|BCT_CANCEL_NOT_FINISH|${buildInfo.projectId}|job#$vmSeqId|$taskId")
                BuildStatus.CANCELED
            }
            result.success -> {
                pipelineTaskService.removeRetryCache(buildId, result.taskId)
                pipelineTaskService.removeFailTaskVar(
                    buildId = buildId, projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId, taskId = result.taskId
                ) // 清理插件错误信息（重试插件成功的情况下）
                BuildStatus.SUCCEED
            }
            else -> {
                when {
                    pipelineTaskService.isRetryWhenFail(
                        projectId = buildInfo.projectId,
                        taskId = result.taskId,
                        buildId = buildId
                    ) -> {
                        BuildStatus.RETRY
                    }
                    else -> { // 记录错误插件信息
                        pipelineTaskService.createFailTaskVar(
                            buildId = buildId, projectId = buildInfo.projectId,
                            pipelineId = buildInfo.pipelineId, taskId = result.taskId
                        )
                        if (result.errorCode == ErrorCode.USER_TASK_OUTTIME_LIMIT) BuildStatus.EXEC_TIMEOUT
                        else BuildStatus.FAILED
                    }
                }
            }
        }
    }

    /**
     * 构建机结束当前Job
     */
    fun buildEndTask(projectId: String, buildId: String, vmSeqId: String, vmName: String): Boolean {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            containerIdLock.lock()
            val task = pipelineTaskService.listContainerBuildTasks(projectId, buildId, vmSeqId)
                .firstOrNull { it.taskId == VMUtils.genEndPointTaskId(it.taskSeq) }

            return if (task == null || task.status.isFinish()) {
                LOG.warn("ENGINE|$buildId|BE_END|$projectId|$vmName|j($vmSeqId)|[${task?.taskName}] ${task?.status}")
                false
            } else {
                pipelineRuntimeService.completeClaimBuildTask(
                    completeTask = CompleteTask(
                        projectId = task.projectId,
                        buildId = buildId,
                        taskId = task.taskId,
                        userId = task.starter,
                        buildStatus = BuildStatus.SUCCEED
                    ),
                    endBuild = true
                )
                LOG.info("ENGINE|$buildId|BE_DONE|${task.stageId}|j($vmSeqId)|${task.taskId}|${task.taskName}")
                buildExtService.endBuild(task)
                true
            }
        } finally {
            redisOperation.delete(key = completeTaskKey(buildId = buildId, vmSeqId = vmSeqId))
            containerIdLock.unlock()
        }
    }

    fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Boolean {
        LOG.info("ENGINE|$buildId|HEART_BEAT|j($vmSeqId)|$vmName")
        buildingHeartBeatUtils.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        return true
    }

    fun heartbeatV1(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        executeCount: Int? = null
    ): HeartBeatInfo {
        LOG.info("ENGINE|$projectId|$buildId|HEART_BEAT|j($vmSeqId)|$vmName|$executeCount")
        buildingHeartBeatUtils.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        var cancelTaskIds: MutableSet<String>? = null
        val key = TaskUtils.getCancelTaskIdRedisKey(buildId, vmSeqId)
        loop@ while (true) {
            // 获取redis队列中被取消的task任务
            val cancelTaskId = redisOperation.rightPop(key) ?: break@loop
            if (cancelTaskIds == null) {
                cancelTaskIds = mutableSetOf()
            }
            cancelTaskIds.add(cancelTaskId)
        }
        return HeartBeatInfo(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            cancelTaskIds = cancelTaskIds
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun sendElementData(projectId: String, buildId: String, result: BuildTaskResult) {
        try {
            if (!result.success && !result.type.isNullOrBlank()) {
                jmxElements.fail(elementType = result.type!!)
            }
            val task: PipelineBuildTask by lazy {
                pipelineTaskService.getBuildTask(
                    projectId = projectId,
                    buildId = buildId,
                    taskId = result.taskId
                )!!
            }
            measureService?.postTaskData(
                task = task,
                startTime = task.startTime?.timestampmilli() ?: 0L,
                status = if (result.success) BuildStatus.SUCCEED else BuildStatus.FAILED,
                type = result.type!!,
                errorType = result.errorType,
                errorCode = result.errorCode,
                errorMsg = result.message,
                monitorDataMap = result.monitorData ?: emptyMap()
            )
        } catch (ignored: Throwable) {
            LOG.warn("ENGINE|$buildId|MEASURE|j(${result.containerId})|${result.taskId}|error=$ignored")
        }
    }

    /**
     *  #4191 输出失败任务流水线日志
     */
    private fun logTaskFailed(task: PipelineBuildTask?, errorType: ErrorType?) {
        task?.run {
            errorType?.also { // #5046 增加错误信息
                val errMsg = "Error: Process completed with exit code $errorCode: $errorMsg. " +
                    when (errorType) {
                        ErrorType.USER -> "Please check your input or service."
                        ErrorType.THIRD_PARTY -> "Please contact the third-party service provider."
                        ErrorType.PLUGIN -> "Please contact the plugin developer."
                        ErrorType.SYSTEM -> "Please contact platform."
                    }
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = errMsg,
                    tag = taskId,
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )
            }
        }
    }

    /**
     * #5046 提交构建机出错失败信息，并结束构建
     */
    fun submitError(projectId: String, pipelineId: String, buildId: String, vmSeqId: String, errorInfo: ErrorInfo) {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            // 加锁防止和引擎并发改task状态的情况
            containerIdLock.lock()
            setStartUpVMStatus(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.getErrorType(errorInfo.errorType),
                errorCode = errorInfo.errorCode,
                errorMsg = errorInfo.errorMsg
            )
        } finally {
            redisOperation.delete(key = completeTaskKey(buildId = buildId, vmSeqId = vmSeqId))
            AtomRuntimeUtil.deleteRunningAtom(redisOperation = redisOperation, buildId = buildId, vmSeqId = vmSeqId)
            containerIdLock.unlock()
        }
    }

    private fun writeRemark(
        buildResult: Map<String, String>,
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String
    ) {
        if (buildResult.containsKey(PIPELINE_BUILD_REMARK)) {
            val remark = buildResult[PIPELINE_BUILD_REMARK]
            if (remark != null) {
                LOG.info("writeRemark by setEnv $projectId|$pipelineId|$buildId|$remark")
                pipelineRuntimeService.updateBuildRemark(projectId, pipelineId, buildId, remark)
                pipelineEventDispatcher.dispatch(
                    PipelineBuildWebSocketPushEvent(
                        source = "writeRemark", projectId = projectId, pipelineId = pipelineId,
                        userId = userId, buildId = buildId, refreshTypes = RefreshType.HISTORY.binary
                    )
                )
            }
        }
    }
}
