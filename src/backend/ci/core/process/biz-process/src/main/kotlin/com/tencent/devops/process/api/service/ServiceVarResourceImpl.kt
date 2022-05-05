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
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceVarResourceImpl @Autowired constructor(
    private val buildVariableService: BuildVariableService,
    private val pipelineContextService: PipelineContextService
) : ServiceVarResource {

    override fun getBuildVar(projectId: String, buildId: String, varName: String?): Result<Map<String, String>> {
        return if (varName.isNullOrBlank()) {
            Result(buildVariableService.getAllVariable(projectId, buildId))
        } else {
            Result(mapOf(varName to (buildVariableService.getVariable(projectId, buildId, varName) ?: "")))
        }
    }

    override fun getContextVar(
        projectId: String,
        pipelineId: String,
        buildId: String,
        contextName: String?
    ): Result<Map<String, String>> {
        val buildVars = buildVariableService.getAllVariable(projectId, buildId)
        return if (contextName.isNullOrBlank()) {
            val contextVar = pipelineContextService.getAllBuildContext(buildVars).toMutableMap()
            Result(
                contextVar.plus(pipelineContextService.buildFinishContext(projectId, pipelineId, buildId))
            )
        } else {
            val context = pipelineContextService.getBuildContext(buildVars, contextName)
            if (context.isNullOrEmpty()) {
                Result(emptyMap())
            } else {
                Result(mapOf(contextName to context))
            }
        }
    }
}
