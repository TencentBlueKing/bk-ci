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

package com.tencent.devops.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.agent.runner.WorkRunner
import com.tencent.devops.common.pipeline.ElementSubTypeRegisterLoader
import com.tencent.devops.worker.common.BUILD_TYPE
import com.tencent.devops.worker.common.Runner
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import com.tencent.devops.worker.common.WorkspaceInterface
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.task.TaskFactory
import java.io.File
import java.lang.RuntimeException

fun main(args: Array<String>) {
    ElementSubTypeRegisterLoader.registerElementForJsonUtil()
    ApiFactory.init()
    TaskFactory.init()
    val buildType = System.getProperty(BUILD_TYPE)
    when (buildType) {
        BuildType.DOCKER.name ->
            Runner.run(object : WorkspaceInterface {
                override fun getWorkspace(variables: Map<String, String>, pipelineId: String): File {
                    val workspace = System.getProperty("devops_workspace")

                    val dir = if (workspace.isNullOrBlank()) {
                        File("/data/landun/workspace") // v1 内部版用的/data/landun/workspace 保持一致
                    } else {
                        File(workspace)
                    }
                    dir.mkdirs()
                    return dir
                }
            })
        BuildType.WORKER.name -> {
            Runner.run(object : WorkspaceInterface {
                override fun getWorkspace(variables: Map<String, String>, pipelineId: String): File {
                    val dir = File("./$pipelineId/src")
                    if (dir.exists()) {
                        if (!dir.isDirectory) {
                            throw RuntimeException("Work space directory conflict: ${dir.canonicalPath}")
                        }
                    } else {
                        dir.mkdirs()
                    }
                    return dir
                }
            })
        }
        BuildType.MACOS.name -> {
            var startBuild:Boolean = false
            val gateyway = AgentEnv.getGateway()
            val url = "http://$gateyway/dispatch-macos/api/gw/macos/startBuild"
            System.out.println("url:$url")
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("X-DEVOPS-BUILD-TYPE", "MACOS")
                .get()
                .build()
            do {
                try {
                    OkhttpUtils.doGet(url).use { resp ->
                        val resoCode = resp.code()
                        val responseStr = resp.body()!!.string()
                        System.out.println("resoCode: $resoCode;responseStr:$responseStr")
                        if(resoCode == 200) {
                            val response: Map<String, String> = jacksonObjectMapper().readValue(responseStr)

                            System.out.println("response:$response")
                            // 将变量写入到property当中
                            response.forEach { (key, value) ->
                                when(key) {
                                    "agentId" -> System.setProperty("devops.agent.id",value)
                                    "secretKey" -> System.setProperty("devops.agent.secret.key",value)
                                    "projectId" -> System.setProperty("devops.project.id",value)
                                    else -> null
                                }
                            }
                            startBuild = true
                        }else {
                            System.out.println("There is no build for this macos,sleep for 5s.")
                        }
                    }
                    if (!startBuild) {
                        Thread.sleep(5000)
                    }
                } catch (e:Exception) {
                    System.out.println("Failed to connect to devops server.")
                }

            } while (!startBuild)
            System.out.println("Start to run.")
            Runner.run(object : WorkspaceInterface {
                override fun getWorkspace(variables: Map<String, String>, pipelineId: String): File {
                    val workspace = System.getProperty("devops_workspace")

                    val dir = if (workspace.isNullOrBlank()) {
                        File("/data/landun/workspace") // v1 内部版用的/data/landun/workspace 保持一致
                    } else {
                        File(workspace)
                    }
                    dir.mkdirs()
                    return dir
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