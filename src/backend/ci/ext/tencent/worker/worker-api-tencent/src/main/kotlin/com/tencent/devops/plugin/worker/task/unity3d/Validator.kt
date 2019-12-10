package com.tencent.devops.plugin.worker.task.unity3d

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.api.ticket.CertResourceApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.task.unity3d.model.AndroidKey
import com.tencent.devops.worker.common.task.unity3d.model.Argument
import com.tencent.devops.worker.common.utils.CredentialUtils
import java.io.File
import java.util.Base64

/**
 * Created by liangyuzhou on 2017/9/26.
 * Powered By Tencent
 */
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

        return AndroidKey(keyStoreName,
                storePass,
                certInfo.alias ?: "",
                aliasPass)
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
