package com.tencent.devops.common.web.mq.alert

import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.web.mq.EXCHANGE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.ROUTE_NOTIFY_MESSAGE
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

object AlertUtils {

    fun doAlert(level: AlertLevel, title: String, message: String) {
        val serviceName = SpringContextUtil.getBean(Profile::class.java).getApplicationName() ?: ""
        doAlert(serviceName, level, title, message)
    }

    fun doAlert(module: String, level: AlertLevel, title: String, message: String) {
        try {
            val alert = Alert(module, level, title, message)
            logger.info("Start to send the notify $alert")
            val rabbitTemplate = SpringContextUtil.getBean(RabbitTemplate::class.java)
            rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY_MESSAGE, ROUTE_NOTIFY_MESSAGE, alert)
        } catch (t: Throwable) {
            logger.warn("Fail to send the notify alert (level=$level, title=$title, message=$message)", t)
        }
    }

    private val logger = LoggerFactory.getLogger(AlertUtils::class.java)
}