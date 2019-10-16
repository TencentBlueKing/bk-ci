package com.tencent.devops.support.services

import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.support.model.wechatwork.enums.UploadMediaType
import com.tencent.devops.support.model.wechatwork.result.UploadMediaResult
import com.tencent.devops.common.wechatwork.model.enums.UploadMediaType as SendMediaType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class WechatWorkMessageService @Autowired constructor(
    private val wechatWorkService: WechatWorkService
) {
    private val logger = LoggerFactory.getLogger(WechatWorkMessageService::class.java)

    fun sendMessage(message: Any) = wechatWorkService.sendMessage(message)

    fun sendRichtextMessage(richitextMessage: RichtextMessage) = wechatWorkService.sendRichText(richitextMessage)

    fun uploadMedia(uploadMediaType: UploadMediaType, mediaName: String, mediaInputStream: InputStream): UploadMediaResult? {
        val uploadMediaResponse = wechatWorkService.uploadMedia(
                SendMediaType.valueOf(uploadMediaType.toString()),
                mediaName, mediaInputStream)
        return if (uploadMediaResponse == null) {
            null
        } else {
            return UploadMediaResult(UploadMediaType.valueOf(uploadMediaResponse.type.toString()), uploadMediaResponse.media_id)
        }
    }
}
