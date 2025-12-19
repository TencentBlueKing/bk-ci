package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserAgentResource
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuild
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentService
) : UserAgentResource {
    override fun listAgentPipelineJobs(
        projectId: String,
        agentId: String?,
        envId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<TPAPipelineBuild>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        return Result(
            thirdPartyAgentService.fetchBuildPipeline(
                projectId = projectId,
                agentId = agentId,
                envId = envRId,
                page = page,
                pageSize = pageSize
            )
        )
    }
}