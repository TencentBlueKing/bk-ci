
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

package com.tencent.devops.quality.resources.v3

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v3.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.api.v3.pojo.response.RuleCreateResponseV3
import com.tencent.devops.common.quality.pojo.RuleCheckResult
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.service.v2.QualityHistoryService
import com.tencent.devops.quality.service.v2.QualityRuleBuildHisService
import com.tencent.devops.quality.service.v2.QualityRuleCheckService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityRuleV3ResourceImpl @Autowired constructor(
    private val qualityRuleBuildHisService: QualityRuleBuildHisService,
    private val historyService: QualityHistoryService,
    private val qualityRuleCheckService: QualityRuleCheckService
) : ServiceQualityRuleResource {
    override fun check(buildCheckParams: BuildCheckParamsV3): Result<RuleCheckResult> {
        return Result(qualityRuleCheckService.checkBuildHis(buildCheckParams))
    }

    override fun create(
        userId: String,
        projectId: String,
        pipelineId: String,
        ruleList: List<RuleCreateRequestV3>
    ): Result<List<RuleCreateResponseV3>> {
        return Result(qualityRuleBuildHisService.serviceCreate(userId, projectId, pipelineId, ruleList))
    }

    override fun listQualityRuleBuildHis(
        userId: String,
        projectId: String,
        pipelineId: String?,
        ruleHashId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val result = historyService.listQualityRuleBuildHisIntercept(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            ruleHashId = ruleHashId,
            startTime = startTime,
            endTime = endTime,
            limit = limit.limit,
            offset = limit.offset
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }
}
