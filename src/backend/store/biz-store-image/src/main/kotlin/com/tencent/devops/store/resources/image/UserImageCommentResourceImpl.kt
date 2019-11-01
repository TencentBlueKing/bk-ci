package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserImageCommentResource
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreStatisticService
import com.tencent.devops.store.service.image.MarketImageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageCommentResourceImpl @Autowired constructor(
    private val marketImageService: MarketImageService,
    private val storeCommentService: StoreCommentService,
    private val storeStatisticService: StoreStatisticService
) :
    UserImageCommentResource {

    override fun getImageCommentScoreInfo(imageCode: String): Result<StoreCommentScoreInfo> {
        return storeStatisticService.getStoreCommentScoreInfo(imageCode, StoreTypeEnum.IMAGE)
    }

    override fun getStoreComment(userId: String, commentId: String): Result<StoreCommentInfo?> {
        return storeCommentService.getStoreComment(userId, commentId)
    }

    override fun getStoreComments(
        userId: String,
        imageCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?> {
        return storeCommentService.getStoreComments(
            userId = userId,
            storeCode = imageCode,
            storeType = StoreTypeEnum.IMAGE,
            page = page,
            pageSize = pageSize
        )
    }

    override fun addImageComment(
        userId: String,
        imageId: String,
        imageCode: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?> {
        // 判断imageId和imageCode是否真实有效
        val result = marketImageService.judgeImageExistByIdAndCode(imageId, imageCode)
        if (result.isNotOk()) {
            return Result(
                status = result.status,
                message = result.message ?: ""
            )
        }
        return storeCommentService.addStoreComment(
            userId = userId,
            storeId = imageId,
            storeCode = imageCode,
            storeCommentRequest = storeCommentRequest,
            storeType = StoreTypeEnum.IMAGE
        )
    }

    override fun updateStoreComment(
        userId: String,
        commentId: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean> {
        return storeCommentService.updateStoreComment(
            userId = userId,
            commentId = commentId,
            storeCommentRequest = storeCommentRequest
        )
    }

    override fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int> {
        return storeCommentService.updateStoreCommentPraiseCount(userId, commentId)
    }
}