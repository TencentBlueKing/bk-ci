package com.tencent.devops.plugin.worker.task.cert

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.api.ticket.CertResourceApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
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
                errorCode = AtomErrorCode.USER_INPUT_INVAILD
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