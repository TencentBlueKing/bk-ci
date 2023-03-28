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

package com.tencent.devops.plugin.worker.task.bk

import com.tencent.devops.common.api.constant.I18NConstant.BK_CANNING_SENSITIVE_INFORMATION
import com.tencent.devops.common.api.constant.I18NConstant.BK_NO_SENSITIVE_INFORMATION
import com.tencent.devops.common.api.constant.I18NConstant.BK_SENSITIVE_INFORMATION
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.element.SensitiveScanElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.WorkerMessageCode.FOLDER_NOT_EXIST
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.pojo.TokenType
import com.tencent.devops.worker.common.api.report.ReportSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.task.script.CommandFactory
import com.tencent.devops.worker.common.utils.BkRepoUtil
import com.tencent.devops.worker.common.utils.TaskUtil
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import org.slf4j.LoggerFactory
import java.io.File
import java.io.Writer
import java.nio.file.Paths

/**
 * 构建脚本任务
 */
@TaskClassType(classTypes = [SensitiveScanElement.classType])
class SensitiveScanTask : ITask() {

    private val reportArchiveResourceApi = ApiFactory.create(ReportSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val excludePath = taskParams["excludePath"] ?: ""
        val elementId = taskParams["id"] ?: taskParams["elementId"] ?: "T-2-1-1" // 给个默认值，而不是抛异常，兼容旧数据

        val command = CommandFactory.create("SHELL")
        val buildId = buildVariables.buildId
        val runtimeVariables = buildVariables.variables
        val projectId = buildVariables.projectId
        val deleteDir = "rm -rf public_script"
        command.execute(
            buildId = buildId,
            script = deleteDir,
            taskParam = taskParams,
            runtimeVariables = runtimeVariables,
            projectId = projectId,
            dir = workspace,
            buildEnvs = buildVariables.buildEnvs
        )

        val writer = object : Writer() {
            override fun write(cbuf: CharArray?, off: Int, len: Int) {
            }

            override fun flush() {
            }

            override fun close() {
            }
        }

        val sensitiveWorkspace = File(workspace, "public_script")
        Git.cloneRepository()
            .setProgressMonitor(TextProgressMonitor(writer))
            .setBare(false)
            .setCloneAllBranches(true)
            .setCloneSubmodules(false)
            .setDirectory(sensitiveWorkspace)
            .setURI("http://gitlab-paas.open.oa.com/bkpublic/public_script.git")
            .call()

        val script = """
                cd public_script && /bin/bash check.sh "$excludePath"
            """
        logger.info("Start to scan the sensitive information.excludePath: $excludePath")
        LoggerService.addNormalLine(
            MessageUtil.getMessageByLocale(
            messageCode = BK_CANNING_SENSITIVE_INFORMATION,
            language = I18nUtil.getDefaultLocaleLanguage()
        ) + "：$excludePath")

        command.execute(buildId, script, taskParams, runtimeVariables, projectId, workspace, buildVariables.buildEnvs)

        val fileDirParam = "public_script/report"
        val indexFileParam = "detect_ssd.html"
        val reportNameParam = MessageUtil.getMessageByLocale(
            messageCode = BK_SENSITIVE_INFORMATION,
            language = I18nUtil.getDefaultLocaleLanguage()
        )

        val fileDir = getFile(workspace, fileDirParam)
        if (!fileDir.isDirectory) {
            throw TaskExecuteException(
                errorMsg = MessageUtil.getMessageByLocale(
                    messageCode = FOLDER_NOT_EXIST,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(fileDirParam)
                ),
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        val indexFile = getFile(fileDir, indexFileParam)
        if (!indexFile.exists()) {
            LoggerService.addNormalLine(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_NO_SENSITIVE_INFORMATION,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
            return
        }

        val fileDirPath = Paths.get(fileDir.canonicalPath)
        val allFileList = recursiveGetFiles(fileDir)
        val token = BkRepoUtil.createBkRepoTemporaryToken(
            userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
            projectId = buildVariables.projectId,
            repoName = "report",
            path = "/${buildVariables.pipelineId}/${buildVariables.buildId}",
            type = TokenType.UPLOAD,
            expireSeconds = TaskUtil.getTimeOut(buildTask)?.times(60)
        )
        allFileList.forEach {
            val relativePath = fileDirPath.relativize(Paths.get(it.canonicalPath)).toString()
            reportArchiveResourceApi.uploadReport(
                file = it,
                taskId = elementId,
                relativePath = relativePath,
                buildVariables = buildVariables,
                token = token
            )
        }
        reportArchiveResourceApi.createReportRecord(
            taskId = elementId,
            indexFile = indexFileParam,
            name = reportNameParam
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

    private fun getFile(workspace: File, filePath: String): File {
        val absPath = filePath.startsWith("/") || (filePath[0].isLetter() && filePath[1] == ':')
        return if (absPath) {
            File(filePath)
        } else {
            File(workspace, filePath)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SensitiveScanTask::class.java)
    }
}
