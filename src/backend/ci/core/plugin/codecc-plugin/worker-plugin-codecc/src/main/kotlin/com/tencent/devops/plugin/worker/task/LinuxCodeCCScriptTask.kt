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

package com.tencent.devops.plugin.worker.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.plugin.worker.task.codecc.util.CodeccEnvHelper
import com.tencent.devops.plugin.worker.task.codecc.util.CodeccRepoHelper
import com.tencent.devops.plugin.worker.task.codecc.util.CodeccUtils
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import java.io.File

/**
 * 构建脚本任务
 */
@TaskClassType(classTypes = [LinuxPaasCodeCCScriptElement.classType, LinuxCodeCCScriptElement.classType])
class LinuxCodeCCScriptTask : ITask() {

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        // 如果指定_CODECC_FILTER_TOOLS，则只做_CODECC_FILTER_TOOLS的扫描
        val repos = CodeccRepoHelper.getCodeccRepos(taskParams["id"] ?: "", buildTask, buildVariables)
        val filterTools = buildVariables.variables["_CODECC_FILTER_TOOLS"] ?: ""
        val coverityConfig = CodeccExecuteConfig(
            scriptType = BuildScriptType.valueOf(taskParams["scriptType"] ?: ""),
            repos = repos,
            buildVariables = buildVariables,
            buildTask = buildTask,
            workspace = workspace,
            tools = JsonUtil.to(taskParams["tools"]!!),
            filterTools = filterTools.split(",").map { it.trim() }.filter { it.isNotBlank() }
        )
        LoggerService.addNormalLine("buildVariables coverityConfig: $coverityConfig")

        // 先写入codecc任务
        CodeccEnvHelper.saveTask(buildVariables)

        // 执行codecc的python脚本
        CodeccUtils.executeCommand(coverityConfig)

        // 写入环境变量
        addEnv(CodeccEnvHelper.getCodeccEnv(workspace))
    }
}