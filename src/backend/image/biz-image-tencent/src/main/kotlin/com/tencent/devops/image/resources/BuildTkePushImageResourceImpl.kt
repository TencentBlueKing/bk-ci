package com.tencent.devops.image.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.api.BuildTkePushImageResource
import com.tencent.devops.image.pojo.PushImageTask
import com.tencent.devops.image.pojo.tke.TkePushImageParam
import com.tencent.devops.image.service.TkeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildTkePushImageResourceImpl @Autowired constructor(
    private val tkeService: TkeService
) : BuildTkePushImageResource {
    override fun pushImage(pushParam: TkePushImageParam): Result<PushImageTask?> {
        return Result(tkeService.pushTkeImage(pushParam))
    }

    override fun queryUploadTask(taskId: String): Result<PushImageTask?> {
        return Result(tkeService.getPushTkeImageTask(taskId))
    }
}