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

package com.tencent.devops.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.agent.runner.WorkRunner
import com.tencent.devops.common.api.enums.EnumLoader
import com.tencent.devops.common.pipeline.ElementSubTypeRegisterLoader
import com.tencent.devops.worker.common.BUILD_TYPE
import com.tencent.devops.worker.common.Runner
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.AGENT_ID
import com.tencent.devops.worker.common.AGENT_SECRET_KEY
import com.tencent.devops.worker.common.JOB_POOL
import okhttp3.Request
import com.tencent.devops.worker.common.WorkspaceInterface
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.task.TaskFactory
import java.io.File
import java.lang.RuntimeException
import com.tencent.devops.worker.common.utils.ExecutorUtil.runCommand
import com.tencent.devops.worker.common.utils.WorkspaceUtils

fun main(args: Array<String>) {
    EnumLoader.enumModified()
    ElementSubTypeRegisterLoader.registerElementForJsonUtil()
    ApiFactory.init()
    TaskFactory.init()
    val buildType = System.getProperty(BUILD_TYPE)
    when (buildType) {
        BuildType.DOCKER.name -> {
            val jobPoolType = getProperty(JOB_POOL)
            // 无编译构建，轮询等待任务
            if (jobPoolType != null &&
                jobPoolType.equals(com.tencent.devops.common.pipeline.type.BuildType.AGENT_LESS.name)) {
                waitBuildLessJobStart()
            }

            Runner.run(object : WorkspaceInterface {
                override fun getWorkspaceAndLogDir(
                    variables: Map<String, String>,
                    pipelineId: String
                ): Pair<File, File> {
                    val workspace = System.getProperty("devops_workspace")

                    val workspaceDir = if (workspace.isNullOrBlank()) {
                        File("/data/landun/workspace") // v1 内部版用的/data/landun/workspace 保持一致
                    } else {
                        File(workspace)
                    }
                    workspaceDir.mkdirs()
                    val logPathDir = WorkspaceUtils.getPipelineLogDir(pipelineId)
                    return Pair(workspaceDir, logPathDir)
                }
            })
        }
        BuildType.WORKER.name -> {
            Runner.run(object : WorkspaceInterface {
                override fun getWorkspaceAndLogDir(
                    variables: Map<String, String>,
                    pipelineId: String
                ): Pair<File, File> {
                    val workspaceDir = WorkspaceUtils.getPipelineWorkspace(
                        pipelineId = pipelineId,
                        workspace = ""
                    )
                    if (workspaceDir.exists()) {
                        if (!workspaceDir.isDirectory) {
                            throw RuntimeException("Work space directory conflict: ${workspaceDir.canonicalPath}")
                        }
                    } else {
                        workspaceDir.mkdirs()
                    }
                    val logPathDir = WorkspaceUtils.getPipelineLogDir(pipelineId)
                    return Pair(workspaceDir, logPathDir)
                }
            })
        }
        BuildType.MACOS.name -> {
            var startBuild: Boolean = false
            val gateyway = AgentEnv.getGateway()
            val url = "http://$gateyway/dispatch-macos/gw/build/macos/startBuild"
            System.out.println("url:$url")
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("X-DEVOPS-BUILD-TYPE", "MACOS")
                .get()
                .build()

            var xcodeVersion = ""
            do {
                try {
                    OkhttpUtils.doHttp(request).use { resp ->
                        val resoCode = resp.code()
                        val responseStr = resp.body()!!.string()
                        System.out.println("resoCode: $resoCode;responseStr:$responseStr")
                        if (resoCode == 200) {
                            val response: Map<String, String> = jacksonObjectMapper().readValue(responseStr)

                            // 将变量写入到property当中
                            response.forEach { (key, value) ->
                                when (key) {
                                    "agentId" -> System.setProperty("devops.agent.id", value)
                                    "secretKey" -> System.setProperty("devops.agent.secret.key", value)
                                    "projectId" -> System.setProperty("devops.project.id", value)
                                    "xcodeVersion" -> xcodeVersion = value
                                    else -> null
                                }
                            }
                            startBuild = true
                        } else {
                            System.out.println("There is no build for this macos,sleep for 5s.")
                        }
                    }
                    if (!startBuild) {
                        Thread.sleep(5000)
                    }
                } catch (e: Exception) {
                    System.out.println("Failed to connect to devops server.")
                }
            } while (!startBuild)
            System.out.println("Start to run.")

            System.out.println("Start to select xcode.")
            // 选择XCODE版本
            val xcodePath = "/Applications/Xcode_$xcodeVersion.app"
            val xcodeFile = File(xcodePath)
            // 当指定XCode版本存在的时候，切换xcode
            if (xcodeFile.exists() && xcodeFile.isDirectory) {
                try {
                    // 删除软链
                    val rmCommand = "sudo rm -rf /Applications/Xcode.app"
                    runCommand(rmCommand, rmCommand)
                    // 新建软链
                    val lnCommand = "sudo ln -s /Applications/Xcode_$xcodeVersion.app  /Applications/Xcode.app"
                    runCommand(lnCommand, lnCommand)
                    // 选择xcode
                    val selectCommand = "sudo xcode-select -s /Applications/Xcode.app/Contents/Developer/"
                    runCommand(selectCommand, selectCommand)
                    System.out.println("End to select xcode:select Xcode_$xcodeVersion.app.")
                } catch (e: Exception) {
                    System.out.println("End to select xcode with error: $e")
                }
            } else {
                System.out.println("End to select xcode:nothing to do.")
            }

            Runner.run(object : WorkspaceInterface {
                override fun getWorkspaceAndLogDir(
                    variables: Map<String, String>,
                    pipelineId: String
                ): Pair<File, File> {
                    val workspace = AgentEnv.getMacOSWorkspace()
                    System.out.println("MacOS workspace: $workspace")
                    val workspaceDir = File(workspace)
                    workspaceDir.mkdirs()
                    val logPathDir = WorkspaceUtils.getPipelineLogDir(pipelineId)
                    logPathDir.mkdirs()
                    return Pair(workspaceDir, logPathDir)
                }
            })
        }
        BuildType.AGENT.name -> {
            WorkRunner.execute(args)
        }
        else -> {
            if (buildType.isNullOrBlank()) {
                throw RuntimeException("The build type is empty")
            }
            throw RuntimeException("Unknown build type - $buildType")
        }
    }
}

private fun waitBuildLessJobStart() {
    val agentId = getProperty(AGENT_ID)
    val agentSecretKey = getProperty(AGENT_SECRET_KEY)
    do {
        println("Docker buildLess waiting start...")
        Thread.sleep(1000)
    } while (agentId.isNullOrBlank() || agentSecretKey.isNullOrBlank())
}

private fun getProperty(prop: String): String? {
    var value = System.getenv(prop)
    if (value.isNullOrBlank()) {
        // Get from java properties
        value = System.getProperty(prop)
    }
    return value
}
