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

package com.tencent.devops.support.robot

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.auth.api.manager.AuthManagerApprovalResource
import com.tencent.devops.auth.pojo.enum.ApprovalType
import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import com.tencent.devops.common.wechatwork.WeworkRobotCustomConfig
import com.tencent.devops.common.wechatwork.aes.WXBizMsgCrypt
import com.tencent.devops.support.robot.pojo.RobotCallback
import com.tencent.devops.common.wechatwork.model.robot.RobotTextSendMsg
import com.tencent.devops.common.wechatwork.model.robot.MsgInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import com.tencent.devops.common.client.Client
import javax.xml.parsers.DocumentBuilderFactory

@Service
class RobotService @Autowired constructor(
    val weworkRobotCustomConfig: WeworkRobotCustomConfig,
    val wechatWorkRobotService: WechatWorkRobotService,
    val client: Client
) {
    private val rototWxcpt =
        WXBizMsgCrypt(weworkRobotCustomConfig.token, weworkRobotCustomConfig.aeskey, "")

    /*
    * 验证geturl
    * */
    fun robotVerifyURL(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
        logger.info("signature:$signature")
        logger.info("timestamp:$timestamp")
        logger.info("nonce:$nonce")
        logger.info("echoStr:$echoStr")
        var verifyResult = ""
        try {
            verifyResult = rototWxcpt.VerifyURL(signature, timestamp.toString(), nonce, echoStr)
            logger.info("verifyResult:$verifyResult")
        } catch (e: Exception) {
            logger.warn("robotVerifyURL: $e")
            // 验证URL，错误原因请查看异常
            e.printStackTrace()
        }
        return verifyResult
    }

    fun robotCallbackPost(signature: String, timestamp: Long, nonce: String, reqData: String?): Boolean {
        logger.info("signature:$signature")
        logger.info("timestamp:$timestamp")
        logger.info("nonce:$nonce")
        logger.info("reqData:$reqData")
        val robotCallBack = getCallbackInfo(signature, timestamp, nonce, reqData)
        logger.info("chitId:${robotCallBack.chatId}")
        logger.info("robotCallBack:$robotCallBack")
        if (robotCallBack.eventType == "enter_chat") {
            // 如为进入机器人单聊, 直接返回空包
            return true
        }
        if (robotCallBack.content.contains("会话ID") || robotCallBack.content.contains("群ID")) {
            val msg = RobotTextSendMsg(
                chatId = robotCallBack.chatId,
                text = MsgInfo(
                    content = "本群ChatId: ${robotCallBack.chatId}"
                )
            )
            wechatWorkRobotService.send(msg.toJsonString())
        }
        val callbackId = robotCallBack.callbackId
        val actionName = robotCallBack.actionName
        val actionValue = robotCallBack.actionValue
        if (callbackId == null || callbackId.isEmpty() || actionValue == null || actionValue.isEmpty()) {
            return true
        }
        when (callbackId) {
            "approval" -> {
                when (actionName) {
                    "agree" -> {
                        client.get(AuthManagerApprovalResource::class).managerApproval(
                            approvalId = actionValue.toInt(),
                            approvalType = ApprovalType.AGREE
                        )
                    }
                    "refuse" -> {
                        client.get(AuthManagerApprovalResource::class).managerApproval(
                            approvalId = actionValue.toInt(),
                            approvalType = ApprovalType.REFUSE
                        )
                    }
                }
            }
            "renewal" -> {
                when (actionName) {
                    "agree" -> {
                        client.get(AuthManagerApprovalResource::class).userRenewalAuth(
                            approvalId = actionValue.toInt(),
                            approvalType = ApprovalType.AGREE
                        )
                    }
                    "refuse" -> {
                        client.get(AuthManagerApprovalResource::class).userRenewalAuth(
                            approvalId = actionValue.toInt(),
                            approvalType = ApprovalType.REFUSE
                        )
                    }
                }
            }
        }
        return true
    }

    /*
  * 获取密文的CallbackInfo对象
  * */
    fun getCallbackInfo(signature: String, timestamp: Long, nonce: String, reqData: String?): RobotCallback {
        val document = getDecrypeDocument(signature, timestamp, nonce, reqData)
        val root = document!!.documentElement
        val chitId = root.getElementsByTagName("ChatId")
        val webhookUrl = root.getElementsByTagName("WebhookUrl")
        val getChatInfoUrl = root.getElementsByTagName("GetChatInfoUrl")
        val msgId = root.getElementsByTagName("MsgId")
        val msgType = root.getElementsByTagName("MsgType")
        val content = root.getElementsByTagName("Content")
        val userName = root.getElementsByTagName("Name")
        val userId = root.getElementsByTagName("Alias")
        val eventType = root.getElementsByTagName("Event") ?: null
        val callbackId = root.getElementsByTagName("CallbackId") ?: null
        val actions = root.getElementsByTagName("Actions") ?: null
        val actionValue = root.getElementsByTagName("Value") ?: null
        logger.info("eventType: $eventType, ${eventType?.item(0)}")
        return RobotCallback(
            chatId = chitId.item(0).textContent,
            webhookUrl = webhookUrl.item(0).textContent,
            getChatInfoUrl = getChatInfoUrl.item(0).textContent,
            msgType = msgType.item(0).textContent,
            msgId = msgId.item(0).textContent,
            content = content?.item(0)?.textContent ?: "",
            name = userName.item(0).textContent,
            userId = userId.item(0).textContent,
            eventType = eventType?.item(0)?.firstChild?.textContent,
            callbackId = callbackId?.item(0)?.textContent,
            actionName = actions?.item(0)?.firstChild?.textContent,
            actionValue = actionValue?.item(0)?.textContent
        )
    }

    /*
    * 获取密文的Document对象
    * */
    fun getDecrypeDocument(signature: String, timestamp: Long, nonce: String, reqData: String?): Document? {
        val xmlString = getDecrypeMsg(signature, timestamp, nonce, reqData)
        logger.info("xmlString:$xmlString")
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        val sr = StringReader(xmlString)
        val document = InputSource(sr)
        return db.parse(document)
    }

    /*
    * 获取密文的xml字符串
    * */
    fun getDecrypeMsg(signature: String, timestamp: Long, nonce: String, reqData: String?): String {
        var xmlString = ""
        try {
            xmlString = rototWxcpt.DecryptMsg(signature, timestamp.toString(), nonce, reqData)
        } catch (e: Exception) {
            // 转换失败，错误原因请查看异常
            e.printStackTrace()
        }
        return xmlString
    }

    companion object {
        val logger = LoggerFactory.getLogger(RobotService::class.java)
    }
}
