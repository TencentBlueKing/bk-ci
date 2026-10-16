package com.tencent.devops.notify.blueking.utils

import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.pojo.VoiceNotifyPost
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.common.service.tenant.TenantUtils
import com.tencent.devops.notify.blueking.sdk.CMSApi
import jakarta.ws.rs.HttpMethod
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class TenantNotifyService(cmsApi: CMSApi) : NotifyService(cmsApi) {
    @Value("\${bk.apigw.cmsi.host:#{null}}")
    val bkApigwCmsiHost: String? = null

    override fun post(url: String, postData: Any, tofConf: Map<String, String>?): NotifyResult {
        logger.info("post url: $url, postData: $postData, tofConf: $tofConf")
        when (url) {
            SEND_MAIL -> {
                val post = postData as EmailNotifyPost
                return sendMail(
                    tenantId = post.tenantId,
                    receiverUserName = post.to.split(",").toList(),
                    sender = post.from,
                    title = post.title,
                    content = post.content,
                    ccUserName = post.cc.split(",").toList()
                ).toResult()
            }

            SEND_SMS -> {
                val post = postData as SmsNotifyPost
                return sendSms(
                    tenantId = post.tenantId,
                    receiverUserName = post.receiver.split(",").toList(),
                    content = post.msgInfo,
                ).toResult()
            }

            SEND_VOICE -> {
                val post = postData as VoiceNotifyPost
                return sendVoice(
                    tenantId = post.tenantId,
                    autoReadMessage = post.content,
                    receiverUserName = post.receiver.split(",").toList()
                ).toResult()
            }

            SEND_WEIXIN -> {
                val post = postData as WechatNotifyPost
                return sendWeixin(
                    tenantId = post.tenantId,
                    receiverUserName = post.receiver.split(",").toList(),
                    messageData = WeixinMessageData(
                        heading = post.sender,
                        message = post.msgInfo
                    )
                ).toResult()
            }

            else -> {
                throw IllegalArgumentException("Unknown message type")
            }
        }
    }

    /**
     * 发送邮件
     */
    private fun sendMail(
        tenantId: String?,
        receiverUserName: List<String>,
        sender: String?,
        title: String,
        content: String,
        ccUserName: List<String>,
        bodyFormat: String? = "HTML",
        isContentBase64: Boolean? = false
    ): SendResp {
        return TenantUtils.callApigw(
            apigwHost = bkApigwCmsiHost!!,
            path = SEND_MAIL,
            params = mapOf(
                "receiver_username" to receiverUserName,
                "sender" to (sender ?: ""),
                "title" to title,
                "content" to content,
                "cc_username" to ccUserName,
                "body_format" to (bodyFormat ?: ""),
                "is_content_base64" to (isContentBase64?.toString() ?: "")
            ),
            tenantId = tenantId,
            method = HttpMethod.POST,
            respType = SendResp::class.java
        )
    }

    /**
     * 发送短信
     */
    private fun sendSms(
        tenantId: String?,
        receiverUserName: List<String>,
        content: String,
        isContentBase64: Boolean? = false
    ): SendResp {
        return TenantUtils.callApigw(
            apigwHost = bkApigwCmsiHost!!,
            path = SEND_SMS,
            params = mapOf(
                "receiver_username" to receiverUserName,
                "content" to content,
                "is_content_base64" to (isContentBase64?.toString() ?: "")
            ),
            tenantId = tenantId,
            method = HttpMethod.POST,
            respType = SendResp::class.java
        )
    }

    /**
     * 发送语音
     */
    private fun sendVoice(
        tenantId: String?,
        autoReadMessage: String,
        receiverUserName: List<String>
    ): SendResp {
        return TenantUtils.callApigw(
            apigwHost = bkApigwCmsiHost!!,
            path = SEND_VOICE,
            params = mapOf(
                "auto_read_message" to autoReadMessage,
                "receiver_username" to receiverUserName
            ),
            tenantId = tenantId,
            method = HttpMethod.POST,
            respType = SendResp::class.java
        )
    }

    /**
     * 发送微信
     */
    private fun sendWeixin(
        tenantId: String?,
        receiverUserName: List<String>,
        messageData: WeixinMessageData
    ): SendResp {
        return TenantUtils.callApigw(
            apigwHost = bkApigwCmsiHost!!,
            path = SEND_WEIXIN,
            params = mapOf(
                "receiver_username" to receiverUserName,
                "message_data" to messageData
            ),
            tenantId = tenantId,
            method = HttpMethod.POST,
            respType = SendResp::class.java
        )
    }


    companion object {
        val logger = LoggerFactory.getLogger(TenantNotifyService::class.java)
        private const val SEND_MAIL = "v1/send_mail/"
        private const val SEND_SMS = "v1/send_sms/"
        private const val SEND_VOICE = "v1/send_voice/"
        private const val SEND_WEIXIN = "v1/send_weixin/"
    }

    data class SendResp(
        val data: Data
    ) {
        fun toResult(): NotifyResult {
            return NotifyResult(
                Ret = if (StringUtils.isBlank(data.message)) 0 else -1,
                ErrCode = data.summary.failed,
                ErrMsg = data.message,
                StackTrace = null,
                data = data.summary
            )
        }
    }

    data class Data(
        val summary: Summary,
        val message: String,
        val details: Any
    )

    data class Summary(
        val total: Int,
        val succeeded: Int,
        val failed: Int
    )

    data class WeixinMessageData(
        val heading: String,
        val message: String
    )

}