package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.BuildImageResource
import com.tencent.devops.store.pojo.image.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.service.image.MarketImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildImageResourceImpl @Autowired constructor(private val marketImageService: MarketImageService) :
    BuildImageResource {

    override fun updateImageBaseInfo(
        userId: String,
        projectCode: String,
        imageCode: String,
        version: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean> {
        return marketImageService.updateImageBaseInfo(userId, projectCode, imageCode, version, imageBaseInfoUpdateRequest)
    }
}