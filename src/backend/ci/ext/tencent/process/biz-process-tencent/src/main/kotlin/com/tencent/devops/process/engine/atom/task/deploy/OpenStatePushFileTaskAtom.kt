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

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.job.JobClient
import com.tencent.devops.common.job.api.pojo.EnvSet
import com.tencent.devops.common.job.api.pojo.OpenStateFastPushFileRequest
import com.tencent.devops.common.pipeline.element.OpenStatePushFileElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.bkjob.ClearJobTempFileEvent
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class OpenStatePushFileTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val client: Client,
    private val jobClient: JobClient,
    private val pipelineUserService: PipelineUserService,
    private val jfrogService: JfrogService,
    private val buildLogPrinter: BuildLogPrinter,
    private val repoGray: RepoGray,
    private val redisOperation: RedisOperation,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<OpenStatePushFileElement> {
    override fun tryFinish(
        task: PipelineBuildTask,
        param: OpenStatePushFileElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val buildId = task.buildId
        val taskId = task.taskId
        val projectId = task.projectId
        val executeCount = task.executeCount ?: 1

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val operator = task.taskParams[STARTER] as String

        logger.info("[$buildId]|LOOP|$taskId|JOB_TASK_ID=$taskInstanceId|startTime=$startTime")
        val timeout = 0L + getTimeout(param)

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            projectId = projectId,
            taskId = taskId,
            containerHashId = task.containerHashId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        if (BuildStatus.isFinish(buildStatus)) {
            clearTempFile(task)
        }

        return AtomResponse(
            buildStatus
        )
    }

    override fun execute(
        task: PipelineBuildTask,
        param: OpenStatePushFileElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        parseParam(param, runVariables)
        val buildStatus = when (param.srcType) {
            "PIPELINE" -> {
                archive(task, param, runVariables)
            }
            "CUSTOMIZE" -> {
                customizeArchive(task, param, runVariables)
            }
            else -> {
                logger.error("unknown srcType(${param.srcType}), terminate!")
                BuildStatus.FAILED
            }
        }

        if (BuildStatus.isFinish(buildStatus)) {
            clearTempFile(task)
        }
        return AtomResponse(buildStatus)
    }

    private fun parseParam(param: OpenStatePushFileElement, runVariables: Map<String, String>) {
        param.srcType = parseVariable(param.srcType, runVariables)
        param.srcPath = parseVariable(param.srcPath, runVariables)
        param.openState = parseVariable(param.openState, runVariables)
        param.maxRunningTime = parseVariable(param.maxRunningTime, runVariables)
    }

    override fun getParamElement(task: PipelineBuildTask): OpenStatePushFileElement {
        return JsonUtil.mapTo(task.taskParams, OpenStatePushFileElement::class.java)
    }

    private fun customizeArchive(
        task: PipelineBuildTask,
        param: OpenStatePushFileElement,
        runVariables: Map<String, String>
    ): BuildStatus {
        return archive(task, param, runVariables, true)
    }

    private fun archive(
        task: PipelineBuildTask,
        param: OpenStatePushFileElement,
        runVariables: Map<String, String>,
        isCustom: Boolean = false
    ): BuildStatus {
        val userId = task.starter
        val buildId = task.buildId
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1

        val srcPath = parseVariable(param.srcPath, runVariables)
        val isRepoGray = repoGray.isGray(projectId, redisOperation)
        buildLogPrinter.addLine(buildId, "use bkrepo: $isRepoGray", taskId, containerId, executeCount)

        // 下载所有文件
        var count = 0
        val destPath = Files.createTempDirectory("openState_").toFile()
        val localFileList = mutableListOf<String>()
        buildLogPrinter.addLine(buildId, "准备匹配文件: $srcPath", taskId, task.containerHashId, executeCount)
        srcPath.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.forEach { path ->
            val files = if (isRepoGray) {
                bkRepoClient.downloadFileByPattern(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    repoName = if (isCustom) "custom" else "pipeline",
                    pathPattern = path,
                    destPath = destPath.canonicalPath
                )
            } else {
                jfrogService.downloadFile(
                    ArtifactorySearchParam(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        regexPath = path,
                        custom = isCustom,
                        executeCount = executeCount,
                        elementId = taskId
                    ),
                    destPath.canonicalPath
                )
            }

            files.forEach { file ->
                localFileList.add(file.absolutePath)
            }
            count += files.size
        }
        if (count == 0) throw throw TaskExecuteException(
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            errorType = ErrorType.USER,
            errorMsg = "没有匹配到需要分发的文件/File not found"
        )
        buildLogPrinter.addLine(buildId, "$count 个文件将被分发/$count files will be distribute", taskId, task.containerHashId, executeCount)

        val localIp = CommonUtils.getInnerIP()
        task.taskParams[BS_TASK_HOST] = localIp
        task.taskParams[TMP_HOST] = localIp
        task.taskParams[TEMP_DEST_PATH] = destPath.absolutePath
        // 执行超时后的保底清理事件
        pipelineEventDispatcher.dispatch(
            ClearJobTempFileEvent(
                source = "archive",
                pipelineId = pipelineId,
                buildId = buildId,
                projectId = projectId,
                userId = task.starter,
                clearFileSet = setOf(destPath.absolutePath),
                taskId = taskId,
                routeKeySuffix = localIp,
                delayMills = getTimeout(param) // 分钟 * 60  * 1000 = X毫秒
            )
        )

        val fileSource = OpenStateFastPushFileRequest.FileSource(
            files = localFileList,
            envSet = EnvSet(listOf(), listOf(), listOf(EnvSet.IpDto(CommonUtils.getInnerIP()))),
            account = "root"
        )
        return distribute(task, param, fileSource)
    }

    private fun getTimeout(param: OpenStatePushFileElement) =
        (param.maxRunningTime?.toInt() ?: 600) * 60 * 1000

    // 分发文件
    private fun distribute(
        task: PipelineBuildTask,
        param: OpenStatePushFileElement,
        fileSource: OpenStateFastPushFileRequest.FileSource
    ): BuildStatus {
        val buildId = task.buildId
        val taskId = task.taskId
        val targetAccount = "user00"
        val timeout = getTimeout(param)
        var operator = task.starter
        val executeCount = task.executeCount ?: 1

        val pipelineId = task.pipelineId
        val lastModifyUserMap = pipelineUserService.listUpdateUsers(setOf(pipelineId))
        val lastModifyUser = lastModifyUserMap[pipelineId]
        if (null != lastModifyUser && operator != lastModifyUser) {
            // 以流水线的最后一次修改人身份执行；如果最后一次修改人也没有这个环境的操作权限，这种情况不考虑，有问题联系产品!
            logger.info("operator:$operator, lastModifyUser:$lastModifyUser")
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "将以用户${lastModifyUser}执行文件传输/Will use $lastModifyUser to distribute file...",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = executeCount
            )

            operator = lastModifyUser
        }
        val projectId = task.projectId
        val targetPath = "/data/home/user00/release"
        buildLogPrinter.addLine(
            buildId = buildId,
            message = "分发目标路径(distribute files to target path) : $targetPath",
            tag = taskId,
            jobId = task.containerHashId,
            executeCount = executeCount
        )

        val envSet = EnvSet(listOf(), listOf(), listOf())
        checkEnvNodeExists(buildId, taskId, task.containerHashId, executeCount, operator, projectId, envSet, client)

        // val fileSource = "{\"files\":[\"$srcPath\"],\"envSet\":{\"nodeHashIds\":[\"$srcNodeId\"]},\"account\":\"$srcAccount\"}"
        val appId = client.get(ServiceProjectResource::class).get(task.projectId).data?.ccAppId?.toInt()
            ?: run {
                buildLogPrinter.addLine(task.buildId, "找不到绑定配置平台的业务ID/can not found CC Business ID", task.taskId,
                    task.containerHashId, executeCount
                )
                return BuildStatus.FAILED
            }
        val fastPushFileReq = OpenStateFastPushFileRequest(
            operator, listOf(fileSource), targetPath, envSet,
            targetAccount, timeout * 1L, appId, param.openState
        )
        val taskInstanceId = jobClient.openStateFastPushFileDevops(fastPushFileReq, projectId)
        buildLogPrinter.addLine(buildId, "查看结果: ${jobClient.getDetailUrl(projectId, taskInstanceId)}", taskId, task.containerId, executeCount)

        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout * 1L,
            projectId = projectId,
            taskId = taskId,
            containerHashId = task.containerHashId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        return buildStatus
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

    private fun checkStatus(
        startTime: Long,
        maxRunningMills: Long,
        projectId: String,
        taskInstanceId: Long,
        operator: String,
        buildId: String,
        taskId: String,
        containerHashId: String?,
        executeCount: Int
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMills) {
            logger.warn("job getTimeout. getTimeout minutes:${maxRunningMills / 60000}")
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "执行超时/Job getTimeout: ${maxRunningMills / 60000} Minutes",
                tag = taskId,
                jobId = containerHashId,
                executeCount = executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = jobClient.getTaskResult(projectId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(buildId, taskResult.msg, taskId, containerHashId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addRedLine(buildId, taskResult.msg, taskId, containerHashId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            buildLogPrinter.addLine(buildId, "执行中/Waiting for job:$taskInstanceId", taskId, containerHashId, executeCount)
            BuildStatus.LOOP_WAITING
        }
    }

    private fun checkEnvNodeExists(
        buildId: String,
        taskId: String,
        containerHashId: String?,
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
                logger.warn("The envIds not exists, id:$noExistsEnvIds")
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "以下这些环境id不存在,请重新修改流水线/Can not found any environment by id ：$noExistsEnvIds",
                    tag = taskId,
                    jobId = containerHashId,
                    executeCount = executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS.toInt(),
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
                logger.warn("The nodeIds not exists, id:$noExistsNodeIds")
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "以下这些节点id不存在,请重新修改流水线/Can not found any node by id ：$noExistsNodeIds",
                    tag = taskId,
                    jobId = containerHashId,
                    executeCount = executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS.toInt(),
                    errorMsg = "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds"
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenStatePushFileElement::class.java)

        private const val JOB_TASK_ID = "_JOB_TASK_ID"
        private const val STARTER = "_STARTER"
        private const val TMP_HOST = "_TMP_HOST"
        private const val TEMP_DEST_PATH = "_TEMP_DEST_PATH"
    }
}
