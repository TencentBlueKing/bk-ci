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

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.worker.common.utils.ExecutorUtil
import org.slf4j.LoggerFactory
import java.io.File

object ScriptEnvUtils {
    private const val ENV_FILE = "result.log"
    private const val QUALITY_GATEWAY_FILE = "gatewayValueFile.ini"
    private val logger = LoggerFactory.getLogger(ScriptEnvUtils::class.java)

    fun cleanEnv(buildId: String, workspace: File) {
        cleanScriptEnv(workspace, getEnvFile(buildId))
        cleanScriptEnv(workspace, "$buildId-$ENV_FILE")
    }

    fun getEnv(buildId: String, workspace: File): Map<String, String> {
        return readScriptEnv(workspace, "$buildId-$ENV_FILE")
            .plus(readScriptEnv(workspace, getEnvFile(buildId)))
    }

    fun getEnvFile(buildId: String): String {
        val randomNum = ExecutorUtil.getThreadLocal()
        return "$buildId-$randomNum-$ENV_FILE"
    }

    fun getDefaultFile(buildId: String): String {
        return "$buildId-$ENV_FILE"
    }

    fun cleanWhenEnd(buildId: String, workspace: File) {
        val defaultFilePath = getDefaultFile(buildId)
        val randomFilePath = getEnvFile(buildId)
        val defaultFile = File(workspace, defaultFilePath)
        if (defaultFile.exists()) {
            defaultFile.delete()
        }

        val randomFile = File(workspace, randomFilePath)
        if (randomFile.exists()) {
            randomFile.delete()
        }
        ExecutorUtil.removeThreadLocal()
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
        if (!f.exists()) {
            return mapOf()
        }
        if (f.isDirectory) {
            return mapOf()
        }

        val lines = f.readLines()
        if (lines.isEmpty()) {
            return mapOf()
        }
        // KEY-VALUE
        return lines.filter { it.contains("=") }.map {
            val split = it.split("=", ignoreCase = false, limit = 2)
            split[0].trim() to split[1].trim()
        }.toMap()
    }

}