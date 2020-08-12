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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.element.ComDistributionElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.bkjob.ClearJobTempFileEvent
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.esb.JobFastPushFile
import com.tencent.devops.process.esb.SourceIp
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ComDistributeTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val jobFastPushFile: JobFastPushFile,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineUserService: PipelineUserService,
    private val client: Client,
    private val commonConfig: CommonConfig,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<ComDistributionElement> {

    override fun getParamElement(task: PipelineBuildTask): ComDistributionElement {
        return JsonUtil.mapTo(task.taskParams, ComDistributionElement::class.java)
    }

    private val praser = JsonParser()

    private var buildId = ""
    private var projectId = ""
    private var pipelineId = ""
    private var containerId = ""

    override fun tryFinish(
        task: PipelineBuildTask,
        param: ComDistributionElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        buildId = task.buildId
        pipelineId = task.pipelineId
        projectId = task.projectId
        containerId = task.containerHashId ?: ""
        val taskId = task.taskId
        val executeCount = task.executeCount ?: 1

        val firstId = task.taskParams[FIRST_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val firstStatus = task.taskParams[FIRST_STATUS] as String?

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()

        logger.info("[$buildId]|LOOP|$taskId|$firstId|startTime=$startTime")
        if (firstStatus.isNullOrBlank()) {
            val targetAppId = task.taskParams[APP_ID]?.toString()?.toInt() ?: param.appid
            val buildStatus = jobFastPushFile.checkStatus(
                startTime = startTime,
                maxRunningMins = param.maxRunningMins,
                targetAppId = targetAppId,
                taskInstanceId = firstId,
                taskId = taskId,
                containerId = containerId,
                buildId = buildId,
                executeCount = executeCount,
                userId = task.taskParams[STARTER] as String
            )
            if (!BuildStatus.isFinish(buildStatus)) { // 未结束--返回后消息会有下一次轮循
                return AtomResponse(buildStatus)
            }

            clearTempFile(task) // 清除掉临时文件

            task.taskParams[FIRST_STATUS] = buildStatus.name
            if (BuildStatus.isFailure(buildStatus)) { // 步骤1失败，终止
                buildLogPrinter.addRedLine(buildId, "构件分发失败/send file to svr fail", taskId, containerId, executeCount)
                return AtomResponse(buildStatus)
            }
            buildLogPrinter.addLine(buildId, "构件分发成功/send file to svr done", taskId, containerId, executeCount)
        }

        return defaultSuccessAtomResponse
    }

    override fun execute(
        task: PipelineBuildTask,
        param: ComDistributionElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        buildId = task.buildId
        pipelineId = task.pipelineId
        projectId = task.projectId
        containerId = task.containerHashId ?: ""
        val taskId = task.taskId
        val executeCount = task.executeCount ?: 1
        // 分发文件
        val buildStatus = sendFileToSvr(task, runVariables, param)
        if (!BuildStatus.isFinish(buildStatus)) { // 未结束
            return AtomResponse(buildStatus)
        }

        clearTempFile(task) // 清理临时文件

        if (BuildStatus.isFailure(buildStatus)) { // 如果失败则结束
            buildLogPrinter.addRedLine(buildId, "send file to svr fail", taskId, containerId, executeCount)
            return AtomResponse(buildStatus)
        } else if (BuildStatus.isFinish(buildStatus)) { // 成功了，继续
            buildLogPrinter.addLine(buildId, "send file to svr done", taskId, containerId, executeCount)
        }

        return AtomResponse(buildStatus)
    }

    private fun sendFileToSvr(
        task: PipelineBuildTask,
        runVariables: Map<String, String>,
        param: ComDistributionElement
    ): BuildStatus {
        var userId = task.starter
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1
        val workspace = java.nio.file.Files.createTempDirectory("${DigestUtils.md5Hex("$buildId-$taskId")}_").toFile()
        val isRepoGray = repoGray.isGray(projectId, redisOperation)
        buildLogPrinter.addLine(buildId, "use bkrepo: $isRepoGray", taskId, containerId, executeCount)

        val localFileList = mutableListOf<String>()
        val appId = client.get(ServiceProjectResource::class).get(task.projectId).data?.ccAppId?.toInt()
            ?: run {
                buildLogPrinter.addLine(task.buildId, "找不到绑定业务ID/can not found business ID", task.taskId,
                    containerId, executeCount
                )
                return BuildStatus.FAILED
            }
        val isCustom = param.customize
        val regexPathsStr = parseVariable(param.regexPaths, runVariables)
        var count = 0

        try {
            val targetIpsStr = parseVariable(param.targetIps, runVariables)
            val targetPathStr = parseVariable(param.targetPath, runVariables)

            regexPathsStr.split(",").forEach { regex ->
                if (isRepoGray) {
                    val fileList = bkRepoClient.matchBkRepoFile(userId, regex, projectId, pipelineId, buildId, isCustom)
                    val repoName = if (isCustom) "custom" else "pipeline"
                    fileList.forEach { bkrepoFile ->
                        buildLogPrinter.addLine(buildId, "匹配到文件：(${bkrepoFile.displayPath})", taskId, containerId, executeCount)
                        count++
                        val destFile = File(workspace, File(bkrepoFile.displayPath).name)
                        bkRepoClient.downloadFile(userId, projectId, repoName, bkrepoFile.fullPath, destFile)
                        localFileList.add(destFile.absolutePath)
                        logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                    }
                } else {
                    val requestBody = getRequestBody(regex.trim(), isCustom)
                    buildLogPrinter.addLine(
                        buildId,
                        "requestBody:" + requestBody.removePrefix("items.find(").removeSuffix(")"),
                        taskId,
                        containerId,
                        executeCount
                    )

                    val searchUrl = "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/jfrog/api/service/search/aql"
                    val request = Request.Builder()
                        .url(searchUrl)
                        .post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
                        .build()

                    OkhttpUtils.doHttp(request).use { response ->
                        val body = response.body()!!.string()

                        val results = praser.parse(body).asJsonObject["results"].asJsonArray
                        for (i in 0 until results.size()) {
                            count++
                            val obj = results[i].asJsonObject
                            val path = getPath(obj["path"].asString, obj["name"].asString, isCustom)
                            val url = getUrl(path, isCustom)

                            val destFile = File(workspace, obj["name"].asString)
                            OkhttpUtils.downloadFile(url, destFile)
                            localFileList.add(destFile.absolutePath)
                            logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                        }
                    }
                }
            }

            buildLogPrinter.addLine(buildId, "$count 个文件将被分发/$count file(s) will be distribute...", taskId, containerId, executeCount)
            if (count == 0) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "找不到传输文件/Can not find any file! regex: $regexPathsStr",
                    tag = taskId,
                    jobId = containerId,
                    executeCount = executeCount
                )
                return BuildStatus.FAILED
            }

            val localIp = CommonUtils.getInnerIP()
            task.taskParams[BS_TASK_HOST] = localIp
            task.taskParams[TMP_HOST] = localIp
            task.taskParams[APP_ID] = appId
            // 执行超时后的保底清理事件
            pipelineEventDispatcher.dispatch(
                ClearJobTempFileEvent(
                    source = "sendFileToJob",
                    pipelineId = pipelineId,
                    buildId = buildId,
                    projectId = task.projectId,
                    userId = task.starter,
                    clearFileSet = setOf(workspace.absolutePath),
                    taskId = taskId,
                    routeKeySuffix = task.taskParams[TMP_HOST] as String,
                    delayMills = param.maxRunningMins * 60 * 1000
                )
            )

            buildLogPrinter.addLine(buildId, "准备发送文件至服务器/Prepare send file to svr", taskId, containerId, executeCount)

            val targetIpList =
                targetIpsStr.split(",", ";", "\n").filter { StringUtils.isNotBlank(it) }.map { SourceIp(it.trim()) }
                    .toList()
            val lastModifyUserMap = pipelineUserService.listUpdateUsers(setOf(pipelineId))
            val lastModifyUser = lastModifyUserMap[pipelineId]
            if (null != lastModifyUser && userId != lastModifyUser) {
                // 以流水线的最后一次修改人身份执行；如果最后一次修改人也没有这个环境的操作权限，这种情况不考虑，有问题联系产品!
                logger.info("userId:$userId, lastModifyUser:$lastModifyUser")
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "将以用户${lastModifyUser}执行文件传输/Will use $lastModifyUser to distribute file...",
                    tag = taskId,
                    jobId = containerId,
                    executeCount = executeCount
                )
                userId = lastModifyUser
            }
            task.taskParams[STARTER] = userId
            task.taskParams[WORKSPACE] = workspace.absolutePath
            buildLogPrinter.addLine(buildId, "开始传输文件/start send file", taskId, containerId, executeCount)
            val taskInstanceId = jobFastPushFile.fastPushFile(
                buildId = buildId,
                operator = userId,
                appId = appId,
                sourceFileList = localFileList,
                targetIpList = targetIpList,
                targetPath = targetPathStr,
                elementId = taskId,
                containerId = containerId ?: "",
                executeCount = executeCount
            )

            val startTime = System.currentTimeMillis()

            val buildStatus = jobFastPushFile.checkStatus(
                startTime = startTime,
                maxRunningMins = param.maxRunningMins,
                targetAppId = appId,
                taskInstanceId = taskInstanceId,
                buildId = buildId,
                taskId = taskId,
                containerId = containerId ?: "",
                executeCount = executeCount,
                userId = userId
            )

            task.taskParams[FIRST_ID] = taskInstanceId
            if (BuildStatus.isFinish(buildStatus)) {
                task.taskParams[FIRST_STATUS] = buildStatus.name
            } else {
                task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime
            }
            logger.info("[$buildId]|sendFileToJob| status=$buildStatus")
            return buildStatus
        } catch (e: Throwable) {
            logger.error("[$buildId]|sendFileToJob fail| e=$e", e)
            workspace.deleteRecursively()
            return BuildStatus.FAILED
        }
    }

    private fun getRequestBody(regex: String, isCustom: Boolean): String {
        val pathPair = getPathPair(regex)
        val parent = pathPair["parent"]
        val child = pathPair["child"]
        return if (isCustom) {
            val path = Paths.get("bk-custom/$projectId$parent").normalize().toString()
            "items.find(\n" +
                "    {\n" +
                "        \"repo\":{\"\$eq\":\"generic-local\"}, \"path\":{\"\$eq\":\"$path\"}, \"name\":{\"\$match\":\"$child\"}\n" +
                "    }\n" +
                ")"
        } else {
            val path = Paths.get("bk-archive/$projectId/$pipelineId/$buildId$parent").normalize().toString()
            "items.find(\n" +
                "    {\n" +
                "        \"repo\":{\"\$eq\":\"generic-local\"}, \"path\":{\"\$eq\":\"$path\"}, \"name\":{\"\$match\":\"$child\"}\n" +
                "    }\n" +
                ")"
        }
    }

    private fun clearTempFile(task: PipelineBuildTask) {
        val starter = task.starter
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

    private fun getPathPair(regex: String): Map<String, String> {
        // 文件夹，匹配所有文件
        if (regex.endsWith("/")) return mapOf("parent" to "/" + regex.removeSuffix("/"), "child" to "*")

        val f = File(regex)
        return if (f.parent.isNullOrBlank()) mapOf("parent" to "", "child" to regex)
        else mapOf("parent" to "/" + f.parent, "child" to f.name)
    }

    // 处理jfrog传回的路径
    private fun getPath(path: String, name: String, isCustom: Boolean): String {
        return if (isCustom) {
            path.substring(path.indexOf("/") + 1).removePrefix("/$projectId") + "/" + name
        } else {
            path.substring(path.indexOf("/") + 1).removePrefix("/$projectId/$pipelineId/$buildId") + "/" + name
        }
    }

    // 获取jfrog传回的url
    private fun getUrl(realPath: String, isCustom: Boolean): String {
        return if (isCustom) {
            "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/jfrog/storage/service/custom/$realPath"
        } else {
            "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/jfrog/storage/service/archive/$realPath"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ComDistributeTaskAtom::class.java)
        private const val STARTER = "_STARTER"
        private const val FIRST_ID = "_FIRST_ID"
        private const val FIRST_STATUS = "_FIRST_STATUS"
        private const val TMP_HOST = "_TMP_HOST"
        private const val APP_ID = "_APP_ID"
        private const val WORKSPACE = "_WORKSPACE"
    }
}
