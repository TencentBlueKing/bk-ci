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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.client.JfrogClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.element.JobCloudsFastPushElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.bkjob.ClearJobTempFileEvent
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.esb.JobCloudsFastPushFile
import com.tencent.devops.process.esb.JobFastPushFile
import com.tencent.devops.process.esb.SourceIp
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class JobCloudsFastPushTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val jobCloudsFastPushFile: JobCloudsFastPushFile,
    private val jobFastPushFile: JobFastPushFile,
    private val buildLogPrinter: BuildLogPrinter,
    private val client: Client,
    private val commonConfig: CommonConfig,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<JobCloudsFastPushElement> {

    override fun getParamElement(task: PipelineBuildTask): JobCloudsFastPushElement {
        return JsonUtil.mapTo(task.taskParams, JobCloudsFastPushElement::class.java)
    }

    @Value("\${clouds.esb.proxyIp}")
    private val cloudStoneIps = ""

    override fun tryFinish(
        task: PipelineBuildTask,
        param: JobCloudsFastPushElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        parseParam(param, runVariables)
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1
        val firstId = task.taskParams[FIRST_ID]?.toString()?.toLong()
            ?: return if (force) AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "JOB_TASK_ID is not correct"
            ) else AtomResponse(task.status)
        val firstStatus = task.taskParams[FIRST_STATUS] as String?

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()

        param.targetAppId = task.taskParams[APP_ID]?.toString()?.toInt() ?: param.targetAppId
        logger.info("[$buildId]|LOOP|$taskId|$firstId|$firstStatus|startTime=$startTime")

        var buildStatus = if (!firstStatus.isNullOrBlank()) {
            try {
                BuildStatus.valueOf(firstStatus!!)
            } catch (ignored: Exception) {
                BuildStatus.RUNNING
            }
        } else {
            BuildStatus.RUNNING
        }

        if (!BuildStatus.isFinish(buildStatus)) {
            buildStatus = jobFastPushFile.checkStatus(
                startTime = startTime,
                maxRunningMins = param.maxRunningMins,
                targetAppId = param.targetAppId,
                taskInstanceId = firstId,
                taskId = taskId,
                containerId = containerId,
                buildId = buildId,
                executeCount = executeCount,
                userId = OPERATOR
            )

            if (BuildStatus.isFinish(buildStatus)) {
                buildLogPrinter.addLine(buildId, "文件分发至云石中转服务器完成/send file to [cloudStone TransferSvr] done", taskId, containerId, executeCount)
            }
        }

        task.taskParams[FIRST_STATUS] = buildStatus.name
        if (!BuildStatus.isFinish(buildStatus)) { // 未结束--返回后消息会有下一次轮循
            buildLogPrinter.addLine(buildId, "[Loop]文件分发云石中转服务器/send file to [cloudStone TransferSvr] status: $buildStatus", taskId, containerId, executeCount)
            return AtomResponse(buildStatus)
        }

        clearTempFile(task) // 清除掉临时文件

        if (BuildStatus.isFailure(buildStatus)) { // 步骤1失败，终止
            buildLogPrinter.addRedLine(buildId, "文件分至云石中转服务器失败/send file to [cloudStone TransferSvr] fail", taskId, containerId, executeCount)
            return AtomResponse(buildStatus)
        }

        val lastId = task.taskParams[LAST_ID]?.toString()?.toLong()
        val lastStatus = task.taskParams[LAST_STATUS] as String?
        buildStatus = if (lastId == null) {
            sendCloudStoneFileToSvr(task, param, runVariables)
        } else if (!lastStatus.isNullOrBlank()) {
            try {
                BuildStatus.valueOf(lastStatus!!)
            } catch (ignored: Exception) {
                BuildStatus.RUNNING
            }
        } else {
            BuildStatus.RUNNING
        }

        if (!BuildStatus.isFinish(buildStatus) && lastId != null) {

            buildStatus = jobCloudsFastPushFile.checkStatus(
                startTime = startTime,
                maxRunningMins = param.maxRunningMins,
                targetAppId = param.targetAppId,
                taskInstanceId = lastId,
                taskId = taskId,
                containerId = containerId,
                buildId = buildId,
                executeCount = executeCount,
                userId = task.starter
            )
        }

        if (!BuildStatus.isFinish(buildStatus)) { // 未结束--返回后消息会有下一次轮循
            buildLogPrinter.addLine(buildId, "[Loop]从云石中转服务器分发至目标服务器/send cloudStone Transfer file to TargetSvr status: $buildStatus", taskId, containerId, executeCount)
            return AtomResponse(buildStatus)
        }

        task.taskParams[LAST_STATUS] = buildStatus.name
        if (BuildStatus.isFailure(buildStatus)) { // 步骤2失败，终止
            buildLogPrinter.addRedLine(buildId, "从云石中转服务器分发至目标服务器失败/send cloudStone Transfer file to TargetSvr fail", taskId, containerId, executeCount)
            return if (buildStatus == BuildStatus.FAILED)
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorMsg = "send cloud stone file to Svr fail"
                )
            else {
                AtomResponse(buildStatus)
            }
        }
        buildLogPrinter.addLine(buildId, "从云石中转服务器分发至目标服务器成功/send cloudStone Transfer file to TargetSvr done", taskId, containerId, executeCount)

        return AtomResponse(BuildStatus.SUCCEED)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: JobCloudsFastPushElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        parseParam(param, runVariables)

        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1
        param.targetAppId = client.get(ServiceProjectResource::class).get(task.projectId).data?.hybridCcAppId?.toInt()
            ?: run {
                buildLogPrinter.addLine(task.buildId, "找不到绑定海外蓝鲸的业务ID/can not found CC Business ID", task.taskId,
                    task.containerHashId, executeCount
                )
                return defaultFailAtomResponse
            }
        task.taskParams[APP_ID] = param.targetAppId
        // 先把文件分发到云石
        val firstStatus = sendFileToCloudStone(task, runVariables, param)
        if (!BuildStatus.isFinish(firstStatus)) { // 未结束
            return AtomResponse(firstStatus)
        }

        clearTempFile(task) // 清理临时文件

        if (BuildStatus.isFailure(firstStatus)) { // 如果失败则结束
            buildLogPrinter.addRedLine(buildId, "文件分至云石中转服务器失败/send file to [cloudStone TransferSvr] fail", taskId, containerId, executeCount)
            return AtomResponse(firstStatus)
        } else if (BuildStatus.isFinish(firstStatus)) { // 成功了，继续
            buildLogPrinter.addLine(buildId, "文件分发至云石中转服务器完成/send file to [cloudStone TransferSvr] done", taskId, containerId, executeCount)
        }

        // 分发文件
        val secondStatus = sendCloudStoneFileToSvr(task, param, runVariables)
        if (!BuildStatus.isFinish(secondStatus)) { // 未结束
            return AtomResponse(secondStatus)
        }
        if (BuildStatus.isFailure(secondStatus)) {
            buildLogPrinter.addRedLine(buildId, "从云石中转服务器分发至目标服务器失败/send cloudStone Transfer file to TargetSvr fail", taskId, containerId, executeCount)
            return AtomResponse(secondStatus)
        } else if (BuildStatus.isFinish(secondStatus)) { // 成功了，继续
            buildLogPrinter.addLine(buildId, "从云石中转服务器分发至目标服务器成功/send cloudStone Transfer file to TargetSvr done", taskId, containerId, executeCount)
        }

        return AtomResponse(secondStatus)
    }

    private fun sendFileToCloudStone(
        task: PipelineBuildTask,
        runVariables: Map<String, String>,
        param: JobCloudsFastPushElement
    ): BuildStatus {
        val userId = task.starter
        val buildId = task.buildId
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val containerId = task.containerHashId ?: ""
        val executeCount = task.executeCount ?: 1
        val workspace = Files.createTempDirectory("${DigestUtils.md5Hex("$buildId-$taskId")}_").toFile()
        val localFileList = mutableListOf<String>()
        // 先把文件分发到云石
        val cloudStonePath = "/data/jobClouds/${simpleDateFormat.format(Date())}/f-" + UUIDUtil.generate()
        val cloudStoneFileList = mutableListOf<String>()

        val isCustom = param.srcType.toUpperCase() == "CUSTOMIZE"
        val regexPathsStr = parseVariable(param.srcPath, runVariables)
        var count = 0

        try {
            val isRepoGray = repoGray.isGray(projectId, redisOperation)
            buildLogPrinter.addLine(buildId, "use bkrepo: $isRepoGray", taskId, containerId, executeCount)
            if (isRepoGray) {
                regexPathsStr.split(",").forEach { regex ->
                    val files = bkRepoClient.downloadFileByPattern(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        repoName = if (isCustom) "custom" else "pipeline",
                        pathPattern = regex.trim(),
                        destPath = workspace.canonicalPath
                    )
                    count += files.size
                    files.forEach { file ->
                        localFileList.add(file.absolutePath)
                        cloudStoneFileList.add("$cloudStonePath/${file.name}")
                    }
                }
            } else {
                val jfrogClinet = JfrogClient(commonConfig.devopsHostGateway!!, projectId, pipelineId, buildId)
                regexPathsStr.split(",").forEach { regex ->
                    val files = jfrogClinet.downloadFile(regex.trim(), isCustom, workspace.canonicalPath)
                    count += files.size
                    files.forEach { file ->
                        localFileList.add(file.absolutePath)
                        cloudStoneFileList.add("$cloudStonePath/${file.name}")
                    }
                }
            }

            buildLogPrinter.addLine(buildId, "Param cloudStonePath=$cloudStonePath", taskId, containerId, executeCount)
            buildLogPrinter.addLine(buildId, "Param isCustom=$isCustom", taskId, containerId, executeCount)
            buildLogPrinter.addLine(buildId, "Param cloudStoneFileList=$cloudStoneFileList", taskId, containerId, executeCount)
            buildLogPrinter.addLine(buildId, "$count 个文件将被分发/$count file(s) will be distribute...", taskId, containerId, executeCount)
            if (count == 0) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "找不到要传输的文件/Can not find any file: $regexPathsStr",
                    tag = taskId,
                    jobId = containerId,
                    executeCount = executeCount
                )
                return BuildStatus.FAILED
            }

            val localIp = CommonUtils.getInnerIP()
            task.taskParams[BS_TASK_HOST] = localIp
            task.taskParams[TMP_HOST] = localIp
            task.taskParams[WORKSPACE] = workspace.absolutePath

            // 执行超时后的保底清理事件
            pipelineEventDispatcher.dispatch(
                ClearJobTempFileEvent(
                    source = "sendFileToCloudStone",
                    pipelineId = pipelineId,
                    buildId = buildId,
                    projectId = projectId,
                    userId = task.starter,
                    clearFileSet = setOf(workspace.absolutePath),
                    taskId = taskId,
                    routeKeySuffix = task.taskParams[TMP_HOST] as String,
                    delayMills = param.maxRunningMins * 60 * 1000
                )
            )

            buildLogPrinter.addLine(buildId, "开始传输文件至云石服务器/start send file to cloud stone", taskId, containerId, executeCount)

            val starter = OPERATOR
            val taskInstanceId = jobFastPushFile.fastPushFile(
                buildId = buildId,
                operator = starter,
                appId = 100205,
                sourceFileList = localFileList,
                targetIpList = listOf(SourceIp(cloudStoneIps)),
                targetPath = cloudStonePath,
                elementId = taskId,
                containerId = containerId,
                executeCount = executeCount
            )

            val startTime = System.currentTimeMillis()

            val buildStatus = jobFastPushFile.checkStatus(
                startTime = startTime,
                maxRunningMins = param.maxRunningMins,
                targetAppId = param.targetAppId,
                taskInstanceId = taskInstanceId,
                buildId = buildId,
                taskId = taskId,
                containerId = containerId,
                executeCount = executeCount,
                userId = starter
            )

            task.taskParams[FIRST_ID] = taskInstanceId
            task.taskParams[STONE_FILE] = cloudStoneFileList // cloudStonePath + "/" + zipFile.name
            if (BuildStatus.isFinish(buildStatus)) {
                task.taskParams[FIRST_STATUS] = buildStatus.name
            } else {
                task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime
            }
            logger.info("[$buildId]|sendFileToCloudStone| status=$buildStatus")
            return buildStatus
        } catch (e: Throwable) {
            logger.error("[$buildId]|sendFileToCloudStone fail| e=$e", e)
            workspace.deleteRecursively()
            return BuildStatus.FAILED
        }
    }

    private fun sendCloudStoneFileToSvr(
        task: PipelineBuildTask,
        param: JobCloudsFastPushElement,
        runVariables: Map<String, String>
    ): BuildStatus {
        val starter = task.starter
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId ?: ""
        val executeCount = task.executeCount ?: 1
        val targetPathStr = parseVariable(param.targetPath, runVariables)
        val cloudStonePath = task.taskParams[STONE_FILE] as List<String>
        buildLogPrinter.addLine(buildId, "Param openState=${param.openState}", taskId, containerId, executeCount)
        buildLogPrinter.addLine(buildId, "Param ipList=${param.ipList}", taskId, containerId, executeCount)
        val ipList: List<String>? = if (param.ipList.isNotBlank()) {
            param.ipList.split(",")
        } else {
            null
        }
        // 分发文件
        buildLogPrinter.addLine(buildId, "从云石中转服务器分发至目标服务器/send cloudStone Transfer file to TargetSvr...", taskId, containerId, executeCount)
        val secondId = jobCloudsFastPushFile.cloudsFastPushFile(
            buildId = buildId,
            operator = starter,
            sourceFileList = cloudStonePath,
            targetPath = targetPathStr,
            openstate = param.openState,
            ipList = ipList,
            targetAppId = param.targetAppId,
            elementId = taskId,
            containerId = containerId,
            executeCount = executeCount
        )
        val startTime = System.currentTimeMillis()
        val buildStatus = jobCloudsFastPushFile.checkStatus(
            startTime = startTime,
            maxRunningMins = param.maxRunningMins,
            targetAppId = param.targetAppId,
            taskInstanceId = secondId,
            taskId = taskId,
            containerId = containerId,
            buildId = buildId,
            executeCount = executeCount,
            userId = starter
        )
        task.taskParams[LAST_ID] = secondId
        if (BuildStatus.isFinish(buildStatus)) {
            task.taskParams[LAST_STATUS] = buildStatus.name
        } else {
            task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime
        }
        logger.info("[$buildId]|sendCloudStoneFileToSvr| status=$buildStatus")
        return buildStatus
    }

    private fun clearTempFile(task: PipelineBuildTask) {
        val starter = OPERATOR
        val buildId = task.buildId
        val taskId = task.taskId
        val workspacePath = task.taskParams[WORKSPACE] as String?

        val clearFileSet = mutableSetOf<String>()
        if (!workspacePath.isNullOrBlank()) {
            clearFileSet.add(workspacePath!!)
        }

        if (clearFileSet.isEmpty()) {
            logger.info("[$buildId]|clearTempFile| no file need clear")
            return
        }

        pipelineEventDispatcher.dispatch(
            ClearJobTempFileEvent(
                source = "clearTempFile",
                pipelineId = task.pipelineId,
                buildId = buildId,
                projectId = task.projectId,
                userId = starter,
                clearFileSet = clearFileSet,
                taskId = taskId,
                routeKeySuffix = task.taskParams[TMP_HOST] as String
            )
        )
    }

    private fun parseParam(param: JobCloudsFastPushElement, runVariables: Map<String, String>) {
        param.srcPath = parseVariable(param.srcPath, runVariables)
        param.srcNodeId = parseVariable(param.srcNodeId, runVariables)
        param.srcAccount = parseVariable(param.srcAccount, runVariables)
        param.maxRunningMins = parseVariable(param.maxRunningMins.toString(), runVariables).toInt()
        param.targetPath = parseVariable(param.targetPath, runVariables)
        param.openState = parseVariable(param.openState, runVariables)
        param.ipList = parseVariable(param.ipList, runVariables)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobCloudsFastPushTaskAtom::class.java)
        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        private const val FIRST_ID = "_FIRST_ID"
        private const val FIRST_STATUS = "_FIRST_STATUS"
        private const val LAST_ID = "_LAST_ID"
        private const val LAST_STATUS = "_LAST_STATUS"
        private const val STONE_FILE = "_STONE_FILE"
        private const val WORKSPACE = "_WORKSPACE"
        private const val TMP_HOST = "_TMP_HOST"
        private const val APP_ID = "_APP_ID"
        private const val OPERATOR = "johuang" // FIXME  这个是临时方案 @johuang
    }
}
