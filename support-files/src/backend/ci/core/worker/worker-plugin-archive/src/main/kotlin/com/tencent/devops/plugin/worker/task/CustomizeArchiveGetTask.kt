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

package com.tencent.devops.plugin.worker.task

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.archive.element.CustomizeArchiveGetElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import java.io.File
import java.net.URLDecoder

@TaskClassType(classTypes = [CustomizeArchiveGetElement.classType])
class CustomizeArchiveGetTask : ITask() {

    private val archiveGetResourceApi = ApiFactory.create(ArchiveSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val destPath = File(workspace, taskParams["destPath"] ?: ".")
        val downloadPaths = taskParams["downloadPaths"] ?: throw RuntimeException("downloadPaths can not be null")
        val notFoundContinue = taskParams["notFoundContinue"] ?: ""
        var count = 0

        LoggerService.addNormalLine("archive get notFoundContinue: $notFoundContinue")
        // 匹配文件
        downloadPaths.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.forEach { srcPath ->

            val fileList = archiveGetResourceApi.getFileDownloadUrls(
                pipelineId = buildVariables.pipelineId,
                buildId = buildVariables.buildId,
                fileType = FileTypeEnum.BK_CUSTOM,
                customFilePath = srcPath
            )

            fileList.forEach { fileUrl ->
                val decodeUrl = URLDecoder.decode(fileUrl, "UTF-8")
                val lastFx = decodeUrl.lastIndexOf("/")
                val file = if (lastFx > 0) {
                    File(destPath, decodeUrl.substring(lastFx + 1))
                } else {
                    File(destPath, decodeUrl)
                }
                LoggerService.addNormalLine("find the file($fileUrl) in repo! [${file.name}")
                archiveGetResourceApi.downloadCustomizeFile(fileUrl, file)
                count++
            }
        }

        LoggerService.addNormalLine("total $count file(s) found")
        if (count == 0 && notFoundContinue == "false") throw RuntimeException("0 file found in path: $downloadPaths")
    }
}