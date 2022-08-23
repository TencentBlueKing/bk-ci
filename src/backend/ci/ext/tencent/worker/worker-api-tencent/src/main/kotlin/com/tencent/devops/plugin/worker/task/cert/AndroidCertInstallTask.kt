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
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.pipeline.element.AndroidCertInstallElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ticket.CertResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import java.io.File
import java.util.Base64

@TaskClassType(classTypes = [AndroidCertInstallElement.classType])
class AndroidCertInstallTask : ITask() {

    private val certResourceApi = CertResourceApi()

    override fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        val certId = buildTask.params!!["certId"] ?: ""
        val destPath = buildTask.params!!["destPath"] ?: ""

        if (certId.isBlank()) {
            throw TaskExecuteException(
                errorMsg = "证书ID为空",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
        }

        val filename = "${destPath.removeSuffix("/")}/$certId.keystore"
        LoggerService.addNormalLine("keystore安装相对路径：$filename")

        val pairKey = DHUtil.initKey()
        val privateKey = pairKey.privateKey
        val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))

        val certInfo = certResourceApi.queryAndroid(certId, publicKey).data!!
        val publicKeyServer = Base64.getDecoder().decode(certInfo.publicKey)
        val keystoreEncryptedContent = Base64.getDecoder().decode(certInfo.jksContent)
        val keystoreContent = DHUtil.decrypt(keystoreEncryptedContent, publicKeyServer, privateKey)
        LoggerService.addNormalLine("Keystore sha1: ${ShaUtils.sha1(keystoreContent)}")

        File(workspace, filename).writeBytes(keystoreContent)
        LoggerService.addNormalLine("Keystore安装成功")
    }
}
