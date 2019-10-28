package com.tencent.devops.support.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.support.api.service.ServiceWechatWorkResource
import com.tencent.devops.support.model.wechatwork.enums.UploadMediaType
import com.tencent.devops.support.model.wechatwork.message.ImageMessage
import com.tencent.devops.support.model.wechatwork.message.TextMessage
import com.tencent.devops.support.model.wechatwork.result.UploadMediaResult
import com.tencent.devops.support.services.WechatWorkMessageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class ServiceWechatWorkResourceImpl @Autowired constructor(private val wechatWorkMessageService: WechatWorkMessageService) : ServiceWechatWorkResource {

    private val logger = LoggerFactory.getLogger(ServiceWechatWorkResourceImpl::class.java)

    override fun sendRichtextMessage(richitextMessage: RichtextMessage): Result<Boolean> {
        return Result(wechatWorkMessageService.sendRichtextMessage(richitextMessage))
    }

    override fun sendTextMessage(textMessage: TextMessage): Result<Boolean> {
        return Result(wechatWorkMessageService.sendMessage(textMessage))
    }

    override fun sendImageMessage(imageMessage: ImageMessage): Result<Boolean> {

        return Result(wechatWorkMessageService.sendMessage(imageMessage))
    }

    override fun uploadMedia(uploadMediaType: UploadMediaType, mediaName: String, mediaInputStream: InputStream): Result<UploadMediaResult?> {
        return Result(wechatWorkMessageService.uploadMedia(uploadMediaType, mediaName, mediaInputStream))
    }
}