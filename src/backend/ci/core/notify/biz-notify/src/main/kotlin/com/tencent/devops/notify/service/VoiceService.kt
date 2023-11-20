package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.VoiceNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.VoiceNotifyMessage

/**
 * 语音服务
 */
interface VoiceService {
    fun sendMqMsg(message: VoiceNotifyMessage)

    fun sendMessage(voiceNotifyMessageWithOperation: VoiceNotifyMessageWithOperation)
}
