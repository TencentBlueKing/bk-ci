package com.tencent.devops.support.resources.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.external.ExternalWechatWorkResource
import com.tencent.devops.support.services.WechatWorkCallbackService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalWechatWorkResourceImpl @Autowired constructor(private val wechatWorkCallbackService: WechatWorkCallbackService) : ExternalWechatWorkResource {
    private val logger = LoggerFactory.getLogger(ExternalWechatWorkResourceImpl::class.java)

    override fun callback(
        signature: String,
        timestamp: Long,
        nonce: String,
        echoStr: String,
        reqData: String?
    ): Result<String> {

        val sMsg = wechatWorkCallbackService.callbackGet(signature, timestamp, nonce, echoStr)

        logger.info(sMsg)
        logger.info(signature)
        logger.info(timestamp.toString())
        logger.info(nonce)
        logger.info(echoStr)
        logger.info(reqData)
        return Result(data = sMsg)
    }

    override fun callback(
        signature: String,
        timestamp: Long,
        nonce: String,
        reqData: String?
    ): Result<Boolean> {

        val xmlDocument = wechatWorkCallbackService.callbackPost(signature, timestamp, nonce, reqData)
//        val document = WechatWorkUtil.getXml(encryptData)
//        var rootElement = document.rootElement
//        var toUserNameElement = rootElement.elementIterator("ToUserName").next() as Element
//        var toUserName = toUserNameElement.text
//        var serviceIdElement = rootElement.elementIterator("ServiceId").next() as Element
//        var serviceId = serviceIdElement.text
//        var agentTypeElement = rootElement.elementIterator("AgentType").next() as Element
//        var agentType = agentTypeElement.text
//        var encryptElement = rootElement.elementIterator("Encrypt").next() as Element
//        var encrypt = encryptElement.text
//        logger.info(signature)
//        logger.info(timestamp.toString())
//        logger.info(nonce)
//        logger.info(encryptData)
        return Result(data = xmlDocument)
    }
}