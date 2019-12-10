package com.tencent.devops.plugin.worker.task.xcode

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.ticket.pojo.CertIOS
import com.tencent.devops.worker.common.api.ticket.CertResourceApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.ExecutorUtil
import com.tencent.devops.worker.common.utils.ShellUtil
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * Created by ddlin on 2018/01/10.
 * Powered By Tencent
 */
class Builder(private val argument: Argument) {
    private val pairKey = DHUtil.initKey()
    private val privateKey = pairKey.privateKey
    private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))
    private var projectPath: File? = null

    fun build(buildVariables: BuildVariables, workspace: File) {
        val index = argument.project.lastIndexOf("/")
        projectPath = File(workspace, argument.project.substring(0, index))

        switchXCode(buildVariables, workspace)
        xCodeBuild(argument.extra, buildVariables, workspace)
        val ipaFile = packageApplication(workspace)

        // 签名
        val certInfo = CertResourceApi().queryIos(argument.certId, publicKey).data!!
        /*
        val certInfo = if (isWorker()) {
            Client.get(BuildCertResource::class).queryIos(BUILD_ID_DEFAULT, VM_SEQ_ID_DEFAULT, VM_NAME_DEFAULT, argument.certId, publicKey)
        }
        else {
            Client.get(BuildAgentCertResource::class).queryIos(
                    getProjectId(),
                    getAgentId(),
                    getAgentSecretKey(),
                    buildVariables.buildId,
                    argument.certId,
                    publicKey)
        }.data!!
        */

        CodeSigner.sign(getProvision(certInfo), ipaFile, getCertIdetity(buildVariables.buildId, certInfo))
    }

    private fun getCertIdetity(buildId: String, certInfo: CertIOS): String {
        val publicKeyServer = Base64.getDecoder().decode(certInfo.publicKey)
        val p12FileContent = Base64.getDecoder().decode(certInfo.p12Content)
        val p12 = DHUtil.decrypt(p12FileContent, publicKeyServer, privateKey)
        val p12File = File.createTempFile("p12_", ".p12")
        p12File.writeBytes(p12)

        val ks = KeyStore.getInstance("PKCS12")

        if (certInfo.credentialId == null) throw TaskExecuteException(
            errorMsg = "certInfo.credentialId is null",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        CredentialUtils.getCredential(buildId, certInfo.credentialId!!)
        ks.load(FileInputStream(p12File), CredentialUtils.getCredential(buildId, certInfo.credentialId!!)[0].toCharArray())
        val alias = ks.aliases().nextElement()
        val cert = ks.getCertificate(alias) as X509Certificate

        return ShaUtils.sha1(cert.encoded)
    }

    private fun getProvision(certInfo: CertIOS): File {
        val publicKeyServer = Base64.getDecoder().decode(certInfo.publicKey)
        val proContent = Base64.getDecoder().decode(certInfo.mobileProvisionContent)
        val provision = DHUtil.decrypt(proContent, publicKeyServer, privateKey)
        val provisionFile = File.createTempFile("provision_", ".mobileprovision")
        provisionFile.writeBytes(provision)

        return provisionFile
    }

    private fun switchXCode(buildVariables: BuildVariables, workspace: File) {
        val command = "sudo /usr/bin/xcode-select --switch \${XCODE_HOME}"
        ShellUtil.execute(buildVariables.buildId, command, workspace, buildVariables.buildEnvs, emptyMap(), null)
    }

    /**
     * 执行 'xcodebuild ...' 步骤，编译生成 .app
     */
    private fun xCodeBuild(extra: String, buildVariables: BuildVariables, workspace: File) {

        try {
            clearBuildDirectory()
        } catch (ex: Exception) {
            LoggerService.addNormalLine("Clear 'build' directory failed, however the build process will go on")
        }

        val command = StringBuilder()
        command.append("xcodebuild -project ")
        with(argument) {
            command.append(project)
            command.append(" CODE_SIGN_IDENTITY=")
            command.append(" PROVISIONING_PROFILE=")
            command.append(" CODE_SIGNING_REQUIRED=NO")
            command.append(" CODE_SIGNING_ALLOWED=NO")

            if (!scheme.isBlank()) {
                command.append(" -scheme $scheme")
            }
            if (!configuration.isBlank()) {
                command.append(" -configuration $configuration")
            }

            if (!enableBitCode) {
                command.append(" ENABLE_BITCODE=NO")
            }

            if (extra.isNotEmpty()) {
                command.append(" ")
                command.append(extra)
            }
        }

        ShellUtil.execute(buildVariables.buildId, command.toString(), workspace, buildVariables.buildEnvs, emptyMap(), null)
    }

    /**
     * just zip the package from app to ipa
     */
    private fun packageApplication(workspace: File): File {
        // 在build目录下，查找.app文件夹
        val buildPath = File(projectPath, "build")
        LoggerService.addNormalLine("find .app in path: " + buildPath.canonicalPath)
        val appFiles = workspace.walk().filter { it.name.endsWith(".app") }.toList()
        if (appFiles.isEmpty()) {
            throw TaskExecuteException(
                errorMsg = "Can not find .app file",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        // 复制临时文件到Payload
        val appPath = appFiles[0].canonicalPath
        LoggerService.addNormalLine("Package app file($appPath) to ipa")
        val appFolder = createTempDir("app")
        val payload = File(appFolder, "Payload")
        payload.mkdir()
        payload.deleteOnExit()
        appFiles[0].parentFile.copyRecursively(payload, true)
        payload.walk().forEach { it.deleteOnExit() }

        // 删掉Payload文件夹首层目录非.app文件夹
        LoggerService.addNormalLine("Copy the app to ${payload.canonicalPath}")
        payload.listFiles().forEach {
            if (!it.name.endsWith(".app")) {
                LoggerService.addNormalLine("Delete the file ${it.name}")
                it.deleteRecursively()
            }
        }

        // 压缩打包
        val outputPath = File(workspace, argument.iosOutPath)
        if (!outputPath.parentFile.exists()) {
            outputPath.parentFile.mkdirs()
        }
        val command = "zip -r ${outputPath.canonicalPath} ${payload.name}"
        ExecutorUtil.runCommand(command, command, appFolder)

        return outputPath
    }

    /**
     * xcodebuild 前先删除 build 目录
     */
    private fun clearBuildDirectory() {
        LoggerService.addNormalLine("Delete existing 'build' directory")
        File(projectPath, "build").deleteRecursively()
    }
}
