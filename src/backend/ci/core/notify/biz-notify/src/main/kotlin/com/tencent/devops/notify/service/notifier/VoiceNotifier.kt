package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
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
    private val dslContext: DSLContext
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
        val taskName = NotifierUtils.replaceContentParams(request.titleParams, voiceTplRecord.taskName)
        val content = NotifierUtils.replaceContentParams(request.bodyParams, voiceTplRecord.content)
        logger.info("send voice msg , ${commonNotifyMessageTemplateRecord.id} , taskName:$taskName , content:$content")

        val message = VoiceNotifyMessage()
        message.receivers = request.receivers
        message.taskName = taskName
        message.content = content
        voiceService.sendMqMsg(message)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VoiceNotifier::class.java)
    }
}
