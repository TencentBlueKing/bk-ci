package com.tencent.devops.support.robot

import com.tencent.devops.common.wechatwork.aes.WXBizMsgCrypt
import com.tencent.devops.common.wechatwork.model.CallbackElement
import com.tencent.devops.common.wechatwork.model.enums.FromType
import com.tencent.devops.common.wechatwork.model.enums.MsgType
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WeworkRobotService @Autowired constructor(
    private val weweorkRobotConfiguration: WeworkRobotCustomConfig,
) {
    private val rototWxcpt =
        WXBizMsgCrypt(weweorkRobotConfiguration.token, weweorkRobotConfiguration.aeskey, null)

    /*
    * 验证geturl
    * */
    fun robotVerifyURL(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
        var verifyResult = ""
        try {
            verifyResult = rototWxcpt.VerifyURL(signature, timestamp.toString(), nonce, echoStr)
        } catch (e: Exception) {
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
        val callbackElement = getCallbackInfo(signature, timestamp, nonce, reqData)
        logger.info("chitId:${callbackElement.chatId}")

        return true
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
    * 获取密文的Document对象
    * */
    fun getDecrypeDocument(signature: String, timestamp: Long, nonce: String, reqData: String?): Document {
        val xmlString = getDecrypeMsg(signature, timestamp, nonce, reqData)
        logger.info("xmlString:$xmlString")
        return DocumentHelper.parseText(xmlString)
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
        val logger = LoggerFactory.getLogger(WeworkRobotService::class.java)
    }
}
