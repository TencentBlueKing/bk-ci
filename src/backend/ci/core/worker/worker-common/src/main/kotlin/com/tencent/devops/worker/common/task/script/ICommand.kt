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

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.WORKSPACE_CONTEXT
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.expression.SpecialFunctions
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.TemplateAcrossInfoUtil
import java.io.File

@Suppress("LongParameterList")
interface ICommand {

    fun execute(
        buildId: String,
        script: String,
        taskParam: Map<String, String>,
        runtimeVariables: Map<String, String>,
        projectId: String,
        dir: File,
        buildEnvs: List<BuildEnv>,
        continueNoneZero: Boolean = false,
        errorMessage: String? = null,
        jobId: String? = null,
        stepId: String? = null,
        charsetType: String? = null,
        taskId: String? = null,
        asCodeEnabled: Boolean? = null
    )

    fun parseTemplate(
        buildId: String,
        command: String,
        variables: Map<String, String>,
        dir: File,
        taskId: String?,
        asCodeEnabled: Boolean?
    ): String {
        // 解析跨项目模板信息
        val acrossTargetProjectId by lazy {
            TemplateAcrossInfoUtil.getAcrossInfo(variables, taskId)?.targetProjectId
        }
        val contextMap = variables.plus(
            mapOf(
                WORKSPACE_CONTEXT to dir.absolutePath,
                CI_TOKEN_CONTEXT to (variables[CI_TOKEN_CONTEXT] ?: ""),
                JOB_OS_CONTEXT to AgentEnv.getOS().name
            )
        )
        return if (asCodeEnabled == true) {
            EnvReplacementParser.parse(
                value = command,
                contextMap = contextMap,
                onlyExpression = true,
                contextPair = EnvReplacementParser.getCustomExecutionContextByMap(
                    variables = contextMap,
                    extendNamedValueMap = listOf(
                        CredentialUtils.CredentialRuntimeNamedValue(targetProjectId = acrossTargetProjectId)
                    )
                ),
                functions = SpecialFunctions.functions,
                output = SpecialFunctions.output
            )
        } else {
            ReplacementUtils.replace(
                command,
                object : KeyReplacement {
                    override fun getReplacement(key: String): String? = contextMap[key] ?: try {
                        CredentialUtils.getCredential(
                            credentialId = key,
                            showErrorLog = false,
                            acrossProjectId = acrossTargetProjectId
                        )[0]
                    } catch (ignore: Exception) {
                        CredentialUtils.getCredentialContextValue(key, acrossTargetProjectId)
                    }
                }
            )
        }
    }
}
