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

package com.tencent.devops.plugin.worker.task.store

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.element.store.AtomRunEnvPrepareElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.BK_CI_ATOM_EXECUTE_ENV_PATH
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.task.market.AtomRunConditionFactory
import org.slf4j.LoggerFactory
import java.io.File

@TaskClassType(classTypes = [AtomRunEnvPrepareElement.classType])
class AtomRunEnvPrepareTask : ITask() {

    private val logger = LoggerFactory.getLogger(AtomRunEnvPrepareTask::class.java)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("AtomRunEnvPrepareTask buildTask: $buildTask,buildVariables: $buildVariables")
        val params = buildTask.params ?: mapOf()
        val language = params["language"] ?: throw TaskExecuteException(
            errorMsg = "param [language] is empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val runtimeVersion = params["runtimeVersion"]
        if (!runtimeVersion.isNullOrBlank()) {
            val atomRunConditionHandleService = AtomRunConditionFactory.createAtomRunConditionHandleService(language)
            // 准备插件运行环境
            atomRunConditionHandleService.prepareRunEnv(
                osType = AgentEnv.getOS(),
                language = language,
                runtimeVersion = runtimeVersion,
                workspace = workspace
            )
            val atomExecutePath = System.getProperty(BK_CI_ATOM_EXECUTE_ENV_PATH)
            atomExecutePath?.let {
                // 把插件执行环境路径加入环境变量
                addEnv(BK_CI_ATOM_EXECUTE_ENV_PATH, atomExecutePath)
            }
        }
    }
}
