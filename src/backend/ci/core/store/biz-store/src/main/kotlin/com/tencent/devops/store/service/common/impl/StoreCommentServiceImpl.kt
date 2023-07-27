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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreCommentRecord
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.COMMENT_UPDATE_NO_PERMISSION
import com.tencent.devops.store.dao.common.StoreCommentDao
import com.tencent.devops.store.dao.common.StoreCommentPraiseDao
import com.tencent.devops.store.dao.common.StoreCommentReplyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.common.STORE_COMMENT_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreNotifyService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

/**
 * store评论业务逻辑类
 *
 * since: 2019-03-26
 */
@Suppress("ALL")
@Service
@RefreshScope
class StoreCommentServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeCommentDao: StoreCommentDao,
    private val storeCommentReplyDao: StoreCommentReplyDao,
    private val storeCommentPraiseDao: StoreCommentPraiseDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeUserService: StoreUserService,
    private val storeNotifyService: StoreNotifyService,
    private val storeCommonService: StoreCommonService,
    private val storeTotalStatisticService: StoreTotalStatisticService
) : StoreCommentService {

    private val logger = LoggerFactory.getLogger(StoreCommentServiceImpl::class.java)

    @Value("\${store.profileUrlPrefix}")
    private lateinit var profileUrlPrefix: String

    @Value("\${store.commentNotifyAdmin}")
    private lateinit var commentNotifyAdmin: String

    override fun getStoreComment(userId: String, commentId: String): Result<StoreCommentInfo?> {
        logger.info("getStoreComment params:[$userId|$commentId]")
        val storeCommentRecord = storeCommentDao.getStoreComment(dslContext, commentId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(commentId),
                language = I18nUtil.getLanguage(userId)

            )
        return Result(generateStoreCommentInfo(userId, storeCommentRecord))
    }

    /**
     * 获取评论信息列表
     */
    override fun getStoreComments(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?> {
        val storeCommentInfoList =
            storeCommentDao.getStoreComments(dslContext, storeCode, storeType.type.toByte(), page, pageSize)?.map {
                generateStoreCommentInfo(userId, it)
            }
        val commentCount = storeCommentDao.getStoreCommentCount(dslContext, storeCode, storeType.type.toByte())
        val totalPages = PageUtil.calTotalPage(pageSize, commentCount)
        return Result(
            Page(
                count = commentCount,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                records = storeCommentInfoList ?: listOf()
            )
        )
    }

    private fun generateStoreCommentInfo(userId: String, it: TStoreCommentRecord): StoreCommentInfo {
        // 判断用户是否已点赞
        var praiseFlag = false
        val count = storeCommentPraiseDao.countByIds(dslContext, userId, it.id)
        if (count > 0) {
            praiseFlag = true
        }
        // 查询该条评论的点赞人数
        var praiseUsers: List<String>? = null
        val praiseRecords = storeCommentPraiseDao.getStoreCommentPraisesById(dslContext, it.id)
        if (null != praiseRecords) {
            praiseUsers = mutableListOf()
            praiseRecords.forEach {
                praiseUsers.add(it.creator)
            }
        }
        // 查询该条评论回复的数量
        val replyCount = storeCommentReplyDao.countReplyNumByCommentId(dslContext, it.id)
        return StoreCommentInfo(
            commentId = it.id,
            commenter = it.creator,
            commentContent = it.commentContent,
            commenterDept = it.commenterDept,
            profileUrl = it.profileUrl,
            praiseCount = it.praiseCount,
            praiseFlag = praiseFlag,
            praiseUsers = praiseUsers,
            score = it.score,
            replyCount = replyCount,
            commentTime = it.createTime.timestampmilli(),
            updateTime = it.updateTime.timestampmilli()
        )
    }

    /**
     * 添加评论
     */
    override fun addStoreComment(
        userId: String,
        storeId: String,
        storeCode: String,
        storeCommentRequest: StoreCommentRequest,
        storeType: StoreTypeEnum
    ): Result<StoreCommentInfo?> {
        logger.info("addStoreComment params:[$userId|$storeId|$storeCode|$storeCommentRequest|$storeType")
        val score = storeCommentRequest.score
        // 校验评分是否合法
        if (!validateScore(score)) return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("score:$score"),
            language = I18nUtil.getLanguage(userId)
        )
        // 校验用户是否已评论过，如果评论过则提示用户去修改评论
        val latestCommentRecord =
            storeCommentDao.getUserLatestCommentByStoreCode(dslContext, userId, storeCode, storeType.type.toByte())
        if (null != latestCommentRecord) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_COMMENT_IS_INVALID,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val userDeptNameResult = storeUserService.getUserFullDeptName(userId)
        if (userDeptNameResult.isNotOk()) {
            return Result(userDeptNameResult.status, userDeptNameResult.message ?: "")
        }
        val profileUrl = "$profileUrlPrefix$userId/profile.jpg"
        val commentId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 添加评论信息
            storeCommentDao.addStoreComment(
                dslContext = context,
                commentId = commentId,
                userId = userId,
                commenterDept = userDeptNameResult.data.toString(),
                storeId = storeId,
                storeCode = storeCode,
                profileUrl = profileUrl,
                storeCommentRequest = storeCommentRequest,
                storeType = storeType.type.toByte()
            )
            // 更新统计信息
            storeStatisticDao.updateCommentInfo(
                dslContext = context,
                userId = userId,
                storeId = storeId,
                storeCode = storeCode,
                storeType = storeType.type.toByte(),
                commentIncrement = 1,
                scoreIncrement = score
            )
            storeTotalStatisticService.updateStoreTotalStatisticByCode(storeCode, storeType.type.toByte())
        }
        sendNotifyMessage(storeCode, storeType, storeId, userId, storeCommentRequest)
        return getStoreComment(userId, commentId)
    }

    private fun sendNotifyMessage(
        storeCode: String,
        storeType: StoreTypeEnum,
        storeId: String,
        userId: String,
        storeCommentRequest: StoreCommentRequest
    ) {
        // 通知插件管理员和开发人员、平台管理员
        val memberRecordList = storeMemberDao.list(dslContext, storeCode, null, storeType.type.toByte())
        val memberList = mutableSetOf<String>()
        memberRecordList?.forEach {
            memberList.add(it.username)
        }
        val receivers = if (memberList.isEmpty()) {
            commentNotifyAdmin.split(";").toSet()
        } else {
            commentNotifyAdmin.split(";").toSet().plus(memberList)
        }
        if (receivers.contains("system")) {
            receivers.toMutableSet().remove("system")
        }
        val url = storeCommonService.getStoreDetailUrl(storeType, storeCode)
        val storeName = storeCommonService.getStoreNameById(storeId, storeType)
        val bodyParams = mapOf(
            "userId" to userId,
            "storeName" to storeName,
            "score" to storeCommentRequest.score.toString(),
            "commentContent" to storeCommentRequest.commentContent,
            "url" to url
        )
        storeNotifyService.sendNotifyMessage(
            templateCode = STORE_COMMENT_NOTIFY_TEMPLATE,
            sender = DEVOPS,
            receivers = receivers.toMutableSet(),
            bodyParams = bodyParams
        )
    }

    private fun validateScore(score: Int): Boolean {
        val requireScoreList = listOf(0, 1, 2, 3, 4, 5)
        if (!requireScoreList.contains(score)) {
            return false
        }
        return true
    }

    /**
     * 更新评论信息
     */
    override fun updateStoreComment(
        userId: String,
        commentId: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean> {
        logger.info("updateStoreComment params:[$userId|$commentId|$storeCommentRequest]")
        val score = storeCommentRequest.score
        // 校验评分是否合法
        if (!validateScore(score)) return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("score:$score"),
            data = false,
            language = I18nUtil.getLanguage(userId)
        )
        // 判断用户更新的记录是不是他自已发表的
        val storeCommentRecord =
            storeCommentDao.getStoreComment(dslContext, commentId) ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(commentId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val creator = storeCommentRecord.creator
        if (userId != creator) {
            return I18nUtil.generateResponseDataObject(
                messageCode = COMMENT_UPDATE_NO_PERMISSION,
                params = null,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val scoreIncrement = score - storeCommentRecord.score
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 添加评论信息
            storeCommentDao.updateStoreComment(dslContext, userId, commentId, storeCommentRequest)
            // 更新统计信息
            storeStatisticDao.updateCommentInfo(
                dslContext = context,
                userId = userId,
                storeId = storeCommentRecord.storeId,
                storeCode = storeCommentRecord.storeCode,
                storeType = storeCommentRecord.storeType,
                commentIncrement = 0,
                scoreIncrement = scoreIncrement
            )
            storeTotalStatisticService.updateStoreTotalStatisticByCode(
                storeCommentRecord.storeCode,
                storeCommentRecord.storeType
            )
        }
        val storeType = StoreTypeEnum.getStoreTypeObj(storeCommentRecord.storeType.toInt())
        sendNotifyMessage(
            storeCode = storeCommentRecord.storeCode,
            storeType = storeType!!,
            storeId = storeCommentRecord.storeId,
            userId = userId,
            storeCommentRequest = storeCommentRequest
        )
        return Result(true)
    }

    /**
     * 评论点赞/取消点赞
     */
    override fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int> {
        logger.info("updateStoreCommentPraiseCount params:[$userId|$commentId]")
        val commentRecord =
            storeCommentDao.getStoreComment(dslContext, commentId) ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(commentId),
                language = I18nUtil.getLanguage(userId)
            )
        var praiseCount = 1
        // 判断用户是否点赞过
        val count = storeCommentPraiseDao.countByIds(dslContext, userId, commentId)
        logger.info("the count is:$count")
        if (count > 0) {
            // 用户已点赞过，再次点击则取消点赞
            praiseCount = -1
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeCommentDao.updateStoreCommentPraiseCount(context, userId, commentId, praiseCount) // 更新点赞数量
            if (count > 0) {
                storeCommentPraiseDao.deleteStoreCommentPraise(context, userId, commentId) // 删除点赞人
            } else {
                storeCommentPraiseDao.addStoreCommentPraise(context, userId, commentId) // 记录点赞人
            }
        }
        return Result(data = commentRecord.praiseCount + praiseCount)
    }

    override fun getStoreUserCommentInfo(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): StoreUserCommentInfo {
        logger.info("getStoreUserCommentInfo params:[$userId|$storeCode|$storeType]")
        val latestCommentRecord =
            storeCommentDao.getUserLatestCommentByStoreCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )
        var commentFlag = false
        if (null != latestCommentRecord) {
            commentFlag = true
        }
        return StoreUserCommentInfo(commentFlag = commentFlag, commentId = latestCommentRecord?.id)
    }
}
