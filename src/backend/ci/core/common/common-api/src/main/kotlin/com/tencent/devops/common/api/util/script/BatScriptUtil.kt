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

package com.tencent.devops.common.api.util.script

import com.tencent.devops.common.api.pojo.CommonEnv
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset

object BatScriptUtil {
    private val logger = LoggerFactory.getLogger(BatScriptUtil::class.java)

    fun executeEnhance(
        script: String,
        runtimeVariables: Map<String, String>,
        dir: File? = null,
        print2Logger: Boolean = false
    ): String {
        val enhanceScript = CommandLineUtils.solveSpecialChar(script)
        return execute(enhanceScript, dir, runtimeVariables, print2Logger)
    }

    private fun execute(
        script: String,
        dir: File?,
        runtimeVariables: Map<String, String>,
        print2Logger: Boolean = false
    ): String {
        try {
            val tmpDir = System.getProperty("java.io.tmpdir")
            val file = if (tmpDir.isNullOrBlank()) {
                File.createTempFile("paas_build_script_", ".bat")
            } else {
                File(tmpDir).mkdirs()
                File.createTempFile("paas_build_script_", ".bat", File(tmpDir))
            }
            file.deleteOnExit()

            val command = StringBuilder()

            command.append("@echo off")
                .append("\r\n")
                .append("set DEVOPS_BUILD_SCRIPT_FILE=${file.absolutePath}\r\n")
                .append("\r\n")

            runtimeVariables.plus(CommonEnv.getCommonEnv())
                .forEach { (name, value) ->
                    // 特殊保留字符转义
                    val clean = value.replace("\"", "\\\"")
                        .replace("&", "^&")
                        .replace("<", "^<")
                        .replace(">", "^>")
                        .replace("|", "^|")
                    command.append("set $name=\"$clean\"\r\n") // 双引号防止变量值有空格而意外截断定义
                    command.append("set $name=%$name:~1,-1%\r\n") // 去除又引号，防止被程序读到有双引号的变量值
                }

            command.append(script.replace("\n", "\r\n"))
                .append("\r\n")
                .append("exit")
                .append("\r\n")

            val charset = Charset.defaultCharset()

            file.writeText(command.toString(), charset)
            return CommandLineUtils.execute("cmd.exe /C \"${file.canonicalPath}\"", dir, print2Logger)
        } catch (ignore: Throwable) {
            logger.warn("Fail to execute bat script", ignore)
            throw ignore
        }
    }
}
