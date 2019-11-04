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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.worker.common.env.AgentEnv.getOS
import com.tencent.devops.worker.common.logger.LoggerService
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.File

object WindowsCommandLineUtils {

    // windows执行命令会把对应的命令也打印出来
    // 命令里面可能有密码等参数，要去掉
    private val sensitiveWords = listOf("PRIVATE_KEY", "PASSWORD")

    private val logger = LoggerFactory.getLogger(CommandLineUtils::class.java)

    private val lineParser = listOf(OauthCredentialLineParser())

    fun execute(command: String, workspace: File?, print2Logger: Boolean, prefix: String = ""): String {

        val result = StringBuilder()

        val cmdLine = CommandLine.parse(command)
        val executor = CommandLineExecutor()
        if (workspace != null) {
            executor.workingDirectory = workspace
        }

        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null)
                    return

                var tmpLine: String = prefix + line

                lineParser.forEach {
                    tmpLine = it.onParseLine(tmpLine)
                }
                if (print2Logger && !containSensitive(tmpLine)) {
                    LoggerService.addNormalLine(tmpLine)
                } else {
                    result.append(tmpLine).append("\n")
                }
            }
        }

        val errorStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }

                var tmpLine: String = prefix + line

                lineParser.forEach {
                    tmpLine = it.onParseLine(tmpLine)
                }
                if (print2Logger) {
                    LoggerService.addRedLine(tmpLine)
                } else {
                    result.append(tmpLine).append("\n")
                }
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream)
        try {
            val exitCode = executor.execute(cmdLine)
            if (exitCode != 0) {
                throw ExecuteException("$prefix Script command execution failed with exit code($exitCode)")
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to execute the command($command)", ignored)
            if (print2Logger) {
                LoggerService.addRedLine("$prefix Fail to execute the command($command)")
            }
            throw ignored
        }
        return result.toString()
    }

    fun execute(file: File, workspace: File?, print2Logger: Boolean, prefix: String = ""): String {
        if (!file.exists()) {
            logger.warn("The file(${file.absolutePath}) is not exist")
            throw ExecuteException("The file(${file.absolutePath}) is not exist")
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

    private fun containSensitive(str: String): Boolean {
        sensitiveWords.forEach {
            if (str.contains(it)) return true
        }
        return false
    }
}