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

import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.quality.QualityGatewaySDKApi
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.script.bat.WindowsScriptTask
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.ExecutorUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLDecoder

/**
 * 构建脚本任务
 */
open class ScriptTask : ITask() {

    private val gatewayResourceApi = ApiFactory.create(QualityGatewaySDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val scriptType = taskParams["scriptType"] ?: throw TaskExecuteException(
            errorMsg = "Unknown script type of build script task",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_INPUT_INVAILD
        )
        val continueNoneZero = taskParams["continueNoneZero"] ?: "false"
        // 如果脚本执行失败之后可以选择归档这个问题
        val archiveFileIfExecFail = taskParams["archiveFile"]
        val script = URLDecoder.decode(taskParams["script"]
                ?: throw TaskExecuteException(
                    errorMsg = "Empty build script content",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                ), "UTF-8").replace("\r", "")
        logger.info("Start to execute the script task($scriptType) ($script)")
        val command = CommandFactory.create(scriptType)
        val buildId = buildVariables.buildId
        val runtimeVariables = buildVariables.variables
        val projectId = buildVariables.projectId

        ScriptEnvUtils.cleanEnv(buildId, workspace)

        val variables = if (buildTask.buildVariable == null) {
            runtimeVariables
        } else {
            runtimeVariables.plus(buildTask.buildVariable!!)
        }
        try {
            command.execute(
                buildId = buildId,
                script = script,
                taskParam = taskParams,
                runtimeVariables = variables,
                projectId = projectId,
                dir = workspace,
                buildEnvs = takeBuildEnvs(buildTask, buildVariables),
                continueNoneZero = continueNoneZero.toBoolean(),
                errorMessage = "Fail to run the plugin"
            )
        } catch (t: Throwable) {
            logger.warn("Fail to run the script task", t)
            if (!archiveFileIfExecFail.isNullOrBlank()) {
                LoggerService.addRedLine("脚本执行失败， 归档${archiveFileIfExecFail}文件")
                val count = ArchiveUtils.archivePipelineFiles(archiveFileIfExecFail!!, workspace, buildVariables)
                if (count == 0) {
                    LoggerService.addRedLine("脚本执行失败之后没有匹配到任何待归档文件")
                }
            }
            throw TaskExecuteException(
                errorMsg = "脚本执行失败",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        } finally {
            // 成功失败都写入环境变量
            addEnv(ScriptEnvUtils.getEnv(buildId, workspace))
            ExecutorUtil.removeThreadLocal()
        }

        // 设置质量红线指标信息
        setGatewayValue(workspace)
    }

    open fun takeBuildEnvs(buildTask: BuildTask, buildVariables: BuildVariables): List<BuildEnv> = buildVariables.buildEnvs

    private fun setGatewayValue(workspace: File) {
        try {
            val gatewayFile = File(workspace, ScriptEnvUtils.getQualityGatewayEnvFile())
            if (!gatewayFile.exists()) return
            val data = gatewayFile.readLines().map {
                val key = it.split("=").getOrNull(0) ?: throw TaskExecuteException(
                    errorMsg = "Illegal gateway key set: $it",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                )
                val value = it.split("=").getOrNull(1) ?: throw TaskExecuteException(
                    errorMsg = "Illegal gateway key set: $it",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                )
                key to value.trim()
            }.toMap()
            val elementType = if (this is WindowsScriptTask) {
                WindowsScriptElement.classType
            } else {
                LinuxScriptElement.classType
            }
            LoggerService.addNormalLine("save gateway value($elementType): $data")
            gatewayResourceApi.saveScriptHisMetadata(elementType, data)
            gatewayFile.delete()
        } catch (e: Exception) {
            LoggerService.addRedLine("save gateway value fail: ${e.message}")
            logger.error(e.message, e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScriptTask::class.java)
    }
}