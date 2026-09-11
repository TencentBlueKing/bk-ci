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

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_MULTILINE_FILE_TOO_LARGE
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_MULTILINE_READ_FAILED
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ExecutorUtil
import org.slf4j.LoggerFactory
import java.io.File

@Suppress("TooManyFunctions")
object ScriptEnvUtils {
    private const val ENV_FILE = "result.log"
    private const val FLAG_FILE = "flag.log"
    private const val CONTEXT_FILE = "context.log"
    private const val ERROR_FILE = "setError.log"
    private const val MULTILINE_FILE = "multiLine.log"
    private const val QUALITY_GATEWAY_FILE = "gatewayValueFile.ini"
    /** 多行输出文件的大小上限（字节） */
    private const val MULTILINE_FILE_MAX_LENGTH = 10 * 1024 * 1024L
    private val keyRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    private val lineSplitRegex = Regex("\\r\\n|\\r|\\n")
    private val logger = LoggerFactory.getLogger(ScriptEnvUtils::class.java)

    fun cleanEnv(buildId: String, workspace: File) {
        cleanScriptEnv(workspace, getEnvFile(buildId))
        cleanScriptEnv(workspace, getDefaultEnvFile(buildId))
    }

    fun cleanContext(buildId: String, workspace: File) {
        cleanScriptEnv(workspace, getContextFile(buildId))
    }

    fun getEnv(buildId: String, workspace: File): Map<String, String> {
        return readScriptEnv(workspace, "$buildId-$ENV_FILE")
            .plus(readScriptEnv(workspace, getEnvFile(buildId)))
    }

    fun getContext(buildId: String, workspace: File): Map<String, String> {
        return readScriptContext(workspace, getContextFile(buildId))
    }

    fun getSetError(buildId: String, workspace: File): Map<String, String> {
        return readScriptContext(workspace, getSetErrorFile(buildId))
    }

    fun getEnvFile(buildId: String): String {
        val randomNum = ExecutorUtil.getThreadLocal()
        return "$buildId-$randomNum-$ENV_FILE"
    }

    fun getContextFile(buildId: String): String {
        val randomNum = ExecutorUtil.getThreadLocal()
        return "$buildId-$randomNum-$CONTEXT_FILE"
    }

    fun getSetErrorFile(buildId: String): String {
        val randomNum = ExecutorUtil.getThreadLocal()
        return "$buildId-$randomNum-$ERROR_FILE"
    }

    fun getMultipleLineFile(buildId: String): String {
        val randomNum = ExecutorUtil.getThreadLocal()
        return "$buildId-$randomNum-$MULTILINE_FILE"
    }

    fun getMultipleLines(buildId: String, workspace: File): List<String> {
        return try {
            readMultipleLines(buildId, workspace)
        } catch (ignore: Throwable) {
            /* 告警写入失败不影响读取结果 */
            runCatching {
                LoggerService.addWarnLine(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_MULTILINE_READ_FAILED,
                        language = AgentEnv.getLocaleLanguage(),
                        params = arrayOf(ignore.message ?: "")
                    )
                )
            }
            emptyList()
        }
    }

    private fun readMultipleLines(buildId: String, workspace: File): List<String> {
        val f = File(workspace, getMultipleLineFile(buildId))
        if (!f.exists() || f.isDirectory) return emptyList()
        if (f.length() > MULTILINE_FILE_MAX_LENGTH) {
            LoggerService.addWarnLine(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_MULTILINE_FILE_TOO_LARGE,
                    language = AgentEnv.getLocaleLanguage(),
                    params = arrayOf(f.length().toString(), MULTILINE_FILE_MAX_LENGTH.toString())
                )
            )
            return emptyList()
        }
        return f.readText(Charsets.UTF_8)
            .removePrefix("\uFEFF")
            .split(lineSplitRegex)
            .let { if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it }
    }
    /*限定文件名*/
    fun getFlagFile(buildId: String): String {
        val randomNum = ExecutorUtil.getThreadLocal()
        return "$buildId-$randomNum-$FLAG_FILE"
    }

    fun readFlagFile(buildId: String, workspace: File): String {
        val flagFile = File(workspace, getFlagFile(buildId))
        if (flagFile.exists()) {
            val flag = flagFile.readText()
            logger.info("Flag: ${flag.replace("\r", "").replace("\n", " ")}")
            return flag
        }
        return ""
    }

    fun deleteFlagFile(buildId: String, workspace: File) {
        val flagFile = getFlagFile(buildId)
        deleteFile(flagFile, workspace)
    }

    private fun getDefaultEnvFile(buildId: String): String {
        return "$buildId-$ENV_FILE"
    }

    fun cleanWhenEnd(buildId: String, workspace: File) {
        val defaultEnvFilePath = getDefaultEnvFile(buildId)
        val randomEnvFilePath = getEnvFile(buildId)
        val randomContextFilePath = getContextFile(buildId)
        val randomSetErrorFilePath = getSetErrorFile(buildId)
        val flagFile = getFlagFile(buildId)
        val multiLineFilePath = getMultipleLineFile(buildId)
        deleteFile(multiLineFilePath, workspace)
        cleanMultilineBlockFiles(buildId)
        deleteFile(defaultEnvFilePath, workspace)
        deleteFile(randomEnvFilePath, workspace)
        deleteFile(randomContextFilePath, workspace)
        deleteFile(randomSetErrorFilePath, workspace)
        deleteFile(flagFile, workspace)
        ExecutorUtil.removeThreadLocal()
    }

    /** 内联多行块临时文件名（与 BatScriptUtil 写入时保持一致） */
    fun getMultipleLineBlockFileName(buildId: String, index: Int): String {
        return "ml_block_${buildId}_${ExecutorUtil.getThreadLocal()}_$index.txt"
    }

    /**
     * 删除内联多行块临时文件：序号由 0 连续递增，遇到第一个不存在的文件即结束
     */
    private fun cleanMultilineBlockFiles(buildId: String) {
        val tmpDir = System.getProperty("java.io.tmpdir") ?: return
        var index = 0
        while (true) {
            val blockFile = File(tmpDir, getMultipleLineBlockFileName(buildId, index))
            if (!blockFile.exists() || !blockFile.delete()) return
            index++
        }
    }

    private fun deleteFile(filePath: String, workspace: File) {
        val defaultFile = File(workspace, filePath)
        if (defaultFile.exists()) {
            defaultFile.delete()
        }
    }

    fun getQualityGatewayEnvFile() = QUALITY_GATEWAY_FILE

    private fun cleanScriptEnv(workspace: File, file: String) {
        val scriptFile = File(workspace, file)
        if (scriptFile.exists()) {
            scriptFile.delete()
        }
        if (!scriptFile.createNewFile()) {
            logger.warn("Fail to create the file - (${scriptFile.absolutePath})")
        } else {
            scriptFile.deleteOnExit()
        }
    }

    private fun readScriptEnv(workspace: File, file: String): Map<String, String> {
        val f = File(workspace, file)
        if (!f.exists() || f.isDirectory) {
            return mapOf()
        }

        val lines = f.readLines()
        return if (lines.isEmpty()) {
            mapOf()
        } else {
            // KEY-VALUE
            lines.filter { it.contains("=") }.map {
                val split = it.split("=", ignoreCase = false, limit = 2)
                split[0].trim() to split[1].trim()
            }.filter {
                // #3453 保存时再次校验key的合法性
                keyRegex.matches(it.first)
            }.toMap()
        }
    }

    private fun readScriptContext(workspace: File, file: String): Map<String, String> {
        val f = File(workspace, file)
        if (!f.exists() || f.isDirectory) {
            return mapOf()
        }

        val lines = f.readLines()
        return if (lines.isEmpty()) {
            mapOf()
        } else {
            // KEY-VALUE
            lines.filter { it.contains("=") }.map {
                val split = it.split("=", ignoreCase = false, limit = 2)
                split[0].trim() to split[1].trim()
            }.toMap()
        }
    }
}
