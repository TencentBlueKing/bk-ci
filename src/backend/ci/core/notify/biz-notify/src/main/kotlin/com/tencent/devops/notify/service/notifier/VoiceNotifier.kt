package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.VoiceMessage
import com.tencent.devops.notify.service.VoiceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class VoiceNotifier @Autowired constructor(
    private val voiceService: VoiceService
) : BaseNotifier() {
    override fun type(): NotifyType = NotifyType.VOICE

    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        // TODO 使用template
        val taskName = "测试"
        val content = "你的流水线 xxx ，运行成功，构建号 1001，请及时关注。"

        val message = VoiceMessage()
        message.receivers = request.receivers
        message.taskName = taskName
        message.content = content
        voiceService.sendMqMsg(message)
    }
}
