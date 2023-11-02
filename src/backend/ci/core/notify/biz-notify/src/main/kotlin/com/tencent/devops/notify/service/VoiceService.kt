package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.VoiceNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.VoiceMessage

/**
 * 语音服务
 */
interface VoiceService {
    fun sendMqMsg(message: VoiceMessage)

    fun sendMessage(voiceNotifyMessageWithOperation: VoiceNotifyMessageWithOperation)
}
