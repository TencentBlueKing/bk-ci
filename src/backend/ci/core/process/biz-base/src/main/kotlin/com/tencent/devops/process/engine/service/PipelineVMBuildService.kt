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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
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
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.BuildingHeartBeatUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.builds.CompleteTask
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.engine.service.detail.TaskBuildDetailService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineTaskPauseService
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
import kotlin.math.min

@Deprecated("replace by EngineVMBuildService")
@Suppress("ALL")
@Service
class PipelineVMBuildService @Autowired(required = false) constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val taskBuildDetailService: TaskBuildDetailService,
    private val buildVariableService: BuildVariableService,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskPauseService: PipelineTaskPauseService,
    private val jmxElements: JmxElements,
    private val buildExtService: PipelineBuildExtService,
    private val client: Client,
    private val buildingHeartBeatUtils: BuildingHeartBeatUtils
) {

    /**
     * Dispatch service startup the vm for the build and then notify to process service
     */
    fun vmStartedByDispatch(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String
    ): Boolean {
        buildingHeartBeatUtils.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        setStartUpVMStatus(
            projectId = projectId, pipelineId = pipelineId,
            buildId = buildId, vmSeqId = vmSeqId, buildStatus = BuildStatus.SUCCEED
        )
        return true
    }

    /**
     * 构建机报告启动完毕
     */
    fun buildVMStarted(projectId: String, buildId: String, vmSeqId: String, vmName: String): BuildVariables {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        Preconditions.checkNotNull(buildInfo, NotFoundException("Pipeline build ($buildId) is not exist"))
        LOG.info("ENGINE|$buildId|Agent|BUILD_VM_START|j($vmSeqId)|vmName($vmName)")
        val variables = buildVariableService.getAllVariable(projectId, buildId)
        val variablesWithType = buildVariableService.getAllVariableWithType(projectId, buildId)
        val model = taskBuildDetailService.getBuildModel(projectId, buildId)
        Preconditions.checkNotNull(model, NotFoundException("Build Model ($buildId) is not exist"))
        var vmId = 1
        model!!.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach c@{
                val c = it.getContainerById(vmSeqId)
                if (c != null) {
                    // 增加判断状态，如果是已经结束的，抛出异常来拒绝重复启动请求
                    Preconditions.checkTrue(
                        condition = !BuildStatus.parse(c.startVMStatus).isFinish(),
                        exception = OperationException("重复启动构建机/Repeat start VM! startVMStatus ${c.startVMStatus}")
                    )
                    var timeoutMills: Long? = null
                    val containerAppResource = client.get(ServiceContainerAppResource::class)
                    val buildEnvs = if (c is VMBuildContainer) {
                        timeoutMills = Timeout.transMinuteTimeoutToMills(c.jobControlOption?.timeout).second
                        if (c.buildEnv == null) {
                            emptyList<BuildEnv>()
                        } else {
                            val list = ArrayList<BuildEnv>()
                            c.buildEnv!!.forEach { build ->
                                val env = containerAppResource.getBuildEnv(
                                    name = build.key, version = build.value, os = c.baseOS.name.toLowerCase()
                                ).data
                                if (env != null) {
                                    list.add(env)
                                }
                            }
                            list
                        }
                    } else {
                        if (c is NormalContainer) {
                            timeoutMills = Timeout.transMinuteTimeoutToMills(c.jobControlOption?.timeout).second
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
                        variables = variables,
                        buildEnvs = buildEnvs,
                        containerId = c.id!!,
                        containerHashId = c.containerHashId ?: "",
                        jobId = c.jobId,
                        variablesWithType = variablesWithType,
                        timeoutMills = timeoutMills!!,
                        containerType = c.getClassType()
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
        var startUpVMTask = pipelineTaskService.getBuildTask(projectId, buildId, VMUtils.genStartVMTaskId(vmSeqId))

        if (startUpVMTask == null) {
            val buildTasks = pipelineTaskService.listContainerBuildTasks(projectId, buildId, vmSeqId)
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
            pipelineTaskService.updateTaskStatus(
                task = startUpVMTask,
                userId = startUpVMTask.starter,
                buildStatus = buildStatus,
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
                containerBuildDetailService.containerStarted(
                    projectId = projectId,
                    buildId = buildId,
                    containerId = vmSeqId,
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
                containerHashId = startUpVMTask.containerHashId,
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
    fun buildClaimTask(projectId: String, buildId: String, vmSeqId: String, vmName: String): BuildTask {
        return buildClaim(projectId, buildId, vmSeqId, vmName)
    }

    private fun buildClaim(projectId: String, buildId: String, vmSeqId: String, vmName: String): BuildTask {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        if (buildInfo == null || buildInfo.status.isFinish()) {
            LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_END|j($vmSeqId)|buildInfo was finish")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        }

        val allTasks = pipelineTaskService.listContainerBuildTasks(projectId, buildId, vmSeqId)
        val queueTasks: MutableList<PipelineBuildTask> = mutableListOf()
        val runningTasks: MutableList<PipelineBuildTask> = mutableListOf()
        var isContainerFailed = false
        var hasFailedTaskInInSuccessContainer = false
        var continueWhenPreTaskFailed = false
        val allVariable = buildVariableService.getAllVariable(projectId, buildId)
        allTasks.forEachIndexed { index, task ->
            val additionalOptions = task.additionalOptions
            when {
                task.status.isFailure() -> {
                    isContainerFailed = true
                    val taskBehindList = allTasks.subList(min(index + 1, allTasks.size), allTasks.size)
                    val taskExecuteList = allTasks.subList(0, min(index + 1, allTasks.size))
                    run lit@{
                        taskBehindList.forEach { taskBehind ->
                            if (taskBehind.status.isReadyToRun()) {
                                val behindAdditionalOptions = taskBehind.additionalOptions
                                val behindElementPostInfo = behindAdditionalOptions?.elementPostInfo
                                // 判断后续是否有可执行的post任务
                                val postExecuteFlag = TaskUtils.getPostExecuteFlag(taskList = taskExecuteList,
                                    task = taskBehind,
                                    isContainerFailed = isContainerFailed)
                                if (behindAdditionalOptions != null &&
                                    behindAdditionalOptions.enable &&
                                    ((behindElementPostInfo == null &&
                                        behindAdditionalOptions.runCondition
                                        in TaskUtils.getContinueConditionListWhenFail()) ||
                                        postExecuteFlag)
                                ) {
                                    LOG.info("ENGINE|$buildId|j($vmSeqId)|name=${taskBehind.taskName}|" +
                                        "t(${taskBehind.taskId})|vm=$vmName| will run when pre task failed")
                                    continueWhenPreTaskFailed = true
                                    return@lit
                                }
                            }
                        }
                    }
                    // 如果失败的任务自己本身没有开启"失败继续"，同时，后续待执行的任务也没有开启"前面失败还要运行"或者该任务属于可执行post任务，则终止
                    if (additionalOptions?.continueWhenFailed == false && !continueWhenPreTaskFailed) {
                        LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_END|j($vmSeqId)|${task.taskName}|other task failed")
                        return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
                    }
                    // 如果失败的任务自己本身开启了"失败继续"，则container认为是成功的，后续所有的插件都要加入待执行队列
                    if (additionalOptions?.continueWhenFailed == true) {
                        isContainerFailed = false
                        hasFailedTaskInInSuccessContainer = true
                    }
                }
                task.status.isReadyToRun() -> {
                    // 如果当前Container已经执行失败了，但是有配置了前置失败还要执行的插件或者该任务属于可执行post任务，则只能添加这样的插件到队列中
                    val currentTaskExecuteList = allTasks.subList(0, index)
                    val currentElementPostInfo = additionalOptions?.elementPostInfo
                    if (isContainerFailed) {
                        val postExecuteFlag = TaskUtils.getPostExecuteFlag(
                            taskList = currentTaskExecuteList,
                            task = task,
                            isContainerFailed = isContainerFailed
                        )
                        if (continueWhenPreTaskFailed && additionalOptions != null && additionalOptions.enable &&
                            ((currentElementPostInfo == null &&
                                additionalOptions.runCondition in TaskUtils.getContinueConditionListWhenFail()) ||
                                postExecuteFlag
                                )
                        ) {
                            queueTasks.add(task)
                        }
                    } else { // 当前Container成功
                        val postExecuteFlag = TaskUtils.getPostExecuteFlag(
                            taskList = currentTaskExecuteList,
                            task = task,
                            isContainerFailed = isContainerFailed,
                            hasFailedTaskInInSuccessContainer = hasFailedTaskInInSuccessContainer
                        )
                        if ((currentElementPostInfo == null && (additionalOptions == null ||
                                additionalOptions.runCondition != RunCondition.PRE_TASK_FAILED_ONLY ||
                                (additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_ONLY &&
                                    hasFailedTaskInInSuccessContainer))) || postExecuteFlag
                        ) {
                            if (!ControlUtils.checkCustomVariableSkip(buildId, additionalOptions, allVariable)) {
                                queueTasks.add(task)
                            }
                        }
                    }
                }
                task.status.isRunning() -> runningTasks.add(task)
            }
        }

        if (runningTasks.size > 0) { // 已经有运行中的任务，禁止再认领，同一个容器不允许并行
            LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_WAITING|j($vmSeqId)|running=${runningTasks.size}")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
        }

        if (queueTasks.isEmpty()) {
            LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_END|j($vmSeqId)|queue=0")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        }
        if (queueTasks.size > 1) {
            queueTasks.forEach nextQueueTask@{
                return claim(
                    task = it,
                    buildId = buildId,
                    userId = buildInfo.startUser,
                    vmSeqId = vmSeqId,
                    allVariable = allVariable
                )
            }
        } else {
            val nextTask = queueTasks[0]
            if (pipelineTaskService.isNeedPause(
                    taskId = nextTask.taskId,
                    buildId = nextTask.buildId,
                    taskRecord = nextTask
                )
            ) {
                pipelineTaskService.executePause(
                    taskId = nextTask.taskId, buildId = nextTask.buildId, taskRecord = nextTask
                )
                LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_PAUSE_END|j($vmSeqId)|${nextTask.taskId}|" +
                    "Next=${nextTask.taskAtom}")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            return claim(
                task = nextTask,
                buildId = buildId,
                userId = buildInfo.startUser,
                vmSeqId = vmSeqId,
                allVariable = allVariable
            )
        }

        LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_WAIT|j($vmSeqId)|queue=0")
        return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
    }

    private fun claim(
        task: PipelineBuildTask,
        buildId: String,
        userId: String,
        vmSeqId: String,
        allVariable: Map<String, String>
    ): BuildTask {
        LOG.info("ENGINE|$buildId|Agent|CLAIM_TASK_ING|j($vmSeqId)|userId=$userId|[${task.taskId}-${task.taskName}]")
        return when {
            task.taskId == VMUtils.genEndPointTaskId(task.taskSeq) -> { // 全部完成了
                pipelineRuntimeService.claimBuildTask(task, userId) // 刷新一下这个结束的任务节点时间
                BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            task.taskAtom.isNotBlank() -> { // 排除非构建机的插件任务 继续等待直到它完成
                LOG.info("ENGINE|$buildId|taskId=${task.taskId}|taskAtom=${task.taskAtom}|NOT VM TASK, SKIP!")
                BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
            }
            pipelineTaskService.isNeedPause(taskId = task.taskId, buildId = task.buildId, taskRecord = task) -> {
                // 如果插件配置了前置暂停, 暂停期间关闭当前构建机，节约资源。
                pipelineTaskService.executePause(taskId = task.taskId, buildId = task.buildId, taskRecord = task)
                LOG.info("ENGINE|$buildId|taskId=${task.taskId}|taskAtom=${task.taskAtom} cfg pause, shutdown agent")
                BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            else -> {
                // 构造扩展变量
                val extMap = buildExtService.buildExt(task, allVariable)
                val buildVariable = mutableMapOf(
                    PIPELINE_VMSEQ_ID to vmSeqId,
                    PIPELINE_ELEMENT_ID to task.taskId
                )
                buildVariable.putAll(extMap)
                PipelineVarUtil.fillOldVar(buildVariable)
                buildVariable.putAll(allVariable)

                // 认领任务
                pipelineRuntimeService.claimBuildTask(task, userId)
                taskBuildDetailService.taskStart(task.projectId, buildId, task.taskId)
                jmxElements.execute(task.taskType)
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStatusBroadCastEvent(
                        source = "vm-build-claim($vmSeqId)", projectId = task.projectId, pipelineId = task.pipelineId,
                        userId = task.starter, buildId = buildId, taskId = task.taskId, actionType = ActionType.START
                    )
                )
                BuildTask(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    status = BuildTaskStatus.DO,
                    taskId = task.taskId,
                    elementId = task.taskId,
                    stepId = task.stepId,
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
    fun buildCompleteTask(projectId: String, buildId: String, vmSeqId: String, vmName: String, result: BuildTaskResult) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: return
        if (!result.success && !result.type.isNullOrBlank()) {
            jmxElements.fail(result.type!!)
        }
        // 只要buildResult不为空，都写入到环境变量里面
        if (result.buildResult.isNotEmpty()) {
            LOG.info("ENGINE|$buildId| Add the build result size(${result.buildResult.size}) to var")
            try {
                buildVariableService.batchUpdateVariable(projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId, buildId = buildId, variables = result.buildResult
                )
            } catch (ignored: Exception) { // 防止因为变量字符过长而失败。
                LOG.warn("ENGINE|$buildId| save var fail: ${ignored.message}", ignored)
            }
        }

        val errorType = if (!result.errorType.isNullOrBlank()) ErrorType.valueOf(result.errorType!!) else null

        val buildStatus = if (result.success) {
            pipelineTaskService.removeRetryCache(buildId, result.taskId)
            pipelineTaskService.removeFailTaskVar(buildId = buildId, projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId, taskId = result.taskId
            ) // 清理插件错误信息（重试插件成功的情况下）
            BuildStatus.SUCCEED
        } else {
            if (pipelineTaskService.isRetryWhenFail(
                    projectId = buildInfo.projectId,
                    taskId = result.taskId,
                    buildId = buildId
                )
            ) {
                BuildStatus.RETRY
            } else { // 记录错误插件信息
                pipelineTaskService.createFailTaskVar(buildId = buildId, projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId, taskId = result.taskId
                )
                BuildStatus.FAILED
            }
        }

        taskBuildDetailService.taskEnd(
            TaskBuildEndParam(
                projectId = buildInfo.projectId,
                buildId = buildId,
                taskId = result.elementId,
                buildStatus = buildStatus,
                errorType = errorType,
                errorCode = result.errorCode,
                errorMsg = result.message,
                atomVersion = result.elementVersion
            )
        )
        // 重置前置暂停插件暂停状态位
        pipelineTaskPauseService.pauseTaskFinishExecute(buildId, result.taskId)
        pipelineRuntimeService.completeClaimBuildTask(
            completeTask = CompleteTask(
                projectId = buildInfo.projectId, buildId = buildId, taskId = result.taskId,
                userId = buildInfo.startUser, buildStatus = buildStatus,
                errorType = errorType, errorCode = result.errorCode, errorMsg = result.message,
                platformCode = result.platformCode, platformErrorCode = result.platformErrorCode
            )
        )

        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "task-end-${result.taskId}", projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId, userId = buildInfo.startUser,
                buildId = buildId, taskId = result.taskId, actionType = ActionType.END
            )
        )
        // 发送度量数据
        sendElementData(projectId = buildInfo.projectId, buildId = buildId, result = result)

        LOG.info("ENGINE|$buildId|Agent|END_TASK|j($vmSeqId)|${result.taskId}|$buildStatus|" +
            "type=$errorType|code=${result.errorCode}|msg=${result.message}]")
        buildLogPrinter.stopLog(
            buildId = buildId,
            tag = result.elementId,
            jobId = result.containerId ?: ""
        )
    }

    /**
     * 构建机结束当前Job
     */
    fun buildEndTask(projectId: String, buildId: String, vmSeqId: String, vmName: String): Boolean {

        val task = pipelineTaskService.listContainerBuildTasks(projectId, buildId, vmSeqId)
            .filter { it.taskId == VMUtils.genEndPointTaskId(it.taskSeq) }.firstOrNull()

        return if (task == null) {
            LOG.warn("ENGINE|$buildId|name=$vmName|containerId=$vmSeqId|There are no stopVM tasks!")
            false
        } else {
            buildingHeartBeatUtils.dropHeartbeat(buildId = buildId, vmSeqId = vmSeqId, executeCount = task.executeCount)
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
            LOG.info("ENGINE|$buildId|Agent|END_JOB|${task.stageId}|j($vmSeqId)|${task.taskId}|${task.taskName}")
            true
        }
    }

    fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Boolean {
        LOG.info("ENGINE|$buildId|Agent|HEART_BEAT|j($vmSeqId)|$vmName")
        buildingHeartBeatUtils.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun sendElementData(projectId: String, buildId: String, result: BuildTaskResult) {
        try {
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
            LOG.warn("ENGINE|$buildId|Agent|MEASURE|j(${result.containerId})|${result.taskId}|error=$ignored")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineVMBuildService::class.java)
    }
}
