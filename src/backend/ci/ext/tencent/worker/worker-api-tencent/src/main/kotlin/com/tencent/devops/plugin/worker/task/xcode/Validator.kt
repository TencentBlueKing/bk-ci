package com.tencent.devops.plugin.worker.task.xcode

import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ShellUtil
import java.io.File
import java.util.regex.Pattern

/**
 * Created by ddlin on 2018/01/10.
 * Powered By Tencent
 */
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

        val result = ShellUtil.execute(buildVariables.buildId, "xcodebuild -showsdks", workspace, buildVariables.buildEnvs, emptyMap(), null)

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