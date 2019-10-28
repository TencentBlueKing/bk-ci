package com.tencent.devops.image.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.api.BuildCcrPushImageResource
import com.tencent.devops.image.pojo.PushImageParam
import com.tencent.devops.image.pojo.PushImageTask
import com.tencent.devops.image.service.PushImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildCcrPushimageResourceImpl @Autowired constructor(
    private val pushImageService: PushImageService
) : BuildCcrPushImageResource {
    override fun pushImage(pushParam: PushImageParam): Result<PushImageTask?> {
        return Result(pushImageService.pushImage(pushParam))
    }

    override fun queryUploadTask(userId: String, taskId: String): Result<PushImageTask?> {
        return Result(pushImageService.getPushImageTask(taskId))
    }
}