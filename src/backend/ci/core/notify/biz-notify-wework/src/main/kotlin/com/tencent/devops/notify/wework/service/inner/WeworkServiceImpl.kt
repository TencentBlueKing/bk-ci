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
package com.tencent.devops.notify.wework.service.inner

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.notify.enums.WeworkMediaType
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_WEWORK
import com.tencent.devops.notify.dao.WeworkNotifyDao
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.service.WeworkService
import com.tencent.devops.notify.wework.config.WeworkConfiguration
import com.tencent.devops.notify.wework.pojo.AbstractSendMessageRequest
import com.tencent.devops.notify.wework.pojo.AccessTokenResp
import com.tencent.devops.notify.wework.pojo.FileSendMessageRequest
import com.tencent.devops.notify.wework.pojo.ImageSendMessageRequest
import com.tencent.devops.notify.wework.pojo.MarkdownSendMessageRequest
import com.tencent.devops.notify.wework.pojo.SendMessageResp
import com.tencent.devops.notify.wework.pojo.TextMessageContent
import com.tencent.devops.notify.wework.pojo.TextSendMessageRequest
import com.tencent.devops.notify.wework.pojo.UploadMediaResp
import com.tencent.devops.notify.wework.pojo.VideoSendMessageRequest
import com.tencent.devops.notify.wework.pojo.VoiceSendMessageRequest
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Optional
import java.util.concurrent.TimeUnit

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["weworkChannel"], havingValue = "weworkAgent")
@Suppress("TooManyFunctions", "LongMethod", "LongParameterList")
class WeworkServiceImpl(
    private val weWorkConfiguration: WeworkConfiguration,
    private val weworkNotifyDao: WeworkNotifyDao,
    private val rabbitTemplate: RabbitTemplate,
    private val redisOperation: RedisOperation
) : WeworkService {

    companion object {
        private const val maxUserNum = 1000
        private const val maxGroupNum = 100
        private const val maxSeconds = 600
        private val LOG = LoggerFactory.getLogger(WeworkServiceImpl::class.java.name)
        private const val WEWORK_ACCESS_TOKEN_KEY = "notify_wework_access_token_key"
    }

    override fun sendMqMsg(message: WeworkNotifyMessageWithOperation) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_WEWORK, message)
    }

    override fun sendMediaMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        with(weworkNotifyMediaMessage) {
            kotlin.runCatching {
                val (toUser, toParty) =
                    receivers.chunkedReceivers(receiverType)
                val mediaId = uploadMedia(mediaType, mediaInputStream, mediaName)
                val requestBodies =
                    (toUser.map { getSendMessageRequest(mediaType = mediaType, mediaId = mediaId, toUser = it) } +
                            toParty.map {
                                getSendMessageRequest(
                                    mediaType = mediaType,
                                    mediaId = mediaId,
                                    toParty = it
                                )
                            })
                        .filter { it.isPresent }
                doSendRequest(requestBodies)
            }.fold({
                LOG.info("send message success, $weworkNotifyMediaMessage")
                saveResult(receivers, "media:[type:$mediaType,name:$mediaName]", true, null)
            }, {
                LOG.warn("send message failed, $weworkNotifyMediaMessage", it)
                saveResult(receivers, "media:[type:$mediaType,name:$mediaName]", false, it.message)
            })
        }
    }

    override fun sendTextMessage(weworkNotifyTextMessage: WeworkNotifyTextMessage): Boolean {
        with(weworkNotifyTextMessage) {
            kotlin.runCatching {
                val (toUser, toParty) =
                    receivers.chunkedReceivers(receiverType)
                val requestBodies = (toUser.map {
                    getSendMessageRequest(
                        content = message,
                        toUser = it,
                        textType = WeworkTextType.text
                    )
                } +
                        toParty.map {
                            getSendMessageRequest(
                                content = message,
                                toUser = it,
                                textType = WeworkTextType.text
                            )
                        }).filter { it.isPresent }
                doSendRequest(requestBodies)
            }.fold({
                LOG.info("send message success, $weworkNotifyTextMessage")
                saveResult(receivers, "type:$textType\n$message", true, null)
            }, {
                LOG.warn("send message failed, $weworkNotifyTextMessage", it)
                saveResult(receivers, "type:$textType\n$message", false, it.message)
            })
        }
        return true
    }

    private fun doSendRequest(requestBodies: List<Optional<AbstractSendMessageRequest>>) {
        if (requestBodies.isEmpty()) {
            throw OperationException("no message to send")
        }
        val errMsg = requestBodies.asSequence().map { it.get() }.map {
            send(it)
        }.filter { it.isPresent }.joinToString(", ")
        if (errMsg.isNotBlank()) {
            throw RemoteServiceException(errMsg)
        }
    }

    private fun saveResult(receivers: Collection<String>, body: String, success: Boolean, errMsg: String?) {
        weworkNotifyDao.insertOrUpdateWeworkNotifyRecord(
            success = success,
            lastErrorMessage = errMsg,
            receivers = receivers.joinToString(","),
            body = body
        )
    }

    private fun send(abstractSendMessageRequest: AbstractSendMessageRequest): Optional<Throwable> {
        val url = buildUrl("${weWorkConfiguration.apiUrl}/cgi-bin/message/send?access_token=${getAccessToken()}")
        val requestBody = JsonUtil.toJson(abstractSendMessageRequest)
        return OkhttpUtils.doPost(url, requestBody).use {
            val responseBody = it.body?.string() ?: ""
            kotlin.runCatching {
                val sendMessageResp = JsonUtil.to(responseBody, jacksonTypeRef<SendMessageResp>())
                if (!it.isSuccessful || 0 != sendMessageResp.errCode) {
                    throw RemoteServiceException(
                        httpStatus = it.code,
                        responseContent = responseBody,
                        errorMessage = "send wework message failed",
                        errorCode = sendMessageResp.errCode
                    )
                }
            }.fold({ Optional.empty() }, { e ->
                LOG.warn("${it.request}|send wework message failed, $responseBody")
                Optional.of(e)
            })
        }
    }

    /**
     *  非文本消息时，默认 mediaId 不为空
     *  文本消息时，默认 content 不为空
     */
    @SuppressWarnings("ComplexMethod")
    private fun getSendMessageRequest(
        mediaType: WeworkMediaType? = null,
        mediaId: String? = null,
        textType: WeworkTextType? = null,
        content: String? = null,
        toUser: String = "",
        toParty: String = ""
    ): Optional<AbstractSendMessageRequest> {
        val agentId =
            weWorkConfiguration.agentId.toIntOrNull() ?: throw OperationException("Wework agent id is invalid")
        return if (mediaType != null && mediaId != null) {
            when (mediaType) {
                WeworkMediaType.file -> {
                    Optional.of<AbstractSendMessageRequest>(
                        FileSendMessageRequest(
                            agentId = agentId,
                            duplicateCheckInterval = weWorkConfiguration.duplicateCheckInterval?.toIntOrNull(),
                            enableDuplicateCheck = weWorkConfiguration.enableDuplicateCheck?.toIntOrNull(),
                            safe = weWorkConfiguration.safe?.toIntOrNull(),
                            toParty = toParty,
                            toTag = "",
                            toUser = toUser,
                            file = AbstractSendMessageRequest.MediaMessageContent(mediaId)
                        )
                    )
                }

                WeworkMediaType.image -> {
                    Optional.of<AbstractSendMessageRequest>(
                        ImageSendMessageRequest(
                            agentId = agentId,
                            duplicateCheckInterval = weWorkConfiguration.duplicateCheckInterval?.toIntOrNull(),
                            enableDuplicateCheck = weWorkConfiguration.enableDuplicateCheck?.toIntOrNull(),
                            safe = weWorkConfiguration.safe?.toIntOrNull(),
                            toParty = toParty,
                            toTag = "",
                            toUser = toUser,
                            image = AbstractSendMessageRequest.MediaMessageContent(mediaId)
                        )
                    )
                }

                WeworkMediaType.video -> {
                    Optional.of<AbstractSendMessageRequest>(
                        VideoSendMessageRequest(
                            agentId = agentId,
                            duplicateCheckInterval = weWorkConfiguration.duplicateCheckInterval?.toIntOrNull(),
                            enableDuplicateCheck = weWorkConfiguration.enableDuplicateCheck?.toIntOrNull(),
                            safe = weWorkConfiguration.safe?.toIntOrNull(),
                            toParty = toParty,
                            toTag = "",
                            toUser = toUser,
                            video = AbstractSendMessageRequest.MediaMessageContent(mediaId)
                        )
                    )
                }

                WeworkMediaType.voice -> {
                    Optional.of<AbstractSendMessageRequest>(
                        VoiceSendMessageRequest(
                            agentId = agentId,
                            duplicateCheckInterval = weWorkConfiguration.duplicateCheckInterval?.toIntOrNull(),
                            enableDuplicateCheck = weWorkConfiguration.enableDuplicateCheck?.toIntOrNull(),
                            safe = weWorkConfiguration.safe?.toIntOrNull(),
                            toParty = toParty,
                            toTag = "",
                            toUser = toUser,
                            voice = AbstractSendMessageRequest.MediaMessageContent(mediaId)
                        )
                    )
                }
            }
        } else if (textType != null && content != null) {
            when (textType) {
                WeworkTextType.text -> Optional.of<AbstractSendMessageRequest>(
                    TextSendMessageRequest(
                        agentId = agentId,
                        duplicateCheckInterval = weWorkConfiguration.duplicateCheckInterval?.toIntOrNull(),
                        enableDuplicateCheck = weWorkConfiguration.enableDuplicateCheck?.toIntOrNull(),
                        enableIdTrans = weWorkConfiguration.enableIdTrans?.toIntOrNull(),
                        safe = weWorkConfiguration.safe?.toIntOrNull(),
                        toParty = toParty,
                        toTag = "",
                        toUser = toUser,
                        text = TextMessageContent(content)
                    )
                )

                WeworkTextType.markdown -> Optional.of<AbstractSendMessageRequest>(
                    MarkdownSendMessageRequest(
                        agentId = agentId,
                        duplicateCheckInterval = weWorkConfiguration.duplicateCheckInterval?.toIntOrNull(),
                        enableDuplicateCheck = weWorkConfiguration.enableDuplicateCheck?.toIntOrNull(),
                        safe = weWorkConfiguration.safe?.toIntOrNull(),
                        toParty = toParty,
                        toTag = "",
                        toUser = toUser,
                        markdown = TextMessageContent(content)
                    )
                )
            }
        } else {
            Optional.empty()
        }
    }

    /**
     *  [发送应用消息](https://work.weixin.qq.com/api/doc/90000/90135/90236#%E6%96%87%E6%9C%AC%E6%B6%88%E6%81%AF)
     *  指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）
     *  指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。
     *  @return first 为 toUser, second 为 toParty
     */
    private fun Collection<String>.chunkedReceivers(
        receiverType: WeworkReceiverType
    ): Pair<List<String>, List<String>> {
        return when (receiverType) {
            WeworkReceiverType.single -> {
                val toUser = HashSet(this).chunked(maxUserNum)
                    .map { it.joinToString(separator = "|") }
                Pair(toUser, emptyList())
            }

            WeworkReceiverType.group -> {
                val toParty = HashSet(this).chunked(maxGroupNum).map { it.joinToString(separator = "|") }
                Pair(emptyList(), toParty)
            }
        }
    }

    private fun uploadMedia(mediaType: WeworkMediaType, inputStream: InputStream, mediaName: String): String {
        val tempDirectory = weWorkConfiguration.tempDirectory
        val tempFile = Files.createTempFile(Paths.get(tempDirectory), mediaName, "")
        try {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            val token = getAccessToken()
            val url =
                buildUrl("${weWorkConfiguration.apiUrl}/cgi-bin/media/upload?access_token=$token&type=$mediaType")
            OkhttpUtils.uploadFile(
                url = url,
                uploadFile = tempFile.toFile(),
                fileFieldName = "media",
                fileName = mediaName
            ).use {
                val responseBody = it.body?.string() ?: "{}"
                return kotlin.runCatching {
                    val uploadMediaResp = JsonUtil.to(responseBody, jacksonTypeRef<UploadMediaResp>())
                    val mediaId = uploadMediaResp.mediaId
                    if (!it.isSuccessful || mediaId.isNullOrBlank()) {
                        LOG.warn("${it.request}|upload media($mediaName) to wework failed, $responseBody")
                        throw RemoteServiceException(
                            httpStatus = it.code,
                            responseContent = responseBody,
                            errorMessage = "upload media($mediaName) to wework failed",
                            errorCode = uploadMediaResp.errCode
                        )
                    }
                    mediaId
                }.onFailure { _ ->
                    LOG.warn("${it.request}|upload media($mediaName) to wework failed, $responseBody")
                }.getOrThrow()
            }
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    fun getAccessToken(): String {
        val now = System.currentTimeMillis()
        // 获取缓存中有效的 token，有申请频率限制
        val accessTokenCache = redisOperation.get(WEWORK_ACCESS_TOKEN_KEY)
            ?.let { json ->
                JsonUtil.to(json, jacksonTypeRef<AccessTokenCache>())
                    .takeIf { (TimeUnit.SECONDS.toMillis(it.expiresIn.toLong()) + now) < it.timestamp }
            }
        if (accessTokenCache != null) {
            return accessTokenCache.accessToken
        } else {
            redisOperation.delete(WEWORK_ACCESS_TOKEN_KEY)
        }

        OkhttpUtils.doGet(
            buildUrl(
                "${weWorkConfiguration.apiUrl}/cgi-bin" +
                        "/gettoken?corpId=${weWorkConfiguration.corpId}&corpSecret=${weWorkConfiguration.corpSecret}"
            )
        ).use {
            val responseBody = it.body?.string() ?: "{}"
            return kotlin.runCatching {
                val accessTokenResp = JsonUtil.to(responseBody, jacksonTypeRef<AccessTokenResp>())
                if (!it.isSuccessful && accessTokenResp.isOk()) {
                    LOG.warn("${it.request}|failed to get wework access token: $responseBody")
                    throw RemoteServiceException(
                        httpStatus = it.code,
                        responseContent = responseBody,
                        errorMessage = "failed to get wework access token: $responseBody",
                        errorCode = accessTokenResp.errCode
                    )
                }
                val accessToken = accessTokenResp.accessToken!!
                val expiresIn = accessTokenResp.expiresIn
                if (expiresIn != null && expiresIn > maxSeconds) {
                    // 提前 10 分钟过期，防止在操作过程中过期
                    redisOperation.set(
                        key = WEWORK_ACCESS_TOKEN_KEY,
                        value = JsonUtil.toJson(AccessTokenCache(accessToken, expiresIn, now)),
                        expiredInSecond = expiresIn.toLong() - maxSeconds
                    )
                }
                accessToken
            }.onFailure { _ ->
                LOG.warn("${it.request}|failed to get wework access token: $responseBody")
            }.getOrThrow()
        }
    }

    private fun buildUrl(url: String): String {
        return if (url.startsWith("http")) url else "https://$url"
    }

    private data class AccessTokenCache(
        val accessToken: String,
        val expiresIn: Int, // 秒
        val timestamp: Long
    )
}
