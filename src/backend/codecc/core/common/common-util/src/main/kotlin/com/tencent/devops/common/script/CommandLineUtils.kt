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

package com.tencent.devops.common.script

import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CommonMessageCode
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.File

object CommandLineUtils {

    private val logger = LoggerFactory.getLogger(CommandLineUtils::class.java)

    private val specialChars = if (System.getProperty("os.name").startsWith("Windows", true)) {
        listOf('(', ')', '[', ']', '{', '}', '^', ';', '!', ',', '`', '~', '\'', '"')
    } else {
        listOf('|', ';', '&', '$', '>', '<', '`', '!', '\\', '"', '*', '?', '[', ']', '(', ')', '\'')
    }

    fun execute(
        command: String,
        workspace: File?,
        print2Logger: Boolean,
        prefix: String = "",
        execResultHandler: DefaultExecuteResultHandler?
    ): String {

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

                val tmpLine: String = SensitiveLineParser.onParseLine(prefix + line)
                if (print2Logger) {
                    logger.info(tmpLine)
                }
                result.append(tmpLine).append("\n")
            }
        }

        val errorStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }

                val tmpLine: String = SensitiveLineParser.onParseLine(prefix + line)
                if (print2Logger) {
                    logger.error(tmpLine)
                }
                result.append(tmpLine).append("\n")
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream)
        try {
            //同步
            if (null == execResultHandler) {
                val exitCode = executor.execute(cmdLine)
                if (exitCode != 0) {
                    throw CodeCCException(
                        CommonMessageCode.SYSTEM_ERROR
                    )
                }
            } else {
                //异步，不抛异常
                executor.execute(cmdLine, execResultHandler)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to execute the command($command)", t)
            if (print2Logger) {
                logger.error("$prefix Fail to execute the command($command)")
            }
            throw t
        }
        return result.toString()
    }

    fun solveSpecialChar(str: String): String {
        val solveStr = StringBuilder()
        val isWindows = System.getProperty("os.name").startsWith("Windows", true)
        val encodeChar = if (isWindows) '^' else '\\'
        val charArr = str.toCharArray()
        charArr.forEach { ch ->
            if (ch in specialChars) {
                solveStr.append(encodeChar)
            }

            // windows的%还要特殊处理下
            if (isWindows && ch == '%') {
                solveStr.append('%')
            }

            solveStr.append(ch)
        }

        return solveStr.toString()
    }
}