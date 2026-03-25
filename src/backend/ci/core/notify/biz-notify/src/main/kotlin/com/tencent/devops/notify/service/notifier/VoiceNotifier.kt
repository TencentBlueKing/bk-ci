package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.VoiceNotifyMessage
import com.tencent.devops.notify.service.VoiceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class VoiceNotifier @Autowired constructor(
    private val voiceService: VoiceService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext,
    private val commonConfig: CommonConfig
) : INotifier {
    override fun type(): NotifyType = NotifyType.VOICE

    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        val voiceTplRecord = notifyMessageTemplateDao.getVoiceNotifyMessageTemplate(
            dslContext = dslContext,
            commonTemplateId = commonNotifyMessageTemplateRecord.id
        )!!
        // 先对 DB 原始模板做渠道关键字替换（如 CREATIVE_STREAM 渠道将「流水线」替换为「创作流」），再替换占位符
        val language = commonConfig.devopsDefaultLocaleLanguage
        val rawTaskName = NotifierUtils.replaceNotifyKeywordByChannel(voiceTplRecord.taskName, language)
        val rawContent = NotifierUtils.replaceNotifyKeywordByChannel(voiceTplRecord.content, language)
        val finalTaskName = NotifierUtils.replaceContentParams(request.titleParams, rawTaskName)
        val finalContent = NotifierUtils.replaceContentParams(request.bodyParams, rawContent)
        logger.info("send voice msg , ${commonNotifyMessageTemplateRecord.id} , taskName:$finalTaskName , " +
                "content:$finalContent")

        val message = VoiceNotifyMessage()
        message.receivers = request.receivers
        message.taskName = finalTaskName
        message.content = finalContent
        voiceService.sendMqMsg(message)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VoiceNotifier::class.java)
    }
}
