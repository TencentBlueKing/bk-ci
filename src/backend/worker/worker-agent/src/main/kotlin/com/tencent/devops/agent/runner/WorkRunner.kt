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

package com.tencent.devops.agent.runner

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.agent.utils.KillBuildProcessTree
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.log.Ansi
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildInfo
import com.tencent.devops.worker.common.Runner
import com.tencent.devops.worker.common.WorkspaceInterface
import com.tencent.devops.worker.common.api.utils.ThirdPartyAgentBuildInfoUtils
import com.tencent.devops.worker.common.exception.PropertyNotExistException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Base64
import kotlin.system.exitProcess

object WorkRunner {
    private val logger = LoggerFactory.getLogger(WorkRunner::class.java)

    fun execute(args: Array<String>) {
        try {
            val buildInfo = getBuildInfo(args)!!

            logger.info("[${buildInfo.buildId}]|Start worker for build| projectId=${buildInfo.projectId}")

            addKillProcessTreeHook(buildInfo)

            ThirdPartyAgentBuildInfoUtils.setBuildInfo(buildInfo)

            LoggerService.start()

            Runner.run(object : WorkspaceInterface {
                val workspace = buildInfo.workspace
                override fun getWorkspace(variables: Map<String, String>, pipelineId: String): File {
                    val replaceWorkspace = if (workspace.isNotBlank()) {
                        ReplacementUtils.replace(workspace, object : ReplacementUtils.KeyReplacement {
                            override fun getReplacement(key: String): String? {
                                return variables[key] ?: "\${$key}"
                            }
                        })
                    } else {
                        workspace
                    }
                    val dir = WorkspaceUtils.getPipelineWorkspace(pipelineId, replaceWorkspace)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    return dir
                }
            }, false)
            exitProcess(0)
        } catch (e: PropertyNotExistException) {
            logger.warn("The property(${e.key}) is not exist")
            exitProcess(-1)
        } catch (t: Throwable) {
            logger.error("Encounter unknown exception", t)
            LoggerService.addNormalLine(Ansi().fgRed().a("Other unknown error has occurred: " + t.message).reset().toString())
            exitProcess(-1)
        }
    }

    private fun addKillProcessTreeHook(buildInfo: ThirdPartyBuildInfo) {
        try {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("start kill process tree")
                    val killedProcessIds =
                        KillBuildProcessTree.killProcessTree(buildInfo.projectId, buildInfo.buildId, buildInfo.vmSeqId)
                    logger.info("kill process tree done, ${killedProcessIds.size} process(s) killed, pid(s): $killedProcessIds")
                }
            })
        } catch (t: Throwable) {
            logger.warn("Fail to add shutdown hook", t)
        }
    }

    private fun getBuildInfo(args: Array<String>): ThirdPartyBuildInfo? {
        if (args.isEmpty()) {
            logger.error("Empty argument")
            exitProcess(1)
        }
        val buildInfoStr = String(Base64.getDecoder().decode(args[0]))
        try {
            logger.info("Start read the build info ($buildInfoStr)")
            return JsonUtil.getObjectMapper().readValue(buildInfoStr)
        } catch (t: Throwable) {
            logger.warn("Fail to read the build Info", t)
            exitProcess(1)
        }
    }
}