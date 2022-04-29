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

import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.WORKSPACE_CONTEXT
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.TemplateAcrossInfoUtil
import java.io.File

interface ICommand {

    @Suppress("LongParameterList")
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
        data: Map<String, String>,
        dir: File,
        taskId: String?
    ): String {
        // 解析跨项目模板信息
        val acrossTargetProjectId by lazy {
            TemplateAcrossInfoUtil.getAcrossInfo(data, taskId)?.targetProjectId
        }

        return ReplacementUtils.replace(command, object : ReplacementUtils.KeyReplacement {
            override fun getReplacement(key: String, doubleCurlyBraces: Boolean): String = data[key] ?: try {
                CredentialUtils.getCredential(buildId, key, false, acrossTargetProjectId)[0]
            } catch (ignore: Exception) {
                CredentialUtils.getCredentialContextValue(key, acrossTargetProjectId) ?: if (doubleCurlyBraces) {
                    "\${{$key}}"
                } else {
                    "\${$key}"
                }
            }
        }, mapOf(
            WORKSPACE_CONTEXT to dir.absolutePath,
            CI_TOKEN_CONTEXT to (data[CI_TOKEN_CONTEXT] ?: ""),
            JOB_OS_CONTEXT to AgentEnv.getOS().name
        ))
    }
}
