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

import com.google.gson.JsonParser

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.JfrogClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.SecurityElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.api.ServiceFileResource
import com.tencent.devops.plugin.pojo.security.UploadParams
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.BS_ATOM_LOOP_TIMES
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class SecurityTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val commonConfig: CommonConfig
) : IAtomTask<SecurityElement> {

    override fun getParamElement(task: PipelineBuildTask): SecurityElement {
        return JsonUtil.mapTo(task.taskParams, SecurityElement::class.java)
    }

    @Value("\${security.maxTimes}")
    private var maxTimes: Int = 499

    @Value("\${security.isAlert}")
    private var isAlert: Boolean = true

//    @Value("\${gateway.url:#{null}}")
//    private val gatewayUrl: String? = null

    override fun execute(task: PipelineBuildTask, param: SecurityElement, runVariables: Map<String, String>): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        logger.info("Start the security task delegate of build $buildId")
        val apkFiles = parseVariable(param.apkFile, runVariables).trim().split(",")
        val envId = parseVariable(param.envId, runVariables)
        val sourceType = parseVariable(param.sourceType, runVariables)
        val asynchronous = param.asynchronous ?: false

        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val userId = task.starter

        val isCustom = sourceType.toUpperCase() == "CUSTOMIZE"
        val type = if (isCustom) ArtifactoryType.CUSTOM_DIR else ArtifactoryType.PIPELINE
        val buildNo = runVariables[PIPELINE_BUILD_NUM]?.toInt() ?: 0

        buildLogPrinter.addLine(buildId, "maxTimes: $maxTimes", taskId, task.containerHashId, task.executeCount ?: 1)
        buildLogPrinter.addLine(buildId, "isAlert: $isAlert", taskId, task.containerHashId, task.executeCount ?: 1)

        val jfrogClient = JfrogClient(commonConfig.devopsHostGateway!!, projectId, pipelineId, buildId)
        val apkTaskIdMap = mutableMapOf<String, String>()
        val times = 1
        var fileConut = 0
        apkFiles.map { it.trim() }.forEach { regex ->
            val files = jfrogClient.matchFiles(regex, isCustom)
            logger.info("security match files for build($buildId): $files")

            fileConut += files.size
            files.map {
                it.removePrefix("bk-archive/")
                        .removePrefix("bk-custom/")
                        .removePrefix("$projectId/")
            }.forEach { apkFile ->
                if (!apkFile.endsWith(".apk")) {
                    buildLogPrinter.addRedLine(buildId, "非法apkFile: $apkFile", taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(BuildStatus.FAILED)
                }
                val apkTaskId = apkUpload(task, apkFile, type, envId, isCustom, userId, buildNo)
                if (apkTaskId.isBlank()) {
                    buildLogPrinter.addRedLine(task.buildId, "启动[$apkFile]apk包加固任务失败!", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(BuildStatus.FAILED)
                } else {
                    buildLogPrinter.addLine(task.buildId, "启动[$apkFile]apk包加固任务成功!", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    val checkStatus = checkStatus(task, envId, times, apkTaskId)
                    when {
                        BuildStatus.isFailure(checkStatus) -> {
                            buildLogPrinter.addRedLine(task.buildId, "[$apkFile]apk包加固任务失败!", task.taskId, task.containerHashId, task.executeCount ?: 1)
                            return AtomResponse(BuildStatus.FAILED)
                        }
                        BuildStatus.isRunning(checkStatus) -> {
                            buildLogPrinter.addLine(buildId, "[$apkFile]等待上传结果", taskId, task.containerHashId, task.executeCount ?: 1)
                            apkTaskIdMap[apkFile] = apkTaskId
                        }
                        else -> buildLogPrinter.addLine(buildId, "[$apkFile]apk包加固完成", taskId, task.containerHashId, task.executeCount ?: 1)
                    }
                }
            }
        }

        if (fileConut == 0) {
            buildLogPrinter.addRedLine(buildId, "There are 0 file found to execute apk security task", taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.FAILED)
        }

        // 异步直接返回成功
        if (asynchronous) {
            buildLogPrinter.addLine(buildId, "开启异步处理立即返回，详情稍后请前往APK加固：<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/apk/$projectId/$envId/detail'>查看详情</a>", taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        } else if (apkTaskIdMap.isEmpty()) {
            buildLogPrinter.addLine(buildId, "全部加固完成，详情请前往APK加固：<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/apk/$projectId/$envId/detail'>查看详情</a>", taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        }

        task.taskParams["bsApkTaskIdMap"] = JsonUtil.toJson(apkTaskIdMap)
        task.taskParams[BS_ATOM_LOOP_TIMES] = times
        task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = 8000
        return AtomResponse(BuildStatus.LOOP_WAITING)
    }

    override fun tryFinish(task: PipelineBuildTask, param: SecurityElement, runVariables: Map<String, String>, force: Boolean): AtomResponse {
        val buildId = task.buildId
        if (task.taskParams["bsApkTaskIdMap"] == null) {
            buildLogPrinter.addRedLine(buildId, "找不到APK加固任务ID，请联系管理员", task.taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = "找不到APK加固任务ID"
            )
        }
        val oldApkTaskMap = JsonUtil.to<Map<String, String>>(task.taskParams["bsApkTaskIdMap"].toString())
        val times = task.taskParams[BS_ATOM_LOOP_TIMES].toString().toInt() + 1
        val envId = parseVariable(param.envId, runVariables)
        val apkTaskIdMap = mutableMapOf<String, String>()
        var fail = false
        logger.info("[${task.buildId}]|taskId=${task.taskId}|loop_times=$times")
        run outer@{
            oldApkTaskMap.forEach f@{ apkFile, apkTaskId ->
                if (fail)return@f
                val checkStatus = checkStatus(task, envId, times, apkTaskId)
                when {
                    BuildStatus.isFailure(checkStatus) -> {
                        buildLogPrinter.addRedLine(task.buildId, "[$apkFile]apk包加固任务失败!", task.taskId, task.containerHashId, task.executeCount ?: 1)
                        fail = true
                    }
                    BuildStatus.isRunning(checkStatus) -> {
                        apkTaskIdMap[apkFile] = apkTaskId
                    }
                    else -> {
                        buildLogPrinter.addLine(buildId, "[$apkFile]apk包加固完成", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    }
                }
            }
        }

        if (fail) {
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "apk包加固执行失败"
            )
        }

        if (apkTaskIdMap.isEmpty()) {
            buildLogPrinter.addLine(buildId, "全部加固完成，详情请前往APK加固：<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/apk/${task.projectId}/$envId/detail'>查看详情</a>", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        }

        task.taskParams["bsApkTaskIdMap"] = JsonUtil.toJson(apkTaskIdMap)
        task.taskParams[BS_ATOM_LOOP_TIMES] = times
        return AtomResponse(BuildStatus.LOOP_WAITING)
    }

    private fun apkUpload(
        task: PipelineBuildTask,
        apkFile: String,
        type: ArtifactoryType,
        envId: String,
        isCustom: Boolean,
        userId: String,
        buildNo: Int
    ): String {

        buildLogPrinter.addLine(task.buildId, "security file path: $apkFile", task.taskId, task.containerHashId, task.executeCount ?: 1)
        // 获取文件信息，并上传文件
        val jfrogFile = client.get(ServiceArtifactoryResource::class).show(task.projectId, type, apkFile).data!!

        val uploadParams = UploadParams(
                "/" + apkFile.removePrefix("/"),
                task.projectId,
                task.pipelineId,
                task.buildId,
                buildNo,
                task.taskId,
                task.containerHashId ?: "",
                task.executeCount ?: 1,
                isCustom,
                userId,
                envId,
                jfrogFile.size.toString(),
                jfrogFile.checksums.md5,
                jfrogFile.meta["appVersion"] ?: "",
                jfrogFile.meta["appTitle"] ?: "",
                jfrogFile.meta["bundleIdentifier"] ?: ""
        )
        val result = client.getWithoutRetry(ServiceFileResource::class).securityUpload(uploadParams).data ?: ""
        // 解析和保存结果
        buildLogPrinter.addLine(task.buildId, (("security upload response is:\n $result")), task.taskId, task.containerHashId, task.executeCount ?: 1)
        return result.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}")
    }

    private fun checkStatus(task: PipelineBuildTask, envId: String, times: Int, apkTaskId: String): BuildStatus {
        val finalResult = client.get(ServiceFileResource::class).getSecurityResult(envId, task.projectId, task.buildId, task.taskId, apkTaskId).data
        if (parser.parse(finalResult).asJsonObject["ret"].asString == "0") {
            buildLogPrinter.addLine(task.buildId, (("apk包加固完成: $finalResult")), task.taskId, task.containerHashId, task.executeCount ?: 1)
            return BuildStatus.SUCCEED
        }
        buildLogPrinter.addLine(task.buildId, "get the task[$apkTaskId] result is :$finalResult", task.taskId, task.containerHashId, task.executeCount ?: 1)
        // 一个小时多点就算超时
        if (times > maxTimes) {
            if (isAlert) {
                AlertUtils.doAlert("ATOM_SECURITY", AlertLevel.MEDIUM, "apk加固超时",
                        "apk加固超时, projectId: ${task.projectId}, pipelineId: ${task.pipelineId}, buildId: ${task.buildId}\n\n$finalResult")
            }

            buildLogPrinter.addRedLine(task.buildId, "apk加固超时", task.taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_OUTTIME_ERROR,
                errorMsg = "apk加固执行超时"
            )
        }

        // 超过一半时间就告警
        if (times == 250 && isAlert) {
            AlertUtils.doAlert("ATOM_SECURITY", AlertLevel.LOW, "apk加固时间过长告警",
                    "apk加固时间过长, projectId: ${task.projectId}, pipelineId: ${task.pipelineId}, buildId: ${task.buildId}\n\n$finalResult")

            buildLogPrinter.addRedLine(task.buildId, "apk加固过长，继续轮循", task.taskId, task.containerHashId, task.executeCount ?: 1)
        }

        return BuildStatus.LOOP_WAITING
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityTaskAtom::class.java)
        private val parser = JsonParser()
    }
}
