package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.store.dao.common.StoreCommentReplyDao
import com.tencent.devops.store.pojo.common.StoreCommentReplyInfo
import com.tencent.devops.store.pojo.common.StoreCommentReplyRequest
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.atom.tables.records.TStoreCommentReplyRecord
import com.tencent.devops.notify.api.ServiceNotifyResource
import com.tencent.devops.store.dao.common.StoreCommentDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.util.RtxUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * store评论回复业务逻辑类
 * author: carlyin
 * since: 2019-03-26
 */
@Service
class StoreCommentReplyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeCommentDao: StoreCommentDao,
    private val storeCommentReplyDao: StoreCommentReplyDao,
    private val storeUserService: StoreUserService,
    private val storeCommonService: StoreCommonService,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(StoreCommentReplyService::class.java)

    @Value("\${store.profileUrlPrefix}")
    private lateinit var profileUrlPrefix: String

    @Value("\${store.commentNotifyAdmin}")
    private lateinit var commentNotifyAdmin: String

    /**
     * 获取评论信息列表
     */
    fun getStoreCommentReplysByCommentId(commentId: String): Result<List<StoreCommentReplyInfo>?> {
        logger.info("commentId is :$commentId")
        val storeCommentReplyInfoList = storeCommentReplyDao.getStoreCommentReplysByCommentId(dslContext, commentId)?.map {
            generateStoreCommentReplyInfo(it)
        }
        return Result(storeCommentReplyInfoList)
    }

    fun getStoreCommentReply(replyId: String): Result<StoreCommentReplyInfo?> {
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
    fun addStoreCommentReply(userId: String, commentId: String, storeCommentReplyRequest: StoreCommentReplyRequest): Result<StoreCommentReplyInfo?> {
        logger.info("userId is :$userId,commentId is :commentId, storeCommentReplyRequest is :$storeCommentReplyRequest")
        val storeCommentRecord = storeCommentDao.getStoreComment(dslContext, commentId) ?: return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(commentId))
        logger.info("the storeCommentRecord is:$storeCommentRecord")
        val userDeptNameResult = storeUserService.getUserFullDeptName(userId)
        logger.info("the userDeptNameResult is:$userDeptNameResult")
        if (userDeptNameResult.isNotOk()) {
            return Result(userDeptNameResult.status, userDeptNameResult.message ?: "")
        }
        val profileUrl = "$profileUrlPrefix$userId/profile.jpg"
        val replyId = UUIDUtil.generate()
        storeCommentReplyDao.addStoreCommentReply(dslContext, replyId, userId, userDeptNameResult.data.toString(), commentId, profileUrl, storeCommentReplyRequest)

        // RTX 通知被回复人和蓝盾管理员
        val receivers = if (storeCommentReplyRequest.replyToUser == "") {
            storeCommentRecord.creator.plus(";").plus(commentNotifyAdmin).split(";").toSet()
        } else {
            storeCommentReplyRequest.replyToUser.plus(";").plus(commentNotifyAdmin).split(";").toSet()
        }
        logger.info("the receivers is:$receivers")

        val storeType = StoreTypeEnum.getStoreTypeObj(storeCommentRecord.storeType.toInt())
        val url = "${HomeHostUtil.innerServerHost()}/console/store/atomStore/detail/${storeType!!.name.toLowerCase()}/${storeCommentRecord.storeCode}"
        val storeName = storeCommonService.getStoreNameById(storeCommentRecord.storeId, storeType)
        val rtxMessage = RtxUtil.makeCommentReplyNotifyMessage(
            userId,
            storeName,
            if (storeCommentReplyRequest.replyToUser == "") { storeCommentRecord.creator } else { storeCommentReplyRequest.replyToUser },
            storeCommentReplyRequest.replyContent,
            storeCommentRecord.commentContent,
            url,
            receivers)
        client.get(ServiceNotifyResource::class).sendRtxNotify(rtxMessage)

        return getStoreCommentReply(replyId)
    }
}
