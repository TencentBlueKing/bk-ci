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
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_UNSUPPORTED_MEDIA_TYPE
import com.tencent.devops.notify.dao.WeworkNotifyDao
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.ImageContent
import com.tencent.devops.notify.pojo.MediaContent
import com.tencent.devops.notify.pojo.WeweokRobotBaseMessage
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotContentMessage
import com.tencent.devops.notify.pojo.WeworkRobotFileMessage
import com.tencent.devops.notify.pojo.WeworkRobotImageMessage
import com.tencent.devops.notify.pojo.WeworkRobotMarkdownMessage
import com.tencent.devops.notify.pojo.WeworkRobotSingleTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotUploadResponse
import com.tencent.devops.notify.pojo.WeworkSendMessageResp
import com.tencent.devops.notify.service.WeworkService
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.Optional
import java.util.UUID
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

    @Value("\${wework.tempDirectory:}")
    private var tempDirectory: String = ""

    override fun sendMediaMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        val mediaType = weworkNotifyMediaMessage.mediaType
        // 校验媒体类型，仅支持file和image
        if (mediaType == WeworkMediaType.voice || mediaType == WeworkMediaType.video) {
            logger.warn("wework robot does not support media type: $mediaType, skip sending")
            saveResult(
                receivers = weworkNotifyMediaMessage.receivers,
                body = "mediaType:$mediaType, mediaName:${weworkNotifyMediaMessage.mediaName}",
                success = false,
                errMsg = "wework robot does not support media type: $mediaType"
            )
            return
        }

        try {
            // 根据媒体类型选择不同的发送方式
            when (mediaType) {
                WeworkMediaType.image -> {
                    // 图片使用base64+md5方式发送
                    sendImageMessage(weworkNotifyMediaMessage)
                }
                WeworkMediaType.file -> {
                    // 文件使用media_id方式发送
                    sendFileMessage(weworkNotifyMediaMessage)
                }
                else -> {
                    logger.warn("unsupported media type: $mediaType")
                    saveResult(
                        receivers = weworkNotifyMediaMessage.receivers,
                        body = "mediaType:$mediaType, mediaName:${weworkNotifyMediaMessage.mediaName}",
                        success = false,
                        errMsg = "unsupported media type: $mediaType"
                    )
                    throw ErrorCodeException(
                        errorCode = ERROR_NOTIFY_UNSUPPORTED_MEDIA_TYPE,
                        params = arrayOf(mediaType.name)
                    )
                }
            }
        } catch (e: Throwable) {
            logger.warn("failed to send media message", e)
            saveResult(
                receivers = weworkNotifyMediaMessage.receivers,
                body = "mediaType:$mediaType, mediaName:${weworkNotifyMediaMessage.mediaName}",
                success = false,
                errMsg = e.message
            )
        }
    }

    /**
     * 发送图片消息（使用base64+md5方式）
     */
    private fun sendImageMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        val mediaType = weworkNotifyMediaMessage.mediaType

        // 读取输入流并转换为base64和md5
        val imageBytes = weworkNotifyMediaMessage.mediaInputStream.use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toByteArray()
            }
        }
        val base64Content = Base64.getEncoder().encodeToString(imageBytes)
        val md5Hash = calculateMd5(imageBytes)

        // 统一遍历接收者发送消息
        val errMsgs = sendToReceivers(weworkNotifyMediaMessage.receivers) { receiver ->
            val requestBody = buildImageMessageBody(base64 = base64Content, md5 = md5Hash, chatId = null)
            sendMediaRequest(requestBody = requestBody, key = receiver)
        }

        // 记录发送结果
        saveMediaResult(
            receivers = weworkNotifyMediaMessage.receivers,
            mediaType = mediaType,
            mediaName = weworkNotifyMediaMessage.mediaName,
            errMsgs = errMsgs,
            messageType = "image"
        )
    }

    /**
     * 发送文件消息（使用media_id方式）
     */
    private fun sendFileMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        val mediaType = weworkNotifyMediaMessage.mediaType
        val fileBytes = weworkNotifyMediaMessage.mediaInputStream.use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toByteArray()
            }
        }

        // 统一遍历接收者发送消息
        val errMsgs = sendToReceivers(weworkNotifyMediaMessage.receivers) { receiver ->
            val mediaId = uploadMediaToRobot(
                fileBytes = fileBytes,
                fileName = weworkNotifyMediaMessage.mediaName,
                mediaType = mediaType.name,
                key = receiver
            )
            val requestBody = buildFileMessageBody(mediaId = mediaId, chatId = null)
            sendMediaRequest(requestBody = requestBody, key = receiver)
        }

        // 记录发送结果
        saveMediaResult(
            receivers = weworkNotifyMediaMessage.receivers,
            mediaType = mediaType,
            mediaName = weworkNotifyMediaMessage.mediaName,
            errMsgs = errMsgs,
            messageType = "file"
        )
    }

    /**
     * 统一遍历接收者发送消息
     */
    private fun sendToReceivers(
        receivers: Collection<String>,
        sendAction: (receiver: String) -> Unit
    ): List<String> {
        val errMsgs = mutableListOf<String>()
        receivers.forEach { receiver ->
            try {
                sendAction(receiver)
            } catch (e: Throwable) {
                logger.warn("failed to send media message to receiver: $receiver", e)
                errMsgs.add("receiver=$receiver: ${e.message}")
            }
        }
        return errMsgs
    }

    /**
     * 保存媒体消息发送结果
     */
    private fun saveMediaResult(
        receivers: Collection<String>,
        mediaType: WeworkMediaType,
        mediaName: String,
        errMsgs: List<String>,
        messageType: String
    ) {
        val body = "mediaType:$mediaType, mediaName:$mediaName"
        if (errMsgs.isEmpty()) {
            logger.info("send $messageType message success, mediaName=$mediaName")
            saveResult(receivers = receivers, body = body, success = true, errMsg = null)
        } else {
            saveResult(receivers = receivers, body = body, success = false, errMsg = errMsgs.joinToString("; "))
        }
    }

    /**
     * 计算MD5哈希值
     */
    private fun calculateMd5(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 上传媒体文件到企业微信机器人
     */
    private fun uploadMediaToRobot(
        fileBytes: ByteArray,
        fileName: String,
        mediaType: String,
        key: String
    ): String {
        val tempDir = File(System.getProperty("java.io.tmpdir"), tempDirectory)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val tempFile = File(tempDir, "${UUID.randomUUID()}_$fileName")
        try {
            // 将字节数组写入临时文件
            tempFile.outputStream().use { output ->
                output.write(fileBytes)
            }

            // 构建multipart请求
            val url = buildUrl("$weworkHost/cgi-bin/webhook/upload_media?key=$key&type=$mediaType")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "media",
                    fileName,
                    tempFile.asRequestBody("application/octet-stream".toMediaType())
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
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * 构建图片消息请求体（base64+md5方式）
     */
    private fun buildImageMessageBody(base64: String, md5: String, chatId: String?): String {
        val message = WeworkRobotImageMessage(
            chatId = chatId,
            image = ImageContent(base64 = base64, md5 = md5)
        )
        return JsonUtil.toJson(message)
    }

    /**
     * 构建文件消息请求体（media_id方式）
     */
    private fun buildFileMessageBody(mediaId: String, chatId: String?): String {
        val message = WeworkRobotFileMessage(
            chatId = chatId,
            file = MediaContent(mediaId = mediaId)
        )
        return JsonUtil.toJson(message)
    }

    /**
     * 发送媒体消息请求
     */
    private fun sendMediaRequest(requestBody: String, key: String) {
        val url = buildUrl("$weworkHost/cgi-bin/webhook/send?key=$key")
        OkhttpUtils.doPost(url, requestBody).use { response ->
            val responseBody = response.body?.string() ?: ""
            val sendMessageResp = JsonUtil.to(responseBody, WeworkSendMessageResp::class.java)
            if (!response.isSuccessful || sendMessageResp.errCode != 0) {
                throw RemoteServiceException(
                    httpStatus = response.code,
                    responseContent = responseBody,
                    errorMessage = "send wework robot media message failed: ${sendMessageResp.errMsg}",
                    errorCode = sendMessageResp.errCode
                )
            }
        }
    }

    override fun sendTextMessage(weworkNotifyTextMessage: WeworkNotifyTextMessage): Boolean {
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
                return false
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
    }
}
