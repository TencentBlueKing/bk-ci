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

package com.tencent.devops.plugin.worker.task.cert

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.worker.common.api.ticket.CertResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.ExecutorUtil.runCommand
import java.io.File
import java.util.Base64

object CertInstaller {
    private val pairKey = DHUtil.initKey()
    private val privateKey = pairKey.privateKey
    private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))

    fun install(buildId: String, taskParams: Map<String, String>, vmPassword: String) {
        LoggerService.addNormalLine("Start to install p12 & provision file")
        val certId = taskParams["certId"] ?: ""

        // 获取证书信息
        val certInfo = CertResourceApi().queryIos(certId, publicKey).data!!
        /*
        val certInfo = if (isWorker()) Client.get(BuildCertResource::class).queryIos(BUILD_ID_DEFAULT, VM_SEQ_ID_DEFAULT, VM_NAME_DEFAULT, certId, publicKey).data!!
        else Client.get(BuildAgentCertResource::class).queryIos(getProjectId(), getAgentId(), getAgentSecretKey(), buildId, certId, publicKey).data!!
        */

        val publicKeyServer = Base64.getDecoder().decode(certInfo.publicKey)
        val p12FileContent = Base64.getDecoder().decode(certInfo.p12Content)
        val proContent = Base64.getDecoder().decode(certInfo.mobileProvisionContent)

        val p12 = DHUtil.decrypt(p12FileContent, publicKeyServer, privateKey)
        val provision = DHUtil.decrypt(proContent, publicKeyServer, privateKey)

        val p12File = File.createTempFile("p12_", ".p12")
        p12File.writeBytes(p12)

        val provisionFile = File.createTempFile("provision_", ".mobileprovision")
        provisionFile.writeBytes(provision)

        val credPassword = if (!certInfo.credentialId.isNullOrBlank()) CredentialUtils.getCredential(buildId, certInfo.credentialId!!)[0] else "''"
        val provisionName = certInfo.mobileProvisionFileName

        // 安装证书
        installCert(p12File, provisionFile, vmPassword, credPassword, provisionName)
        unlockKeyChain(vmPassword)
        p12File.delete()
        provisionFile.delete()
        LoggerService.addNormalLine("Finish installing the p12 & provision files")
    }

    private fun installCert(p12File: File, provisionFile: File, password: String, credPassword: String, provisionName: String) {
        val homePath = System.getProperty("user.home")

        val unlockCommand = "security unlock-keychain -p $password  $homePath/Library/Keychains/login.keychain-db"
        val unlockCommandMask = "security unlock-keychain -p *****  $homePath/Library/Keychains/login.keychain-db"
        runCommand(unlockCommand, unlockCommandMask)

        val listCommand = "security list-keychains -s  $homePath/Library/Keychains/login.keychain-db"
        runCommand(listCommand, listCommand)

        val importCommand = "security import ${p12File.canonicalPath} -k $homePath/Library/Keychains/login.keychain-db -P $credPassword -T /usr/bin/codesign"
        val importCommandMask = "security import ${p12File.canonicalPath} -k $homePath/Library/Keychains/login.keychain-db -P ****** -T /usr/bin/codesign"
        runCommand(importCommand, importCommandMask)

        // get uuid
        var isUuid = false
        var uuid = ""
        run outside@{
            provisionFile.readLines().forEach {
                if (it.contains("<key>UUID</key>")) {
                    isUuid = true
                    return@forEach
                }
                if (isUuid) {
                    uuid = it.trim().removePrefix("<string>").removeSuffix("</string>")
                    return@outside
                }
            }
        }
        if (uuid.isEmpty()) throw TaskExecuteException(
            errorMsg = "Fail to get uuid",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_INPUT_INVAILD
        )

        // Copy the provision file
        LoggerService.addNormalLine("Copy provision file($provisionName)")
        val provisionFolder = "${System.getProperty("user.home")}/Library/MobileDevice/Provisioning Profiles"
        val provisionFilePath = "$provisionFolder/$uuid.mobileprovision"
        val provisionRootFolder = File(provisionFolder)
        if (!provisionRootFolder.exists()) {
            if (!provisionRootFolder.mkdirs()) {
                LoggerService.addNormalLine("Fail to create provision folder - ${provisionRootFolder.canonicalPath}")
                throw TaskExecuteException(
                    errorMsg = "Fail to create provision folder",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
                )
            }
        }
        val provisionPath = File(provisionFilePath)
        provisionFile.copyTo(provisionPath, true)
    }

    private fun unlockKeyChain(password: String) {
        val command = "security set-key-partition-list -S apple-tool:,apple: -s -k \"$password\"  login.keychain-db"
        val maskCommand = "security set-key-partition-list -S apple-tool:,apple: -s -k \"******\"  login.keychain-db"
        runCommand(command, maskCommand)
    }
}
