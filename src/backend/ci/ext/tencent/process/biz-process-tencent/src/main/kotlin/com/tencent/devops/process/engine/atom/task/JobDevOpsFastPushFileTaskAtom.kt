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

@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.job.JobClient
import com.tencent.devops.common.job.api.pojo.EnvSet
import com.tencent.devops.common.job.api.pojo.FastPushFileRequest
import com.tencent.devops.common.pipeline.element.JobDevOpsFastPushFileElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.bkjob.ClearJobTempFileEvent
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ENV_ID_IS_NULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ENV_NAME_IS_NULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_TARGETENV_TYPE_IS_NULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.util.CommonUtils
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

@Component
@Scope(SCOPE_PROTOTYPE)
class JobDevOpsFastPushFileTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val client: Client,
    private val jobClient: JobClient,
    private val pipelineUserService: PipelineUserService,
    private val rabbitTemplate: RabbitTemplate,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray
) : IAtomTask<JobDevOpsFastPushFileElement> {
    override fun getParamElement(task: PipelineBuildTask): JobDevOpsFastPushFileElement {
        return JsonUtil.mapTo(task.taskParams, JobDevOpsFastPushFileElement::class.java)
    }

    @Value("\${devopsGateway.idc:#{null}}")
    private val gatewayUrl: String? = null

    override fun tryFinish(
        task: PipelineBuildTask,
        param: JobDevOpsFastPushFileElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val buildId = task.buildId
        val taskId = task.taskId
        val projectId = task.projectId
        val executeCount = task.executeCount ?: 1
        val containerId = task.containerHashId

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val operator = task.taskParams[STARTER] as String

        logger.info("[$buildId]|LOOP|$taskId|JOB_TASK_ID=$taskInstanceId|startTime=$startTime")
        val timeout = 0L + (param.timeout ?: 600) * 60000

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            projectId = projectId,
            taskId = taskId,
            containerId = containerId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        if (BuildStatus.isFinish(buildStatus)) {
            clearTempFile(task)
        }

        return AtomResponse(buildStatus)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: JobDevOpsFastPushFileElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val srcType = parseVariable(param.srcType, runVariables)
        val buildStatus = when (srcType) {
            "PIPELINE" -> {
                archive(task, param, runVariables)
            }
            "CUSTOMIZE" -> {
                customizeArchive(task, param, runVariables)
            }
            "REMOTE" -> {
                remotePush(task, param, runVariables)
            }
            else -> {
                logger.error("unknown srcType($srcType), terminate!")
                BuildStatus.FAILED
            }
        }
        if (BuildStatus.isFinish(buildStatus)) {
            clearTempFile(task)
        }
        return AtomResponse(buildStatus)
    }

    private fun customizeArchive(
        task: PipelineBuildTask,
        param: JobDevOpsFastPushFileElement,
        runVariables: Map<String, String>
    ): BuildStatus {
        return archive(task, param, runVariables, true)
    }

    private fun archive(
        task: PipelineBuildTask,
        param: JobDevOpsFastPushFileElement,
        runVariables: Map<String, String>,
        isCustom: Boolean = false
    ): BuildStatus {
        val buildId = task.buildId
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1
        val srcPath = parseVariable(param.srcPath, runVariables)
        val isRepoGray = repoGray.isGray(projectId, redisOperation)
        LogUtils.addLine(rabbitTemplate, buildId, "use bkrepo: $isRepoGray", taskId, containerId, executeCount)

        // 下载所有文件
        var count = 0
        val destPath = Files.createTempDirectory("").toFile().absolutePath
        val localFileList = mutableListOf<String>()
        LogUtils.addLine(rabbitTemplate, buildId, "准备匹配文件: $srcPath", taskId, containerId, executeCount)
        srcPath.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.forEach { path ->
            if (isRepoGray) {
                val fileList = matchBkRepoFile(path, projectId, pipelineId, buildId, isCustom)
                val repoName = if (isCustom) "custom" else "pipeline"
                fileList.forEach { bkrepoFile ->
                    LogUtils.addLine(rabbitTemplate, buildId, "匹配到文件：(${bkrepoFile.displayPath})", taskId, containerId, executeCount)
                    count++
                    val url = "$gatewayUrl/bkrepo/api/service/generic/$projectId/$repoName/${bkrepoFile.fullPath}"
                    val destFile = File(destPath, File(bkrepoFile.displayPath).name)
                    OkhttpUtils.downloadFile(url, destFile, mapOf("X-BKREPO-UID" to "admin")) // todo user
                    localFileList.add(destFile.absolutePath)
                    logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                }
            } else {
                val fileList = matchFile(path, projectId, pipelineId, buildId, isCustom)
                fileList.forEach { jfrogFile ->
                    LogUtils.addLine(rabbitTemplate, buildId, "匹配到文件：(${jfrogFile.uri})", taskId, containerId, executeCount)
                    count++
                    val url = if (isCustom) "$gatewayUrl/jfrog/storage/service/custom/$projectId${jfrogFile.uri}"
                    else "$gatewayUrl/jfrog/storage/service/archive/$projectId/$pipelineId/$buildId${jfrogFile.uri}"
                    val destFile = File(destPath, File(jfrogFile.uri).name)
                    OkhttpUtils.downloadFile(url, destFile)
                    localFileList.add(destFile.absolutePath)
                    logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                }
            }
        }
        if (count == 0) throw RuntimeException("没有匹配到需要分发的文件")
        LogUtils.addLine(rabbitTemplate, buildId, "$count 个文件将被分发", taskId, containerId, executeCount)

        val fileSource = FastPushFileRequest.FileSource(
            files = localFileList,
            envSet = EnvSet(listOf(), listOf(), listOf(EnvSet.IpDto(CommonUtils.getInnerIP()))),
            account = "root"
        )

        val localIp = CommonUtils.getInnerIP()
        task.taskParams[BS_TASK_HOST] = localIp
        task.taskParams[TMP_HOST] = localIp
        task.taskParams[TEMP_DEST_PATH] = destPath
        // 执行超时后的保底清理事件
        pipelineEventDispatcher.dispatch(
            ClearJobTempFileEvent(
                source = "archive",
                pipelineId = pipelineId,
                buildId = buildId,
                projectId = projectId,
                userId = task.starter,
                clearFileSet = setOf(destPath),
                taskId = taskId,
                routeKeySuffix = task.taskParams[TMP_HOST] as String,
                delayMills = (param.timeout ?: 600) * 60000 // 分钟 * 60  * 1000 = X毫秒
            )
        )
        return distribute(task, param, runVariables, fileSource)
    }

    private fun remotePush(
        task: PipelineBuildTask,
        param: JobDevOpsFastPushFileElement,
        runVariables: Map<String, String>
    ): BuildStatus {
        val srcPath = parseVariable(param.srcPath, runVariables)
        val srcNodeId = parseVariable(param.srcNodeId, runVariables)
        val srcAccount = parseVariable(param.srcAccount, runVariables)
        val fileSource = FastPushFileRequest.FileSource(
            listOf(srcPath),
            EnvSet(listOf(), listOf(srcNodeId), listOf(EnvSet.IpDto(""))),
            srcAccount
        )
        return distribute(task, param, runVariables, fileSource)
    }

    // 分发文件
    private fun distribute(
        task: PipelineBuildTask,
        param: JobDevOpsFastPushFileElement,
        runVariables: Map<String, String>,
        fileSource: FastPushFileRequest.FileSource
    ): BuildStatus {
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val targetAccount = parseVariable(param.targetAccount, runVariables)
        val timeout = 0L + (param.timeout ?: 600) * 60000
        var operator = task.starter
        val executeCount = task.executeCount ?: 1

        val pipelineId = task.pipelineId
        val lastModifyUserMap = pipelineUserService.listUpdateUsers(setOf(pipelineId))
        val lastModifyUser = lastModifyUserMap[pipelineId]
        if (null != lastModifyUser && operator != lastModifyUser) {
            // 以流水线的最后一次修改人身份执行；如果最后一次修改人也没有这个环境的操作权限，这种情况不考虑，有问题联系产品!
            logger.info("operator:$operator, lastModifyUser:$lastModifyUser")
            LogUtils.addLine(
                rabbitTemplate,
                buildId,
                "Will use $lastModifyUser to distribute file...",
                taskId,
                containerId,
                executeCount
            )

            operator = lastModifyUser
        }
        val projectId = task.projectId
        val targetPath = parseVariable(param.targetPath, runVariables)
        LogUtils.addLine(rabbitTemplate, buildId, "distribute files to target path : $targetPath", taskId, containerId, executeCount)

        val targetEnvType = parseVariable(param.targetEnvType, runVariables)

        val envSet = when (targetEnvType) {
            "NODE" -> {
                if (param.targetNodeId == null || param.targetNodeId!!.isEmpty()) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "EnvId is not init", taskId, containerId, executeCount)
                    throw BuildTaskException(
                        errorType = ErrorType.USER,
                        errorCode = ERROR_BUILD_TASK_ENV_ID_IS_NULL,
                        errorMsg = "EnvId is not init"
                    )
                }
                val targetNodeId = parseVariable(param.targetNodeId!!.joinToString(","), runVariables).split(",")
                EnvSet(listOf(), targetNodeId, listOf())
            }
            "ENV" -> {
                if (param.targetEnvId == null || param.targetEnvId!!.isEmpty()) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "EnvId is not init", taskId, containerId, executeCount)
                    throw BuildTaskException(
                        errorType = ErrorType.USER,
                        errorCode = ERROR_BUILD_TASK_ENV_ID_IS_NULL,
                        errorMsg = "EnvId is not init"
                    )
                }
                val targetEnvId = parseVariable(param.targetEnvId!!.joinToString(","), runVariables).split(",")
                EnvSet(targetEnvId, listOf(), listOf())
            }
            "ENV_NAME" -> {
                if (param.targetEnvName == null || param.targetEnvName!!.isEmpty()) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "EnvName is not init", taskId, containerId, executeCount)
                    throw BuildTaskException(
                        errorType = ErrorType.USER,
                        errorCode = ERROR_BUILD_TASK_ENV_NAME_IS_NULL,
                        errorMsg = "EnvName is not init"
                    )
                }
                val targetEnvName =
                    parseVariable(param.targetEnvName!!.joinToString(","), runVariables).split(",").map { it.trim() }
                val envIdList = checkAuth(buildId, taskId, containerId, executeCount, operator, projectId, targetEnvName, client)
                EnvSet(envIdList, listOf(), listOf())
            }
            else -> {
                LogUtils.addRedLine(
                    rabbitTemplate,
                    buildId,
                    "Unsupported targetEnvType: $targetEnvType ",
                    taskId,
                    containerId,
                    executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_TARGETENV_TYPE_IS_NULL,
                    errorMsg = "Unsupported targetEnvType: $targetEnvType "
                )
            }
        }

        checkEnvNodeExists(buildId, taskId, containerId, executeCount, operator, projectId, envSet, client)

        // val fileSource = "{\"files\":[\"$srcPath\"],\"envSet\":{\"nodeHashIds\":[\"$srcNodeId\"]},\"account\":\"$srcAccount\"}"
        val fastPushFileReq = FastPushFileRequest(
            operator, listOf(fileSource), targetPath, envSet,
            targetAccount, timeout
        )
        val taskInstanceId = jobClient.fastPushFileDevops(fastPushFileReq, projectId)
        LogUtils.addLine(rabbitTemplate, buildId, "查看结果: ${jobClient.getDetailUrl(projectId, taskInstanceId)}", task.taskId, containerId, executeCount)
        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            projectId = projectId,
            taskId = taskId,
            containerId = containerId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (BuildStatus.isFinish(buildStatus)) {
            clearTempFile(task)
        } else {
            LogUtils.addLine(rabbitTemplate, buildId, "Waiting for job:$taskInstanceId", taskId, containerId, executeCount)
        }

        return buildStatus
    }

    private fun checkStatus(
        startTime: Long,
        maxRunningMills: Long,
        projectId: String,
        taskInstanceId: Long,
        operator: String,
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMills) {
            logger.warn("job getTimeout. getTimeout minutes:${maxRunningMills / 60000}")
            LogUtils.addRedLine(
                rabbitTemplate,
                buildId,
                "Job getTimeout: ${maxRunningMills / 60000} Minutes",
                taskId,
                containerId,
                executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = jobClient.getTaskResult(projectId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addLine(rabbitTemplate, buildId, taskResult.msg, taskId, containerId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addRedLine(rabbitTemplate, buildId, taskResult.msg, taskId, containerId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            BuildStatus.LOOP_WAITING
        }
    }

    private fun clearTempFile(task: PipelineBuildTask) {
        val buildId = task.buildId
        val taskId = task.taskId
        val workspacePath = task.taskParams[TEMP_DEST_PATH] as String?
        val starter = task.taskParams[STARTER] as String?

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
                userId = starter!!,
                clearFileSet = clearFileSet,
                taskId = taskId,
                routeKeySuffix = task.taskParams[TMP_HOST] as String
            )
        )
    }

    private fun checkAuth(
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int,
        operator: String,
        projectId: String,
        envNameList: List<String>,
        client: Client
    ): MutableList<String> {
        val envList =
            client.get(ServiceEnvironmentResource::class).listRawByEnvNames(operator, projectId, envNameList).data
        val envNameExistsList = mutableListOf<String>()
        val envIdList = mutableListOf<String>()
        envList!!.forEach {
            envNameExistsList.add(it.name)
            envIdList.add(it.envHashId)
        }
        val noExistsEnvNames = envNameList.subtract(envNameExistsList)
        if (noExistsEnvNames.isNotEmpty()) {
            logger.error("The envNames not exists, name:$noExistsEnvNames")
            LogUtils.addRedLine(rabbitTemplate, buildId, "以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames", taskId, containerId, executeCount)
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS,
                errorMsg = "以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames"
            )
        }

        // 校验权限
        val userEnvList = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(operator, projectId).data
        val userEnvIdList = mutableListOf<String>()
        userEnvList!!.forEach {
            userEnvIdList.add(it.envHashId)
        }

        val noAuthEnvIds = envIdList.subtract(userEnvIdList)
        if (noAuthEnvIds.isNotEmpty()) {
            logger.error("User does not permit to access the env: $noAuthEnvIds")
            LogUtils.addRedLine(rabbitTemplate, buildId, "用户没有操作这些环境的权限！环境ID：$noAuthEnvIds", taskId, containerId, executeCount)
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI,
                errorMsg = "用户没有操作这些环境的权限！环境ID：$noAuthEnvIds"
            )
        }
        return envIdList
    }

    private fun checkEnvNodeExists(
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int,
        operator: String,
        projectId: String,
        envSet: EnvSet,
        client: Client
    ) {
        if (envSet.envHashIds.isNotEmpty()) {
            val envList = client.get(ServiceEnvironmentResource::class)
                .listRawByEnvHashIds(operator, projectId, envSet.envHashIds).data
            val envIdList = mutableListOf<String>()
            envList!!.forEach {
                envIdList.add(it.envHashId)
            }
            val noExistsEnvIds = envSet.envHashIds.subtract(envIdList)
            if (noExistsEnvIds.isNotEmpty()) {
                logger.error("The envIds not exists, id:$noExistsEnvIds")
                LogUtils.addRedLine(
                    rabbitTemplate,
                    buildId,
                    "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds",
                    taskId,
                    containerId,
                    executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS,
                    errorMsg = "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds"
                )
            }
        }
        if (envSet.nodeHashIds.isNotEmpty()) {
            val nodeList =
                client.get(ServiceNodeResource::class).listRawByHashIds(operator, projectId, envSet.nodeHashIds).data
            val nodeIdList = mutableListOf<String>()
            nodeList!!.forEach {
                nodeIdList.add(it.nodeHashId)
            }
            val noExistsNodeIds = envSet.nodeHashIds.subtract(nodeIdList)
            if (noExistsNodeIds.isNotEmpty()) {
                logger.error("The nodeIds not exists, id:$noExistsNodeIds")
                LogUtils.addRedLine(
                    rabbitTemplate,
                    buildId,
                    "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds",
                    taskId,
                    containerId,
                    executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS,
                    errorMsg = "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds"
                )
            }
        }
    }

    // 匹配文件
    fun matchFile(
        srcPath: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): List<JfrogFile> {
        val result = mutableListOf<JfrogFile>()
        val data = getAllFiles(projectId, pipelineId, buildId, isCustom)

        val matcher = FileSystems.getDefault()
            .getPathMatcher("glob:$srcPath")
        data.files.forEach { jfrogFile ->
            if (matcher.matches(Paths.get(jfrogFile.uri.removePrefix("/")))) {
                result.add(jfrogFile)
            }
        }
        return result
    }

    fun matchBkRepoFile(
        srcPath: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): List<BkRepoFile> {
        val result = mutableListOf<BkRepoFile>()
        val bkRepoData = getAllBkRepoFiles(projectId, pipelineId, buildId, isCustom)
        val matcher = FileSystems.getDefault().getPathMatcher("glob:$srcPath")
        val pipelinePathPrefix = "/$pipelineId/$buildId/"
        bkRepoData.data?.forEach { bkrepoFile ->
            val repoPath = if (isCustom) {
                bkrepoFile.fullPath.removePrefix("/")
            } else {
                bkrepoFile.fullPath.removePrefix(pipelinePathPrefix)
            }
            if (matcher.matches(Paths.get(repoPath))) {
                bkrepoFile.displayPath = repoPath
                result.add(bkrepoFile)
            }
        }
        return result
    }

    private fun getAllBkRepoFiles(projectId: String, pipelineId: String, buildId: String, isCustom: Boolean): BkRepoData {
        logger.info("getAllBkrepoFiles, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, isCustom: $isCustom")
        var url = if (isCustom) {
            "$gatewayUrl/bkrepo/api/service/generic/list/$projectId/custom?includeFolder=true&deep=true"
        } else {
            "$gatewayUrl/bkrepo/api/service/generic/list/$projectId/pipeline/$pipelineId/$buildId?includeFolder=true&deep=true"
        }
        val request = Request.Builder()
            .url(url)
            .header("X-BKREPO-UID", "admin") // todo user
            .get()
            .build()

        // 获取所有的文件和文件夹
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("get bkrepo files fail: $responseBody")
                throw RuntimeException("构建分发获取文件失败")
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, BkRepoData::class.java)
            } catch (e: Exception) {
                logger.error("get bkrepo files fail: $responseBody")
                throw RuntimeException("构建分发获取文件失败")
            }
        }
    }

    // 获取所有的文件和文件夹
    private fun getAllFiles(projectId: String, pipelineId: String, buildId: String, isCustom: Boolean): JfrogFilesData {

        val cusListFilesUrl = "$gatewayUrl/jfrog/api/service/custom/$projectId?list&deep=1&listFolders=1"
        val listFilesUrl = "$gatewayUrl/jfrog/api/service/archive"

        val url = if (!isCustom) "$listFilesUrl/$projectId/$pipelineId/$buildId?list&deep=1&listFolders=1"
        else cusListFilesUrl

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // 获取所有的文件和文件夹
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("get jfrog files($url) fail:\n $responseBody")
                throw RuntimeException("构建分发获取文件失败")
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, JfrogFilesData::class.java)
            } catch (e: Exception) {
                logger.error("get jfrog files($url) fail\n$responseBody")
                throw RuntimeException("构建分发获取文件失败")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobDevOpsFastPushFileTaskAtom::class.java)
        private const val JOB_TASK_ID = "_JOB_TASK_ID"
        private const val STARTER = "_STARTER"
        private const val TMP_HOST = "_TMP_HOST"
        private const val TEMP_DEST_PATH = "_TEMP_DEST_PATH"
    }

    data class JfrogFilesData(
        val uri: String,
        val created: String,
        val files: List<JfrogFile>
    )

    data class JfrogFile(
        val uri: String,
        val size: Long,
        val lastModified: String,
        val folder: Boolean,
        @JsonProperty(required = false)
        val sha1: String = ""
    )

    data class BkRepoFile(
        val fullPath: String,
        var displayPath: String?,
        val size: Long,
        val folder: Boolean
    )

    data class BkRepoData(
        var code: Int,
        var message: String?,
        var data: List<BkRepoFile>
    )
}
