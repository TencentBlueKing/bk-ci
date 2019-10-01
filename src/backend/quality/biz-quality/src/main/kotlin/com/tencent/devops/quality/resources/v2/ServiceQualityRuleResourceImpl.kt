package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.pojo.RuleCheckResult
import com.tencent.devops.quality.service.v2.QualityRuleCheckService
import com.tencent.devops.quality.service.v2.QualityRuleService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityRuleResourceImpl @Autowired constructor(
    private val ruleCheckService: QualityRuleCheckService,
    private val ruleService: QualityRuleService
) : ServiceQualityRuleResource {

    override fun matchRuleList(projectId: String, pipelineId: String, templateId: String?, startTime: Long): Result<List<QualityRuleMatchTask>> {
        val ruleList = mutableListOf<QualityRuleMatchTask>()
        ruleList.addAll(ruleCheckService.userGetMatchRuleList(projectId, pipelineId))
        ruleList.addAll(ruleCheckService.userGetMatchTemplateList(projectId, templateId))
        return Result(ruleList)
    }

    override fun getAuditUserList(projectId: String, pipelineId: String, buildId: String, taskId: String): Result<Set<String>> {
        return Result(ruleCheckService.getAuditUserList(projectId, pipelineId, buildId, taskId))
    }

    override fun check(buildCheckParams: BuildCheckParams): Result<RuleCheckResult> {
        return Result(ruleCheckService.check(buildCheckParams))
    }

    override fun copyRule(request: CopyRuleRequest): Result<List<String>> {
        return Result(ruleService.copyRule(request))
    }
}
