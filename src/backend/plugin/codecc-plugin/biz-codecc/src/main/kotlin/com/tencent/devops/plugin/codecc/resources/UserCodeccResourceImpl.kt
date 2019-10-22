package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.api.UserCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.plugin.codecc.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService,
    private val codeccApi: CodeccApi
) : UserCodeccResource {

    override fun getCodeccReport(buildId: String): Result<CodeccCallback?> {
        return Result(codeccService.getCodeccReport(buildId))
    }

    override fun getCodeccRuleSet(projectId: String, userId: String, toolName: String): Result<Map<String, Any>> {
        return codeccApi.getRuleSets(projectId, userId, toolName)
    }
}