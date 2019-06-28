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

import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import java.io.File

object ExecutorUtil {
    private val executor = DefaultExecutor()

    fun runCommand(command: String, maskCommand: String, workDir: File? = null): Int {
        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                LoggerService.addNormalLine("$line")
            }
        }
        return runCommand(command, maskCommand, outputStream, outputStream, workDir)
    }

    fun runCommand(
        command: String,
        maskCommand: String,
        stdout: LogOutputStream,
        stderr: LogOutputStream,
        workDir: File? = null
    ): Int {
        LoggerService.addNormalLine("Start to run the command - $maskCommand")
        val commandLine = CommandLine.parse(command)
        val streamHandler = PumpStreamHandler(stdout, stderr)
        executor.streamHandler = streamHandler
        if (workDir != null)
            executor.workingDirectory = workDir

        val exitValue = executor.execute(commandLine)
        if (exitValue != 0) {
            LoggerService.addNormalLine("Fail to execute the command($maskCommand) with exit code ($exitValue)")
            throw ExecuteException("Fail to run the command - $maskCommand")
        }
        LoggerService.addNormalLine("Finish the command, exitValue=$exitValue")
        return exitValue
    }
}
