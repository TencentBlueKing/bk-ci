package com.tencent.devops.notify.blueking.consumer

import com.tencent.devops.notify.blueking.EXCHANGE_NOTIFY
import com.tencent.devops.notify.blueking.QUEUE_NOTIFY_EMAIL
import com.tencent.devops.notify.blueking.QUEUE_NOTIFY_RTX
import com.tencent.devops.notify.blueking.QUEUE_NOTIFY_SMS
import com.tencent.devops.notify.blueking.QUEUE_NOTIFY_WECHAT
import com.tencent.devops.notify.blueking.ROUTE_EMAIL
import com.tencent.devops.notify.blueking.ROUTE_RTX
import com.tencent.devops.notify.blueking.ROUTE_SMS
import com.tencent.devops.notify.blueking.ROUTE_WECHAT
import com.tencent.devops.notify.blueking.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.blueking.model.RtxNotifyMessageWithOperation
import com.tencent.devops.notify.blueking.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.blueking.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.blueking.service.RtxService
import com.tencent.devops.notify.blueking.service.EmailService
import com.tencent.devops.notify.blueking.service.WechatService
import com.tencent.devops.notify.blueking.service.SmsService
import com.tencent.devops.notify.blueking.service.OrgService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NotifyMessageConsumer @Autowired constructor(
    private val rtxService: RtxService,
    private val emailService: EmailService,
    private val smsService: SmsService,
    private val wechatService: WechatService,
    private val orgService: OrgService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NotifyMessageConsumer::class.java)
    }

    @RabbitListener(containerFactory = "rabbitListenerContainerFactory",
            bindings = [
                    QueueBinding(
                            key = ROUTE_RTX,
                            value = Queue(value = QUEUE_NOTIFY_RTX, durable = "true"),
                            exchange = Exchange(value = EXCHANGE_NOTIFY, durable = "true", delayed = "true", type = "topic"))])
    fun onReceiveRtxMessage(rtxNotifyMessageWithOperation: RtxNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(rtxNotifyMessageWithOperation.getReceivers())
            rtxNotifyMessageWithOperation.clearReceivers()
            rtxNotifyMessageWithOperation.addAllReceivers(parseStaff)
            rtxService.sendMessage(rtxNotifyMessageWithOperation)
        } catch (ex: Exception) {
            logger.error("Failed process received RTX message", ex)
        }
    }

    @RabbitListener(containerFactory = "rabbitListenerContainerFactory",
            bindings = [
                    QueueBinding(
                            key = ROUTE_EMAIL,
                            value = Queue(value = QUEUE_NOTIFY_EMAIL, durable = "true"),
                            exchange = Exchange(value = EXCHANGE_NOTIFY, durable = "true", delayed = "true", type = "topic"))])
    fun onReceiveEmailMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(emailNotifyMessageWithOperation.getReceivers())
            val parseBcc = orgService.parseStaff(emailNotifyMessageWithOperation.getBcc())
            val parseCc = orgService.parseStaff(emailNotifyMessageWithOperation.getCc())

            emailNotifyMessageWithOperation.clearReceivers()
            emailNotifyMessageWithOperation.clearBcc()
            emailNotifyMessageWithOperation.clearCc()

            emailNotifyMessageWithOperation.addAllBccs(parseBcc)
            emailNotifyMessageWithOperation.addAllCcs(parseCc)
            emailNotifyMessageWithOperation.addAllReceivers(parseStaff)
            emailService.sendMessage(emailNotifyMessageWithOperation)
        } catch (ex: Exception) {
            logger.error("Failed process received Email message", ex)
        }
    }

    @RabbitListener(containerFactory = "rabbitListenerContainerFactory",
            bindings = [
                    QueueBinding(
                            key = ROUTE_SMS,
                            value = Queue(value = QUEUE_NOTIFY_SMS, durable = "true"),
                            exchange = Exchange(value = EXCHANGE_NOTIFY, durable = "true", delayed = "true", type = "topic"))])
    fun onReceiveSmsMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(smsNotifyMessageWithOperation.getReceivers())
            smsNotifyMessageWithOperation.clearReceivers()
            smsNotifyMessageWithOperation.addAllReceivers(parseStaff)
            smsService.sendMessage(smsNotifyMessageWithOperation)
        } catch (ex: Exception) {
            logger.error("Failed process received SMS message", ex)
        }
    }

    @RabbitListener(containerFactory = "rabbitListenerContainerFactory",
            bindings = [
                    QueueBinding(
                            key = ROUTE_WECHAT,
                            value = Queue(value = QUEUE_NOTIFY_WECHAT, durable = "true"),
                            exchange = Exchange(value = EXCHANGE_NOTIFY, durable = "true", delayed = "true", type = "topic"))])
    fun onReceiveWechatMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(wechatNotifyMessageWithOperation.getReceivers())
            wechatNotifyMessageWithOperation.clearReceivers()
            wechatNotifyMessageWithOperation.addAllReceivers(parseStaff)
            wechatService.sendMessage(wechatNotifyMessageWithOperation)
        } catch (ex: Exception) {
            logger.error("Failed process received Wechat message", ex)
        }
    }
}