package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.AppQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.service.v2.QualityRuleCheckService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppQualityRuleResourceImpl @Autowired constructor(
    private val ruleCheckService: QualityRuleCheckService
) : AppQualityRuleResource {
    override fun matchRuleList(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<QualityRuleMatchTask>> {
        checkParam(userId, projectId)
        val result = ruleCheckService.userGetMatchRuleList(projectId, pipelineId)
        return Result(result)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}
