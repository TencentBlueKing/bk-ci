package com.tencent.devops.remotedev.service.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.cvd.CvdCreateTaskRequest
import com.tencent.devops.remotedev.pojo.cvd.CvdDeleteTaskRequest
import com.tencent.devops.remotedev.pojo.cvd.CvdPoolDetail
import com.tencent.devops.remotedev.pojo.cvd.CvdTaskResponse
import com.tencent.devops.remotedev.pojo.cvd.CvdTaskStatusResponse
import com.tencent.devops.remotedev.pojo.cvd.CvdUserPoolInfoResponse
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CvdService {

    @Value("\${remoteDev.appCode:}")
    private val appId: String = ""

    @Value("\${cvd.token:}")
    private val cvdToken: String = ""

    @Value("\${cvd.url:}")
    private val cvdUrl: String = ""

    fun createTask(request: CvdCreateTaskRequest): CvdTaskResponse {
        val body = mapOf(
            "username" to request.username,
            "bkProjectId" to request.bkProjectId,
            "poolId" to request.poolId,
            "diskId" to request.diskId
        )
        val resp = doPost<CvdApiResponse<CvdTaskResponse>>(
            path = "/app/cvd/ccTask/create",
            body = body,
            typeRef = object : TypeReference<CvdApiResponse<CvdTaskResponse>>() {}
        )
        return resp.data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
            params = arrayOf("cvdCreateTask", resp.message ?: "empty data")
        )
    }

    fun deleteTask(request: CvdDeleteTaskRequest): CvdTaskResponse {
        val body = mapOf(
            "username" to request.username,
            "bkProjectId" to request.bkProjectId,
            "poolId" to request.poolId,
            "instanceId" to request.instanceId
        )
        val resp = doPost<CvdApiResponse<CvdTaskResponse>>(
            path = "/app/cvd/ccTask/delete",
            body = body,
            typeRef = object : TypeReference<CvdApiResponse<CvdTaskResponse>>() {}
        )
        return resp.data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
            params = arrayOf("cvdDeleteTask", resp.message ?: "empty data")
        )
    }

    fun getTaskStatus(taskId: String): CvdTaskStatusResponse {
        val body = mapOf("taskId" to taskId)
        val resp = doPost<CvdApiResponse<CvdTaskStatusResponse>>(
            path = "/app/cvd/ccTask/taskStatus",
            body = body,
            typeRef = object : TypeReference<CvdApiResponse<CvdTaskStatusResponse>>() {}
        )
        return resp.data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
            params = arrayOf("cvdTaskStatus", resp.message ?: "empty data")
        )
    }

    fun getUserPoolInfo(
        username: String
    ): CvdUserPoolInfoResponse {
        val body = mapOf("username" to username)
        val resp = doPost<CvdApiResponse<CvdUserPoolInfoResponse>>(
            path = "/app/cvd/ccTask/userPoolInfo",
            body = body,
            typeRef = object : TypeReference<CvdApiResponse<CvdUserPoolInfoResponse>>() {}
        )
        return resp.data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
            params = arrayOf(
                "cvdUserPoolInfo",
                resp.message ?: "empty data"
            )
        )
    }

    fun getProjectPoolInfo(
        bkProjectId: String
    ): List<CvdPoolDetail> {
        val body = mapOf("bkProjectId" to bkProjectId)
        val resp = doPost<CvdApiResponse<List<CvdPoolDetail>>>(
            path = "/app/cvd/ccTask/projectPoolInfo",
            body = body,
            typeRef = object : TypeReference<CvdApiResponse<List<CvdPoolDetail>>>() {}
        )
        return resp.data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
            params = arrayOf(
                "cvdProjectPoolInfo",
                resp.message ?: "empty data"
            )
        )
    }

    private fun <T : CvdApiResponse<*>> doPost(
        path: String,
        body: Map<String, Any>,
        typeRef: TypeReference<T>
    ): T {
        val url = "$cvdUrl$path"
        val jsonBody = JsonUtil.toJson(body)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        logger.debug("cvdRequest|$url|$jsonBody")
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .headers(buildAuthHeaders())
            .build()

        try {
            OkhttpUtils.doHttp(request).use { resp ->
                val responseStr = resp.body!!.string()
                logger.debug("cvdRequest|$path|$responseStr|$request")
                if (!resp.isSuccessful) {
                    logger.warn(
                        "cvdRequest failed|$path|" +
                            "${resp.code}|$responseStr"
                    )
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
                        params = arrayOf(
                            path, "${resp.code}|$responseStr"
                        )
                    )
                }
                val data = JsonUtil.to(responseStr, typeRef)
                if (data.code != CVD_SUCCESS_CODE) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
                        params = arrayOf(
                            path, "${data.code}|${data.message}"
                        )
                    )
                }
                return data
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.warn("cvdRequest error|$path", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
                params = arrayOf(path, e.localizedMessage ?: "unknown")
            )
        }
    }

    private fun buildAuthHeaders(): okhttp3.Headers {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val random = RandomStringUtils.randomAlphanumeric(RANDOM_LENGTH)
        val encKey = ShaUtils.sha256("$cvdToken$timestamp$random")
        return mapOf(
            "APPID" to appId,
            "RANDOM" to random,
            "TIMESTP" to timestamp,
            "ENCKEY" to encKey,
            "content-type" to "application/json"
        ).toHeaders()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CvdService::class.java)
        private const val CVD_SUCCESS_CODE = 200
        private const val RANDOM_LENGTH = 8
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdApiResponse<T>(
    val code: Int?,
    val data: T?,
    val message: String?
)
