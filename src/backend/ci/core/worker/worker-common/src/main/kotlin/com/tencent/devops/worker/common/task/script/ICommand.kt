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

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.WORKSPACE_CONTEXT
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.expression.SpecialFunctions
import com.tencent.devops.worker.common.service.CIKeywordsService
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.BuildVarOverflowExprSupport
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
        taskId: String? = null
    )

    fun parseTemplate(
        buildId: String,
        command: String,
        variables: Map<String, String>,
        dir: File,
        taskId: String?
    ): String {
        // 解析跨项目模板信息
        val acrossTargetProjectId by lazy {
            TemplateAcrossInfoUtil.getAcrossInfo(variables, taskId)?.targetProjectId
        }
        // 按预置映射关系补齐 ci.* 上下文（如 ci.build_id 取自 BK_CI_BUILD_ID），返回值已包含原始构建变量
        val contextMap = PipelineVarUtil.fillContextVarMap(variables).toMutableMap()
        // 任务运行期上下文优先级最高，必须在预置填充之后覆盖：
        // 否则同名构建变量（如 WORKSPACE）会把 ci.workspace 改写成非本任务工作空间的值
        contextMap.putAll(
            mapOf(
                WORKSPACE_CONTEXT to dir.absolutePath,
                CI_TOKEN_CONTEXT to (variables[CI_TOKEN_CONTEXT] ?: ""),
                JOB_OS_CONTEXT to AgentEnv.getOS().name
            )
        )
        val dialect = PipelineDialectUtil.getPipelineDialect(variables[PIPELINE_DIALECT])
        return if (dialect.supportUseExpression()) {
            val (overflowKeys, overflowLoader) = BuildVarOverflowExprSupport.resolveOverflowOptions(contextMap)
            EnvReplacementParser.parse(
                value = command,
                contextMap = contextMap,
                dialect = dialect,
                contextPair = EnvReplacementParser.getCustomExecutionContextByMap(
                    variables = contextMap,
                    extendNamedValueMap = listOf(
                        CredentialUtils.CredentialRuntimeNamedValue(targetProjectId = acrossTargetProjectId),
                        CIKeywordsService.CIKeywordsRuntimeNamedValue()
                    ),
                    overflowKeys = overflowKeys,
                    overflowLoader = overflowLoader
                ),
                functions = SpecialFunctions.functions,
                output = SpecialFunctions.output,
                overflowKeys = overflowKeys,
                overflowLoader = overflowLoader
            )
        } else {
            // 经典方言：脚本正文的 ${{ 大变量 }} 由这里解析（process 侧 claim 对 URL 编码脚本是 no-op）。
            // 把被引用到的大变量重写成合成 key 并注入真实值，未被引用的大变量保持引用串不变。
            val (overflowKeys, overflowLoader) = BuildVarOverflowExprSupport.resolveOverflowOptions(contextMap)
            val (rewrittenCommand, synthVars) =
                BuildVarOverflowExprSupport.rewriteOverflowText(command, overflowKeys, overflowLoader)
            val effectiveContext = if (synthVars.isEmpty()) contextMap else contextMap.plus(synthVars)
            ReplacementUtils.replace(
                rewrittenCommand,
                object : KeyReplacement {
                    override fun getReplacement(key: String): String? = effectiveContext[key] ?: try {
                        if (key == CI_TOKEN_CONTEXT) {
                            CIKeywordsService.getOrRequestToken()
                        } else {
                            CredentialUtils.getCredential(
                                credentialId = key,
                                showErrorLog = false,
                                acrossProjectId = acrossTargetProjectId
                            )[0]
                        }
                    } catch (ignore: Exception) {
                        if (key == CI_TOKEN_CONTEXT) {
                            null
                        } else {
                            CredentialUtils.getCredentialContextValue(key, acrossTargetProjectId)
                        }
                    }
                }
            )
        }
    }
}
