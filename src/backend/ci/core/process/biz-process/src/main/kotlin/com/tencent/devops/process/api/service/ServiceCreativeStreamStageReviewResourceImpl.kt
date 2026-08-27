package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.creative.CreativeStreamStageReviewContent
import com.tencent.devops.process.service.creative.CreativeStreamImateStageReviewService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCreativeStreamStageReviewResourceImpl @Autowired constructor(
    private val creativeStreamImateStageReviewService: CreativeStreamImateStageReviewService
) : ServiceCreativeStreamStageReviewResource {

    override fun getContent(userId: String?, taskId: String): Result<CreativeStreamStageReviewContent> {
        return Result(creativeStreamImateStageReviewService.getContent(taskId))
    }
}
