package com.tencent.devops.support.robot

import com.tencent.bkrepo.common.api.util.toJsonString
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
import javax.xml.parsers.DocumentBuilderFactory

@Service
class RobotService @Autowired constructor(
    val weworkRobotCustomConfig: WeworkRobotCustomConfig,
    val wechatWorkRobotService: WechatWorkRobotService
) {
    private val rototWxcpt =
        WXBizMsgCrypt(weworkRobotCustomConfig.token, weworkRobotCustomConfig.aeskey, "")

    /*
    * 验证geturl
    * */
    fun robotVerifyURL(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
        logger.info("signature:$signature")
        logger.info("timestamp:${timestamp}")
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
        if (robotCallBack.content.contains("会话ID")) {
            val msg = RobotTextSendMsg(
                chatId = robotCallBack.chatId,
                text = MsgInfo(
                    content = "本群ChatId: ${robotCallBack.chatId}"
                )
            )
            wechatWorkRobotService.send(msg.toJsonString())
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
        return RobotCallback(
            chatId = chitId.item(0).textContent,
            webhookUrl = webhookUrl.item(0).textContent,
            getChatInfoUrl = getChatInfoUrl.item(0).textContent,
            msgType = msgType.item(0).textContent,
            msgId = msgId.item(0).textContent,
            content = content.item(0).textContent,
            name = userName.item(0).textContent,
            userId = userId.item(0).textContent
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
        val `is` = InputSource(sr)
        return db.parse(`is`)
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
