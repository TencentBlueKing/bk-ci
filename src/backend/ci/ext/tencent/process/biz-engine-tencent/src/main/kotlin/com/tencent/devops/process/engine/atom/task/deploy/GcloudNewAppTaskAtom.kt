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

package com.tencent.devops.process.engine.atom.task.deploy

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.client.JfrogClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.gcloud.HistoryTaskGcloudClient
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.history.GetUploadTaskStatParam
import com.tencent.devops.common.gcloud.api.pojo.history.NewAppParam
import com.tencent.devops.common.gcloud.api.pojo.history.NewResParam
import com.tencent.devops.common.gcloud.api.pojo.history.NewUploadTaskParam
import com.tencent.devops.common.gcloud.api.pojo.history.PrePublishParam
import com.tencent.devops.common.gcloud.api.pojo.history.QueryVersionParam
import com.tencent.devops.common.gcloud.api.pojo.history.UploadUpdateFileParam
import com.tencent.devops.common.pipeline.element.GcloudAppElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.api.ServiceGcloudConfResource
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.gcloud.TicketUtil
import org.apache.commons.lang.math.NumberUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GcloudNewAppTaskAtom @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter,
    private val commonConfig: CommonConfig,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<GcloudAppElement> {

    override fun getParamElement(task: PipelineBuildTask): GcloudAppElement {
        return JsonUtil.mapTo(task.taskParams, GcloudAppElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: GcloudAppElement, runVariables: Map<String, String>): AtomResponse {
        parseParam(param, runVariables)
        buildLogPrinter.addLine(task.buildId, "gcloud element params:\n $param", task.taskId, task.containerHashId, task.executeCount
            ?: 1)

        val gcloudUtil = TicketUtil(client)
        val configId = parseVariable(param.configId, runVariables)
        val gameId = param.gameId
        val ticketId = parseVariable(param.ticketId, runVariables)
        val fileTicketId = parseVariable(param.fileTicketId, runVariables)
        val productId = parseVariable(param.productId, runVariables)
        val versionStr = parseVariable(param.versionStr, runVariables)
        val fileSource = parseVariable(param.fileSource, runVariables)
        val filePath = parseVariable(param.filePath, runVariables)
        val preVersionCount = parseVariable(param.preVersionCount, runVariables)

        var type = param.type ?: "newApp"
        val regionId = param.regionId
        val skipUpload = param.skipUpload
        var https = param.https
        var downloadLink = if (param.downloadLink != null) parseVariable(param.downloadLink, runVariables) else null
        var grayRuleID = if (param.grayRuleId != null) parseVariable(param.grayRuleId, runVariables) else null
        var versionDes = if (param.versionDes != null) parseVariable(param.versionDes, runVariables) else null
        var customStr = if (param.customStr != null) parseVariable(param.customStr, runVariables) else null
        var versionType = if (param.versionType != null) parseVariable(param.versionType, runVariables) else null
        var normalUserCanUse = if (param.normalUserCanUse != null) parseVariable(param.normalUserCanUse, runVariables) else null
        var grayUserCanUse = if (param.grayUserCanUse != null) parseVariable(param.grayUserCanUse, runVariables) else null
        var appVersionStr = if (param.appVersionStr != null) parseVariable(param.appVersionStr, runVariables) else null

        if (type == "null") type = "newApp"
        if (https == "null") https = null
        if (downloadLink == "null") downloadLink = null
        if (grayRuleID == "null") grayRuleID = null
        if (versionDes == "null") versionDes = null
        if (customStr == "null") customStr = null
        if (versionType == "null") versionType = null
        if (normalUserCanUse == "null") normalUserCanUse = null
        if (grayUserCanUse == "null") grayUserCanUse = null
        if (appVersionStr == "null" || appVersionStr.isNullOrBlank()) appVersionStr = null

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val userId = task.starter

        /*
        *
        * 版本目标用户。可选值：
        * 0 - 不可用，1 - 普通用户可用，2 - 灰度用户可用，
        * 3 - 普通用户和灰度用户都可用，4 - 审核版本，缺省 0
        *
        * */
        val availableType = if (versionType == "0") {
            if (normalUserCanUse == "1" && grayUserCanUse == "0") {
                1
            } else if (normalUserCanUse == "0" && grayUserCanUse == "1") {
                2
            } else if (normalUserCanUse == "1" && grayUserCanUse == "1") {
                3
            } else {
                0
            }
        } else {
            4
        }
        val isCustom = fileSource.toUpperCase() == "CUSTOMIZE"
        val destPath = Files.createTempDirectory("gcloud").toAbsolutePath().toString()

        val isRepoGray = repoGray.isGray(projectId, redisOperation)
        buildLogPrinter.addLine(buildId, "use bkrepo: $isRepoGray", taskId, task.containerHashId, task.executeCount
            ?: 1)

        val downloadFileList = if (isRepoGray) {
            bkRepoClient.downloadFileByPattern(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                repoName = if (isCustom) "custom" else "pipeline",
                pathPattern = filePath,
                destPath = destPath
            )
        } else {
            val jfrogClient = JfrogClient(commonConfig.devopsHostGateway ?: "", projectId, pipelineId, buildId)
            jfrogClient.downloadFile(filePath, isCustom, destPath)
        }

        // 获取accessId和accessKey
        val keyPair = gcloudUtil.getAccesIdAndToken(projectId, ticketId)
        val accessId = keyPair.first
        val accessKey = keyPair.second

        // 获取文件上传的accessId和accessKey
        var fileAccessId = accessId
        var fileAccessKey = accessKey
        if (fileTicketId.isNotEmpty()) {
            val fileKeyPair = gcloudUtil.getAccesIdAndToken(projectId, fileTicketId)
            fileAccessId = fileKeyPair.first
            fileAccessKey = fileKeyPair.second
        }

        val commonParam = CommonParam(gameId, accessId, accessKey)
        val fileCommonParam = CommonParam(gameId, fileAccessId, fileAccessKey)

        val host = client.get(ServiceGcloudConfResource::class).getByConfigId(configId.toInt()).data
        if (host == null) {
            buildLogPrinter.addRedLine(buildId, "unknown configId($configId)", taskId, task.containerHashId, task.executeCount
                ?: 1)
            return AtomResponse(BuildStatus.FAILED)
        }

        val gcloudClient = HistoryTaskGcloudClient(objectMapper, host.address, host.fileAddress)

        if (downloadFileList.isEmpty()) {
            buildLogPrinter.addRedLine(buildId, "匹配不到待分发的文件: $filePath", taskId, task.containerHashId, task.executeCount
                ?: 1)
            return AtomResponse(BuildStatus.FAILED)
        }

        val diffVersions = getPreVersion(productId, preVersionCount, commonParam, gcloudClient)?.joinToString("|")

        downloadFileList.forEach { downloadFile ->
            buildLogPrinter.addLine(buildId, "开始对文件（${downloadFile.path}）执行Gcloud相关操作，详情请去gcloud官方地址查看：<a target='_blank' href='http://console.gcloud.oa.com/dolphin/channel/$gameId'>查看详情</a>\n", taskId, task.containerHashId, task.executeCount
                ?: 1)

            try {
                // step1
                buildLogPrinter.addLine(buildId, "开始执行 \"创建上传任务\" 操作\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val uploadTaskType = if (type == "newApp") 0 else 1
                val taskParams = NewUploadTaskParam(userId, productId.toInt(), versionStr, uploadTaskType, diffVersions)
                buildLogPrinter.addLine(buildId, "\"创建上传任务\" 参数: $taskParams\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val uploadResult = gcloudClient.newUploadTask(taskParams, commonParam)
                buildLogPrinter.addLine(buildId, "\"创建上传任务\" 响应结果: $uploadResult\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)

                // step2
                buildLogPrinter.addLine(buildId, "开始执行 \"上传更新文件\" 操作\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val gCloudTaskId = uploadResult.result!!["UploadTaskID"] as String
                val taskInfo = uploadResult.result!!["TaskInfo"] as String
                val uploadUpdateFileParam = UploadUpdateFileParam(gCloudTaskId, taskInfo, FileUtil.getMD5(downloadFile))
                buildLogPrinter.addLine(buildId, "\"上传更新文件\" 参数: $uploadUpdateFileParam\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val uploadUpdateFileResult = gcloudClient.uploadUpdateFile(downloadFile, uploadUpdateFileParam, fileCommonParam)
                buildLogPrinter.addLine(buildId, "\"上传更新文件\" 响应结果: $uploadUpdateFileResult\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)

                // step3
                buildLogPrinter.addLine(buildId, "开始执行 \"查询上传任务状态\" 操作\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val getUploadTaskParam = GetUploadTaskStatParam(gCloudTaskId)
                buildLogPrinter.addLine(buildId, "\"查询上传任务状态\" 参数: $getUploadTaskParam\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val versionInfo: String
                loop@ while (true) {
                    val getTaskResult = gcloudClient.getUploadTaskStat(getUploadTaskParam, commonParam)
                    val state = getTaskResult.result!!["State"] as String
                    val message = getTaskResult.message
                    when (state) {
                        "new", "processing", "ready" -> {
                            buildLogPrinter.addLine(buildId, "\"等待查询版本上传任务状态\" 操作执行完毕: \n", taskId, task.containerHashId, task.executeCount
                                ?: 1)
                            buildLogPrinter.addLine(buildId, "\"$getTaskResult\n\n", taskId, task.containerHashId, task.executeCount
                                ?: 1)
                            Thread.sleep(1000 * 6)
                        }
                        "succeeded" -> {
                            buildLogPrinter.addLine(buildId, "\"查询版本上传任务状态\" 操作 成功执行完毕\n", taskId, task.containerHashId, task.executeCount
                                ?: 1)
                            versionInfo = getTaskResult.result!!["VersionInfo"] as String
                            break@loop
                        }
                        else -> {
                            buildLogPrinter.addRedLine(buildId, "上传文件失败: $message($state)", taskId, task.containerHashId, task.executeCount
                                ?: 1)
                            return AtomResponse(BuildStatus.FAILED)
                        }
                    }
                }

                // step 4
                if (type == "newApp") {
                    buildLogPrinter.addLine(buildId, "开始执行 \"创建程序版本\" 操作\n", taskId, task.containerHashId, task.executeCount
                        ?: 1)
                    val newAppParam = NewAppParam(
                        userId, productId.toInt(), versionStr, versionInfo, diffVersions, availableType,
                        if (NumberUtils.isNumber(grayRuleID)) grayRuleID?.toInt() else null,
                        versionDes, customStr)
                    buildLogPrinter.addLine(buildId, "\"创建程序版本\" 参数: $newAppParam\n", taskId, task.containerHashId, task.executeCount
                        ?: 1)
                    val newResResult = gcloudClient.newApp(newAppParam, commonParam)
                    buildLogPrinter.addLine(buildId, "\"创建程序版本\" 响应结果: $newResResult\n", taskId, task.containerHashId, task.executeCount
                        ?: 1)
                } else {
                    buildLogPrinter.addLine(buildId, "开始执行 \"创建资源版本\" 操作\n", taskId, task.containerHashId, task.executeCount
                        ?: 1)
                    val newResParam = NewResParam(appVersionStr ?: "",
                        userId, productId.toInt(), versionStr, versionInfo, availableType,
                        if (NumberUtils.isNumber(grayRuleID)) grayRuleID?.toInt() else null,
                        versionDes, customStr)
                    buildLogPrinter.addLine(buildId, "\"创建资源版本\" 参数: $newResParam\n", taskId, task.containerHashId, task.executeCount
                        ?: 1)
                    val newResResult = gcloudClient.newRes(newResParam, commonParam)
                    buildLogPrinter.addLine(buildId, "\"创建资源版本\" 响应结果: $newResResult\n", taskId, task.containerHashId, task.executeCount
                        ?: 1)
                }

                // step5
                buildLogPrinter.addLine(buildId, "开始执行 \"预发布单个或多个渠道\" 操作\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
                val prePublishParam = PrePublishParam(userId, productId)
                val prePubResult = gcloudClient.prePublish(prePublishParam, commonParam)
                buildLogPrinter.addLine(buildId, "预发布单个或多个渠道响应结果: $prePubResult\n", taskId, task.containerHashId, task.executeCount
                    ?: 1)
            } finally {
                downloadFile.delete()
            }
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun getPreVersion(productId: String, preVersionCount: String, commonParam: CommonParam, gcloudClient: HistoryTaskGcloudClient): List<String>? {
        if (!NumberUtils.isDigits(preVersionCount)) return null
        val queryVersionParam = QueryVersionParam(productId)
        val result = gcloudClient.queryVersion(queryVersionParam, commonParam).result?.map { it["VersionStr"] as String }
        return result?.subList(0, Math.min(10, preVersionCount.toInt())) // 最多十个
    }

    private fun parseParam(param: GcloudAppElement, runVariables: Map<String, String>) {
        param.type = parseVariable(param.type, runVariables)
        param.configId = parseVariable(param.configId, runVariables)
        param.gameId = parseVariable(param.gameId, runVariables)
        param.ticketId = parseVariable(param.ticketId, runVariables)
        param.fileTicketId = parseVariable(param.fileTicketId, runVariables)
        param.productId = parseVariable(param.productId, runVariables)
        param.versionStr = parseVariable(param.versionStr, runVariables)
        param.diffVersions = parseVariable(param.diffVersions, runVariables)
        param.regionId = parseVariable(param.regionId, runVariables)
        param.skipUpload = parseVariable(param.skipUpload, runVariables)
        param.downloadLink = parseVariable(param.downloadLink, runVariables)
        param.https = parseVariable(param.https, runVariables)
        param.filePath = parseVariable(param.filePath, runVariables)
        param.fileSource = parseVariable(param.fileSource, runVariables)
        param.versionType = parseVariable(param.versionType, runVariables)
        param.normalUserCanUse = parseVariable(param.normalUserCanUse, runVariables)
        param.grayUserCanUse = parseVariable(param.grayUserCanUse, runVariables)
        param.grayRuleId = parseVariable(param.grayRuleId, runVariables)
        param.versionDes = parseVariable(param.versionDes, runVariables)
        param.customStr = parseVariable(param.customStr, runVariables)
        param.appVersionStr = parseVariable(param.appVersionStr, runVariables)
    }
}
