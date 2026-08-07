package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwCreativeFlowShareResourceV4
import com.tencent.devops.process.api.service.ServiceCreativeFlowShareResource
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantRevokeRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertResult
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantVo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCreativeFlowShareResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwCreativeFlowShareResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwCreativeFlowShareResourceV4Impl::class.java)
    }

    override fun upsertGrants(
        appCode: String?,
        apigwType: String?,
        userId: String,
        request: CreativeFlowShareGrantUpsertRequest
    ): Result<CreativeFlowShareGrantUpsertResult> {
        if (apigwType != "apigw-app" && apigwType != "apigw") {
            throw PermissionForbiddenException("share grant write is only allowed via apigw-app")
        }
        logger.info("OPENAPI_CREATIVE_FLOW_V4|$userId|upsertGrants|shareId=${request.shareId}")
        return client.get(ServiceCreativeFlowShareResource::class).upsertGrants(
            userId = userId,
            request = request
        )
    }

    override fun listGrants(
        appCode: String?,
        apigwType: String?,
        userId: String,
        shareId: String?,
        flowId: String?,
        talentCode: String?,
        sourceProjectId: String?,
        sourcePipelineId: String?
    ): Result<List<CreativeFlowShareGrantVo>> {
        logger.info("OPENAPI_CREATIVE_FLOW_V4|$userId|listGrants|shareId=$shareId")
        return client.get(ServiceCreativeFlowShareResource::class).listGrants(
            userId = userId,
            shareId = shareId,
            flowId = flowId,
            talentCode = talentCode,
            sourceProjectId = sourceProjectId,
            sourcePipelineId = sourcePipelineId,
            includeRevoked = false
        )
    }

    override fun revokeGrants(
        appCode: String?,
        apigwType: String?,
        userId: String,
        request: CreativeFlowShareGrantRevokeRequest
    ): Result<Int> {
        if (apigwType != "apigw-app" && apigwType != "apigw") {
            throw PermissionForbiddenException("share grant revoke is only allowed via apigw-app")
        }
        logger.info("OPENAPI_CREATIVE_FLOW_V4|$userId|revokeGrants|shareId=${request.shareId}|talentCode=${request.talentCode}")
        return client.get(ServiceCreativeFlowShareResource::class).revokeGrants(
            userId = userId,
            request = request
        )
    }
}
