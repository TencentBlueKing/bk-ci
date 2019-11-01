package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("IMAGE_HANDLE_BUILD_RESULT")
class ImageHandleBuildResultService @Autowired constructor(
    private val marketImageService: MarketImageService
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(ImageHandleBuildResultService::class.java)

    override fun handleStoreBuildResult(storeBuildResultRequest: StoreBuildResultRequest): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val buildParams = storeBuildResultRequest.buildParams
        var imageStatus = ImageStatusEnum.TESTING // 验证成功将镜像状态置位测试状态
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            imageStatus = ImageStatusEnum.CHECK_FAIL // 验证失败
        }
        marketImageService.setImageBuildStatusByImageCode(
            imageCode = buildParams["imageCode"] as String,
            version = buildParams["version"] as String,
            userId = storeBuildResultRequest.userId,
            imageStatus = imageStatus,
            msg = null
        )
        return Result(true)
    }
}
