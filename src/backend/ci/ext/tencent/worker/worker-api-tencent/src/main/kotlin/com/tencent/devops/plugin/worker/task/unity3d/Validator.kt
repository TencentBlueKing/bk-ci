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

package com.tencent.devops.plugin.worker.task.unity3d

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.api.ticket.CertResourceApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.plugin.worker.task.unity3d.model.AndroidKey
import com.tencent.devops.plugin.worker.task.unity3d.model.Argument
import com.tencent.devops.worker.common.utils.CredentialUtils
import java.io.File
import java.util.Base64

object Validator {

    private val pairKey = DHUtil.initKey()
    private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))

    fun getArgument(buildId: String, taskParams: Map<String, String>, workspace: File, platform: String): Argument {
        return Argument(
                Platform.valueOf(platform.toUpperCase()),
                taskParams["executeMethod"],
                taskParams["debug"]!!.toBoolean(),
                validateRootDir(taskParams, workspace),
                validateAndroidKey(buildId, taskParams, workspace),
                validateAndroidAPKPath(taskParams, workspace),
                taskParams["apkName"] ?: "",
                taskParams["xcodeProjectName"]!!,
                taskParams["enableBitCode"]!!.toBoolean()
        )
    }

    private fun validateRootDir(taskParams: Map<String, String>, workspace: File): File {
        val rootPath = if (taskParams["rootDir"].isNullOrBlank()) {
            "./${workspace.path}"
        } else {
            "./${workspace.path}/${taskParams["rootDir"]!!.removePrefix("/")}"
        }
        val rootDir = File(rootPath)
        if (!rootDir.exists()) {
            throw TaskExecuteException(
                errorMsg = "The specified unity3d project root path '${rootDir.canonicalPath}' does not exist",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
        if (!rootDir.isDirectory) {
            throw TaskExecuteException(
                errorMsg = "The specified unity3d project root path '${rootDir.canonicalPath}' is not a directory",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
        return rootDir
    }

    private fun validateAndroidKey(buildId: String, taskParams: Map<String, String>, workspace: File): AndroidKey {

        val certId = taskParams["certId"] ?: return AndroidKey()

        val certInfo = CertResourceApi().queryAndroid(certId, publicKey).data!!
        /*
        val certInfo = if (isWorker()) {
            Client.get(BuildCertResource::class).queryAndroid(BUILD_ID_DEFAULT, VM_SEQ_ID_DEFAULT, VM_NAME_DEFAULT, certId, publicKey)
        }
        else {
            Client.get(BuildAgentCertResource::class).queryAndroid(
                    getProjectId(),
                    getAgentId(),
                    getAgentSecretKey(),
                    buildId,
                    certId, publicKey
            )
        }.data!!
        */

        val keyStoreName = certInfo.jksFileName

        val fileName = if (taskParams["rootDir"].isNullOrBlank()) {
            "./${workspace.path}/$keyStoreName"
        } else {
            "./${workspace.path}/${taskParams["rootDir"]}/$keyStoreName"
        }
        val file = File(fileName)
        if (!file.exists()) {
            throw TaskExecuteException(
                errorMsg = "The specified android key store file name '$keyStoreName' does not exist",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
        if (!file.isFile) {
            throw TaskExecuteException(
                errorMsg = "The specified android key store file name '$keyStoreName' is not a file",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        val storePass = if (certInfo.credentialId.isNullOrBlank()) "" else CredentialUtils.getCredential(buildId, certInfo.credentialId!!)[0]
        val aliasPass = if (certInfo.aliasCredentialId.isNullOrBlank()) "" else CredentialUtils.getCredential(buildId, certInfo.aliasCredentialId!!)[0]

        return AndroidKey(
            storeName = keyStoreName,
            storePass = storePass,
            aliasName = certInfo.alias ?: "",
            aliasPass = aliasPass
        )
    }

    private fun validateAndroidAPKPath(taskParams: Map<String, String>, workspace: File): String {
        // 创建Android的输出目录
        val path = if (taskParams["apkPath"].isNullOrBlank()) "bin/android/" else taskParams["apkPath"]!!
        val file = File(workspace, path)
        if (!file.exists()) {
            file.mkdirs()
        }
        return path
    }
}
