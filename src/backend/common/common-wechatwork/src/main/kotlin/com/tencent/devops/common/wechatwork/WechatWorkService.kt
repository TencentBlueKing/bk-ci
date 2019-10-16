package com.tencent.devops.common.wechatwork

// import com.fasterxml.jackson.databind.ObjectMapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JacksonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.wechatwork.aes.WXBizMsgCrypt
import com.tencent.devops.common.wechatwork.model.CallbackElement
import com.tencent.devops.common.wechatwork.model.enums.FromType
import com.tencent.devops.common.wechatwork.model.enums.MsgType
import com.tencent.devops.common.wechatwork.model.enums.UploadMediaType
import com.tencent.devops.common.wechatwork.model.nameconvert.UserIdList
import com.tencent.devops.common.wechatwork.model.response.AccessTokenResponse
import com.tencent.devops.common.wechatwork.model.response.CreateChatResponse
import com.tencent.devops.common.wechatwork.model.response.UploadMediaResponse
import com.tencent.devops.common.wechatwork.model.response.UserIdNameResponse
import com.tencent.devops.common.wechatwork.model.response.UserIdsConvertResponse
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.ops.common.wechatwork.WechatWorkConfiguration
import okhttp3.MediaType
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
    private val wxcpt =
        WXBizMsgCrypt(wechatWorkConfiguration.token, wechatWorkConfiguration.aesKey, wechatWorkConfiguration.serviceId)

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
    fun getAccessToken(): String? {
        if (accessToken == "" || accessToken == null || timeOutStamp == null || between(
                timeOutStamp,
                LocalDateTime.now()
            ).toMillis() > 7000 * 1000
        ) {
            val accessTokenURL =
                wechatWorkApiURL + "/cgi-bin/gettoken?corpid=${wechatWorkConfiguration.corpId}&corpsecret=${wechatWorkConfiguration.secret}"
            val accessTokenRequest = Request.Builder()
                .url(accessTokenURL)
                .get()
                .build()
            OkhttpUtils.doHttp(accessTokenRequest).use { response ->
                //            httpClient.newCall(accessTokenRequest).execute().use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    throw RuntimeException("Fail to send msg to yqwx. $responseContent")
                }
//                var accessTokenResponse = mapper.readValue(responseContent,AccessTokenResponse::class.java)
                val accessTokenResponse = mapper.readValue<AccessTokenResponse>(responseContent)
                if (accessTokenResponse.errcode == 0) {
                    accessToken = accessTokenResponse.access_token
                    timeOutStamp = LocalDateTime.now()
                } else {
                    throw RuntimeException("Fail to get wechat-work access_token：${accessTokenResponse.errmsg}")
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
        val jsonString = """
{
   "receiver":
   {
       "type": "single",
       "id": "$receiver"
   },
   "msgtype": "text",
   "text":
   {
       "content": "$text"
   }
}
            """

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
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
        val jsonString = """
{
   "receiver":
   {
       "type": "group",
       "id": "$chatId"
   },
   "msgtype": "text",
   "text":
   {
       "content": "$text"
   }
}
            """

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
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
        val jsonString = """
{
   "receiver":
   {
       "type": "group",
       "id": "$chatId"
   },
   "msgtype": "markdown",
   "markdown":
   {
       "content": "$markdown"
   }
}
            """

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
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
        val jsonString = objectMapper.writeValueAsString(richtextMessage)

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
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
        val jsonString = """
{
   "receiver":
   {
       "type": "single",
       "id": "$receiver"
   },
   "msgtype": "rich_text",
   "rich_text": [
   {
       "type": "text",
       "text": {
         "content": "$text"
       }
   },
   {
       "type": "link",
       "link": {
         "type": "click",
         "text": "人工服务",
         "key": "humanService"
       }
   },
  ]
}
"""

        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/send?access_token=$accessToken"
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    /*
    * 拉起新的群,企业微信咨询群
    * */
    fun createChat(title: String, userName: String): String {
        val userNameList = listOf("brandonliu", "zanyzhao", userName)
        return createChatByUserNames(title, userNameList)
    }

    /*
    * 拉起新的群，可以指定用户
    * */
    fun createChatByUserNames(title: String, userNameList: List<String>): String {
        var chatId = ""
        val accessToken = getAccessToken()
        var userNameString = ""
        var i = 0
        userNameList.forEach {
            userNameString += if (i == 0) {
                "\"$it\""
            } else {
                ",\"$it\""
            }
            i++
        }
        val jsonString = """
{
   "name": "$title",
   "userlist": [$userNameString]
}
"""
        val sendURL = "$wechatWorkApiURL/cgi-bin/tencent/chat/create?access_token=$accessToken"
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
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
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    // 发送Post请求
    private fun sendPostToWechatWorkApi(url: String, message: Any): Boolean {

        val accessToken = getAccessToken()
        val sendURL = "$url?access_token=$accessToken"
        val jsonString = objectMapper.writeValueAsString(message)
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
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

        val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), mediaInputStream.readBytes())
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
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send media to yqwx. $responseContent")
            }
            val uploadMediaResponse = mapper.readValue<UploadMediaResponse>(responseContent)
            if (uploadMediaResponse.errcode == 0) {
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
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        val sendRequest = Request.Builder()
            .url(sendURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            //        httpClient.newCall(sendRequest).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
            val userIdsConvertResponse = mapper.readValue<UserIdsConvertResponse>(responseContent)
            if (userIdsConvertResponse.errcode == 0) {
                userNameList.addAll(userIdsConvertResponse.user_list)
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