package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserCodeccResource
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import com.tencent.devops.plugin.service.CodeccService
import com.tencent.devops.common.pipeline.utils.CoverityUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService
) : UserCodeccResource {

    override fun getCodeccReport(buildId: String): Result<CodeccCallback?> {
        return Result(codeccService.getCodeccReport(buildId))
    }

    override fun getCodeccRuleSet(projectId: String, userId: String, toolName: String): Result<Map<String, Any>> {
        return CoverityUtils.getRuleSets(projectId, userId, toolName)
    }
}