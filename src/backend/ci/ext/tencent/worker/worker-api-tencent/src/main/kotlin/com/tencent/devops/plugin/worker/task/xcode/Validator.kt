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

package com.tencent.devops.plugin.worker.task.xcode

import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ShellUtil
import java.io.File
import java.util.regex.Pattern

object Validator {

    fun validate(taskParams: Map<String, String>, workspace: File, buildVariables: BuildVariables): Argument {
        val rootDir = taskParams["rootDir"] ?: ""
        val certId = taskParams["certId"] ?: ""
        val project = taskParams["project"] ?: throw TaskExecuteException(
            errorMsg = "xcode project path is null",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val bitcode = taskParams["enableBitCode"] ?: "false"
        return Argument(
                project,
                getIphoneosSdk(workspace, buildVariables),
                taskParams["scheme"] ?: "",
                certId,
                taskParams["configuration"] ?: "",
                this.validateIosOutputPath(taskParams["iosPath"], taskParams["iosName"]),
                workspace.path + File.separator + rootDir,
                bitcode.toBoolean(),
                taskParams["extraParams"] ?: ""
        )
    }

    private fun getIphoneosSdk(workspace: File, buildVariables: BuildVariables): String {

        val result = ShellUtil.execute("xcodebuild -showsdks", workspace, buildVariables.buildEnvs, emptyMap())

        val matcher = Pattern.compile("iOS\\s*(\\S*)\\s*(\\S*)\\s*-sdk\\s*(iphoneos\\S*)").matcher(result)
        val resultSdk = if (matcher.find()) matcher.group(3).removeSuffix("iOS") else ""
//        if (resultSdk.isEmpty()) {
//            throw OperationException("No default sdk found")
//        } else {
            LoggerService.addNormalLine("Found '$resultSdk' as the default sdk")
//        }
        return resultSdk
    }

    /**
     * 需要校验路径，先求得path相对于当前工作路径的路径，如果以 .. 开头，则不合规
     */
    private fun validateIosPath(iosPath: String): String {
        if (iosPath.isEmpty()) {
            return "result/"
        }
        if (iosPath.startsWith("..")) {
            throw TaskExecuteException(
                errorMsg = "Invalid iosPath($iosPath): the path is beyond workspace directory",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD
            )
        }
        return iosPath
    }

    private fun validateIosName(iosName: String): String {
        if (iosName.isEmpty()) {
            return "output.ipa"
        }
        if (iosName.contains("/")) {
            throw TaskExecuteException(
                errorMsg = "Invalid iosName($iosName): must not contain `/` character",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD
            )
        }
        return iosName
    }

    private fun validateIosOutputPath(iosPath: String?, iosName: String?): String {
        val path = validateIosPath(iosPath ?: "")
        val name = validateIosName(iosName ?: "")

        return path.removeSuffix("/") + "/" + name
    }
}