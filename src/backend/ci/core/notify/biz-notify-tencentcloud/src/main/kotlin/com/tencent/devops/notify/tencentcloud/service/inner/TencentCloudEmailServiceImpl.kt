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
package com.tencent.devops.notify.tencentcloud.service.inner

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.model.notify.tables.records.TNotifyEmailRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_EMAIL
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_TENCENT_CLOUD_EMAIL_SEND_FAIL
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.tencentcloud.config.TencentCloudConfiguration
import com.tencent.devops.notify.tencentcloud.pojo.EmailBody
import com.tencent.devops.notify.tencentcloud.pojo.EmailResponse
import com.tencent.devops.notify.tencentcloud.pojo.EmailSignatureConfig
import com.tencent.devops.notify.tencentcloud.pojo.Template
import com.tencent.devops.notify.tencentcloud.utils.TencentCloudSignatureUtil
import java.util.stream.Collectors
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
class TencentCloudEmailServiceImpl @Autowired constructor(
    private val emailNotifyDao: EmailNotifyDao,
    private val rabbitTemplate: RabbitTemplate,
    private val configuration: TencentCloudConfiguration
) : EmailService {

    private val logger = LoggerFactory.getLogger(TencentCloudEmailServiceImpl::class.java)

    companion object {
        val couldRetryError = listOf(
            "FailedOperation.HighRejectionRate"/*拒信率过高，被临时block。*/,
            "FailedOperation.FrequencyLimit" /*触发频率控制，短时间内对同一地址发送过多邮件。*/,
            "FailedOperation.TemporaryBlocked"/*因触发了某些规则导致临时Block。*/,
            "InternalError"/*内部错误。*/,
            "RequestLimitExceeded"/*请求的次数超过了频率限制。*/,
            "UnknownParameter"/*未知参数错误。*/
        )
    }

    override fun sendMqMsg(message: EmailNotifyMessage) {
        if (message.tencentCloudTemplateId == null) {
            logger.warn("TencentCloudEmailServiceImpl|tencentCloudTemplateId is empty ,return.")
            return
        }
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_EMAIL, message)
    }

    override fun sendMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation) {
        val emailNotifyPost = generateEmailNotifyPost(emailNotifyMessageWithOperation)

        if (emailNotifyPost == null) {
            logger.warn("EmailNotifyPost is empty after being processed: $emailNotifyMessageWithOperation")
            return
        }
        val payload = JsonUtil.toJson(emailNotifyPost, false)
        val retryCount = emailNotifyMessageWithOperation.retryCount
        val id = emailNotifyMessageWithOperation.id ?: UUIDUtil.generate()
        val emailSignatureConfig = EmailSignatureConfig(
            payload = payload,
            secretId = configuration.secretId,
            secretKey = configuration.secretKey,
            region = configuration.emailRegion
        )
        kotlin.runCatching {
            doSend(emailSignatureConfig.url, payload, TencentCloudSignatureUtil.signature(emailSignatureConfig))
        }.fold(
            {
                emailNotifyDao.insertOrUpdateEmailNotifyRecord(
                    success = true,
                    source = emailNotifyMessageWithOperation.source,
                    id = id,
                    retryCount = retryCount,
                    lastErrorMessage = null,
                    to = emailNotifyPost.destination.joinToString(),
                    cc = null,
                    bcc = null,
                    sender = emailNotifyPost.fromEmailAddress,
                    title = emailNotifyPost.subject,
                    body = emailNotifyMessageWithOperation.body,
                    type = EnumEmailType.OUTER_MAIL.getValue(),
                    format = EnumEmailFormat.HTML.getValue(),
                    priority = emailNotifyMessageWithOperation.priority.getValue().toInt(),
                    contentMd5 = id,
                    frequencyLimit = null,
                    tofSysId = null,
                    fromSysId = null
                )
            },
            {
                // 写入失败记录
                emailNotifyDao.insertOrUpdateEmailNotifyRecord(
                    success = false,
                    source = emailNotifyMessageWithOperation.source,
                    id = id,
                    retryCount = retryCount,
                    lastErrorMessage = it.message,
                    to = emailNotifyPost.destination.joinToString(),
                    cc = null,
                    bcc = null,
                    sender = emailNotifyPost.fromEmailAddress,
                    title = emailNotifyPost.subject,
                    body = emailNotifyMessageWithOperation.body,
                    type = EnumEmailType.OUTER_MAIL.getValue(),
                    format = EnumEmailFormat.HTML.getValue(),
                    priority = emailNotifyMessageWithOperation.priority.getValue().toInt(),
                    contentMd5 = id,
                    frequencyLimit = null,
                    tofSysId = null,
                    fromSysId = null
                )
                // 针对指定错误情况（maybe 限流）进行重试
                if (it is ErrorCodeException && retryCount < 3 && couldRetryError(it)) {
                    logger.warn("TENCENT_CLOUD_SEND_EMAIL_FAIL|RETRY|${it.message}|$retryCount")
                    // 开始重试
                    reSendMessage(
                        message = emailNotifyMessageWithOperation,
                        source = emailNotifyMessageWithOperation.source,
                        retryCount = retryCount + 1,
                        id = id
                    )
                } else {
                    logger.warn("TENCENT_CLOUD_SEND_EMAIL_FAIL|NOT_RETRY|${it.message}")
                }
            }
        )
    }

    private fun couldRetryError(error: ErrorCodeException): Boolean {
        error.params?.firstOrNull()?.let {
            if (it in couldRetryError) return true
        }
        return false
    }

    fun doSend(
        url: String,
        body: String,
        headers: Map<String, String>
    ): EmailResponse {
        val response = OkhttpUtils.doPost(url = url, jsonParam = body, headers = headers)
        val responseContent = response.body!!.string()
        if (!response.isSuccessful) {
            throw RemoteServiceException(
                httpStatus = response.code,
                responseContent = responseContent,
                errorMessage = "send tencent cloud email message failed"
            )
        }
        return JsonUtil.to(responseContent, object : TypeReference<EmailResponse>() {}).also {
            if (it.response.error != null) {
                throw ErrorCodeException(
                    errorCode = ERROR_NOTIFY_TENCENT_CLOUD_EMAIL_SEND_FAIL,
                    params = arrayOf(it.response.error.code)
                )
            }
        }
    }

    private fun reSendMessage(
        message: EmailNotifyMessageWithOperation,
        source: EnumNotifySource,
        retryCount: Int,
        id: String
    ) {
        message.apply {
            this.id = id
            this.retryCount = retryCount
            this.source = source
        }
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_EMAIL, message) { msg ->
            var delayTime = 0
            when (retryCount) {
                1 -> delayTime = 30000
                2 -> delayTime = 120000
                3 -> delayTime = 300000
            }
            if (delayTime > 0) {
                msg.messageProperties.setHeader("x-delay", delayTime)
            }
            msg
        }
    }

    private fun generateEmailNotifyPost(emailNotifyMessage: EmailNotifyMessage): EmailBody? {
        if (emailNotifyMessage.getReceivers().isEmpty()) {
            return null
        }

        return EmailBody(
            destination = emailNotifyMessage.getReceivers().toList(),
            fromEmailAddress = configuration.emailSender,
            subject = emailNotifyMessage.title,
            template = Template(
                templateID = emailNotifyMessage.tencentCloudTemplateId,
                templateData = emailNotifyMessage.variables?.let { JsonUtil.toJson(it, false) }
            )
        )
    }

    override fun listByCreatedTime(
        page: Int,
        pageSize: Int,
        success: Boolean?,
        fromSysId: String?,
        createdTimeSortOrder: String?
    ): NotificationResponseWithPage<EmailNotifyMessageWithOperation> {
        val count = emailNotifyDao.count(success, fromSysId)
        val result: List<NotificationResponse<EmailNotifyMessageWithOperation>> = if (count == 0) {
            listOf()
        } else {
            val emailRecords = emailNotifyDao.list(page, pageSize, success, fromSysId, createdTimeSortOrder)
            emailRecords.stream().map(this::parseFromTNotifyEmailToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(count, page, pageSize, result)
    }

    private fun parseFromTNotifyEmailToResponse(
        record: TNotifyEmailRecord
    ): NotificationResponse<EmailNotifyMessageWithOperation> {

        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.to.isNullOrEmpty()) {
            receivers.addAll(record.to.split(";"))
        }
        val cc: MutableSet<String> = mutableSetOf()
        if (!record.cc.isNullOrEmpty()) {
            cc.addAll(record.cc.split(";"))
        }
        val bcc: MutableSet<String> = mutableSetOf()
        if (!record.bcc.isNullOrEmpty()) {
            bcc.addAll(record.bcc.split(";"))
        }

        val message = EmailNotifyMessageWithOperation()
        message.apply {
            frequencyLimit = record.frequencyLimit
            fromSysId = record.fromSysId
            tofSysId = record.tofSysId
            format = EnumEmailFormat.parse(record.format)
            type = EnumEmailType.parse(record.type)
            body = record.body
            sender = record.sender
            title = record.title
            priority = EnumNotifyPriority.parse(record.priority.toString())
            source = EnumNotifySource.parseName(record.source)
            retryCount = record.retryCount
            lastError = record.lastError
            addAllReceivers(receivers)
            addAllCcs(cc)
            addAllBccs(bcc)
        }

        return NotificationResponse(
            id = record.id, success = record.success,
            createdTime = if (record.createdTime == null) {
                null
            } else {
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.createdTime)
            },
            updatedTime = if (record.updatedTime == null) {
                null
            } else {
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.updatedTime)
            },
            contentMD5 = record.contentMd5, notificationMessage = message
        )
    }
}
