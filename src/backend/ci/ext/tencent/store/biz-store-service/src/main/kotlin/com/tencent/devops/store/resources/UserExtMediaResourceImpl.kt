package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtMediaResource
import com.tencent.devops.store.pojo.common.MediaInfoReq
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMediaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtMediaResourceImpl @Autowired constructor(
    val storeMediaService: StoreMediaService
) : UserExtMediaResource {
    override fun createServiceMedia(
        userId: String,
        serviceCode: String,
        mediaInfoList: List<MediaInfoReq>
    ): Result<Boolean> {
        mediaInfoList.forEach {
            val storeMediaInfoRequest = StoreMediaInfoRequest(
                storeCode = serviceCode,
                mediaUrl = it.mediaUrl,
                mediaType = it.mediaType.toString(),
                modifier = userId
            )
            storeMediaService.add(
                userId = userId,
                type = StoreTypeEnum.SERVICE,
                storeMediaInfo = storeMediaInfoRequest
            )
        }
        return Result(true)
    }

    override fun updateSericeMedia(userId: String, mediaId: String, serviceCode: String, mediaInfoReq: MediaInfoReq): Result<Boolean> {
        return storeMediaService.update(
            userId = userId,
            id = mediaId,
            storeMediaInfo = StoreMediaInfoRequest(
                storeCode = serviceCode,
                mediaUrl = mediaInfoReq.mediaUrl,
                mediaType = mediaInfoReq.mediaType.toString(),
                modifier = userId
            )
        )
    }

    override fun getServiceMedia(userId: String, mediaId: String): Result<StoreMediaInfo?> {
        return storeMediaService.get(
            userId = userId,
            id = mediaId
        )
    }

    override fun getServiceMediaByServiceCode(userId: String, serviceCode: String): Result<List<StoreMediaInfo>?> {
        return storeMediaService.getByCode(
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }
}