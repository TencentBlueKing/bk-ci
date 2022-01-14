package com.tencent.devops.dispatch.docker.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.docker.pojo.PerformanceMap
import com.tencent.devops.dispatch.docker.pojo.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsMap
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsShow
import com.tencent.devops.dispatch.docker.pojo.resource.UserDockerResourceOptionsVO
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DevcloudDebugServiceImpl @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val commonConfig: CommonConfig
) : ExtDebugService {

    private val logger = LoggerFactory.getLogger(DevcloudDebugServiceImpl::class.java)

    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String
    ): String? {
        return getDevcloudDebugUrl(userId, projectId, pipelineId, vmSeqId)
    }

    private fun getDevcloudDebugUrl(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String
    ): String {
        val url = String.format(
            "%s/ms/dispatch-devcloud/api/service/dispatchDevcloud/startDebug/projects/s%/pipeline/s%/vmSeq/s%",
            projectId,
            pipelineId,
            vmSeqId
        )
        val request = Request.Builder().url(url)
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader(AUTH_HEADER_DEVOPS_USER_ID, userId)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body()!!.string()
            logger.info("[$projectId get devcloud debugUrl responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["code"] == 0) {
                val debugResponse = objectMapper.readValue(JsonUtil.toJson(response["data"] ?: ""),
                    DevCloudDebugResponse::class.java)

                return debugResponse.websocketUrl
            } else {
                val msg = response["message"] as String
                logger.error("[$projectId] get devcloud debugUrl failed, msg: $msg")
                throw DockerServiceException(errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
                    errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                    errorMsg = "Get devcloud debugUrl failed, msg: $msg")
            }
        }
    }
}
