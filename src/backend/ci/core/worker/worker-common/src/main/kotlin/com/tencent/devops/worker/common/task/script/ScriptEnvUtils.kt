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
    /** 多行输出的单次内容上限与读取预算（UTF-8 字节）：脚本侧注入预检与 Kotlin 侧读取共用 */
    const val MULTILINE_FILE_MAX_LENGTH = 10 * 1024 * 1024L

    const val VAR_NAME_SEGMENT = "[a-zA-Z_][a-zA-Z0-9_]*"

    /** 合法变量名的完整匹配正则 */
    val varNameRegex = Regex("^$VAR_NAME_SEGMENT$")
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

    /**
     * 读取本次任务的多行输出记录（每行一条 `::set-output name=KEY::VALUE`，由 `format_multiple_lines` 写入）。
     *
     * 达到读取预算时截断，**已读记录仍会返回**；读取过程中的异常同样不丢弃已读部分。
     * 截断与读取失败各以一条告警写入构建日志，告警失败不影响返回值。
     */
    fun getMultipleLines(buildId: String, workspace: File): List<String> {
        val result = mutableListOf<String>()
        val truncatedAt = try {
            readMultipleLines(buildId, workspace, result)
        } catch (ignore: Throwable) {
            warnQuietly(BK_MULTILINE_READ_FAILED, arrayOf(ignore.message ?: ""))
            return result
        }
        if (truncatedAt != null) {
            warnQuietly(
                BK_MULTILINE_FILE_TOO_LARGE,
                arrayOf(truncatedAt.toString(), MULTILINE_FILE_MAX_LENGTH.toString())
            )
        }
        return result
    }

    /**
     * 按 [messageCode] 与 [params] 渲染并写出一条多行输出告警。
     *
     * 告警链路自身抛出的异常（含静态初始化失败等 `Error`）在此被忽略，调用方的读取结果不受其影响。
     */
    private fun warnQuietly(messageCode: String, params: Array<String>) {
        try {
            LoggerService.addWarnLine(
                MessageUtil.getMessageByLocale(
                    messageCode = messageCode,
                    language = AgentEnv.getLocaleLanguage(),
                    params = params
                )
            )
        } catch (ignored: Throwable) {
        }
    }

    /**
     * 逐行读取 `<buildId>-<randomNum>-multiLine.log`，已读记录追加到 [result]。
     *
     * 读取预算 [MULTILINE_FILE_MAX_LENGTH] 按**编码后**内容的 UTF-8 字节累计，不含行分隔符；
     * 每行至少计 1 字节，达到预算即停止读取（已读部分不回退）。首行的 BOM 会被剥离。
     *
     * @param result 出参：由调用方持有，读取中途异常时其中已读记录仍然可见
     * @return 因超预算截断时的已读字节数；未截断、文件不存在或为目录时返回 null
     */
    private fun readMultipleLines(buildId: String, workspace: File, result: MutableList<String>): Long? {
        val f = File(workspace, getMultipleLineFile(buildId))
        if (!f.exists() || f.isDirectory) return null
        var consumed = 0L
        var truncatedAt: Long? = null
        f.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                // 空行按 1 字节计入预算，使换行密集内容同样消耗预算
                val cost = maxOf(line.toByteArray(Charsets.UTF_8).size, 1)
                if (consumed + cost > MULTILINE_FILE_MAX_LENGTH) {
                    truncatedAt = consumed
                    break
                }
                consumed += cost
                result.add(if (result.isEmpty()) line.removePrefix("\uFEFF") else line)
            }
        }
        return truncatedAt
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
     * 删除内联多行块临时文件：序号由 0 连续递增，遇到第一个不存在的文件即结束。
     * 单个文件删除失败不终止循环，避免后续序号的文件残留
     */
    private fun cleanMultilineBlockFiles(buildId: String) {
        val tmpDir = System.getProperty("java.io.tmpdir") ?: return
        var index = 0
        while (true) {
            val blockFile = File(tmpDir, getMultipleLineBlockFileName(buildId, index))
            if (!blockFile.exists()) return
            if (!blockFile.delete()) {
                logger.warn("Fail to delete the multiline block file - (${blockFile.absolutePath})")
            }
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
                varNameRegex.matches(it.first)
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
