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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.notify.enums.WeworkMediaType
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.constant.NotifyMessageCode.BK_CONTROL_MESSAGE_LENGTH
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_SEND_MEDIA_MESSAGE_FAIL
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_UNSUPPORTED_MEDIA_TYPE
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_WEWORK_GROUP_NOT_SUPPORTED
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_WEWORK_RECEIVERS_EMPTY
import com.tencent.devops.notify.dao.WeworkNotifyDao
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.ImageContent
import com.tencent.devops.notify.pojo.MediaContent
import com.tencent.devops.notify.pojo.WeweokRobotBaseMessage
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotContentMessage
import com.tencent.devops.notify.pojo.WeworkRobotMarkdownMessage
import com.tencent.devops.notify.pojo.WeworkRobotSingleFileMessage
import com.tencent.devops.notify.pojo.WeworkRobotSingleImageMessage
import com.tencent.devops.notify.pojo.WeworkRobotSingleTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotUploadResponse
import com.tencent.devops.notify.pojo.WeworkSendMessageResp
import com.tencent.devops.notify.service.WeworkService
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.Optional
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["weworkChannel"], havingValue = "weworkRobot")
class WeworkRobotServiceImpl @Autowired constructor(
    private val streamBridge: StreamBridge,
    private val weworkNotifyDao: WeworkNotifyDao
) : WeworkService {
    override fun sendMqMsg(message: WeworkNotifyMessageWithOperation) {
        message.sendTo(streamBridge)
    }

    @Value("\${wework.apiUrl:https://qyapi.weixin.qq.com}")
    lateinit var weworkHost: String

    @Value("\${wework.robotKey}")
    lateinit var robotKey: String

    override fun sendMediaMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        weworkNotifyMediaMessage.mediaInputStream.use {
            validateMediaMessage(weworkNotifyMediaMessage)
            dispatchMediaMessage(weworkNotifyMediaMessage)
        }
    }

    /**
     * 校验媒体消息参数
     */
    private fun validateMediaMessage(message: WeworkNotifyMediaMessage) {
        if (message.receivers.isEmpty()) {
            throw ErrorCodeException(errorCode = ERROR_NOTIFY_WEWORK_RECEIVERS_EMPTY)
        }
        if (message.receiverType == WeworkReceiverType.group) {
            throw ErrorCodeException(errorCode = ERROR_NOTIFY_WEWORK_GROUP_NOT_SUPPORTED)
        }
    }

    /**
     * 分发媒体消息到指定会话
     */
    private fun dispatchMediaMessage(message: WeworkNotifyMediaMessage) {
        val body = "mediaType:${message.mediaType}, mediaName:${message.mediaName}"
        try {
            sendMediaMessageByChatId(message)
            logger.info("send media message success, $message")
            saveResult(receivers = message.receivers, body = body, success = true, errMsg = null)
        } catch (e: Throwable) {
            handleMediaMessageError(message = message, body = body, error = e)
        }
    }

    /**
     * 处理媒体消息发送异常
     */
    private fun handleMediaMessageError(message: WeworkNotifyMediaMessage, body: String, error: Throwable): Nothing {
        logger.warn("send media message fail, $message", error)
        saveResult(receivers = message.receivers, body = body, success = false, errMsg = error.message)
        if (error is ErrorCodeException) throw error
        throw ErrorCodeException(errorCode = ERROR_NOTIFY_SEND_MEDIA_MESSAGE_FAIL)
    }

    /**
     * 通过chatid发送多媒体消息到指定会话
     */
    private fun sendMediaMessageByChatId(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        val mediaType = weworkNotifyMediaMessage.mediaType
        val sendRequest = mutableListOf<WeweokRobotBaseMessage>()
        val inputStream = weworkNotifyMediaMessage.mediaInputStream

        when (mediaType) {
            WeworkMediaType.image -> {
                // 图片消息：流式计算base64和md5，避免内存溢出
                val (base64Content, md5Hash) = processImageStream(inputStream)
                weworkNotifyMediaMessage.receivers.forEach { chatid ->
                    sendRequest.add(
                        WeworkRobotSingleImageMessage(
                            chatid = chatid,
                            postId = null,
                            image = ImageContent(base64 = base64Content, md5 = md5Hash),
                            visibleToUser = null
                        )
                    )
                }
            }
            WeworkMediaType.file -> {
                // 文件消息：流式写入临时文件后上传，避免内存溢出
                val mediaId = uploadMediaToRobotFromStream(
                    inputStream = inputStream,
                    fileName = weworkNotifyMediaMessage.mediaName,
                    mediaType = mediaType.name,
                    key = robotKey
                )
                weworkNotifyMediaMessage.receivers.forEach { chatid ->
                    sendRequest.add(
                        WeworkRobotSingleFileMessage(
                            chatid = chatid,
                            postId = null,
                            file = MediaContent(mediaId = mediaId),
                            visibleToUser = null
                        )
                    )
                }
            }
            else -> {
                logger.warn("unsupported media type for chatid send: $mediaType")
                throw ErrorCodeException(
                    errorCode = ERROR_NOTIFY_UNSUPPORTED_MEDIA_TYPE,
                    params = arrayOf(mediaType.name)
                )
            }
        }

        // 发送请求
        doSendRequest(sendRequest)
    }

    /**
     * 流式处理图片：同时计算base64和md5，避免内存溢出
     */
    private fun processImageStream(inputStream: InputStream): Pair<String, String> {
        val md = MessageDigest.getInstance("MD5")
        val digestInputStream = DigestInputStream(inputStream, md)
        val base64Content = Base64.getEncoder().encodeToString(digestInputStream.readBytes())
        val md5Hash = md.digest().joinToString("") { "%02x".format(it) }
        return Pair(base64Content, md5Hash)
    }

    /**
     * 流式上传媒体文件到企业微信机器人，避免内存溢出
     */
    private fun uploadMediaToRobotFromStream(
        inputStream: InputStream,
        fileName: String,
        mediaType: String,
        key: String
    ): String {
        val tempDir = Files.createTempDirectory(WEWORK_UPLOAD_TEMP_PREFIX).toFile()
        val tempFile = File(tempDir, fileName)
        try {
            // 流式写入临时文件，避免全量加载到内存
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            // 构建multipart请求
            val url = buildUrl("$weworkHost/cgi-bin/webhook/upload_media?key=$key&type=$mediaType")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "media",
                    fileName,
                    tempFile.asRequestBody(mediaType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // 发送请求
            OkhttpUtils.doHttp(request).use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw RemoteServiceException(
                        httpStatus = response.code,
                        responseContent = responseBody,
                        errorMessage = "upload media to wework robot failed, httpCode=${response.code}"
                    )
                }

                val uploadResponse = JsonUtil.to(responseBody, WeworkRobotUploadResponse::class.java)
                if (uploadResponse.errCode != 0) {
                    logger.warn("upload media failed, errCode=${uploadResponse.errCode}, errMsg=${uploadResponse.errMsg}")
                    throw RemoteServiceException(
                        errorMessage = "upload media to wework robot failed: ${uploadResponse.errMsg}",
                        errorCode = uploadResponse.errCode
                    )
                }

                return uploadResponse.mediaId
                    ?: throw RemoteServiceException(errorMessage = "upload media response missing media_id")
            }
        } finally {
            // 清理临时文件和临时目录
            kotlin.runCatching {
                if (tempDir.exists() && !tempDir.deleteRecursively()) {
                    logger.warn("failed to delete temp dir: ${tempDir.absolutePath}")
                }
            }.onFailure { e -> logger.warn("failed to clean temp upload files", e) }
        }
    }

    override fun sendTextMessage(weworkNotifyTextMessage: WeworkNotifyTextMessage): Boolean {
        if (weworkNotifyTextMessage.receivers.isEmpty()) {
            throw ErrorCodeException(errorCode = ERROR_NOTIFY_WEWORK_RECEIVERS_EMPTY)
        }
        val sendRequest = mutableListOf<WeweokRobotBaseMessage>()
        val attachments = weworkNotifyTextMessage.attachments
        val content = if (checkMessageSize(weworkNotifyTextMessage.message)) {
            weworkNotifyTextMessage.message.replace("\\n", "\n")
        } else {
            weworkNotifyTextMessage.message.replace("\\n", "\n").substring(0, WEWORK_MAX_SIZE - 1) +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_CONTROL_MESSAGE_LENGTH,
                        params = arrayOf(WEWORK_MAX_SIZE.toString())
                    )
        }
        weworkNotifyTextMessage.message = content
        when (weworkNotifyTextMessage.receiverType) {
            WeworkReceiverType.group -> {
                throw ErrorCodeException(errorCode = ERROR_NOTIFY_WEWORK_GROUP_NOT_SUPPORTED)
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
        return try {
            doSendRequest(sendRequest)
            logger.info("send message success, $weworkNotifyTextMessage")
            saveResult(weworkNotifyTextMessage.receivers, "type:${weworkNotifyTextMessage.message}\n", true, null)
            true
        } catch (e: Exception) {
            logger.warn("send message fail, $weworkNotifyTextMessage")
            saveResult(weworkNotifyTextMessage.receivers, "type:${weworkNotifyTextMessage.message}\n", false, e.message)
            false
        }
    }

    private fun doSendRequest(requestBodies: List<WeweokRobotBaseMessage>) {
        if (requestBodies.isEmpty()) {
            throw OperationException("no message to send")
        }
        val errMsg = requestBodies.asSequence().map {
            send(it)
        }.filter { it.isPresent }.map { it.get().message }.joinToString(", ")
        if (errMsg.isNotBlank()) {
            throw RemoteServiceException(errMsg)
        }
    }

    private fun send(weworkMessage: WeweokRobotBaseMessage): Optional<Throwable> {
        if (weworkMessage.chatid.isNullOrBlank() || weworkMessage.chatid == "null") {
            logger.warn("failed to send wework robot message,chatid can't be empty")
            return Optional.empty()
        }
        val url = buildUrl("$weworkHost/cgi-bin/webhook/send?key=$robotKey")
        val requestBody = JsonUtil.toJson(weworkMessage)
        return OkhttpUtils.doPost(url, requestBody).use {
            val responseBody = it.body?.string() ?: ""
            kotlin.runCatching {
                val sendMessageResp = JsonUtil.to(responseBody, jacksonTypeRef<WeworkSendMessageResp>())
                if (!it.isSuccessful || 0 != sendMessageResp.errCode) {
                    throw RemoteServiceException(
                        httpStatus = it.code,
                        responseContent = responseBody,
                        errorMessage = "send wework robot message failed：errMsg = ${sendMessageResp.errMsg}" +
                                "|chatid = ${weworkMessage.chatid} ;",
                        errorCode = sendMessageResp.errCode
                    )
                }
            }.fold({ Optional.empty() }, { e ->
                logger.warn("${it.request}|send wework robot message failed, $responseBody")
                Optional.of(e)
            })
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
        const val WEWORK_UPLOAD_TEMP_PREFIX = "wework_upload_"
    }
}
