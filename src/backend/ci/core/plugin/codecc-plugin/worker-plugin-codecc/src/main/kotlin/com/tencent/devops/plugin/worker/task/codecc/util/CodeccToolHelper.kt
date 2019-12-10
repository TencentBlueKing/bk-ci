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

package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.plugin.codecc.pojo.CodeccToolType
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants
import com.tencent.devops.plugin.worker.task.codecc.WindowsCodeccConstants
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.dispatch.CodeccDownloadApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import org.springframework.http.HttpStatus
import java.io.File

class CodeccToolHelper {
    private val codeccApi = ApiFactory.create(CodeccDownloadApi::class)

    fun downloadCovScript() {
        val covFile = LinuxCodeccConstants.getCovPyFile()
        val covScriptMd5 = FileUtil.getMD5(covFile)
        val covScriptResponse = codeccApi.downloadCovScript(AgentEnv.getOS(), covScriptMd5)
        OkhttpUtils.downloadFile(covScriptResponse, covFile)
    }

    fun downloadToolScript() {
        val toolFile = LinuxCodeccConstants.getToolPyFile()
        val toolsScriptMd5 = FileUtil.getMD5(toolFile)
        val toolsScriptResponse = codeccApi.downloadToolScript(AgentEnv.getOS(), toolsScriptMd5)
        OkhttpUtils.downloadFile(toolsScriptResponse, toolFile)
    }

    fun windowsDownloadCovScript() {
        val covScriptMd5 = FileUtil.getMD5(WindowsCodeccConstants.WINDOWS_COV_PY_FILE)
        val covScriptResponse = codeccApi.downloadCovScript(AgentEnv.getOS(), covScriptMd5)
        OkhttpUtils.downloadFile(covScriptResponse, WindowsCodeccConstants.WINDOWS_COV_PY_FILE)
    }

    fun windowsDownloadToolScript() {
        val toolsScriptMd5 = FileUtil.getMD5(WindowsCodeccConstants.WINDOWS_TOOL_PY_FILE)
        val toolsScriptResponse = codeccApi.downloadToolScript(AgentEnv.getOS(), toolsScriptMd5)
        OkhttpUtils.downloadFile(toolsScriptResponse, WindowsCodeccConstants.WINDOWS_TOOL_PY_FILE)
    }

    // toolFile: 工具文件下载到绝对路径
    // callback: 下载完工具执行的操作
    fun getTool(toolName: CodeccToolType, toolFile: File, callback: Runnable = Runnable { }) {
        val md5File = File(toolFile.canonicalPath + ".md5")
        val md5 = if (md5File.exists()) md5File.readText() else ""
        val response = codeccApi.downloadTool(toolName.name, AgentEnv.getOS(), md5, AgentEnv.is32BitSystem())
        OkhttpUtils.downloadFile(response, toolFile)
        if (response.code() != HttpStatus.NOT_MODIFIED.value()) {
            callback.run()
            // 写入md5
            md5File.writeText(FileUtil.getMD5(toolFile))
        } else {
            LoggerService.addNormalLine("$toolName is newest, do not install")
        }
    }
}