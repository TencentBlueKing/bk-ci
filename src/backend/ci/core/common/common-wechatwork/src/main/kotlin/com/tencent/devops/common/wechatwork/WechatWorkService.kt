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

package com.tencent.devops.common.wechatwork

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.BK_HUMAN_SERVICE
import com.tencent.devops.common.api.util.JacksonUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.aes.WXBizMsgCrypt
import com.tencent.devops.common.wechatwork.model.CallbackElement
import com.tencent.devops.common.wechatwork.model.CreateChatItem
import com.tencent.devops.common.wechatwork.model.LinkItem
import com.tencent.devops.common.wechatwork.model.ReceiverItem
import com.tencent.devops.common.wechatwork.model.RichTextItem
import com.tencent.devops.common.wechatwork.model.SendInfo
import com.tencent.devops.common.wechatwork.model.TextItem
import com.tencent.devops.common.wechatwork.model.enums.FromType
import com.tencent.devops.common.wechatwork.model.enums.MsgType
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.enums.UploadMediaType
import com.tencent.devops.common.wechatwork.model.nameconvert.UserIdList
import com.tencent.devops.common.wechatwork.model.response.AccessTokenResponse
import com.tencent.devops.common.wechatwork.model.response.CreateChatResponse
import com.tencent.devops.common.wechatwork.model.response.UploadMediaResponse
import com.tencent.devops.common.wechatwork.model.response.UserIdNameResponse
import com.tencent.devops.common.wechatwork.model.response.UserIdsConvertResponse
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Duration.between
import java.time.LocalDateTime

@Service
class WechatWorkService @Autowired constructor(
    private val wechatWorkConfiguration: WechatWorkConfiguration
) {
    private val wechatWorkApiURL = wechatWorkConfiguration.url
    private var timeOutStamp: LocalDateTime? = null
    private var accessToken: String? = null
    private var mapper = jacksonObjectMapper()

    //    private var httpClient = OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
    private val wxcpt by lazy {
        WXBizMsgCrypt(
            wechatWorkConfiguration.token,
            wechatWorkConfiguration.aesKey,
            wechatWorkConfiguration.serviceId
        )
    }
    private val objectMapper = JacksonUtil.createObjectMapper()
    private val sendMessageApiURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send"
    private val uploadMediaApiURL = "$wechatWorkApiURL/cgi-bin/media/upload"
    private val converUserIdtoUserNameApiURL = "${wechatWorkConfiguration.url}/cgi-bin/tencent/user/convert_to_name"

    private val logger = LoggerFactory.getLogger(WechatWorkService::class.java)

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
    * 验证geturl
    * */
    fun verifyURL(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
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
    * 获取调用接口时使用的access_token，有效事件为7200秒（2小时），这里做7000秒的失效
    * */
    @Suppress("ComplexCondition")
    fun getAccessToken(): String? {
        if (accessToken == "" || accessToken == null || timeOutStamp == null || between(
                timeOutStamp,
                LocalDateTime.now()
            ).toMillis() > 7000 * 1000
        ) {
            val accessTokenURL = wechatWorkApiURL + "/cgi-bin/gettoken?" +
                "corpid=${wechatWorkConfiguration.corpId}&corpsecret=${wechatWorkConfiguration.secret}"
            val accessTokenRequest = Request.Builder()
                .url(accessTokenURL)
                .get()
                .build()
            OkhttpUtils.doHttp(accessTokenRequest).use { response ->
                //            httpClient.newCall(accessTokenRequest).execute().use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw RuntimeException("Fail to send msg to yqwx. $responseContent")
                }
//                var accessTokenResponse = mapper.readValue(responseContent,AccessTokenResponse::class.java)
                val accessTokenResponse = mapper.readValue<AccessTokenResponse>(responseContent)
                if (accessTokenResponse.errCode == 0) {
                    accessToken = accessTokenResponse.accessToken
                    timeOutStamp = LocalDateTime.now()
                } else {
                    throw RuntimeException("Fail to get wechat-work access_token：${accessTokenResponse.errMsg}")
                }
            }
        }
        return accessToken
    }

    /*
    * 发送简单文本
    * */
    fun sendTextSingle(text: String, receiver: String) {
        val accessToken = getAccessToken()
        val requestData = SendInfo(
            receiver = ReceiverItem(
                type = "single",
                id = receiver
            ),
            messageType = "text",
            text = TextItem(
                content = text
            ),
            markdown = null,
            richText = null
        )
        val jsonString = JsonUtil.toJson(requestData)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    /*
    * 发送群文本
    * */
    fun sendTextGroup(text: String, chatId: String) {
        val accessToken = getAccessToken()
        val requestData = SendInfo(
            receiver = ReceiverItem(
                type = "group",
                id = chatId
            ),
            messageType = "text",
            text = TextItem(
                content = text
            ),
            markdown = null,
            richText = null
        )
        val jsonString = JsonUtil.toJson(requestData)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    /*
    * 发送群文本
    * */
    fun sendMarkdownGroup(markdown: String, chatId: String) {
        val accessToken = getAccessToken()
        val requestData = SendInfo(
            receiver = ReceiverItem(
                type = "group",
                id = chatId
            ),
            messageType = "markdown",
            text = null,
            markdown = TextItem(
                content = markdown
            ),
            richText = null
        )
        val jsonString = JsonUtil.toJson(requestData)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    /*
    * 发送富文本
    * */
    fun sendRichText(richtextMessage: RichtextMessage): Boolean {
        val accessToken = getAccessToken()
        val jsonString = JsonUtil.toJson(richtextMessage, false)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
        return true
    }

    /*
    * 发送富文本2
    * */
    fun sendRichTextCommon(text: String, receiver: String) {
        val accessToken = getAccessToken()
        val requestData = SendInfo(
            receiver = ReceiverItem(
                type = "single",
                id = receiver
            ),
            messageType = "rich_text",
            text = null,
            markdown = null,
            richText = listOf(
                RichTextItem(
                    type = "text",
                    text = TextItem(
                        content = text
                    ),
                    link = null
                ),
                RichTextItem(
                    type = "link",
                    link = LinkItem(
                        type = "click",
                        text = I18nUtil.getCodeLanMessage(BK_HUMAN_SERVICE),
                        key = "humanService"
                    ),
                    text = null
                )
            )
        )
        val jsonString = JsonUtil.toJson(requestData)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    /*
    * 拉起新的群，可以指定用户
    * */
    fun createChatByUserNames(title: String, userNameList: List<String>): String {
        var chatId = ""
        val accessToken = getAccessToken()

        val requestData = CreateChatItem(
            name = title,
            userList = userNameList
        )
        val jsonString = JsonUtil.toJson(requestData)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/create?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
            val createChatReponse = mapper.readValue<CreateChatResponse>(responseContent)
            if (createChatReponse.errcode == 0) {
                chatId = createChatReponse.chatid
            }
        }
        return chatId
    }

    // 发送Get请求
    fun sendGetToWechatWorkApi(url: String, params: String?) {
        val accessToken = getAccessToken()
        val sendURL = "$url?$params&access_token=$accessToken"
        val sendRequest = Request.Builder()
            .url(sendURL)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    fun sendByApp(
        chatId: String,
        content: String,
        markerDownFlag: Boolean
    ) {
        logger.info("send group msg by app: $chatId")
        if (markerDownFlag) {
            sendMarkdownGroup(content!!.replace("\\n", "\n"), chatId)
        } else {
            val receiver = Receiver(ReceiverType.group, chatId)
            val richTextContentList = mutableListOf<RichtextContent>()
            richTextContentList.add(
                RichtextText(RichtextTextText(content))
            )
            val richTextMessage = RichtextMessage(receiver, richTextContentList)
            sendRichText(richTextMessage)
        }
    }

    // 发送Post请求
    private fun sendPostToWechatWorkApi(url: String, message: Any): Boolean {

        val accessToken = getAccessToken()
        val sendURL = "$url?access_token=$accessToken"
        logger.info("send to wework sendURL:$sendURL")
        val jsonString = if (message is String) message else objectMapper.writeValueAsString(message)

        logger.info("send to wework send json:$jsonString")
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()

            logger.info("send to wework responseContent:$responseContent")
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
            return true
        }
    }

    // 发送消息信息
    fun sendMessage(message: Any): Boolean {
        val url = sendMessageApiURL
        return sendPostToWechatWorkApi(url, message)
    }

    // 上传临时素材
    fun uploadMedia(
        mediaType: UploadMediaType,
        mediaName: String,
        mediaInputStream: InputStream
    ): UploadMediaResponse? {
        val accessToken = getAccessToken()
        val url = "$uploadMediaApiURL?access_token=$accessToken&type=$mediaType"

        val fileBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), mediaInputStream.readBytes())
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("media", mediaName, fileBody)
            .build()
        val uploadRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(uploadRequest).use { response ->
            //        httpClient.newCall(uploadRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send media to yqwx. $responseContent")
            }
            val uploadMediaResponse = mapper.readValue<UploadMediaResponse>(responseContent)
            if (uploadMediaResponse.errCode == 0) {
                return uploadMediaResponse
            } else {
                throw RuntimeException("send media to yqwx's response is error. $responseContent")
            }
        }
    }

    // 将userid批量转为rtx名
    fun getUserNamesByUserIds(userIds: List<String>): List<UserIdNameResponse> {
        val userNameList = mutableListOf<UserIdNameResponse>()
        val userIdList = UserIdList(userIds)
        val jsonString = objectMapper.writeValueAsString(userIdList)
        val accessToken = getAccessToken()
        val sendURL = "$converUserIdtoUserNameApiURL?access_token=$accessToken"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
            val userIdsConvertResponse = mapper.readValue<UserIdsConvertResponse>(responseContent)
            if (userIdsConvertResponse.errCode == 0) {
                userNameList.addAll(userIdsConvertResponse.userList)
            }
        }
        return userNameList
    }

    // 将userid转为rtx名
    fun getUserNameByUserId(userId: String): String {
        var result = ""
        val userIdList = mutableListOf(userId)
        val userNameList = getUserNamesByUserIds(userIdList)
        if (userNameList.isNotEmpty()) {
            result = userNameList[0].name
        }
        return result
    }
}
