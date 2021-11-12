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
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.api.util.SensitiveApiUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.engine.api.pojo.HeartBeatInfo
import com.tencent.devops.process.engine.common.Timeout.transMinuteTimeoutToMills
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.BuildingHeartBeatUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.lock.ContainerIdLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.builds.CompleteTask
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.engine.service.detail.TaskBuildDetailService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import com.tencent.devops.process.service.PipelineTaskPauseService
import com.tencent.devops.process.service.PipelineTaskService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.process.utils.PIPELINE_VMSEQ_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Suppress("LongMethod", "LongParameterList", "ReturnCount", "TooManyFunctions")
@Service
class EngineVMBuildService @Autowired(required = false) constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val taskBuildDetailService: TaskBuildDetailService,
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
    private val buildingHeartBeatUtils: BuildingHeartBeatUtils,
    private val redisOperation: RedisOperation
) {

    /**
     * 构建机报告启动完毕
     */
    fun buildVMStarted(buildId: String, vmSeqId: String, vmName: String, retryCount: Int): BuildVariables {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            containerIdLock.lock()
            return handleStartUpVMBus(buildId, vmSeqId, vmName, retryCount)
        } finally {
            containerIdLock.unlock()
        }
    }

    @Suppress("ThrowsCount", "ComplexMethod")
    private fun handleStartUpVMBus(
        buildId: String,
        vmSeqId: String,
        vmName: String,
        retryCount: Int
    ): BuildVariables {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
        Preconditions.checkNotNull(buildInfo, NotFoundException("Pipeline build ($buildId) is not exist"))
        LOG.info("ENGINE|$buildId|Agent|BUILD_VM_START|j($vmSeqId)|vmName($vmName)")
        // var表中获取环境变量，并对老版本变量进行兼容
        val variables = buildVariableService.getAllVariable(buildId)
        // 环境变量替换上下文
        var context = pipelineContextService.getAllBuildContext(variables)
        val variablesWithType = buildVariableService.getAllVariableWithType(buildId)
        val model = containerBuildDetailService.getBuildModel(buildId)
        Preconditions.checkNotNull(model, NotFoundException("Build Model ($buildId) is not exist"))
        var vmId = 1

        model!!.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach c@{

                if (vmId.toString() == vmSeqId) {
                    val container = pipelineRuntimeService.getContainer(buildId, s.id, vmSeqId)
                        ?: throw NotFoundException("j($vmSeqId)|vmName($vmName) is not exist")
                    // 如果取消等操作已经发出关机消息了，则不允许构建机认领任务
                    if (container.status.isFinish()) {
                        throw OperationException("vmName($vmName) has been shutdown")
                    }
                    // #3769 如果是已经启动完成并且不是网络故障重试的(retryCount>0), 都属于构建机的重复无效启动请求,要抛异常拒绝
                    Preconditions.checkTrue(
                        condition = !BuildStatus.parse(it.startVMStatus).isFinish() || retryCount > 0,
                        exception = OperationException("重复启动构建机/VM Start already: ${it.startVMStatus}")
                    )
                    var timeoutMills: Long? = null
                    val containerAppResource = client.get(ServiceContainerAppResource::class)

                    if (it is VMBuildContainer) {
                        // 对customBuildEnv 的占位符进行替换 之后再塞入 variables
                        context = context.plus(it.customBuildEnv?.map { mit ->
                            "envs.${mit.key}" to EnvUtils.parseEnv(mit.value, context)
                        }?.toMap() ?: emptyMap())
                    }
                    val buildEnvs = if (it is VMBuildContainer) {
                        timeoutMills = transMinuteTimeoutToMills(it.jobControlOption?.timeout).second
                        if (it.buildEnv == null) {
                            emptyList<BuildEnv>()
                        } else {
                            val list = ArrayList<BuildEnv>()
                            it.buildEnv!!.forEach { build ->
                                val env = containerAppResource.getBuildEnv(
                                    name = build.key, version = build.value, os = it.baseOS.name.toLowerCase()
                                ).data
                                if (env != null) {
                                    list.add(env)
                                }
                            }
                            list
                        }
                    } else {
                        if (it is NormalContainer) {
                            timeoutMills = transMinuteTimeoutToMills(it.jobControlOption?.timeout).second
                        }
                        emptyList()
                    }
                    buildingHeartBeatUtils.addHeartBeat(buildId, vmSeqId, System.currentTimeMillis())
                    // # 2365 将心跳监听事件 构建机主动上报成功状态时才触发
                    buildingHeartBeatUtils.dispatchHeartbeatEvent(buildInfo = buildInfo!!, containerId = vmSeqId)
                    setStartUpVMStatus(
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        buildStatus = BuildStatus.SUCCEED
                    )

                    return BuildVariables(
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        vmName = vmName,
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        variables = context,
                        buildEnvs = buildEnvs,
                        containerId = it.id!!,
                        containerHashId = it.containerId ?: "",
                        variablesWithType = variablesWithType,
                        timeoutMills = timeoutMills!!,
                        containerType = it.getClassType()
                    )
                }
                vmId++
            }
        }
        LOG.info("ENGINE|$buildId|Agent|BUILD_VM_START|j($vmSeqId)|$vmName|Not Found VMContainer")
        throw NotFoundException("Fail to find the vm build container")
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
        var startUpVMTask = pipelineRuntimeService.getBuildTask(buildId, VMUtils.genStartVMTaskId(vmSeqId))

        if (startUpVMTask == null) {
            val buildTasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
            if (buildTasks.isNotEmpty()) {
                startUpVMTask = buildTasks[0]
            }
        }

        LOG.info("ENGINE|$buildId|Agent|SETUP_VM_STATUS|j($vmSeqId)|${startUpVMTask?.taskId}|status=$buildStatus")
        if (startUpVMTask == null) {
            return false
        }

        // 如果是完成状态，则更新构建机启动插件的状态
        if (buildStatus.isFinish()) {
            pipelineRuntimeService.updateTaskStatus(
                task = startUpVMTask,
                userId = startUpVMTask.starter,
                buildStatus = buildStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )

            // #2043 上报启动构建机状态时，重新刷新开始时间，以防止调度的耗时占用了Job的超时时间
            if (!startUpVMTask.status.isFinish()) { // #2043 构建机当前启动状态是未结束状态，才进行刷新开始时间
                pipelineRuntimeService.updateContainerStatus(
                    buildId = buildId,
                    stageId = startUpVMTask.stageId,
                    containerId = startUpVMTask.containerId,
                    startTime = LocalDateTime.now(),
                    endTime = null,
                    buildStatus = BuildStatus.RUNNING
                )
                containerBuildDetailService.containerStarted(
                    buildId = buildId,
                    containerId = vmSeqId.toInt(),
                    containerBuildStatus = buildStatus
                )
            }
        }

        // 失败的话就发终止事件
        val message: String?
        val actionType = when {
            buildStatus.isTimeout() -> {
                message = "Job Timeout"
                ActionType.TERMINATE
            }
            buildStatus.isFailure() -> {
                message = "Agent failed to start!"
                ActionType.TERMINATE
            }
            else -> {
                message = "Agent startup!"
                ActionType.START
            }
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "container_startup_$buildStatus",
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = startUpVMTask.starter,
                stageId = startUpVMTask.stageId,
                containerId = startUpVMTask.containerId,
                containerType = startUpVMTask.containerType,
                actionType = actionType,
                reason = message
            )
        )
        return true
    }

    /**
     * 构建机请求执行任务
     */
    fun buildClaimTask(buildId: String, vmSeqId: String, vmName: String): BuildTask {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            containerIdLock.lock()
            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            if (buildInfo == null || buildInfo.status.isFinish()) {
                LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_END|j($vmSeqId|$vmName|buildInfo ${buildInfo?.status}")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            val container = pipelineRuntimeService.getContainer(buildId, stageId = null, containerId = vmSeqId)
            if (container == null || container.status.isFinish()) {
                LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_END|j($vmSeqId)|container ${container?.status}")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }

            val allTasks = pipelineRuntimeService.listContainerBuildTasks(
                buildId = buildId,
                containerId = vmSeqId,
                buildStatusSet = setOf(BuildStatus.QUEUE_CACHE, BuildStatus.RUNNING)
            )

            val task = allTasks.firstOrNull()
                ?: return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)

            return claim(task = task, buildId = buildId, userId = task.starter, vmSeqId = vmSeqId)
        } finally {
            containerIdLock.unlock()
        }
    }

    private fun claim(task: PipelineBuildTask, buildId: String, userId: String, vmSeqId: String): BuildTask {
        LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_ING|j($vmSeqId)|[${task.taskId}-${task.taskName}]")
        return when {
            task.status == BuildStatus.QUEUE -> { // 初始化状态，表明任务还未准备好
                LOG.info("ENGINE|$buildId|taskId=${task.taskId}|name=${task.taskName}|NOT_READY, WAIT!")
                BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
            }
            task.taskAtom.isNotBlank() -> { // 排除非构建机的插件任务 继续等待直到它完成
                LOG.info("ENGINE|$buildId|taskId=${task.taskId}|taskAtom=${task.taskAtom}|NOT VM TASK, SKIP!")
                BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
            }
            task.taskId == VMUtils.genEndPointTaskId(task.taskSeq) -> { // 全部完成了
                pipelineRuntimeService.claimBuildTask(task, userId) // 刷新一下这个结束的任务节点时间
                BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            pipelineTaskService.isNeedPause(taskId = task.taskId, buildId = task.buildId, taskRecord = task) -> {
                // 如果插件配置了前置暂停, 暂停期间关闭当前构建机，节约资源。
                pipelineTaskService.executePause(taskId = task.taskId, buildId = task.buildId, taskRecord = task)
                LOG.info("ENGINE|$buildId|taskId=${task.taskId}|taskAtom=${task.taskAtom} cfg pause, shutdown agent")
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
            }
            else -> {
                val allVariable = buildVariableService.getAllVariable(buildId)
                // 构造扩展变量
                val extMap = buildExtService.buildExt(task, allVariable)
                val buildVariable = mutableMapOf(
                    PIPELINE_VMSEQ_ID to vmSeqId,
                    PIPELINE_ELEMENT_ID to task.taskId
                )
                buildVariable.putAll(extMap)
                PipelineVarUtil.fillOldVar(buildVariable)
                buildVariable.putAll(allVariable)

                // 如果状态未改变，则做认领任务动作
                if (!task.status.isRunning()) {
                    pipelineRuntimeService.claimBuildTask(task, userId)
                    taskBuildDetailService.taskStart(buildId, task.taskId)
                    jmxElements.execute(task.taskType)
                }
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStatusBroadCastEvent(
                        source = "vm-build-claim($vmSeqId)", projectId = task.projectId, pipelineId = task.pipelineId,
                        userId = task.starter, buildId = buildId, taskId = task.taskId, actionType = ActionType.START
                    )
                )
                // 标记正在运行的atom_code
                if (!task.atomCode.isNullOrBlank()) {
                    redisOperation.set(
                        key = SensitiveApiUtil.getRunningAtomCodeKey(
                            buildId = buildId,
                            vmSeqId = vmSeqId
                        ),
                        value = task.atomCode!!,
                        expiredInSecond = transMinuteTimeoutToMills(task.additionalOptions?.timeout?.toInt()).second
                    )
                }
                BuildTask(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    status = BuildTaskStatus.DO,
                    taskId = task.taskId,
                    elementId = task.taskId,
                    elementName = task.taskName,
                    type = task.taskType,
                    params = task.taskParams.map {
                        val obj = ObjectReplaceEnvVarUtil.replaceEnvVar(it.value, buildVariable)
                        it.key to JsonUtil.toJson(obj, formatted = false)
                    }.filter {
                        !it.first.startsWith("@type")
                    }.toMap(),
                    buildVariable = buildVariable,
                    containerType = task.containerType
                )
            }
        }
    }

    /**
     * 构建机完成任务请求
     */
    fun buildCompleteTask(buildId: String, vmSeqId: String, vmName: String, result: BuildTaskResult) {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            // 加锁防止和引擎并发改task状态的情况
            containerIdLock.lock()
            executeCompleteTaskBus(buildId, result, vmName, vmSeqId)
        } finally {
            redisOperation.delete(SensitiveApiUtil.getRunningAtomCodeKey(buildId = buildId, vmSeqId = vmSeqId))
            containerIdLock.unlock()
        }
    }

    private fun executeCompleteTaskBus(
        buildId: String,
        result: BuildTaskResult,
        vmName: String,
        vmSeqId: String
    ) {
        val buildTask = pipelineRuntimeService.getBuildTask(buildId, result.taskId)
        val taskStatus = buildTask?.status
        if (taskStatus == null) {
            // 当上报的任务不存在，则直接返回
            LOG.warn("BKSystemErrorMonitor|ENGINE|$buildId|$vmName|${result.taskId}|invalid")
            return
        }

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: run {
            LOG.warn("BKSystemErrorMonitor|ENGINE|$buildId|$vmName|buildInfo is null")
            return // 数据为空是平台异常，正常情况不应该出现
        }

        // 只要buildResult不为空，都写入到环境变量里面
        if (result.buildResult.isNotEmpty()) {
            LOG.info("ENGINE|$buildId| Add the build result size(${result.buildResult.size}) to var")
            try {
                buildVariableService.batchUpdateVariable(
                    projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId, buildId = buildId, variables = result.buildResult
                )
            } catch (ignored: Exception) { // 防止因为变量字符过长而失败。
                LOG.warn("ENGINE|$buildId| save var fail: ${ignored.message}", ignored)
            }
        }
        if (taskStatus.isFinish()) {
            // 当上报的任务状态已经结束，则直接返回
            LOG.warn("BKSystemErrorMonitor|ENGINE|$buildId|$vmName|${result.taskId}|finish")
            return
        }
        val errorType = ErrorType.getErrorType(result.errorType)
        val buildStatus = getCompleteTaskBuildStatus(result, buildId, buildInfo)
        taskBuildDetailService.taskEnd(
            buildId = buildId, taskId = result.elementId, buildStatus = buildStatus,
            errorType = errorType, errorCode = result.errorCode, errorMsg = result.message
        )
        // 重置前置暂停插件暂停状态位
        pipelineTaskPauseService.pauseTaskFinishExecute(buildId, result.taskId)
        val task = pipelineRuntimeService.completeClaimBuildTask(
            completeTask = CompleteTask(
                buildId = buildId, taskId = result.taskId,
                userId = buildInfo.startUser, buildStatus = buildStatus,
                errorType = errorType, errorCode = result.errorCode, errorMsg = result.message
            )
        )

        if (buildStatus.isFailure()) {
            logTaskFailed(task, errorType)
            // 打印出失败继续的日志
            if (ControlUtils.continueWhenFailure(task?.additionalOptions)) {
                buildLogPrinter.addRedLine(
                    buildId = task!!.buildId,
                    message = "Plugin[${task.taskName}]: 失败自动跳过/continue when error",
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
        sendElementData(buildId = buildId, result = result)

        LOG.info(
            "ENGINE|$buildId|Agent|END_TASK|j($vmSeqId)|${result.taskId}|$buildStatus|" +
                "type=$errorType|code=${result.errorCode}|msg=${result.message}]"
        )
        buildLogPrinter.stopLog(buildId = buildId, tag = result.elementId, jobId = result.containerId ?: "")
    }

    private fun getCompleteTaskBuildStatus(
        result: BuildTaskResult,
        buildId: String,
        buildInfo: BuildInfo
    ): BuildStatus {
        return if (result.success) {
            pipelineTaskService.removeRetryCache(buildId, result.taskId)
            pipelineTaskService.removeFailTaskVar(
                buildId = buildId, projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId, taskId = result.taskId
            ) // 清理插件错误信息（重试插件成功的情况下）
            BuildStatus.SUCCEED
        } else {
            when {
                pipelineTaskService.isRetryWhenFail(taskId = result.taskId, buildId = buildId) -> {
                    BuildStatus.RETRY
                }
                result.errorCode == ErrorCode.USER_TASK_OUTTIME_LIMIT -> {
                    BuildStatus.EXEC_TIMEOUT
                }
                else -> { // 记录错误插件信息
                    pipelineTaskService.createFailTaskVar(
                        buildId = buildId, projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId, taskId = result.taskId
                    )
                    BuildStatus.FAILED
                }
            }
        }
    }

    /**
     * 构建机结束当前Job
     */
    fun buildEndTask(buildId: String, vmSeqId: String, vmName: String): Boolean {
        val containerIdLock = ContainerIdLock(redisOperation, buildId, vmSeqId)
        try {
            containerIdLock.lock()
            val task = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
                .firstOrNull { it.taskId == VMUtils.genEndPointTaskId(it.taskSeq) }

            buildingHeartBeatUtils.dropHeartbeat(buildId = buildId, vmSeqId = vmSeqId)
            return if (task == null || task.status.isFinish()) {
                LOG.warn("ENGINE|$buildId|Agent|$vmName|END_JOB|j($vmSeqId)|Task[${task?.taskName}] ${task?.status}")
                false
            } else {
                pipelineRuntimeService.completeClaimBuildTask(
                    completeTask = CompleteTask(
                        buildId = buildId,
                        taskId = task.taskId,
                        userId = task.starter,
                        buildStatus = BuildStatus.SUCCEED
                    ),
                    endBuild = true
                )
                LOG.info("ENGINE|$buildId|Agent|END_JOB|${task.stageId}|j($vmSeqId)|${task.taskId}|${task.taskName}")
                buildExtService.endBuild(task)
                true
            }
        } finally {
            containerIdLock.unlock()
        }
    }

    fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Boolean {
        LOG.info("ENGINE|$buildId|Agent|HEART_BEAT|j($vmSeqId)|$vmName")
        buildingHeartBeatUtils.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        return true
    }

    fun heartbeatV1(projectId: String, buildId: String, vmSeqId: String, vmName: String): HeartBeatInfo {
        LOG.info("ENGINE|$projectId|$buildId|Agent|HEART_BEAT|j($vmSeqId)|$vmName")
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
    private fun sendElementData(buildId: String, result: BuildTaskResult) {
        try {
            if (!result.success && !result.type.isNullOrBlank()) {
                jmxElements.fail(elementType = result.type!!)
            }
            val task: PipelineBuildTask by lazy { pipelineRuntimeService.getBuildTask(buildId, result.taskId)!! }
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
            LOG.warn("ENGINE|$buildId|Agent|MEASURE|j(${result.containerId})|${result.taskId}|error=$ignored")
        }
    }

    /**
     *  #4191 输出失败任务流水线日志
     */
    private fun logTaskFailed(task: PipelineBuildTask?, errorType: ErrorType?) {
        task?.run {
            errorType?.also {
                val errMsg = when (errorType) {
                    ErrorType.USER ->
                        "Error: Process completed with exit code $errorCode. Please check your input or service."
                    ErrorType.THIRD_PARTY ->
                        "Error: Process completed with exit code $errorCode." +
                            " Please contact the third-party service provider."
                    ErrorType.PLUGIN ->
                        "Error: Process completed with exit code $errorCode. Please contact the plugin developer."
                    ErrorType.SYSTEM ->
                        "Error: Process completed with exit code $errorCode. Please contact DevOps-helper."
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

    companion object {
        private val LOG = LoggerFactory.getLogger(EngineVMBuildService::class.java)
    }
}
