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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.utils.REPORT_DYNAMIC_ROOT_URL
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.report.ReportSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException

@TaskClassType(classTypes = [ReportArchiveElement.classType])
class ReportArchiveTask : ITask() {

    private val api = ApiFactory.create(ReportSDKApi::class)

    private val regex = Pattern.compile("[,|;]")

    private val logger = LoggerFactory.getLogger(ReportArchiveTask::class.java)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("the buildTask is:$buildTask,buildVariables is:$buildVariables,workspace is:${workspace.absolutePath}")
        val taskParams = buildTask.params ?: mapOf()
        buildTask.buildId
        val elementId = taskParams["id"] ?: taskParams["elementId"]
        ?: throw ParamBlankException("param [elementId] is empty")
        val reportNameParam = taskParams["reportName"] ?: throw ParamBlankException("param [reportName] is empty")
        val reportType = taskParams["reportType"] ?: ReportTypeEnum.INTERNAL.name
        val indexFileParam: String
        if (reportType == ReportTypeEnum.INTERNAL.name) {
            val fileDirParam = taskParams["fileDir"] ?: throw ParamBlankException("param [fileDir] is empty")
            indexFileParam = taskParams["indexFile"] ?: throw ParamBlankException("param [indexFile] is empty")

            val fileDir = getFile(workspace, fileDirParam)
            if (!fileDir.isDirectory) {
                throw NotFoundException("文件夹($fileDirParam)不存在")
            }

            val indexFile = getFile(fileDir, indexFileParam)
            if (!indexFile.exists()) {
                throw RuntimeException("入口文件($indexFileParam)不在文件夹($fileDirParam)下")
            }
            LoggerService.addNormalLine("入口文件检测完成")
            val reportRootUrl = api.getRootUrl(elementId).data!!
            addEnv(REPORT_DYNAMIC_ROOT_URL, reportRootUrl)

            var indexFileContent = indexFile.readBytes().toString(Charset.defaultCharset())
            indexFileContent = indexFileContent.replace("\${$REPORT_DYNAMIC_ROOT_URL}", reportRootUrl)
            indexFile.writeBytes(indexFileContent.toByteArray())

            val allFileList = recursiveGetFiles(fileDir)
            allFileList.forEach {
                val relativePath = it.parentFile.absolutePath.removePrefix(fileDir.absolutePath)
                api.uploadReport(it, elementId, relativePath, buildVariables)
            }
            LoggerService.addNormalLine("上传自定义产出物成功，共产生了${allFileList.size}个文件")
        } else {
            val reportUrl = taskParams["reportUrl"] as String
            indexFileParam = reportUrl // 第三方构建产出物链接
        }
        logger.info("indexFileParam is:$indexFileParam,reportNameParam is:$reportNameParam,reportType is:$reportType")
        api.createReportRecord(elementId, indexFileParam, reportNameParam, reportType)
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