package com.tencent.devops.process.util

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.support.api.service.ServiceWechatWorkResource
import com.tencent.devops.support.model.wechatwork.base.Receiver
import com.tencent.devops.support.model.wechatwork.base.Text
import com.tencent.devops.support.model.wechatwork.enums.MsgType
import com.tencent.devops.support.model.wechatwork.enums.ReceiverType
import com.tencent.devops.support.model.wechatwork.message.TextMessage
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * deng
 * 2018/9/20
 */
object NotifyUtils {

    fun sendSMS(
        client: Client,
        users: Set<String>,
        pipelineId: String,
        smsBody: String,
        mapData: Map<String, String>
    ) {
        val message = SmsNotifyMessage().apply {
            addAllReceivers(users)
            body = parseMessageTemplate(smsBody, mapData)
            sender = "blueking"
        }
        logger.info("Start to send the sms message($message) for the pipeline($pipelineId)")
        val result = client.get(ServiceNotifyResource::class).sendSmsNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the email message($message) because of ${result.message}")
        }
    }

    fun sendEmail(
        client: Client,
        users: Set<String>,
        pipelineId: String,
        emailBody: String,
        emailTitle: String,
        mapData: Map<String, String>
    ) {
        val message = EmailNotifyMessage().apply {
            addAllReceivers(users)
            format = EnumEmailFormat.HTML
            body = parseMessageTemplate(emailBody, mapData)
            title = parseMessageTemplate(emailTitle, mapData)
            sender = "DevOps"
        }
        logger.info("Start to send the email message($message) for the pipeline($pipelineId)")
        val result = client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the email message($message) because of ${result.message}")
        }
    }

    fun sendWechat(
        client: Client,
        users: Set<String>,
        pipelineId: String,
        wechatBody: String,
        mapData: Map<String, String>
    ) {
        val message = WechatNotifyMessage().apply {
            addAllReceivers(users)
            body = parseMessageTemplate(wechatBody, mapData)
        }
        logger.info("Start to send the wechat message($message) for the pipeline($pipelineId)")
        val result = client.get(ServiceNotifyResource::class).sendWechatNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the email message($message) because of ${result.message}")
        }
    }

    fun sendRTX(
        client: Client,
        users: Set<String>,
        pipelineId: String,
        rtxBody: String,
        rtxTitle: String,
        mapData: Map<String, String>
    ) {
        val message = RtxNotifyMessage().apply {
            addAllReceivers(users)
            body = parseMessageTemplate(rtxBody, mapData)
            title = parseMessageTemplate(rtxTitle, mapData)
            sender = "蓝鲸助手"
        }
        logger.info("Start to send the rtx message($message) for the pipeline($pipelineId)")
        val result = client.get(ServiceNotifyResource::class).sendRtxNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the rtx message($message) because of ${result.message}")
        }
    }

    fun sendWechatWorkGroup(
        client: Client,
        wechatGropId: String,
        pipelineId: String,
        wechatBody: String,
        mapData: Map<String, String>
    ) {
        val message = TextMessage(
            Receiver(ReceiverType.group, wechatGropId),
            MsgType.text,
            Text(parseMessageTemplate(wechatBody, mapData))
        )
        logger.info("Start to send the wechat work group message($message) for the pipeline($pipelineId)")
        val result = client.get(ServiceWechatWorkResource::class).sendTextMessage(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the email message($message) because of ${result.message}")
        }
    }

    fun parseMessageTemplate(content: String, data: Map<String, String>): String {
        if (content.isBlank()) {
            return content
        }
        val pattern = Pattern.compile("#\\{([^}]+)}")
        val newValue = StringBuffer(content.length)
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            val key = matcher.group(1)
            val variable = data[key] ?: ""
            matcher.appendReplacement(newValue, variable)
        }
        matcher.appendTail(newValue)
        return newValue.toString()
    }

    private val logger = LoggerFactory.getLogger(NotifyUtils::class.java)
}