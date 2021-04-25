package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwQualityResourceV3
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwQualityResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwQualityResourceV3 {

    override fun listRule(appCode: String?, apigwType: String?, projectId: String, userId: String, page: Int?, pageSize: Int?): Result<Page<QualityRuleSummaryWithPermission>> {
        return client.get(ServiceQualityRuleResource::class).list(userId, projectId, page, pageSize)
    }

    override fun createRule(appCode: String?, apigwType: String?, projectId: String, userId: String, rule: RuleCreateRequest): Result<String> {
        return client.get(ServiceQualityRuleResource::class).create(userId, projectId, rule)
    }

    override fun updateRule(appCode: String?, apigwType: String?, projectId: String, userId: String, ruleHashId: String, rule: RuleUpdateRequest): Result<Boolean> {
        return client.get(ServiceQualityRuleResource::class).update(userId, projectId, ruleHashId, rule)
    }

    override fun deleteRule(appCode: String?, apigwType: String?, projectId: String, userId: String, ruleHashId: String): Result<Boolean> {
        return client.get(ServiceQualityRuleResource::class).delete(userId, projectId, ruleHashId)
    }

    override fun listIntercepts(appCode: String?, apigwType: String?, projectId: String, userId: String, pipelineId: String?, ruleHashId: String?, interceptResult: RuleInterceptResult?, startTime: Long?, endTime: Long?, page: Int?, pageSize: Int?): Result<Page<RuleInterceptHistory>> {
        return client.get(ServiceQualityInterceptResource::class).list(userId, projectId, pipelineId, ruleHashId, interceptResult, startTime, endTime, page, pageSize)
    }
}