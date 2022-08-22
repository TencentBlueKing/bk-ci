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

package com.tencent.devops.notify.service.inner

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_WEWORK
import com.tencent.devops.notify.dao.WeworkNotifyDao
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.WeweokRobotBaseMessage
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotMarkdownMessage
import com.tencent.devops.notify.pojo.WeworkRobotSingleTextMessage
import com.tencent.devops.notify.pojo.WeworkSendMessageResp
import com.tencent.devops.notify.pojo.WeworkRobotContentMessage
import com.tencent.devops.notify.service.WeworkService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["weworkChannel"], havingValue = "weworkRobot")
class WeworkRobotServiceImpl @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val weworkNotifyDao: WeworkNotifyDao
) : WeworkService {
    override fun sendMqMsg(message: WeworkNotifyMessageWithOperation) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_WEWORK, message)
    }

    @Value("\${wework.apiUrl:https://qyapi.weixin.qq.com}")
    lateinit var weworkHost: String

    @Value("\${wework.robotKey}")
    lateinit var robotKey: String

    override fun sendMediaMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        return
    }

    override fun sendTextMessage(weworkNotifyTextMessage: WeworkNotifyTextMessage) {
        val sendRequest = mutableListOf<WeweokRobotBaseMessage>()
        val attachments = weworkNotifyTextMessage.attachments
        val content = if (checkMessageSize(weworkNotifyTextMessage.message)) {
            weworkNotifyTextMessage.message.replace("\\n", "\n")
        } else {
            weworkNotifyTextMessage.message.replace("\\n", "\n").substring(0, WEWORK_MAX_SIZE - 1) +
                "...(消息长度超$WEWORK_MAX_SIZE 已截断,请控制消息长度)"
        }
        weworkNotifyTextMessage.message = content
        when (weworkNotifyTextMessage.receiverType) {
            WeworkReceiverType.group -> {
                return
            }
            WeworkReceiverType.single -> {
                weworkNotifyTextMessage.receivers.forEach {
                    if (weworkNotifyTextMessage.textType == WeworkTextType.text) {
                        sendRequest.add(
                            WeworkRobotSingleTextMessage(
                                chatid = it,
                                text = WeworkRobotContentMessage(
                                    content = content,
                                    mentionedList = null,
                                    mentionedMobileList = null
                                ),
                                visibleToUser = null,
                                postId = null
                            )
                        )
                    } else if (weworkNotifyTextMessage.textType == WeworkTextType.markdown) {
                        sendRequest.add(
                            WeworkRobotMarkdownMessage(
                                chatid = it,
                                markdown = WeworkRobotContentMessage(
                                    content = content,
                                    mentionedList = null,
                                    mentionedMobileList = null,
                                    attachments = attachments
                                ),
                                postId = null
                            )
                        )
                    }
                }
            }
        }
        val errMsg = doSendRequest(sendRequest)
        if (errMsg.isNotEmpty()) {
            logger.warn(
                "send wework robot message fail : weworkNotifyTextMessage = $weworkNotifyTextMessage " +
                    "| errorMessage :$errMsg"
            )
            saveResult(weworkNotifyTextMessage.receivers, "type:${weworkNotifyTextMessage.message}\n", false, errMsg)
            throw RemoteServiceException("send wework robot message fail")
        } else {
            logger.info("send wework robot message success : $weworkNotifyTextMessage")
            saveResult(weworkNotifyTextMessage.receivers, "type:${weworkNotifyTextMessage.message}\n", true, null)
        }
    }

    private fun doSendRequest(requestBodies: List<WeweokRobotBaseMessage>): String {
        if (requestBodies.isEmpty()) {
            throw OperationException("no message to send")
        }
        var errMsg = ""
        requestBodies.map {
            try {
                send(it)
            } catch (e: RemoteServiceException) {
                errMsg += e
            }
        }
        return errMsg
    }

    private fun send(weworkMessage: WeweokRobotBaseMessage) {
        val url = buildUrl("$weworkHost/cgi-bin/webhook/send?key=$robotKey")
        val requestBody = JsonUtil.toJson(weworkMessage)
        OkhttpUtils.doPost(url, requestBody).use {
            val responseBody = it.body()?.string() ?: ""
            logger.info(
                "sendTextMessage : chatid = ${weworkMessage.chatid} | " +
                    "responseBody = $responseBody"
            )
            val sendMessageResp = JsonUtil.to(responseBody, jacksonTypeRef<WeworkSendMessageResp>())
            if (!it.isSuccessful || 0 != sendMessageResp.errCode) {
                throw RemoteServiceException(
                    httpStatus = it.code(),
                    responseContent = responseBody,
                    errorMessage = "send wework robot message failed：errMsg = ${sendMessageResp.errMsg}" +
                        "|chatid = ${weworkMessage.chatid} ;",
                    errorCode = sendMessageResp.errCode
                )
            }
        }
    }

    private fun buildUrl(url: String): String {
        return if (url.startsWith("http")) url else "https://$url"
    }

    private fun saveResult(receivers: Collection<String>, body: String, success: Boolean, errMsg: String?) {
        weworkNotifyDao.insertOrUpdateWeworkNotifyRecord(
            success = success,
            lastErrorMessage = errMsg,
            receivers = receivers.joinToString(","),
            body = body
        )
    }

    private fun checkMessageSize(message: String): Boolean {
        if (message.length < WEWORK_MAX_SIZE) {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(WeworkRobotServiceImpl::class.java)
        const val WEWORK_MAX_SIZE = 4000
    }
}
