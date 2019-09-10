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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.EnvUtils.parseEnv
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.jmx.elements.JmxElements
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.process.utils.PIPELINE_TURBO_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_VMSEQ_ID
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.NotFoundException

/**
 * deng
 * 17/01/2018
 */
@Service
class PipelineVMBuildService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildDetailService: PipelineBuildDetailService,
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val jmxElements: JmxElements,
    private val consulClient: ConsulDiscoveryClient?,
    private val client: Client
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
        addHeartBeat(buildId, vmSeqId, System.currentTimeMillis())
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

        val variables = pipelineRuntimeService.getAllVariable(buildId)
        val model = (buildDetailService.getBuildModel(buildId)
            ?: throw NotFoundException("Does not exist resource in the pipeline"))
        var vmId = 1
        model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach c@{

                if (vmId.toString() == vmSeqId) {
                    val buildEnvs = if (it is VMBuildContainer) {
                        if (it.buildEnv == null) {
                            emptyList<BuildEnv>()
                        } else {
                            val list = ArrayList<BuildEnv>()
                            it.buildEnv!!.forEach { build ->
                                val env = client.get(ServiceContainerAppResource::class).getBuildEnv(
                                    build.key,
                                    build.value,
                                    it.baseOS.name.toLowerCase()
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
                        emptyList()
                    }
                    buildDetailService.containerStart(buildId, vmSeqId.toInt())
                    addHeartBeat(buildId, vmSeqId, System.currentTimeMillis())
                    setStartUpVMStatus(
                        projectId = buildInfo.projectId, pipelineId = buildInfo.pipelineId,
                        buildId = buildId, vmSeqId = vmSeqId, buildStatus = BuildStatus.SUCCEED
                    )
                    return BuildVariables(
                        buildId, vmSeqId, vmName,
                        buildInfo.projectId, buildInfo.pipelineId, variables, buildEnvs
                    )
                }
                vmId++
            }
        }

        logger.warn("Fail to find the vm build container($vmSeqId) of $model")
        throw IllegalStateException("Fail to find the vm build container")
    }

    fun setStartUpVMStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        buildStatus: BuildStatus
    ): Boolean {
        val buildTasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
        logger.info("[$buildId]|setStartUpVMStatus|vmSeqId=$vmSeqId|status=$buildStatus|size=${buildTasks.size}")
        if (buildTasks.isNotEmpty()) {
            val startUpVMTask = buildTasks[0]
            // 如果是成功的状态，则更新构建机启动插件的状态
            if (BuildStatus.isFinish(buildStatus)) {
                pipelineRuntimeService.updateTaskStatus(
                    buildId = buildId, taskId = startUpVMTask.taskId,
                    userId = startUpVMTask.starter, buildStatus = buildStatus
                )
            }

            // 失败的话就发终止事件
            val actionType = if (BuildStatus.isFailure(buildStatus)) {
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
                    actionType = actionType
                )
            )
            return true
        }
        return false
    }

//    fun pluginClaimTask(buildId: String, vmSeqId: String, vmName: String): BuildTask {
//        return buildClaim(buildId, vmSeqId, vmName)
//    }

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
                logger.error("[$buildId]| buildInfo not found, End")
                return BuildTask(buildId, vmSeqId, BuildTaskStatus.END)
            }
        val allTasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
        val queueTasks: MutableList<PipelineBuildTask> = mutableListOf()
        val runningTasks: MutableList<PipelineBuildTask> = mutableListOf()
        var isContainerFailed = false
        var hasFailedTaskInInSuccessContainer = false
        var continueWhenPreTaskFailed = false
        val allVariable = pipelineRuntimeService.getAllVariable(buildId)
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
                                logger.info(
                                    "[$buildId]|containerId=$vmSeqId|name=${taskBehind.taskName}|" +
                                        "taskId=${taskBehind.taskId}|vm=$vmName| will run when pre task failed"
                                )
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
                            if (!checkCustomVariableSkip(buildId, additionalOptions, allVariable)) {
                                queueTasks.add(task)
                            }
                        }
                    }
                }
                BuildStatus.isRunning(task.status) -> runningTasks.add(task)
            }
        }

        if (runningTasks.size > 0) { // 已经有运行中的任务，禁止再认领，同一个容器不允许并行
            logger.info(
                "[$buildId]|containerId=$vmSeqId|runningTasks=${runningTasks.size}" +
                    "|vm=$vmName| wait for running task finish!"
            )
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

        val turboTaskId = getTurboTask(task.pipelineId, task.taskId)

        // 认领任务
        pipelineRuntimeService.claimBuildTask(buildId, task, userId)

        val buildVariable = allVariable
            .plus(PIPELINE_VMSEQ_ID to vmSeqId)
            .plus(PIPELINE_ELEMENT_ID to task.taskId)
            .plus(PIPELINE_TURBO_TASK_ID to turboTaskId).toMap()

        val buildTask = BuildTask(buildId,
            vmSeqId,
            BuildTaskStatus.DO,
            task.taskId,
            task.taskId,
            task.taskName,
            task.taskType,
            task.taskParams.map {
                it.key to parseEnv(JsonUtil.toJson(it.value), buildVariable)
            }.filter {
                !it.first.startsWith("@type")
            }.toMap(), buildVariable
        )

        logger.info("[$buildId]|Claim the task - ($buildTask)")
        buildDetailService.taskStart(buildId, task.taskId)
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
                pipelineRuntimeService.batchSetVariable(buildId, result.buildResult)
            } catch (ignored: Exception) {
                // 防止因为变量字符过长而失败。做下拦截
                logger.warn("[$buildId]| save var fail: ${ignored.message}", ignored)
            }
        }

        val buildStatus = if (result.success) BuildStatus.SUCCEED else BuildStatus.FAILED
        buildDetailService.pipelineTaskEnd(buildId, result.elementId, buildStatus)

        logger.info("Complete the task(${result.taskId}) of build($buildId) and seqId($vmSeqId)")
        pipelineRuntimeService.completeClaimBuildTask(buildId, result.taskId, buildInfo.startUser, buildStatus)
        LogUtils.stopLog(rabbitTemplate, buildId, result.elementId)
    }

    /**
     * 构建机结束当前Job
     */
    fun buildEndTask(buildId: String, vmSeqId: String, vmName: String): Boolean {

        val tasks = pipelineRuntimeService.listContainerBuildTasks(buildId, vmSeqId)
            .filter { it.taskId == "end-${it.taskSeq}" }

        if (tasks.isEmpty()) {
            logger.error("[$buildId]|name=$vmName|containerId=$vmSeqId|There are no stopVM tasks!")
            return false
        }
        if (tasks.size > 1) {
            logger.error("[$buildId]|name=$vmName|containerId=$vmSeqId|There are multiple stopVM tasks!")
            return false
        }
        redisOperation.delete(HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId))
        pipelineRuntimeService.completeClaimBuildTask(buildId, tasks[0].taskId, tasks[0].starter, BuildStatus.SUCCEED)
        logger.info("Success to complete the build($buildId) of seq($vmSeqId)")
        return true
    }

    fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Boolean {
        logger.info("[$buildId]|Do the heart ($vmSeqId$vmName)")
        addHeartBeat(buildId, vmSeqId, System.currentTimeMillis())
        return true
    }

    @Suppress("UNCHECKED_CAST")
    fun getTurboTask(pipelineId: String, elementId: String): String {
        try {
            val instances = consulClient!!.getInstances("turbo")
                ?: throw ClientException("找不到任何有效的turbo服务提供者")
            if (instances.isEmpty()) {
                throw ClientException("找不到任何有效的turbo服务提供者")
            }
            val url = "${if (instances[0].isSecure) "https" else
                "http"}://${instances[0].host}:${instances[0].port}/api/service/turbo/task/pipeline/$pipelineId/$elementId"

            logger.info("Get turbo task info, request url: $url")
            val request = Request.Builder().url(url).get().build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()?.string() ?: return ""
                logger.info("Get turbo task info, response: $data")
                if (!response.isSuccessful) {
                    throw RemoteServiceException(data)
                }
                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
                val code = responseData["status"] as Int
                if (0 == code) {
                    val dataMap = responseData["data"] as Map<String, Any>
                    return dataMap["taskId"] as String? ?: ""
                } else {
                    throw RemoteServiceException(data)
                }
            }
        } catch (e: Throwable) {
            logger.warn("Get turbo task info failed, $e")
            return ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVMBuildService::class.java)
    }
}
