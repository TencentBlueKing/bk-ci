package com.tencent.devops.notify.blueking.service.inner

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.notify.utils.Configuration
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.WECHAT_URL
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.model.notify.tables.records.TNotifyWechatRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_WECHAT
import com.tencent.devops.notify.dao.WechatNotifyDao
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.service.WechatService
import com.tencent.devops.common.notify.utils.CommonUtils
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class WechatServiceImpl @Autowired constructor(
    private val tofService: NotifyService,
    private val wechatNotifyDao: WechatNotifyDao,
    private val rabbitTemplate: RabbitTemplate,
    private val configuration: Configuration
) : WechatService {

    private val logger = LoggerFactory.getLogger(WechatServiceImpl::class.java)

    override fun sendMqMsg(message: WechatNotifyMessage) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_WECHAT, message)
    }

    /**
     * 发送短信消息
     * @param wechatNotifyMessageWithOperation 消息对象
     */
    override fun sendMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation) {
        val wechatNotifyPost = generateWechatNotifyPost(wechatNotifyMessageWithOperation)
        if (wechatNotifyPost == null) {
            logger.warn("WechatNotifyPost is empty after being processed, WechatNotifyMessageWithOperation: " + wechatNotifyMessageWithOperation.toString())
            return
        }

        val retryCount = wechatNotifyMessageWithOperation.retryCount
        val id = wechatNotifyMessageWithOperation.id ?: UUIDUtil.generate()
        val tofConfs = configuration.getConfigurations(wechatNotifyMessageWithOperation.tofSysId)
        val result = tofService.post(
            WECHAT_URL, wechatNotifyPost, tofConfs!!)
        if (result.Ret == 0) {
            // 成功
            wechatNotifyDao.insertOrUpdateWechatNotifyRecord(true, wechatNotifyMessageWithOperation.source, id,
                retryCount, null, wechatNotifyPost.receiver, wechatNotifyPost.sender, wechatNotifyPost.msgInfo, wechatNotifyPost.priority.toInt(),
                wechatNotifyPost.contentMd5, wechatNotifyPost.frequencyLimit,
                tofConfs["sys-id"], wechatNotifyPost.fromSysId)
        } else {
            // 写入失败记录
            wechatNotifyDao.insertOrUpdateWechatNotifyRecord(false, wechatNotifyMessageWithOperation.source, id,
                retryCount, result.ErrMsg, wechatNotifyPost.receiver, wechatNotifyPost.sender, wechatNotifyPost.msgInfo, wechatNotifyPost.priority.toInt(),
                wechatNotifyPost.contentMd5, wechatNotifyPost.frequencyLimit,
                tofConfs["sys-id"], wechatNotifyPost.fromSysId)
            if (retryCount < 3) {
                // 开始重试
                reSendMessage(wechatNotifyPost, wechatNotifyMessageWithOperation.source, retryCount + 1, id)
            }
        }
    }

    private fun reSendMessage(post: WechatNotifyPost, source: EnumNotifySource, retryCount: Int, id: String) {
        val wechatNotifyMessageWithOperation = WechatNotifyMessageWithOperation()
        wechatNotifyMessageWithOperation.apply {
            this.id = id
            this.retryCount = retryCount
            this.source = source
            sender = post.sender
            addAllReceivers(Sets.newHashSet(post.receiver.split(",")))
            priority = EnumNotifyPriority.parse(post.priority)
            body = post.msgInfo
            frequencyLimit = post.frequencyLimit
            tofSysId = post.tofSysId
            fromSysId = post.fromSysId
        }

        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_WECHAT, wechatNotifyMessageWithOperation) { message ->
            var delayTime = 0
            when (retryCount) {
                1 -> delayTime = 30000
                2 -> delayTime = 120000
                3 -> delayTime = 300000
            }
            if (delayTime > 0) {
                message.messageProperties.setHeader("x-delay", delayTime)
            }
            message
        }
    }

    private fun generateWechatNotifyPost(wechatNotifyMessage: WechatNotifyMessage): WechatNotifyPost? {
        val contentMd5 = CommonUtils.getMessageContentMD5("", wechatNotifyMessage.body)
        val receivers = Lists.newArrayList(filterReceivers(
            wechatNotifyMessage.getReceivers(), contentMd5, wechatNotifyMessage.frequencyLimit)
        )
        if (receivers == null || receivers.isEmpty()) {
            return null
        }

        val post = WechatNotifyPost()
        post.apply {
            receiver = wechatNotifyMessage.getReceivers().joinToString(",")
            msgInfo = wechatNotifyMessage.body
            priority = wechatNotifyMessage.priority.getValue()
            sender = wechatNotifyMessage.sender
            this.contentMd5 = contentMd5
            frequencyLimit = wechatNotifyMessage.frequencyLimit
            tofSysId = wechatNotifyMessage.tofSysId
            fromSysId = wechatNotifyMessage.fromSysId
        }

        return post
    }

    private fun filterReceivers(receivers: Set<String>, contentMd5: String, frequencyLimit: Int): Set<String> {
        val filteredReceivers = HashSet(receivers)
        val filteredOutReceivers = HashSet<String>()
        if (frequencyLimit > 0) {
            val recordedReceivers = wechatNotifyDao.getReceiversByContentMd5AndTime(
                contentMd5, (frequencyLimit * 60).toLong()
            )
            receivers.forEach { rec ->
                for (recordedRec in recordedReceivers) {
                    if (",$recordedRec,".contains(rec)) {
                        filteredReceivers.remove(rec)
                        filteredOutReceivers.add(rec)
                        break
                    }
                }
            }
            logger.warn("Filtered out receivers:" + filteredOutReceivers)
        }
        return filteredReceivers
    }

    override fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<WechatNotifyMessageWithOperation> {
        val count = wechatNotifyDao.count(success, fromSysId)
        val result: List<NotificationResponse<WechatNotifyMessageWithOperation>> = if (count == 0) {
            listOf()
        } else {
            val emailRecords = wechatNotifyDao.list(page, pageSize, success, fromSysId, createdTimeSortOrder)
            emailRecords.stream().map(this::parseFromTNotifyWechatToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(count, page, pageSize, result)
    }

    private fun parseFromTNotifyWechatToResponse(record: TNotifyWechatRecord): NotificationResponse<WechatNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.receivers.isNullOrEmpty())
            receivers.addAll(record.receivers.split(";"))

        val message = WechatNotifyMessageWithOperation()
        message.apply {
            frequencyLimit = record.frequencyLimit
            fromSysId = record.fromSysId
            tofSysId = record.tofSysId
            body = record.body
            sender = record.sender
            priority = EnumNotifyPriority.parse(record.priority.toString())
            source = EnumNotifySource.parseName(record.source)
            retryCount = record.retryCount
            lastError = record.lastError
            addAllReceivers(receivers)
        }

        return NotificationResponse(record.id, record.success,
            if (record.createdTime == null) null
            else
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.createdTime),
            if (record.updatedTime == null) null
            else
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.updatedTime),
            record.contentMd5, message)
    }
}