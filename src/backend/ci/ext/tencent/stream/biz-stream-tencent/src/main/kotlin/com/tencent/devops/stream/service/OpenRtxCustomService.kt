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

package com.tencent.devops.stream.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_GROUP_ID
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_SESSION_ID
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_THIS_GROUP_ID
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.aes.WXBizMsgCrypt
import com.tencent.devops.common.wechatwork.model.CallbackElement
import com.tencent.devops.common.wechatwork.model.enums.FromType
import com.tencent.devops.common.wechatwork.model.enums.MsgType
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.stream.config.RtxCustomConfig
import com.tencent.devops.stream.constant.StreamMessageCode.BK_STREAM_MESSAGE_NOTIFICATION
import com.tencent.devops.stream.trigger.listener.notify.RtxCustomApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpenRtxCustomService constructor(
    private val rtxCustomConfig: RtxCustomConfig
) {

    private val logger = LoggerFactory.getLogger(OpenRtxCustomService::class.java)

    private val wxcpt = WXBizMsgCrypt(rtxCustomConfig.token, rtxCustomConfig.aeskey, rtxCustomConfig.serviceId)

    fun callbackGet(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
        return verifyURL(signature, timestamp, nonce, echoStr)
    }

    fun callbackPost(signature: String, timestamp: Long, nonce: String, reqData: String?): Boolean {

        logger.info("signature:$signature")
        logger.info("timestamp:$timestamp")
        logger.info("nonce:$nonce")
        logger.info("reqData:$reqData")
        val callbackElement = getCallbackInfo(signature, timestamp, nonce, reqData)

        val chatId = callbackElement.chatId
        val receiverType: ReceiverType
        // 用户ID,企业微信自己维护的
        val userId: String
        // 用户名，也就是英文名，rtx名称
//        val userName: String

        // 以回调消息类型来做逻辑区分

        var mentionType = "0"
        // 对于在群里面的没有@机器人的都直接忽略：
        if (callbackElement.fromType == FromType.group) {
            try {
                mentionType = (callbackElement.msgElement.elementIterator("MentionedType").next() as Element).text
            } catch (e: Exception) {
                logger.info("This group does not have mention element")
            }
            // 当被没有被@到,而且不为事件的时候，不做任何处理
            if (mentionType == "0" && callbackElement.msgType != MsgType.Event) {
                return true
            }

            // 转换成
//            userId = (callbackElement.fromElement.elementIterator("Sender").next() as Element).text
            receiverType = ReceiverType.group
        } else {
//            userId = (callbackElement.fromElement.elementIterator("Id").next() as Element).text
            receiverType = ReceiverType.single
        }

//        userName = wechatWorkService.getUserNameByUserId(userId)

//        // 处理msgid,个人会话，群会发并@，click事件
//        if ((receiverType == ReceiverType.group && mentionType != "0") ||
//            receiverType == ReceiverType.single ||
//            (callbackElement.msgType == MsgType.Event && EventType.valueOf(
//                (callbackElement.msgElement.elementIterator("Event").next() as Element).text
//            ) == EventType.click)
//        ) {
//            val msgId = (callbackElement.msgElement.elementIterator("MsgId").next() as Element).text
//            if (wechatWorkMessageDAO.exist(dslContext, msgId)) {
//                // 已存在则直接返回
//                return true
//            } else {
//                // 还没有存在则插入并往下走
//                wechatWorkMessageDAO.insertMassageId(dslContext, msgId)
//            }
//        }

        // 从这里开始统一处理消息
        when (callbackElement.msgType) {
            MsgType.emotion, MsgType.file, MsgType.forward, MsgType.image, MsgType.vocie -> {
            }
            MsgType.Event -> {
            }
            MsgType.text -> { // 注意这个块
                val content = (callbackElement.msgElement.elementIterator("Content").next() as Element).text
                // 针对文本内容的@情况进行处理

                // 当被@到,但没有@到机器人的名字的时候也不做处理。 @Stream消息通知
                if (receiverType == ReceiverType.group && !content.contains(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_STREAM_MESSAGE_NOTIFICATION,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        )
                )) {
                    return true
                }
                logger.info("content = $content")
                // 返回群会话ID关键词
                if (receiverType == ReceiverType.group && (
                    content.contains(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_SESSION_ID,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ), true) || content.contains(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_GROUP_ID,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ),
                            true
                        )
                    )
                ) {
                    logger.info("chatId = $chatId")
                    val receiver = Receiver(receiverType, chatId)
                    val richtextContentList = mutableListOf<RichtextContent>()
                    richtextContentList.add(RichtextText(RichtextTextText(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_THIS_GROUP_ID,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            params = arrayOf(chatId)
                        )
                    )))
                    val richtextMessage = RichtextMessage(receiver, richtextContentList)
                    sendRichText(richtextMessage)
                }
            }
        }
        return true
    }

    /*
    * 验证geturl
    * */
    private fun verifyURL(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
        var verifyResult = ""
        try {
            verifyResult = wxcpt.VerifyURL(signature, timestamp.toString(), nonce, echoStr)
        } catch (e: Exception) {
            // 验证URL，错误原因请查看异常
            e.printStackTrace()
        }
        return verifyResult
    }

    /*
    * 获取密文的xml字符串
    *
    * */
    fun getDecrypeMsg(signature: String, timestamp: Long, nonce: String, reqData: String?): String {
        var xmlString = ""
        try {
            xmlString = wxcpt.DecryptMsg(signature, timestamp.toString(), nonce, reqData)
        } catch (e: Exception) {
            // 转换失败，错误原因请查看异常
            e.printStackTrace()
        }
        return xmlString
    }

    /*
    * 获取密文的Document对象
    * */
    fun getDecrypeDocument(signature: String, timestamp: Long, nonce: String, reqData: String?): Document {
        val xmlString = getDecrypeMsg(signature, timestamp, nonce, reqData)
        logger.info("xmlString:$xmlString")
        return DocumentHelper.parseText(xmlString)
    }

    /*
    * 获取密文的CallbackInfo对象
    * */
    fun getCallbackInfo(signature: String, timestamp: Long, nonce: String, reqData: String?): CallbackElement {
        val document = getDecrypeDocument(signature, timestamp, nonce, reqData)
        val rootElement = document.rootElement
        val toUserName = (rootElement.elementIterator("ToUserName").next() as Element).text
        val serviceId = (rootElement.elementIterator("ServiceId").next() as Element).text
        val agentType = (rootElement.elementIterator("AgentType").next() as Element).text
        val msgElement = rootElement.elementIterator("Msg").next() as Element
        val msgType = MsgType.valueOf((msgElement.elementIterator("MsgType").next() as Element).text)
        val fromElement = msgElement.elementIterator("From").next() as Element
        val fromType = FromType.valueOf((fromElement.elementIterator("Type").next() as Element).text)
        val chatId = (fromElement.elementIterator("Id").next() as Element).text
        return CallbackElement(
            toUserName,
            serviceId,
            agentType,
            chatId,
            msgType,
            fromType,
            msgElement,
            fromElement
        )
    }

    /*
    * 发送富文本
    * */
    fun sendRichText(richtextMessage: RichtextMessage): Boolean {
        val accessToken = RtxCustomApi.getAccessToken(
            corpSecret = rtxCustomConfig.corpSecret,
            corpId = rtxCustomConfig.corpId,
            urlPrefix = rtxCustomConfig.rtxUrl
        )
        val jsonString = jacksonObjectMapper().writeValueAsString(richtextMessage)

        val sendURL = "${rtxCustomConfig.rtxUrl}/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException(
                    "RtxCustomApi sendRichText error code: ${response.code} " +
                            "messge: ${response.message}"
                )
            }
        }
        return true
    }
}
