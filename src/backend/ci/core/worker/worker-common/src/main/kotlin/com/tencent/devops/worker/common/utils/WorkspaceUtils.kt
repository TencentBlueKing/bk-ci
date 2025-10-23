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

    /**
     * 在临时目录下生成一个目录名DEVOPS_BUILD_LOGS_[pipelineId]_{{System.nanoTime}}_{{attempt}}的文件夹并返回。
     * 当发生错误时，每隔10 * attempt毫秒进行重试，以3次重试为例，则间隔10、20、30毫秒
     *
     * attempt 代表重试了多少次。最大3次重试
     * System.nanoTime 代表当前时间戳纳秒，出错时每次重试都会重新获取最新时间以尽可能避免因时间相同导致的冲突失败。
     *
     * @return File
     * @throws IOException 当无权限创建失败等情况
     *
     */
    @Suppress("MagicNumber", "NestedBlockDepth")
    fun getPipelineLogDir(pipelineId: String, maxRetries: Int = 3): File {
        val tmpDir = System.getProperty("java.io.tmpdir")
        var errorMsg = ""
        repeat(times = maxRetries) { attempt ->
            try {
                val dir = File(tmpDir, "DEVOPS_BUILD_LOGS_${pipelineId}_${System.nanoTime()}_$attempt")
                // 如果不是文件夹，则先做删除
                if (dir.exists() && !dir.isDirectory && !dir.delete()) {
                    errorMsg = "temporary file delete failed [${dir.absolutePath}]"
                    if (attempt < maxRetries - 1) {
                        Thread.sleep(/* millis = */ 10L * (attempt + 1))
                    }
                    return@repeat
                }
                // 如果文件夹创建成功或者已经存在，则直接返回
                if (dir.mkdir() || dir.exists()) {
                    return dir
                }

                errorMsg = "temporary directory create failed [${dir.absolutePath}]"
                if (attempt < maxRetries - 1) {
                    Thread.sleep(/* millis = */ 10L * (attempt + 1))
                }
            } catch (ise: Exception) {
                when (ise) {
                    is IOException,
                    is SecurityException -> {
                        errorMsg = ise.message ?: "temporary directory create failed"
                        if (attempt < maxRetries - 1) {
                            Thread.sleep(/* millis = */ 10L * (attempt + 1))
                        }
                    }

                    else -> throw ise
                }
            }
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
