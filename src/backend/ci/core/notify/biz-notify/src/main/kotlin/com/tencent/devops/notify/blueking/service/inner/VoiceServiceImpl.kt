package com.tencent.devops.notify.blueking.service.inner

import com.tencent.devops.common.api.util.JsonUtil.deepCopy
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.notify.utils.Configuration
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.VOICE_URL
import com.tencent.devops.notify.dao.VoiceNotifyDao
import com.tencent.devops.notify.model.VoiceNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.VoiceNotifyMessage
import com.tencent.devops.notify.service.VoiceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge

class VoiceServiceImpl @Autowired constructor(
    private val notifyService: NotifyService,
    private val voiceNotifyDao: VoiceNotifyDao,
    private val streamBridge: StreamBridge,
    private val configuration: Configuration,
    private val dslContext: DSLContext
) : VoiceService {
    override fun sendMqMsg(message: VoiceNotifyMessage) {
        message.sendTo(streamBridge)
    }

    /**
     * 发送语音消息
     */
    override fun sendMessage(voiceNotifyMessageWithOperation: VoiceNotifyMessageWithOperation) {
        val voiceNotifyPost = try {
            voiceNotifyMessageWithOperation.asPost()
        } catch (e: Exception) {
            logger.warn("send message failed.", e)
            return
        }

        val tofConfig = configuration.getConfigurations(voiceNotifyMessageWithOperation.tofSysId)
        if (null == tofConfig) {
            logger.warn("tofConfig is null , $voiceNotifyMessageWithOperation")
            return
        }
        val tofResult = notifyService.post(VOICE_URL, voiceNotifyPost, tofConfig)
        val retryCount = voiceNotifyMessageWithOperation.retryCount
        val tofSuccess = tofResult.Ret == 0
        val id = voiceNotifyMessageWithOperation.id ?: UUIDUtil.generate()

        voiceNotifyDao.insertOrUpdate(
            dslContext = dslContext,
            id = id,
            success = tofSuccess,
            receivers = voiceNotifyPost.receiver,
            taskName = voiceNotifyPost.taskName,
            content = voiceNotifyPost.content,
            transferReceiver = voiceNotifyPost.transferReceiver,
            retryCount = retryCount,
            lastError = if (tofSuccess) null else tofResult.ErrMsg,
            tofSysId = tofConfig["sys-id"],
            fromSysId = voiceNotifyPost.fromSysId
        )

        // 失败重试 , 最多3次
        if (!tofSuccess && retryCount < 3) {
            val deepCopyMessage = voiceNotifyMessageWithOperation.deepCopy<VoiceNotifyMessageWithOperation>()
            deepCopyMessage.id = id
            deepCopyMessage.retryCount = retryCount + 1
            var delayTime = 0
            when (retryCount) {
                1 -> delayTime = 30000
                2 -> delayTime = 120000
                3 -> delayTime = 300000
            }
            if (delayTime > 0) {
                deepCopyMessage.delayMills = delayTime
            }
            deepCopyMessage.sendTo(streamBridge)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VoiceServiceImpl::class.java)
    }
}
