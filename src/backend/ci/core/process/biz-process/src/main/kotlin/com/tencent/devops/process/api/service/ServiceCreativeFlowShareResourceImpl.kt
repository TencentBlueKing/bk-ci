package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantCondition
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantRevokeRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertResult
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantVo
import com.tencent.devops.process.service.creative.CreativeFlowShareGrantService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCreativeFlowShareResourceImpl @Autowired constructor(
    private val creativeFlowShareGrantService: CreativeFlowShareGrantService
) : ServiceCreativeFlowShareResource {

    override fun upsertGrants(
        userId: String,
        request: CreativeFlowShareGrantUpsertRequest
    ): Result<CreativeFlowShareGrantUpsertResult> {
        return Result(creativeFlowShareGrantService.upsertGrants(userId, request))
    }

    override fun listGrants(
        userId: String,
        shareId: String?,
        flowId: String?,
        talentCode: String?,
        sourceProjectId: String?,
        sourcePipelineId: String?,
        includeRevoked: Boolean?
    ): Result<List<CreativeFlowShareGrantVo>> {
        return Result(
            creativeFlowShareGrantService.listGrants(
                userId = userId,
                condition = CreativeFlowShareGrantCondition(
                    shareId = shareId,
                    flowId = flowId,
                    talentCode = talentCode,
                    sourceProjectId = sourceProjectId,
                    sourcePipelineId = sourcePipelineId,
                    includeRevoked = includeRevoked ?: false
                )
            )
        )
    }

    override fun revokeGrants(
        userId: String,
        request: CreativeFlowShareGrantRevokeRequest
    ): Result<Int> {
        return Result(creativeFlowShareGrantService.revokeGrants(userId, request))
    }
}
