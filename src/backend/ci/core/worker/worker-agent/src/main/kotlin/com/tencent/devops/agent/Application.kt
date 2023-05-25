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
import com.tencent.devops.worker.WorkRunner
import com.tencent.devops.common.api.enums.EnumLoader
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.ElementSubTypeRegisterLoader
import com.tencent.devops.worker.common.BUILD_TYPE
import com.tencent.devops.worker.common.Runner
import com.tencent.devops.worker.common.WorkspaceInterface
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.env.DockerEnv
import com.tencent.devops.worker.common.task.TaskFactory
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.time.LocalDateTime

fun main(args: Array<String>) {
    // 调用 DHUtil 初始化 SecurityProvider
    DHUtil
    EnumLoader.enumModified()
    ElementSubTypeRegisterLoader.registerElementForJsonUtil()
    ApiFactory.init()
    TaskFactory.init()
    val buildType = System.getProperty(BUILD_TYPE)
    when (buildType) {
        BuildType.DOCKER.name -> {
            val jobPoolType = DockerEnv.getJobPool()
            // 无编译构建，轮询等待任务
            if (jobPoolType != null &&
                jobPoolType == "BUILD_LESS"
            ) {
                waitBuildLessJobStart()
            }

            Runner.run(object : WorkspaceInterface {
                override fun getWorkspaceAndLogDir(
                    variables: Map<String, String>,
                    pipelineId: String
                ): Pair<File, File> {
                    val workspaceDir = WorkspaceUtils.getWorkspaceDir(
                        buildType = BuildType.DOCKER,
                        workspace = "/data/devops/workspace"
                    )
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
                    val workspaceDir = WorkspaceUtils.getPipelineWorkspace(pipelineId, "")
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
    var startFlag = false
    val dockerHostIp = DockerEnv.getDockerHostIp()
    val dockerHostPort = Integer.valueOf(DockerEnv.getDockerHostPort())
    val hostname = DockerEnv.getHostname()
    val loopUrl = "http://$dockerHostIp:$dockerHostPort/build/task/claim?containerId=$hostname"

    val request = Request.Builder()
        .url(loopUrl)
        .header("Accept", "application/json")
        .get()
        .build()
    do {
        println("${LocalDateTime.now()} BuildLess loopUrl: $loopUrl")

        try {
            OkhttpUtils.doHttp(request).use { resp ->
                startFlag = doResponse(resp)
            }
        } catch (e: Exception) {
            println("${LocalDateTime.now()} Get buildLessTask error. continue loop... \n$e")
        }

        if (!startFlag) {
            Thread.sleep(1000)
        }
    } while (!startFlag)
}

private fun doResponse(
    resp: Response
): Boolean {
    val responseBody = resp.body?.string() ?: ""
    println("${LocalDateTime.now()} Get buildLessTask response: $responseBody")
    return if (resp.isSuccessful && responseBody.isNotBlank()) {
        val buildLessTask: Map<String, String> = jacksonObjectMapper().readValue(responseBody)
        buildLessTask.forEach { (t, u) ->
            when (t) {
                "agentId" -> DockerEnv.setAgentId(u)
                "secretKey" -> DockerEnv.setAgentSecretKey(u)
                "projectId" -> DockerEnv.setProjectId(u)
                "buildId" -> DockerEnv.setBuildId(u)
            }
        }
        true
    } else {
        println("${LocalDateTime.now()} No buildLessTask, resp: ${resp.body} continue loop...")
        false
    }
}
