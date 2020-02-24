package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceCommentResource
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreStatisticService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceCommentResourceImpl @Autowired constructor(
    private val storeStatisticService: StoreStatisticService,
    private val storeCommentService: StoreCommentService
) : UserExtServiceCommentResource {
    override fun createServiceComment(
        userId: String,
        serviceId: String,
        serviceCodes: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?> {
        return storeCommentService.addStoreComment(
            userId = userId,
            storeId = serviceId,
            storeCode = serviceCodes,
            storeCommentRequest = storeCommentRequest,
            storeType = StoreTypeEnum.SERVICE
        )
    }

    override fun updateServiceComment(
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

    override fun getServiceComment(userId: String, commentId: String): Result<StoreCommentInfo?> {
        return storeCommentService.getStoreComment(
            userId = userId,
            commentId = commentId
        )
    }

    override fun getServiceCommentByServiceCode(
        userId: String,
        serviceCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?> {
        return storeCommentService.getStoreComments(
            userId = userId,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getServiceCommentScoreInfo(serviceCode: String): Result<StoreCommentScoreInfo> {
        return storeStatisticService.getStoreCommentScoreInfo(
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }

    override fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int> {
        return storeCommentService.updateStoreCommentPraiseCount(userId, commentId)
    }
}