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

import com.tencent.devops.common.api.constant.KEY_CHANNEL
import com.tencent.devops.common.api.constant.KEY_END_TIME
import com.tencent.devops.common.api.constant.KEY_START_TIME
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.AtomMonitorData
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.OrganizationDetailInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.measure.AtomMonitorReportBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.api.ServiceJobQuotaBusinessResource
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.HeartbeatControl
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineTaskPauseService
import com.tencent.devops.process.service.PipelineTaskService
import com.tencent.devops.process.service.measure.AtomMonitorEventDispatcher
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.process.utils.PIPELINE_VMSEQ_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.common.KEY_VERSION
import org.apache.lucene.util.RamUsageEstimator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.ws.rs.NotFoundException

@Service
class PipelineVMBuildService @Autowired(required = false) constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val buildVariableService: BuildVariableService,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val atomMonitorEventDispatcher: AtomMonitorEventDispatcher,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineTaskPauseService: PipelineTaskPauseService,
    private val redisOperation: RedisOperation,
    private val jmxElements: JmxElements,
    private val buildExtService: PipelineBuildExtService,
    private val client: Client,
    private val heartbeatControl: HeartbeatControl
) {

    @Value("\${build.atomMonitorData.report.switch:false}")
    private val atomMonitorSwitch: String = "false"

    @Value("\${build.atomMonitorData.report.maxMonitorDataSize:1677216}")
    private val maxMonitorDataSize: String = "1677216"

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
        heartbeatControl.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
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
        if (buildInfo == null) {
            logger.warn("The pipeline build ($buildId) is not exist")
            throw NotFoundException("Pipeline build ($buildId) is not exist")
        }
        logger.info("[$buildId]|Start the build vmSeqId($vmSeqId) and vmName($vmName)")
        redisOperation.delete(ContainerUtils.getContainerStartupKey(buildInfo.pipelineId, buildId, vmSeqId))

        val variables = buildVariableService.getAllVariable(buildId)
        val variablesWithType = buildVariableService.getAllVariableWithType(buildId)
        val model = (pipelineBuildDetailService.getBuildModel(buildId)
            ?: throw NotFoundException("Does not exist resource in the pipeline"))
        var vmId = 1
        model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach c@{

                if (vmId.toString() == vmSeqId) {
                    // 增加判断状态，如果是已经结束的，拒绝重复启动请求
                    if (BuildStatus.parse(it.startVMStatus).isFinish()) {
                        logger.info("[$buildId]|Start the build vmSeqId($vmSeqId) and vmName($vmName)")
                        throw OperationException("重复启动构建机/Repeat start VM! startVMStatus ${it.startVMStatus}")
                    }
                    var timeoutMills: Long? = null
                    val containerAppResource = client.get(ServiceContainerAppResource::class)
                    val buildEnvs = if (it is VMBuildContainer) {
                        timeoutMills = Timeout.transMinuteTimeoutToMills(it.jobControlOption?.timeout).second
                        if (it.buildEnv == null) {
                            emptyList<BuildEnv>()
                        } else {
                            val list = ArrayList<BuildEnv>()
                            it.buildEnv!!.forEach { build ->
                                val env = containerAppResource.getBuildEnv(name = build.key, version = build.value, os = it.baseOS.name.toLowerCase()).data
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
                    heartbeatControl.addHeartBeat(buildId, vmSeqId, System.currentTimeMillis())
                    // # 2365 将心跳监听事件 构建机主动上报成功状态时才触发
                    heartbeatControl.dispatchHeartbeatEvent(buildInfo = buildInfo, containerId = vmSeqId)
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

        logger.info("[$buildId]|setStartUpVMStatus|taskId=${startUpVMTask?.taskId}|vmSeqId=$vmSeqId|status=$buildStatus")
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
            if (!BuildStatus.isFinish(startUpVMTask.status)) { // #2043 构建机当前启动状态是未结束状态，才进行刷新开妈时间
                pipelineRuntimeService.updateContainerStatus(
                    buildId = buildId,
                    stageId = startUpVMTask.stageId,
                    containerId = startUpVMTask.containerId,
                    startTime = LocalDateTime.now(),
                    endTime = null,
                    buildStatus = BuildStatus.RUNNING
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
        pipelineBuildDetailService.updateStartVMStatus(
            buildId = buildId,
            containerId = startUpVMTask.containerId,
            buildStatus = buildStatus
        )
        return true
    }

    /**
     * 构建机请求执行任务
     */
    fun buildClaimTask(buildId: String, vmSeqId: String, vmName: String): BuildTask {
        return buildClaim(buildId, vmSeqId, vmName)
    }

    private fun addHeartBeat(buildId: String, vmSeqId: String, time: Long, retry: Int = 10) {
        try {
            redisOperation.set(
                HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId),
                time.toString(), TimeUnit.MINUTES.toSeconds(30)
            )
        } catch (t: Throwable) {
            if (retry > 0) {
                logger.warn("Fail to set heart beat variable($vmSeqId -> $time) of $buildId")
                addHeartBeat(buildId, vmSeqId, time, retry - 1)
            } else {
                throw t
            }
        }
    }

    private fun checkCustomVariableSkip(
        buildId: String,
        additionalOptions: ElementAdditionalOptions?,
        variables: Map<String, String>
    ): Boolean {
        // 自定义变量全部满足时不运行
        if (skipWhenCustomVarMatch(additionalOptions)) {
            for (names in additionalOptions?.customVariables!!) {
                val key = names.key
                val value = names.value
                val existValue = variables[key]
                if (value != existValue) {
                    logger.info("buildId=[$buildId]|CUSTOM_VARIABLE_MATCH_NOT_RUN|exists=$existValue|expect=$value")
                    return false
                }
            }
            // 所有自定义条件都满足，则跳过
            return true
        }

        // 自定义变量全部满足时运行
        if (notSkipWhenCustomVarMatch(additionalOptions)) {
            for (names in additionalOptions?.customVariables!!) {
                val key = names.key
                val value = names.value
                val existValue = variables[key]
                if (value != existValue) {
                    logger.info("buildId=[$buildId]|CUSTOM_VARIABLE_MATCH|exists=$existValue|expect=$value")
                    return true
                }
            }
            // 所有自定义条件都满足，则不能跳过
            return false
        }
        return false
    }

    private fun notSkipWhenCustomVarMatch(additionalOptions: ElementAdditionalOptions?) =
        additionalOptions != null && additionalOptions.runCondition == RunCondition.CUSTOM_VARIABLE_MATCH &&
            additionalOptions.customVariables != null && additionalOptions.customVariables!!.isNotEmpty()

    private fun skipWhenCustomVarMatch(additionalOptions: ElementAdditionalOptions?) =
        additionalOptions != null && additionalOptions.runCondition == RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN &&
            additionalOptions.customVariables != null && additionalOptions.customVariables!!.isNotEmpty()

    private fun buildClaim(buildId: String, vmSeqId: String, vmName: String): BuildTask {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            ?: run {
                logger.warn("[$buildId]| buildInfo not found, End")
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
                    val taskBehindList = allTasks.subList(
                        if (index + 1 > allTasks.size)
                            allTasks.size else index + 1, allTasks.size
                    )
                    taskBehindList.forEach { taskBehind ->
                        if (BuildStatus.isReadyToRun(taskBehind.status)) {
                            if (taskBehind.additionalOptions != null &&
                                taskBehind.additionalOptions!!.enable &&
                                (taskBehind.additionalOptions!!.runCondition == RunCondition.PRE_TASK_FAILED_BUT_CANCEL ||
                                    taskBehind.additionalOptions!!.runCondition == RunCondition.PRE_TASK_FAILED_ONLY)
                            ) {
                                logger.info("[$buildId]|containerId=$vmSeqId|name=${taskBehind.taskName}|taskId=${taskBehind.taskId}|vm=$vmName| will run when pre task failed")
                                continueWhenPreTaskFailed = true
                            }
                        }
                    }
                    // 如果失败的任务自己本身没有开启"失败继续"，同时，后续待执行的任务也没有开启"前面失败还要运行"，则终止
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
                    // 如果当前Container已经执行失败了，但是有配置了前置失败还要执行的插件，则只能添加这样的插件到队列中
                    if (isContainerFailed) {
                        if (continueWhenPreTaskFailed && additionalOptions != null && additionalOptions.enable &&
                            (additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_BUT_CANCEL ||
                                additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_ONLY)
                        ) {
                            queueTasks.add(task)
                        }
                    } else { // 当前Container成功
                        if (additionalOptions == null ||
                            additionalOptions.runCondition != RunCondition.PRE_TASK_FAILED_ONLY ||
                            (additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_ONLY &&
                                hasFailedTaskInInSuccessContainer)
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
            logger.info("[$buildId]|containerId=$vmSeqId|runningTasks=${runningTasks.size}|vm=$vmName| wait for running task finish!")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
        }

        if (queueTasks.isEmpty()) {
            logger.info("[$buildId]|containerId=$vmSeqId|queueTasks is empty|vm=$vmName| End")
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
            if (pipelineTaskService.isPause(
                    taskId = nextTask.taskId,
                    buildId = nextTask.buildId
                )
            ) {
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
            val buildTask = claim(
                task = queueTasks[0],
                buildId = buildId,
                userId = buildInfo.startUser,
                vmSeqId = vmSeqId,
                allVariable = allVariable
            )
            if (buildTask != null) {
                return buildTask
            }
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
    ): BuildTask? {
        logger.info("[${task.pipelineId}]|userId=$userId|Claiming task[${task.taskId}-${task.taskName}]")
        if (task.taskId == "end-${task.taskSeq}") {
            pipelineRuntimeService.claimBuildTask(buildId, task, userId) // 刷新一下这个结束的任务节点时间
            // 全部完成了
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        }

        // 排除非构建机的插件任务 继续等待直到它完成
        if (task.taskAtom.isNotBlank()) {
            logger.info("[$buildId]|taskId=${task.taskId}|taskAtom=${task.taskAtom}|do not run in vm agent, skip!")
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.WAIT)
        }

        // 构造扩展变量
        val extMap = buildExtService.buildExt(task)
        // 如果插件配置了前置暂停, 暂停期间关闭当前构建机，节约资源。
        if (pipelineTaskService.isPause(
                taskId = task.taskId,
                buildId = task.buildId
            )
        ) {
            return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
        }

        // 认领任务
        pipelineRuntimeService.claimBuildTask(buildId, task, userId)

        val buildVariable = mutableMapOf(
            PIPELINE_VMSEQ_ID to vmSeqId,
            PIPELINE_ELEMENT_ID to task.taskId
        )

        buildVariable.putAll(extMap)

        PipelineVarUtil.fillOldVar(buildVariable)

        buildVariable.putAll(allVariable)

        val buildTask = BuildTask(
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

        logger.info("[$buildId]|Claim the task - ($buildTask)")
        pipelineBuildDetailService.taskStart(buildId, task.taskId)

        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "vm-build-claim($vmSeqId)",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = task.starter,
                buildId = buildId,
                taskId = task.taskId,
                actionType = ActionType.START
            )
        )

        jmxElements.execute(task.taskType)
        return buildTask
    }

    /**
     * 构建机完成任务请求
     */
    fun buildCompleteTask(buildId: String, vmSeqId: String, vmName: String, result: BuildTaskResult) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return

        if (!result.success) {
            if (result.type.isNullOrBlank()) {
                logger.warn("The element type is null of build $buildId and result $result")
            } else {
                jmxElements.fail(result.type!!)
            }
        }

        // 只要buildResult不为空，都写入到环境变量里面
        if (result.buildResult.isNotEmpty()) {
            logger.info("[$buildId]| Add the build result(${result.buildResult}) to var")
            try {
                buildVariableService.batchUpdateVariable(
                    projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId,
                    buildId = buildId,
                    variables = result.buildResult
                )
            } catch (ignored: Exception) {
                // 防止因为变量字符过长而失败。做下拦截
                logger.warn("[$buildId]| save var fail: ${ignored.message}", ignored)
            }
        }
        logger.info("[$buildId]completeClaimBuildTask|errorType=${result.errorType}|errorCode=${result.errorCode}|message=${result.message}] ")
        val errorType = if (!result.errorType.isNullOrBlank()) {
            ErrorType.valueOf(result.errorType!!)
        } else null

        val buildStatus = if (result.success) {
            pipelineTaskService.removeRetryCache(buildId, result.taskId)
            // 清理插件错误信息（重试插件成功的情况下）
            pipelineTaskService.removeFailVarWhenSuccess(
                buildId = buildId,
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId,
                taskId = result.taskId
            )
            BuildStatus.SUCCEED
        } else {
            if (pipelineTaskService.isRetryWhenFail(result.taskId, buildId)) {
                logger.info("task fail,user setting retry, build[$buildId], taskId[${result.taskId}, elementId[${result.elementId}]]")
                // 此处休眠5s作为重试的冷却时间
                Thread.sleep(5000)
                BuildStatus.RETRY
            } else {
                // 记录错误插件信息
                pipelineTaskService.createFailElementVar(
                    buildId = buildId,
                    projectId = buildInfo.projectId,
                    pipelineId = buildInfo.pipelineId,
                    taskId = result.taskId
                )
                BuildStatus.FAILED
            }
        }

        pipelineBuildDetailService.pipelineTaskEnd(
            buildId = buildId,
            taskId = result.elementId,
            buildStatus = buildStatus,
            errorType = errorType,
            errorCode = result.errorCode,
            errorMsg = result.message
        )

        // 重置前置暂停插件暂停状态位
        pipelineTaskPauseService.pauseTaskFinishExecute(buildId, result.taskId)

        logger.info("Complete the task(${result.taskId}) of build($buildId) and seqId($vmSeqId)")
        pipelineRuntimeService.completeClaimBuildTask(
            buildId = buildId,
            taskId = result.taskId,
            userId = buildInfo.startUser,
            buildStatus = buildStatus,
            errorType = errorType,
            errorCode = result.errorCode,
            errorMsg = result.message
        )
        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "task-end-${result.taskId}",
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId,
                userId = buildInfo.startUser,
                buildId = buildInfo.buildId,
                taskId = result.taskId,
                actionType = ActionType.END
            )
        )

        // 发送度量数据
        sendElementData(
            buildId = buildId,
            vmSeqId = vmSeqId,
            result = result
        )

        buildLogPrinter.stopLog(
            buildId = buildId,
            tag = result.elementId,
            jobId = result.containerId ?: ""
        )
    }

    /**
     * 构建机结束当前Job
     */
    fun buildEndTask(buildId: String, vmSeqId: String, vmName: String): Boolean {

        val tasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
            .filter { it.taskId == "end-${it.taskSeq}" }

        if (tasks.isEmpty()) {
            logger.warn("[$buildId]|name=$vmName|containerId=$vmSeqId|There are no stopVM tasks!")
            return false
        }
        if (tasks.size > 1) {
            logger.warn("[$buildId]|name=$vmName|containerId=$vmSeqId|There are multiple stopVM tasks!")
            return false
        }
        heartbeatControl.dropHeartbeat(buildId = buildId, vmSeqId = vmSeqId)
        pipelineRuntimeService.completeClaimBuildTask(buildId, tasks[0].taskId, tasks[0].starter, BuildStatus.SUCCEED)
        logger.info("Success to complete the build($buildId) of seq($vmSeqId)")
        return true
    }

    fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Boolean {
        logger.info("[$buildId]|Do the heart ($vmSeqId$vmName)")
        heartbeatControl.addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = System.currentTimeMillis())
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun sendElementData(
        buildId: String,
        vmSeqId: String,
        result: BuildTaskResult
    ) {
        val switchFlag = redisOperation.get("atomMonitorSwitch") ?: atomMonitorSwitch
        if (measureService == null && !switchFlag.toBoolean()) {
            return
        }
        val taskId = result.taskId
        try {
            val task = pipelineRuntimeService.getBuildTask(buildId, taskId)!!
            val buildStatus = if (result.success) BuildStatus.SUCCEED else BuildStatus.FAILED
            val taskParams = task.taskParams
            val atomCode = task.atomCode ?: taskParams["atomCode"] as String? ?: task.taskType
            measureService?.postTaskData(
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                taskId = taskId,
                atomCode = atomCode,
                name = task.taskName,
                buildId = buildId,
                startTime = task.startTime?.timestampmilli() ?: 0L,
                status = buildStatus,
                type = result.type!!,
                executeCount = task.executeCount,
                errorType = result.errorType,
                errorCode = result.errorCode,
                errorMsg = result.message,
                userId = task.starter
            )
            // 上报插件监控数据
            val specReportAtoms = redisOperation.get("specReportAtoms")
            if (!switchFlag.toBoolean() || (specReportAtoms != null && specReportAtoms.split(",").contains(atomCode))) {
                // 上报开关关闭或者不在指定上报插件范围内则无需上报监控数据
                return
            }
            val monitorDataMap = result.monitorData
            if (monitorDataMap != null) {
                val monitorDataSize = RamUsageEstimator.sizeOfMap(monitorDataMap)
                if (monitorDataSize > maxMonitorDataSize.toLong()) {
                    // 上报的监控对象大小大于规定的值则不上报
                    logger.info("the build($buildId) of atom($atomCode) monitorDataSize($monitorDataSize) is too large,maxMonitorDataSize is:$maxMonitorDataSize")
                    return
                }
            }
            val version = taskParams[KEY_VERSION] as String? ?: ""
            val monitorStartTime = monitorDataMap?.get(KEY_START_TIME)
            val startTime = (monitorStartTime as? Long) ?: task.startTime?.timestampmilli()
            val monitorEndTime = monitorDataMap?.get(KEY_END_TIME)
            val endTime = (monitorEndTime as? Long) ?: task.endTime?.timestampmilli()
            var project: ProjectVO? = null
            try {
                project = client.get(ServiceProjectResource::class).get(task.projectId).data
            } catch (e: Exception) {
                logger.error("get project(${task.projectId}) info error", e)
            }
            val atomMonitorData = AtomMonitorData(
                errorCode = result.errorCode ?: -1,
                errorMsg = result.message,
                errorType = result.errorType,
                atomCode = atomCode,
                version = version,
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                startTime = startTime,
                endTime = endTime,
                elapseTime = (endTime ?: 0) - (startTime ?: 0),
                channel = monitorDataMap?.get(KEY_CHANNEL) as? String,
                starter = task.starter,
                organizationDetailInfo = OrganizationDetailInfo(
                    bgId = project?.bgId?.toInt(),
                    bgName = project?.bgName,
                    centerId = project?.centerId?.toInt(),
                    centerName = project?.centerName,
                    deptId = project?.deptId?.toInt(),
                    deptName = project?.deptName
                ),
                extData = monitorDataMap?.get("extData") as? Map<String, Any>
            )
            atomMonitorEventDispatcher.dispatch(AtomMonitorReportBroadCastEvent(atomMonitorData))
        } catch (t: Throwable) {
            logger.warn("[$buildId]| Fail to send the measure element data", t)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVMBuildService::class.java)
    }
}
