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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.pipeline.enums.CharsetType
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.WORKSPACE_ENV
import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileLock
import java.nio.charset.Charset

object BatScriptUtil {
    private const val setEnv = ":setEnv\r\n" +
        "    set file_save_dir=\"##resultFile##\"\r\n" +
        "    echo %~1=%~2 >>%file_save_dir%\r\n" +
        "    set %~1=%~2\r\n" +
        "    goto:eof\r\n"

    private const val setGateValue = ":setGateValue\r\n" +
        "    set file_save_dir=\"##gateValueFile##\"\r\n" +
        "    echo %~1=%~2 >>%file_save_dir%\r\n" +
        "    set %~1=%~2\r\n" +
        "    goto:eof\r\n"

    private val logger = LoggerFactory.getLogger(BatScriptUtil::class.java)

    // 2021-06-11 batchScript需要过滤掉上下文产生的变量，防止注入到环境变量中
    private val specialKey = listOf("variables.", "settings.", "envs.", "ci.", "job.", "jobs.", "steps.", "matrix.")

    private val specialValue = listOf("\n", "\r")
    private val escapeValue = mapOf(
        "&" to "^&",
        "<" to "^<",
        ">" to "^>",
        "|" to "^|",
        "\"" to "\\\""
    )

    @Suppress("ALL")
    fun execute(
        buildId: String,
        script: String,
        runtimeVariables: Map<String, String>,
        dir: File,
        prefix: String = "",
        errorMessage: String? = null,
        workspace: File = dir,
        print2Logger: Boolean = true,
        jobId: String? = null,
        stepId: String? = null,
        charsetType: String? = null
    ): String {
        var fileLock: FileLock? = null
        try {
            val file = getCommandFile(
                buildId = buildId,
                script = script,
                runtimeVariables = runtimeVariables,
                dir = dir,
                workspace = workspace,
                charsetType = charsetType
            )

            fileLock = tryLock(file) // 拿不锁也照样执行
            return CommandLineUtils.execute(
                command = "cmd.exe /C \"${file.canonicalPath}\"",
                workspace = dir,
                print2Logger = print2Logger,
                prefix = prefix,
                executeErrorMessage = "",
                buildId = buildId,
                jobId = jobId,
                stepId = stepId,
                charsetType = charsetType
            )
        } catch (ignore: Throwable) {
            val errorInfo = errorMessage ?: "Fail to execute bat script $script"
            logger.warn(errorInfo, ignore)
            throw ignore
        } finally {
            fileLock?.release()
        }
    }

    /**
     * #5672 补充丢失的代码：feat: 对BatchScript脚本执行过程加锁，防止被系统清理掉 #5527
     */
    private fun tryLock(file: File, shared: Boolean = true): FileLock? =
        try {
            logger.info("lock file ${file.absolutePath}")
            FileInputStream(file).channel.tryLock(0, file.length(), shared)
        } catch (ignore: Exception) { // 异常忽略，不应该让执行失败
            logger.warn("lock ${file.absolutePath} fail: ", ignore)
            null
        }

    @Suppress("ALL")
    fun getCommandFile(
        buildId: String,
        script: String,
        runtimeVariables: Map<String, String>,
        dir: File,
        workspace: File = dir,
        charsetType: String? = null
    ): File {
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
            .append("set $WORKSPACE_ENV=${workspace.absolutePath}\r\n")
            .append("set DEVOPS_BUILD_SCRIPT_FILE=${file.absolutePath}\r\n")
            .append("\r\n")

        runtimeVariables.plus(CommonEnv.getCommonEnv())
            .filter { !specialEnv(it.key, it.value) }
            .forEach { (name, value) ->
                // 特殊保留字符转义
                val clean = escapeEnv(value)
                command.append("set $name=\"$clean\"\r\n") // 双引号防止变量值有空格而意外截断定义
                command.append("set $name=%$name:~1,-1%\r\n") // 去除双引号，防止被程序读到有双引号的变量值
            }

        command.append(script.replace("\n", "\r\n"))
            .append("\r\n")
            .append("exit")
            .append("\r\n")
            .append(
                setEnv.replace(
                    oldValue = "##resultFile##",
                    newValue = File(dir, ScriptEnvUtils.getEnvFile(buildId)).absolutePath
                )
            )
            .append(
                setGateValue.replace(
                    oldValue = "##gateValueFile##",
                    newValue = File(dir, ScriptEnvUtils.getQualityGatewayEnvFile()).canonicalPath
                )
            )

        // #4601 没有指定编码字符集时采用获取系统的默认字符集
        val charset = when (charsetType?.let { CharsetType.valueOf(it) }) {
            CharsetType.UTF_8 -> Charsets.UTF_8
            CharsetType.GBK -> Charset.forName(CharsetType.GBK.name)
            else -> Charset.defaultCharset()
        }

        logger.info("The default charset is $charset")

        file.writeText(command.toString(), charset)
        logger.info("start to run windows script - ($command)")
        return file
    }

    private fun specialEnv(key: String, value: String): Boolean {
        var match = false
        for (it in specialKey) {
            if (key.trim().startsWith(it)) {
                match = true
                break
            }
        }

        for (it in specialValue) {
            if (value.contains(it)) {
                match = true
                break
            }
        }
        return match
    }

    private fun escapeEnv(value: String): String {
        var result = value
        escapeValue.forEach { (k, v) ->
            result = result.replace(k, v)
        }
        return result
    }
}
