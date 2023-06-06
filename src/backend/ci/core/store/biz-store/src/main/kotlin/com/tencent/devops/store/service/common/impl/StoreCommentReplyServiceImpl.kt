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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreCommentReplyRecord
import com.tencent.devops.store.dao.common.StoreCommentDao
import com.tencent.devops.store.dao.common.StoreCommentReplyDao
import com.tencent.devops.store.pojo.common.STORE_COMMENT_REPLY_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreCommentReplyInfo
import com.tencent.devops.store.pojo.common.StoreCommentReplyRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommentReplyService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreNotifyService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

/**
 * store评论回复业务逻辑类
 *
 * since: 2019-03-26
 */
@Suppress("ALL")
@Service
@RefreshScope
class StoreCommentReplyServiceImpl @Autowired constructor() : StoreCommentReplyService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var storeCommentDao: StoreCommentDao
    @Autowired
    lateinit var storeCommentReplyDao: StoreCommentReplyDao
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var storeNotifyService: StoreNotifyService
    @Autowired
    lateinit var client: Client

    @Value("\${store.profileUrlPrefix}")
    private lateinit var profileUrlPrefix: String

    @Value("\${store.commentNotifyAdmin}")
    private lateinit var commentNotifyAdmin: String

    /**
     * 获取评论信息列表
     */
    override fun getStoreCommentReplysByCommentId(commentId: String): Result<List<StoreCommentReplyInfo>?> {
        val storeCommentReplyInfoList = storeCommentReplyDao.getStoreCommentReplysByCommentId(
            dslContext = dslContext,
            commentId = commentId
        )?.map {
            generateStoreCommentReplyInfo(it)
        }
        return Result(storeCommentReplyInfoList)
    }

    override fun getStoreCommentReply(replyId: String): Result<StoreCommentReplyInfo?> {
        val storeCommentReplyRecord = storeCommentReplyDao.getStoreCommentReplyById(dslContext, replyId)
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
        val storeCommentRecord = storeCommentDao.getStoreComment(dslContext, commentId)
        ?: return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(commentId),
            language = I18nUtil.getLanguage(userId)
        )
        val userDeptNameResult = storeUserService.getUserFullDeptName(userId)
        if (userDeptNameResult.isNotOk()) {
            return Result(userDeptNameResult.status, userDeptNameResult.message ?: "")
        }
        val profileUrl = "$profileUrlPrefix$userId/profile.jpg"
        val replyId = UUIDUtil.generate()
        storeCommentReplyDao.addStoreCommentReply(
            dslContext = dslContext,
            replyId = replyId,
            userId = userId,
            replyerDept = userDeptNameResult.data.toString(),
            commentId = commentId,
            profileUrl = profileUrl,
            storeCommentReplyRequest = storeCommentReplyRequest
        )

        // RTX 通知被回复人和蓝盾管理员
        val receivers = if (storeCommentReplyRequest.replyToUser == "") {
            storeCommentRecord.creator.plus(";").plus(commentNotifyAdmin).split(";").toSet()
        } else {
            storeCommentReplyRequest.replyToUser.plus(";").plus(commentNotifyAdmin).split(";").toSet()
        }
        val storeType = StoreTypeEnum.getStoreTypeObj(storeCommentRecord.storeType.toInt())
        val storeCode = storeCommentRecord.storeCode
        val url = storeCommonService.getStoreDetailUrl(storeType!!, storeCode)
        val storeName = storeCommonService.getStoreNameById(storeCommentRecord.storeId, storeType)
        val bodyParams = mapOf(
            "userId" to userId,
            "storeName" to storeName,
            "replyToUser" to if (storeCommentReplyRequest.replyToUser == "") {
                storeCommentRecord.creator
            } else {
                storeCommentReplyRequest.replyToUser
            },
            "replyContent" to storeCommentReplyRequest.replyContent,
            "replyComment" to storeCommentRecord.commentContent,
            "url" to url
        )
        storeNotifyService.sendNotifyMessage(
            templateCode = STORE_COMMENT_REPLY_NOTIFY_TEMPLATE,
            sender = DEVOPS,
            receivers = receivers.toMutableSet(),
            bodyParams = bodyParams
        )
        return getStoreCommentReply(replyId)
    }
}
