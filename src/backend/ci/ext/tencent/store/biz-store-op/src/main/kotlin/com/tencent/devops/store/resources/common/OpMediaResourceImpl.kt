package com.tencent.devops.store.resources.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.OpMediaResource
import com.tencent.devops.store.pojo.common.MediaInfoReq
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMediaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpMediaResourceImpl @Autowired constructor(
    val storeMediaService: StoreMediaService
) : OpMediaResource {
    override fun createStoreMedia(
        userId: String,
        storeCode: String,
        labelType: StoreTypeEnum,
        mediaInfoList: List<MediaInfoReq>
    ): Result<Boolean> {
        mediaInfoList.forEach {
            val storeMediaInfoRequest = StoreMediaInfoRequest(
                storeCode = storeCode,
                mediaUrl = it.mediaUrl,
                mediaType = it.mediaType.toString(),
                modifier = userId
            )
            storeMediaService.add(
                userId = userId,
                type = labelType,
                storeMediaInfo = storeMediaInfoRequest
            )
        }
        return Result(true)
    }

    override fun updateStoreMedia(
        userId: String,
        mediaId: String,
        storeCode: String,
        mediaInfoReq: MediaInfoReq
    ): Result<Boolean> {
        return storeMediaService.update(
            userId = userId,
            id = mediaId,
            storeMediaInfo = StoreMediaInfoRequest(
                storeCode = storeCode,
                mediaUrl = mediaInfoReq.mediaUrl,
                mediaType = mediaInfoReq.mediaType.toString(),
                modifier = userId
            )
        )
    }

    override fun getStoreMedia(userId: String, mediaId: String): Result<StoreMediaInfo?> {
        return storeMediaService.get(
            userId = userId,
            id = mediaId
        )
    }

    override fun getStoreMediaByStoreCode(
        userId: String,
        storeCode: String,
        labelType: StoreTypeEnum
    ): Result<List<StoreMediaInfo>?> {
        return storeMediaService.getByCode(
            storeCode = storeCode,
            storeType = labelType
        )
    }
}