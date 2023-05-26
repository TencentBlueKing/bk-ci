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

package com.tencent.devops.plugin.worker.task.scm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.task.ITask
import java.io.File
import org.slf4j.LoggerFactory

abstract class CodePullTask constructor(private val scmType: ScmType) : ITask() {

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        logger.info("[${buildVariables.buildId}]| Start to pull the code: $taskParams")

        try {
            val env = SCM.getPullCodeSetting(
                scmType = scmType,
                pipelineId = buildVariables.pipelineId,
                buildId = buildVariables.buildId,
                workspace = workspace,
                taskParams = taskParams,
                variables = buildVariables.variablesWithType.map { it.key to it.value.toString() }.toMap()
            ).pullCode()
            if (env != null) {
                addEnv(env)
            }
        } catch (e: Throwable) {
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = MessageUtil.getMessageByLocale(
                    messageCode = WorkerMessageCode.BK_PLUGIN_IS_NO_LONGER_RECOMMENDED,
                    language = AgentEnv.getLocaleLanguage()
                ) + ",${e.message}"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodePullTask::class.java)
    }
}
