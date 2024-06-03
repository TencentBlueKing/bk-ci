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

package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwQualityResourceV4
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.quality.api.v3.ServiceQualityRuleResource as ServiceQualityRuleResourceV3

@RestResource
class ApigwQualityResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwQualityResourceV4 {

    override fun listRule(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<QualityRuleSummaryWithPermission>> {
        logger.info("OPENAPI_QUALITY_V4|$userId|list rule|$projectId|$page|$pageSize")
        return client.get(ServiceQualityRuleResource::class).list(
            userId = userId,
            projectId = projectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20
        )
    }

    override fun createRule(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        rule: RuleCreateRequest
    ): Result<String> {
        logger.info("OPENAPI_QUALITY_V4|$userId|create rule|$projectId|$rule")
        return client.get(ServiceQualityRuleResource::class).create(userId, projectId, rule)
    }

    override fun updateRule(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        ruleHashId: String,
        rule: RuleUpdateRequest
    ): Result<Boolean> {
        logger.info("OPENAPI_QUALITY_V4|$userId|update rule|$projectId|$ruleHashId|$rule")
        return client.get(ServiceQualityRuleResource::class).update(userId, projectId, ruleHashId, rule)
    }

    override fun deleteRule(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        ruleHashId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_QUALITY_V4|$userId|delete rule|$projectId|$ruleHashId")
        return client.get(ServiceQualityRuleResource::class).delete(userId, projectId, ruleHashId)
    }

    override fun listIntercepts(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        pipelineId: String?,
        ruleHashId: String?,
        interceptResult: RuleInterceptResult?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>> {
        logger.info(
            "OPENAPI_QUALITY_V4|$userId|list intercepts|$projectId|$pipelineId|$ruleHashId|$interceptResult" +
                "|$startTime|$endTime|$page|$pageSize"
        )
        return client.get(ServiceQualityInterceptResource::class).list(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            ruleHashId = ruleHashId,
            interceptResult = interceptResult,
            startTime = startTime,
            endTime = endTime,
            page = page ?: 1,
            pageSize = pageSize ?: 20
        )
    }

    override fun listBuildHisRule(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        pipelineId: String?,
        ruleHashId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>> {
        logger.info(
            "OPENAPI_QUALITY_V4|$userId|list build hisRule|$projectId|$pipelineId|$ruleHashId|$startTime" +
                "|$endTime|$page|$pageSize"
        )
        return client.get(ServiceQualityRuleResourceV3::class).listQualityRuleBuildHis(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            ruleHashId = ruleHashId,
            startTime = startTime,
            endTime = endTime,
            page = page ?: 1,
            pageSize = pageSize ?: 20
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwQualityResourceV4Impl::class.java)
    }
}
