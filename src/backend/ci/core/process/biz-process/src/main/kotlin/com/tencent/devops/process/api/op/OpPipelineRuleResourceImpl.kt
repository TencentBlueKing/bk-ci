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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.pipeline.PipelineRule
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineRuleResourceImpl @Autowired constructor(
    private val pipelineRuleService: PipelineRuleService
) : OpPipelineRuleResource {

    override fun getPipelineRule(userId: String, ruleId: String): Result<PipelineRule?> {
        return Result(pipelineRuleService.getPipelineRule(userId, ruleId))
    }

    override fun getPipelineRules(
        userId: String,
        ruleName: String?,
        busCode: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<PipelineRule>?> {
        return Result(
            pipelineRuleService.getPipelineRules(
                userId = userId,
                ruleName = ruleName,
                busCode = busCode,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun savePipelineRule(userId: String, pipelineRule: PipelineRule): Result<Boolean> {
        return Result(pipelineRuleService.savePipelineRule(userId, pipelineRule))
    }

    override fun updatePipelineRule(userId: String, ruleId: String, pipelineRule: PipelineRule): Result<Boolean> {
        return Result(pipelineRuleService.updatePipelineRule(userId, ruleId, pipelineRule))
    }

    override fun deletePipelineRuleById(userId: String, ruleId: String): Result<Boolean> {
        return Result(pipelineRuleService.deletePipelineRule(userId, ruleId))
    }
}
