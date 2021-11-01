package com.tencent.devops.quality.resources.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v3.UserQualityRuleResource
import com.tencent.devops.quality.service.v2.QualityRuleBuildHisService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityRuleV3ResourceImpl @Autowired constructor(
    private val qualityRuleBuildHisService: QualityRuleBuildHisService
) : UserQualityRuleResource {

    override fun update(userId: String, ruleHashId: String, pass: Boolean): Result<Boolean> {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        return Result(qualityRuleBuildHisService.updateStatusService(userId, ruleId, pass))
    }
}
