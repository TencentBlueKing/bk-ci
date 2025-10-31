/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.enums.CharsetType
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.worker.common.env.AgentEnv.getOS
import com.tencent.devops.worker.common.heartbeat.Heartbeat
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
object CommandLineUtils {

    /*OUTPUT_ERROR_CODE 正则匹配规则*/
    private val OUTPUT_ERROR_CODE = Pattern.compile("error_code=([^,:=\\s]*)")

    /*OUTPUT_ERROR_MESSAGE 正则匹配规则*/
    private val OUTPUT_ERROR_MESSAGE = Pattern.compile("error_message=(.*)")
    private val logger = LoggerFactory.getLogger(CommandLineUtils::class.java)

    private val lineParser = listOf(OauthCredentialLineParser())

    fun execute(
        command: String,
        workspace: File?,
        print2Logger: Boolean,
        prefix: String = "",
        executeErrorMessage: String? = null,
        buildId: String? = null,
        jobId: String? = null,
        stepId: String? = null,
        charsetType: String? = null,
        taskId: String? = null
    ): String {

        val result = StringBuilder()
        val errorResult = StringBuilder()

        val cmdLine = CommandLine.parse(command)
        val executor = CommandLineExecutor()
        if (workspace != null) {
            executor.workingDirectory = workspace
        }
        val contextLogFile = buildId?.let { ScriptEnvUtils.getContextFile(buildId) }
        val setErrorFile = buildId?.let { ScriptEnvUtils.getSetErrorFile(buildId) }

        val charset = when (charsetType?.let { CharsetType.valueOf(it) }) {
            CharsetType.UTF_8 -> "UTF-8"
            CharsetType.GBK -> "GBK"
            else -> Charset.defaultCharset().name()
        }

        val outputStream = object : LogOutputStream() {

            override fun processBuffer() {
                val privateStringField = LogOutputStream::class.java.getDeclaredField("buffer")
                privateStringField.isAccessible = true
                val buffer = privateStringField.get(this) as ByteArrayOutputStream
                processLine(buffer.toString(charset))
                buffer.reset()
            }

            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }

                var tmpLine: String = prefix + line

                lineParser.forEach {
                    tmpLine = it.onParseLine(tmpLine)
                }
                reportProgressRate(
                    taskId = taskId,
                    tmpLine = tmpLine
                )
                if (print2Logger) {
                    appendResultToFile(executor.workingDirectory, contextLogFile, tmpLine, jobId, stepId)
                    appendSetErrorToFile(tmpLine, executor.workingDirectory, setErrorFile)
                    appendGateToFile(tmpLine, executor.workingDirectory, ScriptEnvUtils.getQualityGatewayEnvFile())
                    LoggerService.addNormalLine(tmpLine)
                } else {
                    result.append(tmpLine).append("\n")
                }
            }
        }

        val errorStream = object : LogOutputStream() {

            override fun processBuffer() {
                val privateStringField = LogOutputStream::class.java.getDeclaredField("buffer")
                privateStringField.isAccessible = true
                val buffer = privateStringField.get(this) as ByteArrayOutputStream
                processLine(buffer.toString(charset))
                buffer.reset()
            }

            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }

                var tmpLine: String = prefix + line

                lineParser.forEach {
                    tmpLine = it.onParseLine(tmpLine)
                }
                if (print2Logger) {
                    appendResultToFile(executor.workingDirectory, contextLogFile, tmpLine, jobId, stepId)
                    appendSetErrorToFile(tmpLine, executor.workingDirectory, setErrorFile)
                    LoggerService.addErrorLine(tmpLine)
                } else {
                    result.append(tmpLine).append("\n")
                }
                errorResult.append(tmpLine).append("\n")
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream)
        try {
            val exitCode = executor.execute(cmdLine)
            if (exitCode != 0) {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "$prefix Script command execution failed with exit code($exitCode) \n" +
                        "Error message tracking:\n" +
                        errorResult.toString().takeLast(PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX - 200)
                )
            }
        } catch (ignored: Throwable) {
            val errorMessage = executeErrorMessage ?: "Fail to execute the command($command)"
            logger.warn(errorMessage, ignored)
            if (print2Logger) {
                LoggerService.addErrorLine("$prefix $errorMessage")
            }
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_SCRIPT_COMMAND_INVAILD,
                errorMsg = ignored.message ?: ""
            )
        }
        return result.toString()
    }

    fun reportProgressRate(
        taskId: String?,
        tmpLine: String
    ): Double? {
        val pattern = Pattern.compile("^[\"]?::set-progress-rate\\s*(.*)$")
        val matcher = pattern.matcher(tmpLine.trim())
        if (matcher.find()) {
            val progressRate = matcher.group(1).removeSuffix("\"").toDoubleOrNull()
            if (taskId != null && progressRate != null) {
                Heartbeat.recordTaskProgressRate(
                    taskId = taskId,
                    progressRate = progressRate
                )
            }
            logger.info("report progress rate:$tmpLine|$taskId|$progressRate")
            return progressRate
        }
        return null
    }

    private fun appendResultToFile(
        workspace: File?,
        resultLogFile: String?,
        tmpLine: String,
        jobId: String?,
        stepId: String?
    ) {
        // 全局变量直接原key返回
        if (resultLogFile == null) {
            return
        }
        appendVariableToFile(tmpLine, workspace, resultLogFile)
        appendRemarkToFile(tmpLine, workspace, resultLogFile)
        // 上下文返回给全局时追加jobs前缀
        if (jobId.isNullOrBlank() || stepId.isNullOrBlank()) {
            return
        }
        appendOutputToFile(tmpLine, workspace, resultLogFile, jobId, stepId)
    }

    fun appendVariableToFile(
        tmpLine: String,
        workspace: File?,
        resultLogFile: String
    ): String? {
        val pattenVar = "[\"]?::set-variable\\sname=.*"
        val prefixVar = "::set-variable name="
        if (Pattern.matches(pattenVar, tmpLine)) {
            val value = tmpLine.removeSurrounding("\"").removePrefix(prefixVar)
            val keyValue = value.split("::")
            if (keyValue.size >= 2) {
                val res = "variables.${keyValue[0]}=${value.removePrefix("${keyValue[0]}::")}\n"
                File(workspace, resultLogFile).appendText(res)
                return res
            }
        }
        return null
    }

    fun appendRemarkToFile(
        tmpLine: String,
        workspace: File?,
        resultLogFile: String
    ): String? {
        val pattenVar = "[\"]?::set-remark\\s.*"
        val prefixVar = "::set-remark "
        if (Pattern.matches(pattenVar, tmpLine)) {
            val value = tmpLine.removeSurrounding("\"").removePrefix(prefixVar)
            val res = "BK_CI_BUILD_REMARK=$value\n"
            File(workspace, resultLogFile).appendText(res)
            return res
        }
        return null
    }

    fun appendOutputToFile(
        tmpLine: String,
        workspace: File?,
        resultLogFile: String,
        jobId: String,
        stepId: String
    ): String? {
        val pattenOutput = "[\"]?::set-output\\sname=.*"
        val prefixOutput = "::set-output name="
        if (Pattern.matches(pattenOutput, tmpLine)) {
            val value = tmpLine.removeSurrounding("\"").removePrefix(prefixOutput)
            val keyValue = value.split("::")
            val keyPrefix = "jobs.$jobId.steps.$stepId.outputs."
            if (keyValue.size >= 2) {
                val res = "$keyPrefix${keyValue[0]}=${value.removePrefix("${keyValue[0]}::")}\n"
                File(workspace, resultLogFile).appendText(res)
                return res
            }
        }
        return null
    }

    fun appendSetErrorToFile(
        tmpLine: String,
        workspace: File?,
        resultLogFile: String?
    ): String? {
        if (resultLogFile == null) {
            return null
        }
        val pattenError = "[\"]?::set-error\\s(.*)"
        val prefixError = "::set-error "
        if (Pattern.matches(pattenError, tmpLine)) {
            val value = tmpLine.removeSurrounding("\"").removePrefix(prefixError)
            val code = getOutputMarcher(OUTPUT_ERROR_CODE.matcher(value)) ?: ""
            val message = getOutputMarcher(OUTPUT_ERROR_MESSAGE.matcher(value)) ?: ""
            val res = "$code=$message\n"
            File(workspace, resultLogFile).appendText(res)
            return res
        }
        return null
    }

    fun appendGateToFile(
        tmpLine: String,
        workspace: File?,
        resultLogFile: String
    ): String? {
        val pattenOutput = "[\"]?::set-gate-value\\sname=.*"
        val prefixOutput = "::set-gate-value name="
        if (Pattern.matches(pattenOutput, tmpLine)) {
            val value = tmpLine.removeSurrounding("\"").removePrefix(prefixOutput)
            val keyValue = value.split("::")
            if (keyValue.size >= 2) {
                val res = "${keyValue[0]}=${value.removePrefix("${keyValue[0]}::")}\n"
                File(workspace, resultLogFile).appendText(res)
                return res
            }
        }
        return null
    }

    fun execute(file: File, workspace: File?, print2Logger: Boolean, prefix: String = ""): String {
        if (!file.exists()) {
            logger.warn("The file(${file.absolutePath}) is not exist")
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorType = ErrorType.USER,
                errorMsg = "The file(${file.absolutePath}) is not exist"
            )
        }
        val command = if (getOS() == OSType.WINDOWS) {
            file.name
        } else {
            execute("chmod +x ${file.name}", workspace, false)
            "./${file.name}"
        }
        logger.info("Executing command($command) in workspace($workspace)")
        return execute(command, workspace, print2Logger, prefix)
    }

    private fun getOutputMarcher(matcher: Matcher): String? {
        return with(matcher) {
            /*只返回匹配到的第一个，否则返回null*/
            if (this.find()) {
                this.group(1)
            } else null
        }
    }
}
