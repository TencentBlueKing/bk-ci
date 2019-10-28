package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.UserIdeAtomCommentResource
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreStatisticService
import com.tencent.devops.store.service.ideatom.MarketIdeAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserIdeAtomCommentResourceImpl @Autowired constructor(
    private val marketIdeAtomService: MarketIdeAtomService,
    private val storeCommentService: StoreCommentService,
    private val storeStatisticService: StoreStatisticService
) : UserIdeAtomCommentResource {

    override fun getAtomCommentScoreInfo(atomCode: String): Result<StoreCommentScoreInfo> {
        return storeStatisticService.getStoreCommentScoreInfo(atomCode, StoreTypeEnum.IDE_ATOM)
    }

    override fun getStoreComment(userId: String, commentId: String): Result<StoreCommentInfo?> {
        return storeCommentService.getStoreComment(userId, commentId)
    }

    override fun getStoreComments(userId: String, atomCode: String, page: Int, pageSize: Int): Result<Page<StoreCommentInfo>?> {
        return storeCommentService.getStoreComments(userId, atomCode, StoreTypeEnum.IDE_ATOM, page, pageSize)
    }

    override fun addAtomComment(userId: String, atomId: String, atomCode: String, storeCommentRequest: StoreCommentRequest): Result<StoreCommentInfo?> {
        // 判断atomId和atomCode是否真实有效
        val result = marketIdeAtomService.judgeAtomExistByIdAndCode(atomId, atomCode)
        if (result.isNotOk()) {
            return Result(status = result.status, message = result.message ?: "")
        }
        return storeCommentService.addStoreComment(userId, atomId, atomCode, storeCommentRequest, StoreTypeEnum.IDE_ATOM)
    }

    override fun updateStoreComment(userId: String, commentId: String, storeCommentRequest: StoreCommentRequest): Result<Boolean> {
        return storeCommentService.updateStoreComment(userId, commentId, storeCommentRequest)
    }

    override fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int> {
        return storeCommentService.updateStoreCommentPraiseCount(userId, commentId)
    }
}