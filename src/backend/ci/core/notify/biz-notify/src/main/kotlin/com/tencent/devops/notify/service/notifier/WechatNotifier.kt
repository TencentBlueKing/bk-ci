package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.service.WechatService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WechatNotifier @Autowired constructor(
    private val wechatService: WechatService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext,
    private val commonConfig: CommonConfig
) : INotifier {

    override fun type(): NotifyType = NotifyType.WECHAT

    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        val wechatTplRecord = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(
            dslContext = dslContext,
            commonTemplateId = commonNotifyMessageTemplateRecord.id
        )!!
        // 先对 DB 原始模板做渠道关键字替换（如 CREATIVE_STREAM 渠道将「流水线」替换为「创作流」），再替换占位符
        val rawBody = NotifierUtils.replaceNotifyKeywordByChannel(
            wechatTplRecord.body, commonConfig.devopsDefaultLocaleLanguage
        )
        val finalBody = NotifierUtils.replaceContentParams(request.bodyParams, rawBody)
        logger.info("sendWechatNotifyMessage:\nbody:$finalBody")
        val wechatNotifyMessage = WechatNotifyMessage().apply {
            sender = wechatTplRecord.sender
            addAllReceivers(request.receivers)
            body = finalBody
            priority = EnumNotifyPriority.parse(commonNotifyMessageTemplateRecord.priority.toString())
            source = EnumNotifySource.parse(commonNotifyMessageTemplateRecord.source.toInt())
                ?: EnumNotifySource.BUSINESS_LOGIC
        }
        wechatService.sendMqMsg(wechatNotifyMessage)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WechatNotifier::class.java)
    }
}
