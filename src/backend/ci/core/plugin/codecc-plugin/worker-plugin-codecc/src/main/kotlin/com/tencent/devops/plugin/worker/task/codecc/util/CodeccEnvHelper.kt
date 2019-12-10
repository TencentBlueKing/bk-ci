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

package com.tencent.devops.plugin.worker.task.codecc.util

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.codecc.CodeccSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File

object CodeccEnvHelper {

    private val api = ApiFactory.create(CodeccSDKApi::class)

    private val ENV_FILES = arrayOf("result.log", "result.ini")

    fun getCodeccEnv(workspace: File): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        ENV_FILES.map { result.putAll(readScriptEnv(workspace, it)) }
        return result
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

    fun saveTask(buildVariables: BuildVariables) {
        api.saveTask(buildVariables.projectId, buildVariables.pipelineId, buildVariables.buildId)
    }

    // 第三方构建机初始化
    fun thirdInit(coverityConfig: CodeccExecuteConfig) {
        // 第三方构建机安装环境
        val channelCode =  coverityConfig.buildVariables.variables["pipeline.start.channel"] ?: ""
        if (BuildEnv.isThirdParty()) {
            when (AgentEnv.getOS()) {
                OSType.WINDOWS -> {
                    CodeccInstaller.windowsDonwloadScript()
                }
                OSType.LINUX -> {
                    CodeccInstaller.donwloadScript()
                    CodeccInstaller.setUpPython3(coverityConfig)
                }
                OSType.MAC_OS -> {
                    CodeccInstaller.donwloadScript()
                    CodeccInstaller.setupTools(coverityConfig)
                }
                else -> {
                }
            }
        } else {
            // mac公共机需要安装 python3 环境
            if (AgentEnv.getOS() == OSType.MAC_OS) {
                val pythonExist =
                    CodeccInstaller.pythonExist(File("/data/soda/apps/python/3.5/IDLE.app/Contents/MacOS/Python"))
                LoggerService.addNormalLine("check mac python is exist : $pythonExist")
                if (!pythonExist) {
                    LoggerService.addNormalLine("python installing...")
                    CodeccInstaller.installMacPython()
                }
            }
        }

    }
}