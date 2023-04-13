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

import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.worker.common.COMMON_ENV_CONTEXT
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.WORKSPACE_CONTEXT
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildType
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

@Suppress("TooManyFunctions")
object WorkspaceUtils {

    var commonDirMap: MutableMap<String, File> = mutableMapOf()

    fun getLandun() = File(".")

    fun getWorkspace() = File(getLandun(), "workspace")

    fun getPipelineWorkspace(pipelineId: String, workspace: String): File {
        return if (workspace.isNotBlank()) {
            File(workspace) // .normalize() 会导致在windows机器下填写 ./ 时，File.exists() 会返回false，表示文件夹不存在
        } else {
            File(getWorkspace(), "$pipelineId/src").normalize()
        }
    }

    fun getWorkspaceDir(
        buildType: BuildType,
        workspace: String = "",
        pipelineId: String = "",
        variables: Map<String, String> = emptyMap()
    ): File {
        when (buildType) {
            BuildType.DOCKER -> {
                val dockerWorkspace = System.getProperty("devops_workspace")
                val workspaceDir = if (dockerWorkspace.isNullOrBlank()) {
                    File(workspace)
                } else {
                    File(dockerWorkspace)
                }
                workspaceDir.mkdirs()

                initCommonEnvDir(workspaceDir)

                return workspaceDir
            }
            BuildType.AGENT -> {
                val replaceWorkspace = if (workspace.isNotBlank()) {
                    ReplacementUtils.replace(
                        workspace,
                        object : KeyReplacement {
                            override fun getReplacement(key: String): String? {
                                return variables[key]
                                    ?: throw IllegalArgumentException(
                                        MessageUtil.getMessageByLocale(
                                            WorkerMessageCode.UNDEFINED_VARIABLE,
                                            AgentEnv.getLocaleLanguage()
                                        ) + " $workspace"
                                    )
                            }
                        },
                        mapOf(
                            WORKSPACE_CONTEXT to workspace,
                            JOB_OS_CONTEXT to AgentEnv.getOS().name
                        )
                    )
                } else {
                    workspace
                }
                val workspaceDir = getPipelineWorkspace(pipelineId, replaceWorkspace)
                if (!workspaceDir.exists() && !workspaceDir.mkdirs()) { // #5555 第三方构建机工作空间校验
                    throw FileNotFoundException(
                        MessageUtil.getMessageByLocale(
                            WorkerMessageCode.ILLEGAL_WORKSPACE,
                            AgentEnv.getLocaleLanguage()
                        ) + " [$workspaceDir]"
                    )
                }

                initCommonEnvDir(workspaceDir)

                return workspaceDir
            }
            else -> {
                throw IllegalArgumentException(
                    MessageUtil.getMessageByLocale(
                        WorkerMessageCode.UNBEKNOWN_BUILD_TYPE,
                        AgentEnv.getLocaleLanguage()
                    ) + " $buildType"
                )
            }
        }
    }

    fun getPipelineLogDir(pipelineId: String): File {
        val prefix = "DEVOPS_BUILD_LOGS_${pipelineId}_"
        var tmpDir = System.getProperty("java.io.tmpdir")
        val errorMsg = try {
            val dir = File.createTempFile(prefix, null, null)
            dir.delete()
            if (dir.mkdir()) {
                return dir
            }
            if (!dir.startsWith(tmpDir)) { // #5046 做一次修正
                tmpDir = dir.parent
            }
            "temporary directory create failed"
        } catch (ioe: IOException) {
            ioe.message
        }
        throw IOException("$tmpDir: $errorMsg")
    }

    fun getCommonEnvDir(): File? {
        return commonDirMap[COMMON_ENV_CONTEXT]
    }

    @Suppress("LongParameterList")
    fun getBuildLogProperty(
        pipelineLogDir: File,
        pipelineId: String,
        buildId: String,
        elementId: String,
        executeCount: Int,
        logStorageMode: LogStorageMode
    ): TaskBuildLogProperty {
        val childPath = getBuildLogChildPath(pipelineId, buildId, elementId, executeCount)
        val logFile = File(pipelineLogDir, childPath)
        logFile.parentFile.mkdirs()
        logFile.createNewFile()
        return TaskBuildLogProperty(
            elementId = elementId,
            childPath = childPath,
            childZipPath = "$childPath.zip",
            logFile = logFile,
            logStorageMode = logStorageMode
        )
    }

    private fun getBuildLogChildPath(
        pipelineId: String,
        buildId: String,
        elementId: String,
        executeCount: Int
    ) = "/$pipelineId/$buildId/$elementId/$executeCount.log"

    private fun initCommonEnvDir(workspaceDir: File) {
        val commonEnvDir = File(workspaceDir.parentFile, COMMON_ENV_CONTEXT)
        commonEnvDir.mkdir()
        commonDirMap[COMMON_ENV_CONTEXT] = commonEnvDir
    }
}
