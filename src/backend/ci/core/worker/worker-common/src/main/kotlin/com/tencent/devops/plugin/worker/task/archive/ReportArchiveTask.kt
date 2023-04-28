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

package com.tencent.devops.plugin.worker.task.archive

import com.tencent.bkrepo.repository.pojo.token.TokenType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.REPORT_DYNAMIC_ROOT_URL
import com.tencent.devops.worker.common.api.ArtifactApiFactory
import com.tencent.devops.worker.common.api.report.ReportSDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.ENTRANCE_FILE_CHECK_FINISH
import com.tencent.devops.worker.common.constants.WorkerMessageCode.ENTRANCE_FILE_NOT_IN_FOLDER
import com.tencent.devops.worker.common.constants.WorkerMessageCode.FOLDER_NOT_EXIST
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UPLOAD_CUSTOM_OUTPUT_SUCCESS
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.RepoServiceFactory
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.utils.TaskUtil
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

@TaskClassType(classTypes = [ReportArchiveElement.classType])
class ReportArchiveTask : ITask() {

    private val api = ArtifactApiFactory.create(ReportSDKApi::class)

    private val regex = Pattern.compile("[,|;]")

    private val logger = LoggerFactory.getLogger(ReportArchiveTask::class.java)

    @Suppress("ALL")
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val elementId = taskParams["id"] ?: taskParams["elementId"]
        ?: throw ParamBlankException("param [elementId] is empty")
        val reportNameParam = taskParams["reportName"] ?: throw ParamBlankException("param [reportName] is empty")
        val reportType = taskParams["reportType"] ?: ReportTypeEnum.INTERNAL.name
        val indexFileParam: String
        var indexFileContent: String
        val token = RepoServiceFactory.getInstance().getRepoToken(
            userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
            projectId = buildVariables.projectId,
            repoName = "report",
            path = "/${buildVariables.pipelineId}/${buildVariables.buildId}",
            type = TokenType.UPLOAD,
            expireSeconds = TaskUtil.getTimeOut(buildTask).times(60)
        )
        if (reportType == ReportTypeEnum.INTERNAL.name) {
            val fileDirParam = taskParams["fileDir"] ?: throw ParamBlankException("param [fileDir] is empty")
            indexFileParam = taskParams["indexFile"] ?: throw ParamBlankException("param [indexFile] is empty")

            val fileDir = getFile(workspace, fileDirParam)
            if (!fileDir.isDirectory) {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = MessageUtil.getMessageByLocale(
                        FOLDER_NOT_EXIST,
                        AgentEnv.getLocaleLanguage(),
                        arrayOf(fileDirParam)
                    )
                )
            }

            val indexFile = getFile(fileDir, indexFileParam)
            if (!indexFile.exists()) {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = MessageUtil.getMessageByLocale(
                        ENTRANCE_FILE_NOT_IN_FOLDER,
                        AgentEnv.getLocaleLanguage(),
                        arrayOf(indexFileParam, fileDirParam)
                    )
                )
            }
            LoggerService.addNormalLine(
                MessageUtil.getMessageByLocale(ENTRANCE_FILE_CHECK_FINISH, AgentEnv.getLocaleLanguage())
            )
            val reportRootUrl = api.getRootUrl(elementId).data!!
            addEnv(REPORT_DYNAMIC_ROOT_URL, reportRootUrl)

            indexFileContent = indexFile.readText()
            indexFileContent = indexFileContent.replace("\${$REPORT_DYNAMIC_ROOT_URL}", reportRootUrl)
            indexFile.writeText(indexFileContent)

            val fileDirPath = Paths.get(fileDir.canonicalPath)
            val allFileList = recursiveGetFiles(fileDir)
            if (allFileList.size > 10) {
                val executors = Executors.newFixedThreadPool(10)
                allFileList.forEach {
                    executors.execute {
                        uploadReportFile(fileDirPath, it, elementId, buildVariables, token)
                    }
                }
                executors.shutdown()
                if (!executors.awaitTermination(buildVariables.timeoutMills, TimeUnit.MILLISECONDS)) {
                    throw TimeoutException("parallel upload report timeout")
                }
            } else {
                allFileList.forEach {
                    uploadReportFile(fileDirPath, it, elementId, buildVariables, token)
                }
            }
            LoggerService.addNormalLine(
                MessageUtil.getMessageByLocale(
                    UPLOAD_CUSTOM_OUTPUT_SUCCESS,
                    AgentEnv.getLocaleLanguage(),
                    arrayOf("${allFileList.size}")
                )
            )
        } else {
            val reportUrl = taskParams["reportUrl"] as String
            indexFileParam = reportUrl // 第三方构建产出物链接
            indexFileContent = "detail：$reportUrl" // 第三方构建产出物如果需要发邮件，邮件内容展示第三方构建产出物链接
        }

        val enableEmail = taskParams["enableEmail"]?.toBoolean() ?: false
        val emailReceivers = taskParams["emailReceivers"]
        val emailTitle = taskParams["emailTitle"]
        var reportEmail: ReportEmail? = null
        if (enableEmail && !emailReceivers.isNullOrBlank() && emailTitle != null) {
            val receivers = try {
                // 好草蛋：kotlin无法Json串直接转Set，所有[]类型的只能先转List
                JsonUtil.to<List<String>>(emailReceivers!!).toSet()
            } catch (t: Throwable) { // 旧引擎做法是用x,y,z 传递
                regex.split(emailReceivers).toSet()
            }
            reportEmail = ReportEmail(receivers, emailTitle, indexFileContent)
        }

        logger.info("indexFileParam is:$indexFileParam,reportNameParam is:$reportNameParam,reportType is:$reportType")
        api.createReportRecord(
            buildVariables = buildVariables,
            taskId = elementId,
            indexFile = indexFileParam,
            name = reportNameParam,
            reportType = reportType,
            reportEmail = reportEmail,
            token = token
        )
    }

    private fun uploadReportFile(
        fileDirPath: Path,
        file: File,
        elementId: String,
        buildVariables: BuildVariables,
        token: String?
    ) {
        val relativePath = fileDirPath.relativize(Paths.get(file.canonicalPath)).toString()
        HttpRetryUtils.retry(retryTime = 3) {
            api.uploadReport(
                file = file,
                taskId = elementId,
                relativePath = relativePath,
                buildVariables = buildVariables,
                token = token
            )
        }
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

    private fun getFile(workspace: File, filePath: String): File {
        val absPath = filePath.startsWith("/") || (filePath[0].isLetter() && filePath[1] == ':')
        return if (absPath) {
            File(filePath)
        } else {
            File(workspace, filePath)
        }
    }
}
