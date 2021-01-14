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
import com.tencent.devops.dispatch.api.ServiceJobQuotaBusinessResource
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.BuildingHeartBeatUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildVariableService
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
import kotlin.math.min

@Service
class PipelineVMBuildService @Autowired(required = false) constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val buildVariableService: BuildVariableService,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTaskService: PipelineTaskService,
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
    fun buildVMStarted(buildId: String, vmSeqId: String, vmName: String): BuildVariables {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
        Preconditions.checkNotNull(buildInfo, NotFoundException("Pipeline build ($buildId) is not exist"))
        logger.info("[$buildId]|Start the build vmSeqId($vmSeqId) and vmName($vmName)")
        val variables = buildVariableService.getAllVariable(buildId)
        val variablesWithType = buildVariableService.getAllVariableWithType(buildId)
        val model = pipelineBuildDetailService.getBuildModel(buildId)
        Preconditions.checkNotNull(model, NotFoundException("Build Model ($buildId) is not exist"))
        var vmId = 1
        model!!.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach c@{

                if (vmId.toString() == vmSeqId) {
                    // 增加判断状态，如果是已经结束的，拒绝重复启动请求
                    Preconditions.assertTrue(
                        condition = BuildStatus.parse(it.startVMStatus).isFinish(),
                        exception = OperationException("重复启动构建机/Repeat start VM! startVMStatus ${it.startVMStatus}")
                    )
                    var timeoutMills: Long? = null
                    val containerAppResource = client.get(ServiceContainerAppResource::class)
                    val buildEnvs = if (it is VMBuildContainer) {
                        timeoutMills = Timeout.transMinuteTimeoutToMills(it.jobControlOption?.timeout).second
                        if (it.buildEnv == null) {
                            emptyList<BuildEnv>()
                        } else {
                            val list = ArrayList<BuildEnv>()
                            it.buildEnv!!.forEach { build ->
                                val env = containerAppResource.getBuildEnv(
                                    name = build.key, version = build.value, os = it.baseOS.name.toLowerCase()
                                ).data
                                if (env == null) {
                                    logger.warn("The container app($build) is not exist")
                                } else {
                                    list.add(env)
                                }
                            }
                            list
                        }
                    } else {
                        if (it is NormalContainer) {
                            timeoutMills = Timeout.transMinuteTimeoutToMills(it.jobControlOption?.timeout).second
                        }
                        emptyList()
                    }
                    pipelineBuildDetailService.containerStart(buildId = buildId, containerId = vmSeqId.toInt())
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
                    // 告诉dispatch agent启动了，为JOB计时服务
                    try {
                        client.get(ServiceJobQuotaBusinessResource::class)
                            .addRunningAgent(projectId = buildInfo.projectId, buildId = buildId, vmSeqId = vmSeqId)
                    } catch (ignored: Throwable) {
                        logger.error("[$buildId]|FAIL_Job#$vmSeqId|Add running agent to job quota failed.", ignored)
                    }

                    return BuildVariables(
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        vmName = vmName,
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        variables = variables,
                        buildEnvs = buildEnvs,
                        containerId = it.id!!,
                        containerHashId = it.containerId ?: "",
                        variablesWithType = variablesWithType,
                        timeoutMills = timeoutMills!!
                    )
                }
                vmId++
            }
        }

        logger.warn("Fail to find the vm build container($vmSeqId) of $model")
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

        logger.info("[$buildId]|setUpVMStatus|taskId=${startUpVMTask?.taskId}|vmSeqId=$vmSeqId|status=$buildStatus")
        if (startUpVMTask == null) {
            return false
        }

        // 如果是完成状态，则更新构建机启动插件的状态
        if (BuildStatus.isFinish(buildStatus)) {
            pipelineRuntimeService.updateTaskStatus(
                buildId = buildId,
                taskId = startUpVMTask.taskId,
                userId = startUpVMTask.starter,
                buildStatus = buildStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )

            // #2043 上报启动构建机状态时，重新刷新开始时间，以防止调度的耗时占用了Job的超时时间
            if (!BuildStatus.isFinish(startUpVMTask.status)) { // #2043 构建机当前启动状态是未结束状态，才进行刷新开始时间
                pipelineRuntimeService.updateContainerStatus(
                    buildId = buildId,
                    stageId = startUpVMTask.stageId,
                    containerId = startUpVMTask.containerId,
                    startTime = LocalDateTime.now(),
                    endTime = null,
                    buildStatus = BuildStatus.RUNNING
                )
                pipelineBuildDetailService.updateStartVMStatus(
                    buildId = buildId,
                    containerId = startUpVMTask.containerId,
                    buildStatus = buildStatus
                )
            }
        }

        // 失败的话就发终止事件
        var message: String? = null
        val actionType = if (BuildStatus.isFailure(buildStatus)) {
            message = "构建机启动失败，所有插件被终止"
            ActionType.TERMINATE
        } else {
            ActionType.START
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
        return buildClaim(buildId, vmSeqId, vmName)
    }

    private fun buildClaim(buildId: String, vmSeqId: String, vmName: String): BuildTask {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            ?: run {
                logger.warn("[$buildId]| buildInfo not found, End")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
        if (buildInfo.status.isFinish()) {
            logger.warn("[$buildId]| buildInfo is finish, End")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        }

        val allTasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
        val queueTasks: MutableList<PipelineBuildTask> = mutableListOf()
        val runningTasks: MutableList<PipelineBuildTask> = mutableListOf()
        var isContainerFailed = false
        var hasFailedTaskInInSuccessContainer = false
        var continueWhenPreTaskFailed = false
        val allVariable = buildVariableService.getAllVariable(buildId)
        allTasks.forEachIndexed { index, task ->
            val additionalOptions = task.additionalOptions
            when {
                BuildStatus.isFailure(task.status) -> {
                    isContainerFailed = true
                    val taskBehindList = allTasks.subList(min(index + 1, allTasks.size), allTasks.size)
                    val taskExecuteList = allTasks.subList(0, min(index + 1, allTasks.size))
                    run lit@{
                        taskBehindList.forEach { taskBehind ->
                            if (BuildStatus.isReadyToRun(taskBehind.status)) {
                                val behindAdditionalOptions = taskBehind.additionalOptions
                                val behindElementPostInfo = behindAdditionalOptions?.elementPostInfo
                                // 判断后续是否有可执行的post任务
                                val postExecuteFlag = TaskUtils.getPostExecuteFlag(taskExecuteList, taskBehind, isContainerFailed)
                                if (behindAdditionalOptions != null &&
                                    behindAdditionalOptions.enable &&
                                    ((behindElementPostInfo == null && behindAdditionalOptions.runCondition in TaskUtils.getContinueConditionListWhenFail()) || postExecuteFlag)
                                ) {
                                    logger.info("[$buildId]|containerId=$vmSeqId|name=${taskBehind.taskName}|taskId=${taskBehind.taskId}|vm=$vmName| will run when pre task failed")
                                    continueWhenPreTaskFailed = true
                                    return@lit
                                }
                            }
                        }
                    }
                    // 如果失败的任务自己本身没有开启"失败继续"，同时，后续待执行的任务也没有开启"前面失败还要运行"或者该任务属于可执行post任务，则终止
                    if (additionalOptions?.continueWhenFailed == false && !continueWhenPreTaskFailed) {
                        logger.info("[$buildId]|containerId=$vmSeqId|name=${task.taskName}|vm=$vmName| failed, End")
                        return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
                    }
                    // 如果失败的任务自己本身开启了"失败继续"，则container认为是成功的，后续所有的插件都要加入待执行队列
                    if (additionalOptions?.continueWhenFailed == true) {
                        isContainerFailed = false
                        hasFailedTaskInInSuccessContainer = true
                    }
                }
                BuildStatus.isReadyToRun(task.status) -> {
                    // 如果当前Container已经执行失败了，但是有配置了前置失败还要执行的插件或者该任务属于可执行post任务，则只能添加这样的插件到队列中
                    val currentTaskExecuteList = allTasks.subList(0, index)
                    val currentElementPostInfo = additionalOptions?.elementPostInfo
                    if (isContainerFailed) {
                        val postExecuteFlag = TaskUtils.getPostExecuteFlag(currentTaskExecuteList, task, isContainerFailed)
                        if (continueWhenPreTaskFailed && additionalOptions != null && additionalOptions.enable &&
                            ((currentElementPostInfo == null && additionalOptions.runCondition in TaskUtils.getContinueConditionListWhenFail()) || postExecuteFlag)
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
                            if (!ControlUtils.checkCustomVariableSkip(buildId = buildId, additionalOptions = additionalOptions, variables = allVariable)) {
                                queueTasks.add(task)
                                /* #2400 此处逻辑有问题不能在这直接设置Model为跳过：
                                    举例按顺序 Job2 下的Task1 和 Task2 插件都未开始执行， Task2设置为指定条件执行，但条件依赖于Task1生成。
                                    1. Task1正常执行并结束情况下，但执行过程中影响展示上在Task1执行时会提前将Task2设置为跳过，存在误导，但Task1结束后，只要条件满足，Task2仍然会执行
                                    2. Task1失败了，导致条件没设置成功，构建结束。 此时进行重试，Task1重试成功了，条件也设置成功了， 但Task2仍然为SKIP（在启动时就决定了）
                                 */
//                            } else {
//                                pipelineBuildDetailService.taskSkip(buildId, task.taskId)
                            }
                        }
                    }
                }
                BuildStatus.isRunning(task.status) -> runningTasks.add(task)
            }
        }

        if (runningTasks.size > 0) { // 已经有运行中的任务，禁止再认领，同一个容器不允许并行
            logger.info("[$buildId]|c=$vmSeqId|running=${runningTasks.size}|vm=$vmName| wait for running task finish!")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
        }

        if (queueTasks.isEmpty()) {
            logger.info("[$buildId]|c=$vmSeqId|queueTasks is empty|vm=$vmName| End")
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
                ) ?: return@nextQueueTask
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
                logger.info("[$buildId]|PAUSE|taskId=${nextTask.taskId}|taskAtom=${nextTask.taskAtom}|shutdown agent")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            val buildTask = claim(
                task = nextTask,
                buildId = buildId,
                userId = buildInfo.startUser,
                vmSeqId = vmSeqId,
                allVariable = allVariable
            )
            return buildTask
        }

        logger.info("[$buildId]|containerId=$vmSeqId|no found queue task, wait!")
        return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
    }

    private fun claim(
        task: PipelineBuildTask,
        buildId: String,
        userId: String,
        vmSeqId: String,
        allVariable: Map<String, String>
    ): BuildTask {
        logger.info("[$buildId]|userId=$userId|Claiming task[${task.taskId}-${task.taskName}]")
        return if (task.taskId == "end-${task.taskSeq}") { // 全部完成了
            pipelineRuntimeService.claimBuildTask(buildId, task, userId) // 刷新一下这个结束的任务节点时间
            BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        } else if (task.taskAtom.isNotBlank()) { // 排除非构建机的插件任务 继续等待直到它完成
            logger.info("[$buildId]|taskId=${task.taskId}|taskAtom=${task.taskAtom}|do not run in vm agent, skip!")
            BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
        } else if (pipelineTaskService.isNeedPause(taskId = task.taskId, buildId = task.buildId, taskRecord = task)) {
            // 如果插件配置了前置暂停, 暂停期间关闭当前构建机，节约资源。
            pipelineTaskService.executePause(taskId = task.taskId, buildId = task.buildId, taskRecord = task)
            logger.info("[$buildId]|taskId=${task.taskId}|taskAtom=${task.taskAtom} cfg pause, shutdown agent")
            BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        } else {
            // 构造扩展变量
            val extMap = buildExtService.buildExt(task)
            val buildVariable = mutableMapOf(
                PIPELINE_VMSEQ_ID to vmSeqId,
                PIPELINE_ELEMENT_ID to task.taskId
            )
            buildVariable.putAll(extMap)
            PipelineVarUtil.fillOldVar(buildVariable)
            buildVariable.putAll(allVariable)

            // 认领任务
            pipelineRuntimeService.claimBuildTask(buildId, task, userId)
            pipelineBuildDetailService.taskStart(buildId, task.taskId)
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
                elementName = task.taskName,
                type = task.taskType,
                params = task.taskParams.map {
                    val obj = ObjectReplaceEnvVarUtil.replaceEnvVar(it.value, buildVariable)
                    it.key to JsonUtil.toJson(obj)
                }.filter {
                    !it.first.startsWith("@type")
                }.toMap(),
                buildVariable = buildVariable,
                containerType = task.containerType
            )
        }
    }

    /**
     * 构建机完成任务请求
     */
    fun buildCompleteTask(buildId: String, vmSeqId: String, vmName: String, result: BuildTaskResult) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return
        if (!result.success && !result.type.isNullOrBlank()) {
            jmxElements.fail(result.type!!)
        }
        // 只要buildResult不为空，都写入到环境变量里面
        if (result.buildResult.isNotEmpty()) {
            logger.info("[$buildId]| Add the build result size(${result.buildResult.size}) to var")
            try {
                buildVariableService.batchUpdateVariable(projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId, buildId = buildId, variables = result.buildResult
                )
            } catch (ignored: Exception) { // 防止因为变量字符过长而失败。
                logger.warn("[$buildId]| save var fail: ${ignored.message}", ignored)
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
            if (pipelineTaskService.isRetryWhenFail(taskId = result.taskId, buildId = buildId)) {
                BuildStatus.RETRY
            } else { // 记录错误插件信息
                pipelineTaskService.createFailTaskVar(buildId = buildId, projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId, taskId = result.taskId
                )
                BuildStatus.FAILED
            }
        }

        pipelineBuildDetailService.pipelineTaskEnd(
            buildId = buildId, taskId = result.elementId, buildStatus = buildStatus,
            errorType = errorType, errorCode = result.errorCode, errorMsg = result.message
        )
        // 重置前置暂停插件暂停状态位
        pipelineTaskPauseService.pauseTaskFinishExecute(buildId, result.taskId)
        pipelineRuntimeService.completeClaimBuildTask(
            buildId = buildId, taskId = result.taskId, userId = buildInfo.startUser, buildStatus = buildStatus,
            errorType = errorType, errorCode = result.errorCode, errorMsg = result.message
        )

        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "task-end-${result.taskId}", projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId, userId = buildInfo.startUser,
                buildId = buildId, taskId = result.taskId, actionType = ActionType.END
            )
        )
        // 发送度量数据
        sendElementData(buildId = buildId, result = result)

        logger.info("[$buildId]completeTask|c=$vmSeqId|taskId=${result.taskId}|status=$buildStatus" +
            "type=$errorType|code=${result.errorCode}|msg=${result.message}]")
        buildLogPrinter.stopLog(buildId = buildId, tag = result.elementId, jobId = result.containerId ?: "")
    }

    /**
     * 构建机结束当前Job
     */
    fun buildEndTask(buildId: String, vmSeqId: String, vmName: String): Boolean {

        val tasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
            .filter { it.taskId == "end-${it.taskSeq}" }

        return if (tasks.isEmpty()) {
            logger.warn("[$buildId]|name=$vmName|containerId=$vmSeqId|There are no stopVM tasks!")
            false
        } else if (tasks.size > 1) {
            logger.warn("[$buildId]|name=$vmName|containerId=$vmSeqId|There are multiple stopVM tasks!")
            false
        } else {
            buildingHeartBeatUtils.dropHeartbeat(buildId = buildId, vmSeqId = vmSeqId)
            pipelineRuntimeService.completeClaimBuildTask(buildId = buildId, taskId = tasks[0].taskId,
                userId = tasks[0].starter, buildStatus = BuildStatus.SUCCEED
            )
            logger.info("Success to complete the build($buildId) of seq($vmSeqId)")
            true
        }
    }

    fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Boolean {
        logger.info("[$buildId]|Do the heart ($vmSeqId$vmName)")
        buildingHeartBeatUtils.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun sendElementData(buildId: String, result: BuildTaskResult) {
        try {
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
            logger.warn("[$buildId]| Fail to send the measure element data", ignored)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVMBuildService::class.java)
    }
}
