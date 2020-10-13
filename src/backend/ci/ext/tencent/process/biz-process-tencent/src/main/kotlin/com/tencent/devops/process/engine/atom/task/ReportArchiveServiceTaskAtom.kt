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

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.api.JFrogStorageApi
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.util.JFrogUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.job.JobClient
import com.tencent.devops.common.job.api.pojo.EnvSet
import com.tencent.devops.common.job.api.pojo.FastPushFileRequest
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.ReportArchiveServiceElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.process.bkjob.ClearJobTempFileEvent
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_IDX_FILE_NOT_EXITS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.service.ReportService
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.process.utils.REPORT_DYNAMIC_ROOT_URL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.regex.Pattern

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReportArchiveServiceTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val reportService: ReportService,
    private val jFrogStorageApi: JFrogStorageApi,
    private val buildLogPrinter: BuildLogPrinter,
    private val jobClient: JobClient,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<ReportArchiveServiceElement> {

    private val regex = Pattern.compile("[,;]")
    override fun getParamElement(task: PipelineBuildTask): ReportArchiveServiceElement {
        return JsonUtil.mapTo(task.taskParams, ReportArchiveServiceElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: ReportArchiveServiceElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val userId = task.starter
        val buildId = task.buildId
        val taskId = task.taskId
        val projectId = task.projectId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val operator = task.taskParams[STARTER] as String
        val isRepoGray = repoGray.isGray(projectId, redisOperation)

        logger.info("[$buildId]|LOOP|$taskId|JOB_TASK_ID=$taskInstanceId|startTime=$startTime")
        val timeout = getTimeoutMills()

        val buildStatus = checkFileTransferStatus(
            startTime = startTime,
            maxRunningMills = timeout.toLong(),
            projectId = projectId,
            taskId = taskId,
            containerId = containerId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        if (!BuildStatus.isFinish(buildStatus)) {
            return AtomResponse(buildStatus)
        }

        val outputVariables = mutableMapOf<String, Any>()
        val destPath = task.taskParams[TEMP_DEST_PATH] as String
        val fileDirParam = parseVariable(param.fileDir, runVariables)
        recordReportDetail(
            userId = userId,
            isRepoGray = isRepoGray,
            param = param,
            runVariables = runVariables,
            destPath = destPath,
            fileDirParam = fileDirParam,
            buildId = buildId,
            taskId = taskId,
            containerId = containerId,
            outputVariables = outputVariables,
            executeCount = executeCount,
            projectId = projectId,
            pipelineId = task.pipelineId
        )

        clearTempFile(task)

        return AtomResponse(buildStatus, outputVariables)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: ReportArchiveServiceElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val userId = task.starter
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1
        if (param.fileDir.isBlank()) {
            buildLogPrinter.addRedLine(buildId, "fileDir is not init", taskId, containerId, executeCount)
            return AtomResponse(BuildStatus.FAILED)
        }

        if (param.reportName.isBlank()) {
            logger.warn("reportName is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "reportName is not init", taskId, containerId, executeCount)
            return AtomResponse(BuildStatus.FAILED)
        }

        val projectId = task.projectId
        val pipelineId = task.pipelineId

        val operator = task.starter
        val nodeIdParam = parseVariable(param.nodeId, runVariables)
        val fileDirParam = parseVariable(param.fileDir, runVariables)

        val destPath = "/tmp/$projectId/$pipelineId/$buildId/$taskId/"

        val outputVariables = mutableMapOf<String, Any>()
        val isRepoGray = repoGray.isGray(projectId, redisOperation)
        buildLogPrinter.addLine(buildId, "use bkrepo: $isRepoGray", taskId, containerId, executeCount)

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
                delayMills = getTimeoutMills()
            )
        )

        val srcPath = "${fileDirParam.removeSuffix("/")}/"

        val srcEnvSet = EnvSet(listOf(), listOf(nodeIdParam), listOf())
        val destEnvSet = EnvSet(listOf(), listOf(), listOf(EnvSet.IpDto(localIp)))
        val fileSource = FastPushFileRequest.FileSource(listOf(srcPath), srcEnvSet, "root")
        val fileRequest = FastPushFileRequest(
            userId = operator,
            fileSources = listOf(fileSource),
            fileTargetPath = destPath,
            envSet = destEnvSet,
            account = "user00",
            timeout = getTimeoutMills().toLong()
        )
        val taskInstanceId = jobClient.fastPushFileDevops(fileRequest, projectId)
        buildLogPrinter.addLine(buildId, "查看结果: ${jobClient.getDetailUrl(projectId, taskInstanceId)}", task.taskId, containerId, executeCount)
        val startTime = System.currentTimeMillis()

        val buildStatus = checkFileTransferStatus(
            startTime = startTime,
            maxRunningMills = getTimeoutMills().toLong(),
            projectId = projectId,
            taskId = task.taskId,
            containerId = containerId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = task.buildId,
            executeCount = executeCount
        )

        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (!BuildStatus.isFinish(buildStatus)) {
            buildLogPrinter.addLine(task.buildId, "报告正在上传 job:$taskInstanceId", task.taskId, task.containerHashId, executeCount)
            return AtomResponse(buildStatus)
        }

        recordReportDetail(
            userId = userId,
            isRepoGray = isRepoGray,
            param = param,
            runVariables = runVariables,
            destPath = destPath,
            fileDirParam = fileDirParam,
            buildId = buildId,
            taskId = taskId,
            containerId = containerId,
            outputVariables = outputVariables,
            executeCount = executeCount,
            projectId = projectId,
            pipelineId = pipelineId
        )

        clearTempFile(task)

        return AtomResponse(buildStatus, outputVariables)
    }

    private fun recordReportDetail(
        userId: String,
        isRepoGray: Boolean,
        param: ReportArchiveServiceElement,
        runVariables: Map<String, String>,
        destPath: String,
        fileDirParam: String,
        buildId: String,
        taskId: String,
        containerId: String?,
        outputVariables: MutableMap<String, Any>,
        executeCount: Int,
        projectId: String,
        pipelineId: String
    ) {

        val indexFileParam = parseVariable(param.indexFile, runVariables)
        val reportNameParam = parseVariable(param.reportName, runVariables)

        val enableEmail = param.enableEmail ?: false
        val emailTitle = parseVariable(param.emailTitle, runVariables)

        val localDir = "$destPath${File(fileDirParam).name}"
        val localDirFile = File(localDir)
        val localDirFilePath = Paths.get(localDirFile.canonicalPath)
        val emailReceivers = parseVariable(param.emailReceivers?.joinToString(","), runVariables)

        val reportRootUrl = reportService.getRootUrl(buildId, taskId)
        outputVariables[REPORT_DYNAMIC_ROOT_URL] = reportRootUrl

        val indexFile = File(localDir, indexFileParam)
        if (!indexFile.exists()) {
            buildLogPrinter.addLine(
                buildId,
                "入口文件($indexFileParam)不在文件夹($fileDirParam)下",
                taskId,
                containerId,
                executeCount
            )
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_IDX_FILE_NOT_EXITS.toInt(),
                errorMsg = "Index file not exist"
            )
        }
        buildLogPrinter.addLine(buildId, "入口文件检测完成", taskId, containerId, executeCount)

        var indexFileContent = indexFile.readBytes().toString(Charset.defaultCharset())
        indexFileContent = indexFileContent.replace("\${$REPORT_DYNAMIC_ROOT_URL}", reportRootUrl)
        indexFile.writeBytes(indexFileContent.toByteArray())

        // 获取项目下的所有文件，并上传到版本仓库
        val allFileList = recursiveGetFiles(localDirFile)
        val count = allFileList.size
        if (isRepoGray) {
            allFileList.forEach { file ->
                val relativePath = localDirFilePath.relativize(Paths.get(file.canonicalPath)).toString()
                bkRepoClient.uploadLocalFile(userId, projectId, "report", "$pipelineId/$buildId/$taskId/$relativePath", file)
            }
        } else {
            allFileList.forEach { file ->
                val relativePath = localDirFilePath.relativize(Paths.get(file.canonicalPath)).toString()
                upload(projectId, pipelineId, buildId, taskId, relativePath, file)
            }
        }

        var reportEmail: ReportEmail? = null
        if (enableEmail && emailTitle.isNotBlank() && emailReceivers.isNotBlank()) {
            val receivers = regex.split(emailReceivers).toSet()
            reportEmail = ReportEmail(receivers, emailTitle, indexFile.readBytes().toString(Charset.defaultCharset()))
        }

        reportService.create(projectId, pipelineId, buildId, taskId, indexFileParam, reportNameParam, ReportTypeEnum.INTERNAL, reportEmail)
        buildLogPrinter.addLine(buildId, "上传自定义产出物成功，共产生了${count}个文件", taskId, containerId, executeCount)
    }

    private fun checkFileTransferStatus(
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
            buildLogPrinter.addRedLine(
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
                buildLogPrinter.addLine(buildId, "文件推送完成", taskId, containerId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addRedLine(buildId, "自定义产出物报告推送文件失败", taskId, containerId, executeCount)
                buildLogPrinter.addRedLine(buildId, "失败详情: ${taskResult.msg}", taskId, containerId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            BuildStatus.LOOP_WAITING
        }
    }

    private fun getTimeoutMills(): Int = 100 * 60000 // 100 min

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

    private fun recursiveGetFiles(file: File): List<File> {
        val fileList = mutableListOf<File>()
        file.listFiles()?.forEach {
            if (it.isDirectory) {
                val subFileList = recursiveGetFiles(it)
                fileList.addAll(subFileList)
            } else {
                fileList.add(it)
            }
        }
        return fileList
    }

    private fun upload(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        relativePath: String,
        file: File
    ) {
        val path = JFrogUtil.getReportPath(projectId, pipelineId, buildId, taskId, relativePath)
        jFrogStorageApi.deploy(path, file.inputStream())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReportArchiveServiceTaskAtom::class.java)

        private const val JOB_TASK_ID = "_JOB_TASK_ID"
        private const val STARTER = "_STARTER"
        private const val TMP_HOST = "_TMP_HOST"
        private const val TEMP_DEST_PATH = "_TEMP_DEST_PATH"
    }
}
