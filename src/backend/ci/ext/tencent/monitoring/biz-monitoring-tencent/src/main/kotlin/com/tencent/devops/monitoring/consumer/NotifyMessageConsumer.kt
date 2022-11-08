/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.monitoring.consumer

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.common.notify.utils.NotifyDigestUtils
import com.tencent.devops.common.notify.utils.TOFConfiguration
import com.tencent.devops.common.notify.utils.TOFService
import com.tencent.devops.common.notify.utils.TOFService.Companion.EMAIL_URL
import com.tencent.devops.common.notify.utils.TOFService.Companion.RTX_URL
import com.tencent.devops.common.notify.utils.TOFService.Companion.WECHAT_URL
import com.tencent.devops.common.web.mq.EXCHANGE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.QUEUE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.ROUTE_NOTIFY_MESSAGE
import com.tencent.devops.common.web.mq.alert.Alert
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.ERROR_MONITORING_SEND_NOTIFY_FAIL
import com.tencent.devops.monitoring.dao.AlertUserDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class NotifyMessageConsumer @Autowired constructor(
    private val dslContext: DSLContext,
    private val alertUserDao: AlertUserDao,
    private val tofService: TOFService,
    private val tofConfiguration: TOFConfiguration
) {

    private val cache = HashMap<String/*service*/, Map<AlertLevel, NotifyUsers>>()

    private var lastUpdate: Long = 0

    @RabbitListener(
        bindings = [(QueueBinding(
            key = [ROUTE_NOTIFY_MESSAGE],
            value = Queue(value = QUEUE_NOTIFY_MESSAGE, durable = "true"),
            exchange = Exchange(value = EXCHANGE_NOTIFY_MESSAGE, durable = "true", delayed = "true", type = "topic")))
        ]
    )
    fun onReceiveNotifyMessage(alert: Alert) {
        try {
            logger.info("Receive notify alert message - $alert")
            val notifyUsers = getAlertUser(alert.module, alert.level)
            if (notifyUsers.isEmpty()) {
                logger.info("Empty user for the alert")
                return
            }
            val tofConfs = tofConfiguration.getConfigurations(null)
            if (tofConfs == null) {
                logger.warn("Fail to get the tof configuration")
                return
            }

            val rtxUsers = HashSet<String>()
            val emailUsers = HashSet<String>()
            val wechatUsers = HashSet<String>()

            notifyUsers.filter { it.users.isNotEmpty() && it.notifyTypes.isNotEmpty() }.forEach {
                when {
                    it.notifyTypes.contains(NotifyType.RTX) -> rtxUsers.addAll(it.users)
                    it.notifyTypes.contains(NotifyType.EMAIL) -> emailUsers.addAll(it.users)
                    it.notifyTypes.contains(NotifyType.WECHAT) -> wechatUsers.addAll(it.users)
                }
            }

            if (rtxUsers.isNotEmpty()) {
                sendRTX(tofConfs, rtxUsers, alert)
            }
            if (emailUsers.isNotEmpty()) {
                sendEmail(tofConfs, emailUsers, alert)
            }
            if (wechatUsers.isNotEmpty()) {
                sendWechat(tofConfs, wechatUsers, alert)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to send the notify message", ignored)
            throw ErrorCodeException(
                errorCode = ERROR_MONITORING_SEND_NOTIFY_FAIL,
                defaultMessage = "Fail to send the notify message"
            )
        }
    }

    private fun sendEmail(tofConfs: Map<String, String>, users: Set<String>, alert: Alert) {
        val post = generateEmailPost(users, alert)
        if (post == null) {
            logger.warn("The email body is empty")
            return
        }
        tofService.post(EMAIL_URL, post, tofConfs)
    }

    private fun sendRTX(tofConfs: Map<String, String>, users: Set<String>, alert: Alert) {
        tofService.post(RTX_URL, generateRtxNotifyPost(users, alert), tofConfs)
    }

    private fun sendWechat(tofConfs: Map<String, String>, users: Set<String>, alert: Alert) {
        tofService.post(WECHAT_URL, generateWechatNotifyPost(users, alert), tofConfs)
    }

    private fun generateWechatNotifyPost(users: Set<String>, alert: Alert): WechatNotifyPost {
        val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", alert.message)
        return WechatNotifyPost().apply {
            receiver = users.joinToString(",")
            msgInfo = alert.message
            priority = EnumNotifyPriority.HIGH.getValue()
            sender = "DevOps"
            this.contentMd5 = contentMd5
            frequencyLimit = 0
            tofSysId = ""
            fromSysId = ""
        }
    }

    private fun generateRtxNotifyPost(users: Set<String>, alert: Alert): RtxNotifyPost {
        return RtxNotifyPost().apply {
            receiver = users.joinToString(",")
            msgInfo = alert.message
            title = alert.title
            priority = EnumNotifyPriority.HIGH.getValue()
            sender = "DevOps"
            contentMd5 = NotifyDigestUtils.getMessageContentMD5("", alert.message)
            frequencyLimit = 0
            tofSysId = ""
            fromSysId = ""
        }
    }

    private fun generateEmailPost(users: Set<String>, alert: Alert): EmailNotifyPost? {
        val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", alert.message)

        val post = EmailNotifyPost()
        return post.apply {
            title = alert.title
            from = "DevOps"
            to = users.joinToString(",")
            priority = EnumNotifyPriority.HIGH.getValue()
            bodyFormat = EnumEmailFormat.HTML.getValue()
            emailType = EnumEmailType.OUTER_MAIL.getValue()
            content = alert.message
            this.contentMd5 = contentMd5
            frequencyLimit = 0
            tofSysId = ""
            fromSysId = ""
        }
    }

    private fun getAlertUser(service: String, level: AlertLevel): List<NotifyUsers> {
        if (need2RefreshCache()) {
            synchronized(this) {
                if (need2RefreshCache()) {
                    cache.clear()
                    cache.putAll(getAllNotifyUsers())
                    lastUpdate = System.currentTimeMillis()
                }
            }
        }
        val levelUsers = cache[service] ?: return emptyList()
        return levelUsers.filter {
            level.compare(it.key)
        }.map { it.value }
    }

    // Refresh every 2 minutes
    private fun need2RefreshCache(): Boolean =
        System.currentTimeMillis() - lastUpdate >= TimeUnit.MINUTES.toMillis(2)

    private fun getAllNotifyUsers(): Map<String, Map<AlertLevel, NotifyUsers>> {
        logger.info("Start to get all notify users")
        val record = alertUserDao.list(dslContext)
        if (record.isEmpty()) {
            return emptyMap()
        }
        val result = HashMap<String, HashMap<AlertLevel, NotifyUsers>>()

        record.forEach {
            val service = it.service
            val level = try {
                AlertLevel.valueOf(it.level)
            } catch (e: Exception) {
                logger.warn("Fail to translate the alert level ${it.level}", e)
                return@forEach
            }
            var levelUsers = result[service]
            if (levelUsers == null) {
                levelUsers = HashMap()
                result.put(service, levelUsers)
            }

            val notifyTypes = HashSet<NotifyType>()
            it.notifyTypes.split(",").map { it.trim() }.forEach {
                try {
                    notifyTypes.add(NotifyType.valueOf(it))
                } catch (ignored: Throwable) {
                    logger.warn("Fail to translate the notify type $it", ignored)
                }
            }
            val notifyUsers = it.users.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toHashSet()
            levelUsers.put(level, NotifyUsers(notifyUsers, notifyTypes))
        }

        logger.info("Get the notify users $result")
        return result
    }

    private data class NotifyUsers(
        val users: HashSet<String>,
        val notifyTypes: HashSet<NotifyType>
    )

    private enum class NotifyType {
        RTX,
        EMAIL,
        WECHAT
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NotifyMessageConsumer::class.java)
    }
}
