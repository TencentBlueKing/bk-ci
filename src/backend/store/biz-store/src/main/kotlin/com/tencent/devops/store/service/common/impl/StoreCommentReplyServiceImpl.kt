/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.store.tables.records.TStoreCommentReplyRecord
import com.tencent.devops.store.dao.common.StoreCommentReplyDao
import com.tencent.devops.store.pojo.common.StoreCommentReplyInfo
import com.tencent.devops.store.pojo.common.StoreCommentReplyRequest
import com.tencent.devops.store.service.common.StoreCommentReplyService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * store评论回复业务逻辑类
 *
 * since: 2019-03-26
 */
@Service
class StoreCommentReplyServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeCommentReplyDao: StoreCommentReplyDao,
    private val storeUserService: StoreUserService
) : StoreCommentReplyService {

    private val logger = LoggerFactory.getLogger(StoreCommentReplyServiceImpl::class.java)

    @Value("\${store.profileUrlPrefix}")
    private lateinit var profileUrlPrefix: String

    /**
     * 获取评论回复信息列表
     */
    override fun getStoreCommentReplysByCommentId(commentId: String): Result<List<StoreCommentReplyInfo>?> {
        logger.info("commentId is :$commentId")
        val storeCommentReplyInfoList =
            storeCommentReplyDao.getStoreCommentReplysByCommentId(dslContext, commentId)?.map {
                generateStoreCommentReplyInfo(it)
            }
        return Result(storeCommentReplyInfoList)
    }

    /**
     * 获取评论回复信息
     */
    override fun getStoreCommentReply(replyId: String): Result<StoreCommentReplyInfo?> {
        logger.info("replyId is :$replyId")
        val storeCommentReplyRecord = storeCommentReplyDao.getStoreCommentReplyById(dslContext, replyId)
        logger.info("storeCommentReplyRecord is :$storeCommentReplyRecord")
        return if (null != storeCommentReplyRecord) {
            Result(generateStoreCommentReplyInfo(storeCommentReplyRecord))
        } else {
            Result(data = null)
        }
    }

    private fun generateStoreCommentReplyInfo(it: TStoreCommentReplyRecord): StoreCommentReplyInfo {
        return StoreCommentReplyInfo(
            replyId = it.id,
            replyer = it.creator,
            replyContent = it.replyContent,
            replyerDept = it.replyerDept,
            profileUrl = it.profileUrl,
            replyToUser = it.replyToUser,
            replyTime = it.createTime.timestampmilli(),
            updateTime = it.updateTime.timestampmilli()
        )
    }

    /**
     * 添加评论回复
     */
    override fun addStoreCommentReply(
        userId: String,
        commentId: String,
        storeCommentReplyRequest: StoreCommentReplyRequest
    ): Result<StoreCommentReplyInfo?> {
        logger.info("userId is :$userId,commentId is :commentId, storeCommentReplyRequest is :$storeCommentReplyRequest")
        val userDeptNameResult = storeUserService.getUserFullDeptName(userId)
        logger.info("the userDeptNameResult is:$userDeptNameResult")
        if (userDeptNameResult.isNotOk()) {
            return Result(userDeptNameResult.status, userDeptNameResult.message ?: "")
        }
        val profileUrl = "$profileUrlPrefix$userId/profile.jpg"
        val replyId = UUIDUtil.generate()
        storeCommentReplyDao.addStoreCommentReply(
            dslContext,
            replyId,
            userId,
            userDeptNameResult.data.toString(),
            commentId,
            profileUrl,
            storeCommentReplyRequest
        )
        return getStoreCommentReply(replyId)
    }
}
