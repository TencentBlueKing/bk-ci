package com.tencent.devops.dispatch.devcloud.client

import com.tencent.devops.common.api.constant.CommonMessageCode.GET_STATUS_TIMED_OUT
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.ListPerformanceRsp
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.ListPerformancesReq
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceRsp
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.RsType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class DevCloudPerformanceClient {
    private val logger = LoggerFactory.getLogger(DevCloudPerformanceClient::class.java)

    companion object {
        private const val MODEL_LIST_API_PATH = "/api/model/list"
        private const val PERFORMANCE_INFO_API_PATH = "/api/model/info"
    }

    fun getPerformanceList(
        userId: String,
        projectId: String,
        pipelineId: String,
        templateId: String,
        retryTime: Int = 3
    ): List<PerformanceData> {
        return executeWithRetry(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            retryTime = retryTime,
            operation = "getPerformanceList"
        ) {
            val body = ListPerformancesReq(
                username = userId,
                t1 = projectId,
                t2 = pipelineId,
                tx = templateId,
                rsType = RsType.DOCKER.value
            )
            val request = getClientProxy().baseRequest(userId, MODEL_LIST_API_PATH, projectId, pipelineId)
                .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body?.string() ?: ""
                val logPrefix = getLogPrefix(userId, projectId, pipelineId)
                logger.info("$logPrefix getPerformanceList response: $responseContent")
                
                validateHttpResponse(response, logPrefix, "get performance list")
                
                val listPerformanceRsp = JsonUtil.to(responseContent, ListPerformanceRsp::class.java)
                validateBusinessResponse(listPerformanceRsp.actionCode, logPrefix, "get performance list")
                
                listPerformanceRsp.data
            }
        }
    }

    fun getPerformanceInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        performanceUid: String,
        retryTime: Int = 3
    ): PerformanceData {
        return executeWithRetry(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            retryTime = retryTime,
            operation = "getPerformanceInfo"
        ) {
            val logPrefix = getLogPrefix(userId, projectId, pipelineId)
            val body = mapOf("uid" to performanceUid)
            val request = getClientProxy().baseRequest(userId, PERFORMANCE_INFO_API_PATH, projectId, pipelineId)
                .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body?.string() ?: ""
                logger.info("$logPrefix getPerformanceInfo response: $responseContent")

                validateHttpResponse(response, logPrefix, "get performance info")

                val performanceRsp = JsonUtil.to(responseContent, PerformanceRsp::class.java)
                validateBusinessResponse(performanceRsp.actionCode, logPrefix, "get performance info")
                performanceRsp.data
            }
        }

    }

    /**
     * 通用的重试执行方法
     */
    private fun <T> executeWithRetry(
        userId: String,
        projectId: String,
        pipelineId: String,
        retryTime: Int,
        operation: String,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: SocketTimeoutException) {
            handleTimeoutException(userId, projectId, pipelineId, retryTime, operation, e, block)
        } catch (e: Exception) {
            // 对于非超时异常，如果还有重试次数，也进行重试
            if (retryTime > 0) {
                logger.warn("$userId|$projectId|$pipelineId $operation failed, retrying. Error: ${e.message}")
                return executeWithRetry(userId, projectId, pipelineId,retryTime - 1, operation, block)
            }
            throw e
        }
    }

    /**
     * 处理超时异常
     */
    private fun <T> handleTimeoutException(
        userId: String,
        projectId: String,
        pipelineId: String,
        retryTime: Int,
        operation: String,
        exception: SocketTimeoutException,
        block: () -> T
    ): T {
        val logPrefix = getLogPrefix(userId, projectId, pipelineId)
        if (retryTime > 0) {
            logger.info("$logPrefix $operation SocketTimeoutException. retry: $retryTime")
            return executeWithRetry(userId, projectId, pipelineId,retryTime - 1, operation, block)
        } else {
            logger.error("$logPrefix $operation failed after all retries.", exception)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.getErrorMessage(),
                errorMessage = "${I18nUtil.getCodeLanMessage(GET_STATUS_TIMED_OUT)}, operation: $operation"
            )
        }
    }

    /**
     * 验证HTTP响应
     */
    private fun validateHttpResponse(response: okhttp3.Response, logPrefix: String, operation: String) {
        if (!response.isSuccessful) {
            val errorMessage = "${ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.getErrorMessage()} Failed to $operation," +
                    " code: ${response.code}"
            logger.error("$logPrefix HTTP request failed: $errorMessage")
            throw BuildFailureException(
                ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.errorType,
                ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.errorCode,
                ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.getErrorMessage(),
                errorMessage
            )
        }
    }

    /**
     * 验证业务响应
     */
    private fun validateBusinessResponse(actionCode: Int, logPrefix: String, operation: String) {
        if (actionCode != 200) {
            val errorMessage = "${ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.getErrorMessage()} Failed to $operation, " +
                    "code: $actionCode"
            logger.error("$logPrefix Business response failed: $errorMessage")
            throw BuildFailureException(
                ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.errorType,
                ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.errorCode,
                ErrorCodeEnum.PERFORMANCE_INTERFACE_ERROR.getErrorMessage(),
                errorMessage
            )
        }
    }

    private fun getClientProxy(): ClientProxy {
        return SpringContextUtil.getBean(
                ClientProxy::class.java,
                "commonClientProxy"
            )

    }

    private fun getLogPrefix(userId: String, projectId: String, pipelineId: String): String {
        return "$userId|$projectId|$pipelineId"
    }
}
