package com.tencent.devops.monitoring.services

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.monitoring.pojo.NocNoticeBusData
import com.tencent.devops.monitoring.pojo.NocNoticeRequest
import com.tencent.devops.monitoring.pojo.NocNoticeUserInfo
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class NocNoticeService {
    private val logger = LoggerFactory.getLogger(NocNoticeService::class.java)

    @Value("\${gateway.appCode}")
    private lateinit var appCode: String

    @Value("\${gateway.appSecret}")
    private lateinit var appSecret: String

    @Value("\${gateway.uwork.sendNocNoticeUrl}")
    private lateinit var sendNocNoticeUrl: String

    /**
     * 发送noc语音告警
     */
    fun sendNocNotice(
        notifyReceivers: Set<String>,
        notifyTitle: String,
        notifyMessage: String,
        busiDataList: List<NocNoticeBusData>
    ): Result<Boolean> {
        logger.info("the notifyReceivers is:$notifyReceivers,notifyTitle is:$notifyTitle,notifyMessage is:$notifyMessage,busiDataList is:$busiDataList")
        val userInfoList = mutableListOf<NocNoticeUserInfo>()
        notifyReceivers.forEach {
            userInfoList.add(NocNoticeUserInfo(username = it)) // 内部用户无需填手机号，noc系统会根据rtx名称从oa系统查出来
        }
        val nocNoticeRequest = NocNoticeRequest(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            autoReadMessage = notifyTitle,
            headDesc = notifyTitle,
            busiDataList = busiDataList,
            userInfoList = userInfoList,
            noticeInformation = notifyMessage
        )
        val requestBody = JsonUtil.toJson(nocNoticeRequest)
        logger.info("the requestBody is:$requestBody")
        val request = Request.Builder()
            .url(sendNocNoticeUrl)
            .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { res ->
            val data = res.body()!!.string()
            logger.info("the response>> $data")
            if (!res.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            val response: Map<String, Any> = JsonUtil.toMap(data)
            val code = response["code"]
            if (code != "00") {
                return Result(status = code.toString().toInt(), message = response["message"] as String) // 发送失败抛出错误提示
            }
        }
        return Result(true)
    }
}
