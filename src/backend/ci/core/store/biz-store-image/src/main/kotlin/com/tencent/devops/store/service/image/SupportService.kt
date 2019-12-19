package com.tencent.devops.store.service.image

import com.tencent.devops.artifactory.api.service.ServiceImageManageResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/11/21
 * @Version 1.0
 */

@RefreshScope
@Service
class SupportService @Autowired constructor(
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(SupportService::class.java)

    fun getIconDataByLogoUrl(
        logoUrl: String
    ): String? {
        try {
            val iconData = client.get(ServiceImageManageResource::class).compressImage(logoUrl, 18, 18).data
            logger.info("the iconData is :$iconData")
            return iconData
        } catch (e: Exception) {
            logger.error("compressImage error is :$e", e)
        }
        return null
    }

    @Value("\${store.imageExecuteNullNotifyTplCode}")
    private lateinit var imageExecuteNullNotifyTplCode: String

    @Value("\${store.imageAdminUsers}")
    private val imageAdminUsersStr: String? = null

    fun sendImageExecuteNullToManagers(titleParams: Map<String, String>?, bodyParams: Map<String, String>?) {
        val receivers = imageAdminUsersStr!!.trim().split(";").toMutableSet()
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = imageExecuteNullNotifyTplCode,
            sender = "DevOps",
            receivers = receivers,
            cc = receivers,
            titleParams = titleParams,
            bodyParams = bodyParams
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("sendNotifyResult is:$sendNotifyResult")
    }
}
