package com.tencent.devops.support.services

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.support.model.approval.CreateEsbMoaApproveParam
import com.tencent.devops.support.model.approval.CreateEsbMoaCompleteParam
import com.tencent.devops.support.model.approval.CreateMoaApproveRequest
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class MessageApproveService @Autowired constructor() {

    @Value("\${gateway.appCode}")
    private lateinit var appCode: String

    @Value("\${gateway.appSecret}")
    private lateinit var appSecret: String

    @Value("\${gateway.urlPrefix}")
    private lateinit var urlPrefix: String

    @Value("\${gateway.moa.completeUrl}")
    private lateinit var moaCompleteUrl: String

    @Value("\${gateway.moa.pushDataUrl}")
    private lateinit var moaPushDataUrl: String

    fun createMoaMessageApproval(userId: String, createMoaApproveRequest: CreateMoaApproveRequest): Result<Boolean> {
        logger.info("createMoaMessageApproval userId is :$userId, createMoaApproveRequest is :$createMoaApproveRequest")
        val createEsbMoaApproveParam = CreateEsbMoaApproveParam(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            verifier = createMoaApproveRequest.verifier,
            title = createMoaApproveRequest.title,
            taskId = createMoaApproveRequest.taskId,
            startDate = DateTimeUtil.formatDate(Date()),
            backUrl = createMoaApproveRequest.backUrl,
            sysUrl = createMoaApproveRequest.sysUrl
        )
        val requestBody = JsonUtil.toJson(createEsbMoaApproveParam)
        logger.info("the requestBody is:$requestBody")
        val request = Request.Builder()
            .url(urlPrefix + moaPushDataUrl)
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

    fun moaComplete(taskId: String): Result<Boolean> {
        logger.info("moaComplete taskId is :$taskId")
        val createEsbMoaCompleteParam = CreateEsbMoaCompleteParam(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            taskId = taskId
        )
        val requestBody = JsonUtil.toJson(createEsbMoaCompleteParam)
        logger.info("the requestBody is:$requestBody")
        val request = Request.Builder()
            .url(urlPrefix + moaCompleteUrl)
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

    companion object {
        private val logger = LoggerFactory.getLogger(MessageApproveService::class.java)
    }
}
