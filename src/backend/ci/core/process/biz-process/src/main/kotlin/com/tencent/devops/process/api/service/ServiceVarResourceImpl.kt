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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceVarResourceImpl @Autowired constructor(
    private val buildVariableService: BuildVariableService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineContextService: PipelineContextService
) : ServiceVarResource {

    override fun getBuildVar(
        projectId: String,
        buildId: String,
        varName: String?,
        pipelineId: String?
    ): Result<Map<String, String>> {
        val pid = pipelineId
            ?: pipelineRuntimeService.getBuildInfo(projectId, buildId)?.pipelineId
            ?: return Result(emptyMap())
        return if (varName.isNullOrBlank()) {
            Result(buildVariableService.getAllVariable(projectId, pid, buildId))
        } else {
            Result(mapOf(varName to (buildVariableService.getVariable(projectId, pid, buildId, varName) ?: "")))
        }
    }

    override fun getContextVar(
        projectId: String,
        pipelineId: String,
        buildId: String,
        contextName: String?
    ): Result<Map<String, String>> {
        val variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        return if (contextName.isNullOrBlank()) {
            Result(
                variables.plus(
                    pipelineContextService.buildFinishContext(projectId, pipelineId, buildId, variables)
                )
            )
        } else {
            val context = pipelineContextService.getBuildContext(variables, contextName)
            if (context.isNullOrEmpty()) {
                Result(emptyMap())
            } else {
                Result(mapOf(contextName to context))
            }
        }
    }
}
