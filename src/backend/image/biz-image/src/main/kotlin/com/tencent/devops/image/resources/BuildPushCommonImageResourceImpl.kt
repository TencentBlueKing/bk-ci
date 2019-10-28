package com.tencent.devops.image.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.api.BuildPushCommonImageResource
import com.tencent.devops.image.pojo.PushImageParam
import com.tencent.devops.image.pojo.PushImageTask
import com.tencent.devops.image.service.PushImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildPushCommonImageResourceImpl @Autowired constructor(
    private val pushImageService: PushImageService
) : BuildPushCommonImageResource {
    override fun pushImage(pushParam: PushImageParam): Result<PushImageTask?> {
        return Result(pushImageService.pushImage(pushParam))
    }

    override fun queryImageTask(userId: String, taskId: String): Result<PushImageTask?> {
        return Result(pushImageService.getPushImageTask(taskId))
    }
}