package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserImageReleaseResource
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OfflineMarketImageReq
import com.tencent.devops.store.service.image.ImageReleaseService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageReleaseResourceImpl @Autowired constructor(
    private val imageReleaseService: ImageReleaseService
) : UserImageReleaseResource {

    override fun offlineMarketImage(
        userId: String,
        imageCode: String,
        offlineMarketImageReq: OfflineMarketImageReq
    ): Result<Boolean> {
        return imageReleaseService.offlineMarketImage(
            userId = userId,
            imageCode = imageCode,
            version = offlineMarketImageReq.version,
            reason = offlineMarketImageReq.reason,
            interfaceName = "/user/market/desk/image/offline/imageCodes/{imageCode}/versions"
        )
    }

    override fun addMarketImage(
        accessToken: String,
        userId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest
    ): Result<String> {
        return imageReleaseService.addMarketImage(accessToken, userId, imageCode, marketImageRelRequest)
    }

    override fun updateMarketImage(
        userId: String,
        marketImageUpdateRequest: MarketImageUpdateRequest
    ): Result<String?> {
        return imageReleaseService.updateMarketImage(userId, marketImageUpdateRequest)
    }

    override fun getProcessInfo(userId: String, imageId: String): Result<StoreProcessInfo> {
        return imageReleaseService.getProcessInfo(userId, imageId)
    }

    override fun cancelRelease(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.cancelRelease(userId, imageId)
    }

    override fun recheck(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.recheck(userId, imageId)
    }

    override fun passTest(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.passTest(userId, imageId)
    }
}