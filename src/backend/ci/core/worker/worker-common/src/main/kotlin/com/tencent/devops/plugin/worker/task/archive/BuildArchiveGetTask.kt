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
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.archive.element.BuildArchiveGetElement
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.ArtifactApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.process.BuildSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.RepoServiceFactory
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.utils.TaskUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLDecoder

@Suppress("ComplexMethod")
@TaskClassType(classTypes = [BuildArchiveGetElement.classType])
class BuildArchiveGetTask : ITask() {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildArchiveGetTask::class.java)
    }

    private val archiveGetResourceApi = ArtifactApiFactory.create(ArchiveSDKApi::class)
    private val buildApi = ApiFactory.create(BuildSDKApi::class)

    @Suppress("ComplexMethod", "MagicNumber")
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val pipelineId = taskParams["pipelineId"] ?: throw ParamBlankException("pipelineId is null")
        val buildNo = if (taskParams["buildNo"].isNullOrBlank()) "-1" else taskParams["buildNo"]!!
        val buildId = getBuildId(pipelineId, buildVariables, buildNo).id
        val destPath = File(workspace, taskParams["destPath"] ?: ".")
        val srcPaths = taskParams["srcPaths"] ?: throw ParamBlankException("srcPaths can not be null")
        val notFoundContinue = taskParams["notFoundContinue"] ?: "false"
        var count = 0

        LoggerService.addNormalLine("archive get notFoundContinue: $notFoundContinue")
        val files = srcPaths.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.filter { it.isNotBlank() }.flatMap { srcPath ->

            logger.info("[$buildId]|pipelineId=$pipelineId|srcPath=$srcPath")
            val fileList = archiveGetResourceApi.getFileDownloadUrls(
                userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
                projectId = buildVariables.projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                fileType = FileTypeEnum.BK_ARCHIVE,
                customFilePath = srcPath
            )

            fileList.map { fileUrl ->
                val decodeUrl = URLDecoder.decode(fileUrl, "UTF-8")
                val lastFx = decodeUrl.lastIndexOf("/")
                val file = if (lastFx > 0) {
                    File(destPath, decodeUrl.substring(lastFx + 1))
                } else {
                    File(destPath, decodeUrl)
                }
                LoggerService.addNormalLine("find the file($fileUrl) in repo!")
                fileUrl to file
            }
        }
        count = files.size
        LoggerService.addNormalLine("total $count file(s) found")
        files.forEachIndexed { index, (fileUrl, file) ->
            val token = RepoServiceFactory.getInstance().getRepoToken(
                userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
                projectId = buildVariables.projectId,
                repoName = "pipeline",
                path = fileUrl,
                type = TokenType.DOWNLOAD,
                expireSeconds = TaskUtil.getTimeOut(buildTask).times(60)
            )
            archiveGetResourceApi.downloadPipelineFile(
                userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
                projectId = buildVariables.projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                uri = fileUrl,
                destPath = file,
                isVmBuildEnv = TaskUtil.isVmBuildEnv(buildVariables.containerType),
                token = token
            )
            LoggerService.addNormalLine("${index + 1}/$count finished")
        }
        if (count == 0 && notFoundContinue == "false") throw TaskExecuteException(
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            errorType = ErrorType.USER,
            errorMsg = "0 file found in path: $srcPaths"
        )
    }

    private fun getBuildId(pipelineId: String, buildVariables: BuildVariables, buildNo: String): BuildHistory {
        return buildApi.getSingleHistoryBuild(
            projectId = buildVariables.projectId,
            pipelineId = pipelineId,
            buildNum = buildNo,
            channelCode = ChannelCode.BS
        ).data ?: throw TaskExecuteException(
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
            errorType = ErrorType.USER,
            errorMsg = "no build($buildNo) history found in pipeline($pipelineId})"
        )
    }
}
