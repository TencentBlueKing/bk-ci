package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwCreativeStreamStageReviewResourceV4
import com.tencent.devops.process.api.service.ServiceCreativeStreamStageReviewResource
import com.tencent.devops.process.pojo.creative.CreativeStreamStageReviewContent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCreativeStreamStageReviewResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwCreativeStreamStageReviewResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwCreativeStreamStageReviewResourceV4Impl::class.java)
    }

    override fun getContent(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        taskId: String
    ): Result<CreativeStreamStageReviewContent> {
        if (apigwType != "apigw-app" && apigwType != "apigw") {
            throw PermissionForbiddenException("stage review content is only allowed via apigw-app")
        }
        logger.info("OPENAPI_CREATIVE_STREAM_V4|${userId.orEmpty()}|stageReviewContent|taskId=$taskId")
        return client.get(ServiceCreativeStreamStageReviewResource::class).getContent(
            userId = userId,
            taskId = taskId
        )
    }
}
