package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.service.WechatService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WechatNotifier @Autowired constructor(
    private val wechatService: WechatService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext
) : INotifier {
    @Value("\${wework.domain}")
    private val userUseDomain: Boolean = true
    override fun type(): NotifyType = NotifyType.WECHAT
    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        val wechatTplRecord = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(
            dslContext = dslContext,
            commonTemplateId = commonNotifyMessageTemplateRecord.id
        )!!
        // 替换内容里的动态参数
        val body = NotifierUtils.replaceContentParams(request.bodyParams, wechatTplRecord.body)
        sendWechatNotifyMessage(
            commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
            sendNotifyMessageTemplateRequest = request,
            body = body,
            sender = wechatTplRecord.sender
        )
    }

    private fun sendWechatNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        body: String,
        sender: String
    ) {
        logger.info("sendWechatNotifyMessage:\nbody:$body")
        val wechatNotifyMessage = WechatNotifyMessage()
        wechatNotifyMessage.sender = sender
        wechatNotifyMessage.addAllReceivers(sendNotifyMessageTemplateRequest.receivers)
        wechatNotifyMessage.body = body
        wechatNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        wechatNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        wechatService.sendMqMsg(wechatNotifyMessage)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WechatNotifier::class.java)
    }
}
