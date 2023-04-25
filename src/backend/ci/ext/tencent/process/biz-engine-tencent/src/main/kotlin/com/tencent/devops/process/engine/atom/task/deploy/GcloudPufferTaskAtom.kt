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

package com.tencent.devops.process.engine.atom.task.deploy

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.gcloud.DynamicGcloudClient
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.PrePublishParam
import com.tencent.devops.common.gcloud.api.pojo.UploadResParam
import com.tencent.devops.common.gcloud.api.pojo.dyn.DynNewResourceParam
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.GcloudPufferElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.plugin.api.ServiceGcloudConfResource
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.gcloud.TicketUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GcloudPufferTaskAtom @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<GcloudPufferElement> {

    override fun execute(task: PipelineBuildTask, param: GcloudPufferElement, runVariables: Map<String, String>): AtomResponse {
        parseParam(param, runVariables)
        buildLogPrinter.addLine(task.buildId, "gcloud element params:\n $param", task.taskId, task.containerHashId, task.executeCount
            ?: 1)

        val gcloudUtil = TicketUtil(client)
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val buildId = task.buildId
        val taskId = task.taskId
        val userId = task.starter

        with(param) {
            val host = client.get(ServiceGcloudConfResource::class).getByConfigId(configId.toInt()).data
            if (host == null) {
                buildLogPrinter.addRedLine(task.buildId, "unknown configId($configId)", task.taskId,
                    task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(BuildStatus.FAILED)
            }
            val isCustom = fileSource.toUpperCase() == "CUSTOMIZE"
            val destPath = Files.createTempDirectory("gcloud").toAbsolutePath().toString()
            val downloadFileList = bkRepoClient.downloadFileByPattern(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                repoName = if (isCustom) "custom" else "pipeline",
                pathPattern = filePath,
                destPath = destPath
            )

            if (downloadFileList.isEmpty()) {
                buildLogPrinter.addRedLine(buildId, "匹配不到待分发的文件: $filePath", taskId, task.containerHashId,
                    task.executeCount ?: 1)
                return AtomResponse(BuildStatus.FAILED)
            }

            // 获取accessId和accessKey
            val keyPair = gcloudUtil.getAccesIdAndToken(task.projectId, ticketId)
            val accessId = keyPair.first
            val accessKey = keyPair.second

            // 获取文件上传的accessId和accessKey
            var fileAccessId = accessId
            var fileAccessKey = accessKey
            if (!fileTicketId.isNullOrBlank()) {
                val fileKeyPair = gcloudUtil.getAccesIdAndToken(task.projectId, fileTicketId!!)
                fileAccessId = fileKeyPair.first
                fileAccessKey = fileKeyPair.second
            }
            val commonParam = CommonParam(gameId, accessId, accessKey)
            val fileCommonParam = CommonParam(gameId, fileAccessId, fileAccessKey)

            downloadFileList.forEach { downloadFile ->
                try {
                    // step 1
                    buildLogPrinter.addLine(
                        buildId,
                        "开始对文件（${downloadFile.path}）执行Gcloud相关操作，详情请去gcloud官方地址查看：" +
                            "<a target='_blank' href='http://console.gcloud.oa.com/dolphin/channel/$gameId'>" +
                            "查看详情</a>\n",
                        taskId,
                        task.containerHashId,
                        task.executeCount ?: 1)
                    val gcloudClient = DynamicGcloudClient(objectMapper, host.address, host.fileAddress)
                    buildLogPrinter.addLine(buildId, "开始执行 \"上传动态资源版本\" 操作\n", taskId, task.containerHashId,
                        task.executeCount ?: 1)
                    val uploadResParam = UploadResParam(productId.toInt(), resourceVersion,
                        FileUtil.getMD5(downloadFile), null, null, https)
                    buildLogPrinter.addLine(buildId, "\"上传动态资源版本\" 操作参数：$uploadResParam\n", taskId,
                        task.containerHashId, task.executeCount ?: 1)
                    val uploadResult = gcloudClient.uploadDynamicRes(downloadFile, uploadResParam, commonParam)

                    // step 2
                    buildLogPrinter.addLine(buildId, "开始执行 \"查询版本上传 CDN 任务状态\" 操作\n", taskId,
                        task.containerHashId, task.executeCount ?: 1)
                    val gCloudTaskId = uploadResult.first
                    val versionInfo: String
                    loop@ while (true) {
                        val getTaskResult = gcloudClient.getUploadTask(gCloudTaskId, fileCommonParam)
                        val state = getTaskResult["state"]
                        val message = getTaskResult["message"] ?: ""
                        when (state) {
                            "waiting", "processing" -> {
                                buildLogPrinter.addLine(buildId, "\"等待查询版本上传 CDN 任务状态\" 操作执行完毕: \n",
                                    taskId, task.containerHashId, task.executeCount ?: 1)
                                buildLogPrinter.addLine(buildId, "\"$getTaskResult\n\n", taskId, task.containerHashId,
                                    task.executeCount ?: 1)
                                Thread.sleep(1000 * 6)
                            }
                            "finished" -> {
                                buildLogPrinter.addLine(buildId, "\"查询版本上传 CDN 任务状态\" 操作 成功执行完毕\n", taskId,
                                    task.containerHashId, task.executeCount ?: 1)
                                versionInfo = getTaskResult["versionInfo"]!!
                                break@loop
                            }
                            else -> {
                                buildLogPrinter.addRedLine(buildId, "上传文件失败: $message($state)", taskId,
                                    task.containerHashId, task.executeCount ?: 1)
                                return AtomResponse(BuildStatus.FAILED)
                            }
                        }
                    }

                    // step 3
                    buildLogPrinter.addLine(buildId, "开始执行 \"创建资源\" 操作\n", taskId, task.containerHashId,
                        task.executeCount ?: 1)
                    val newResParam = DynNewResourceParam(task.starter, productId.toInt(), resourceVersion,
                        resourceName, versionInfo, versionType.toInt(), versionDes, customStr)
                    buildLogPrinter.addLine(buildId, "\"创建资源\" 操作参数：$newResParam\n", taskId, task.containerHashId,
                        task.executeCount ?: 1)
                    gcloudClient.newResource(newResParam, commonParam)
                    val prePublishParam = PrePublishParam(task.starter, productId.toInt())

                    // step 4
                    buildLogPrinter.addLine(buildId, "开始执行 \"预发布\" 操作\n", taskId, task.containerHashId,
                        task.executeCount ?: 1)
                    val prePubResult = gcloudClient.prePublish(prePublishParam, commonParam)
                    buildLogPrinter.addLine(buildId, "预发布单个或多个渠道响应结果: $prePubResult\n", taskId,
                        task.containerHashId, task.executeCount ?: 1)
                } finally {
                    downloadFile.delete()
                }
            }
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun parseParam(param: GcloudPufferElement, runVariables: Map<String, String>) {
        param.configId = parseVariable(param.configId, runVariables)
        param.gameId = parseVariable(param.gameId, runVariables)
        param.ticketId = parseVariable(param.ticketId, runVariables)
        param.fileTicketId = parseVariable(param.fileTicketId, runVariables)
        param.productId = parseVariable(param.productId, runVariables)
        param.resourceVersion = parseVariable(param.resourceVersion, runVariables)
        param.resourceName = parseVariable(param.resourceName, runVariables)
        param.https = parseVariable(param.https, runVariables)
        param.filePath = parseVariable(param.filePath, runVariables)
        param.fileSource = parseVariable(param.fileSource, runVariables)
        param.versionType = parseVariable(param.versionType, runVariables)
        param.versionDes = parseVariable(param.versionDes, runVariables)
        param.customStr = parseVariable(param.customStr, runVariables)
    }

    override fun getParamElement(task: PipelineBuildTask): GcloudPufferElement {
        return JsonUtil.mapTo(task.taskParams, GcloudPufferElement::class.java)
    }
}
