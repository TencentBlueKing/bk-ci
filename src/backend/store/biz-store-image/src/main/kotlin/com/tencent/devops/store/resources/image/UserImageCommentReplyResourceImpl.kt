package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserImageCommentReplyResource
import com.tencent.devops.store.pojo.common.StoreCommentReplyInfo
import com.tencent.devops.store.pojo.common.StoreCommentReplyRequest
import com.tencent.devops.store.service.common.StoreCommentReplyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageCommentReplyResourceImpl @Autowired constructor(private val storeCommentReplyService: StoreCommentReplyService) :
    UserImageCommentReplyResource {

    override fun getStoreCommentReplysByCommentId(commentId: String): Result<List<StoreCommentReplyInfo>?> {
        return storeCommentReplyService.getStoreCommentReplysByCommentId(commentId)
    }

    override fun addStoreCommentReply(
        userId: String,
        commentId: String,
        storeCommentReplyRequest: StoreCommentReplyRequest
    ): Result<StoreCommentReplyInfo?> {
        return storeCommentReplyService.addStoreCommentReply(
            userId = userId,
            commentId = commentId,
            storeCommentReplyRequest = storeCommentReplyRequest
        )
    }
}