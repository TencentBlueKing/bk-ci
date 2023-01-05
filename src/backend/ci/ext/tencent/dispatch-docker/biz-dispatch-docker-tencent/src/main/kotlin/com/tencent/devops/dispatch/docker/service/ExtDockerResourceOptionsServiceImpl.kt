package com.tencent.devops.dispatch.docker.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_GATEWAY_TAG
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
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
class ExtDockerResourceOptionsServiceImpl @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val commonConfig: CommonConfig,
    private val bkTag: BkTag
) : ExtDockerResourceOptionsService {

    private val logger = LoggerFactory.getLogger(ExtDockerResourceOptionsServiceImpl::class.java)

    override fun getDockerResourceConfigList(
        userId: String,
        projectId: String,
        buildType: String
    ): UserDockerResourceOptionsVO? {
        return if (buildType == BuildType.PUBLIC_DEVCLOUD.name) {
            getDevcloudResourceConfig(userId, projectId)
        } else {
            null
        }
    }

    private fun getDevcloudResourceConfig(
        userId: String,
        projectId: String
    ): UserDockerResourceOptionsVO {
        val url = String.format(
            "%s/ms/dispatch-devcloud/api/service/dispatchDevcloud/project/%s/performanceConfig/list",
            commonConfig.devopsIdcGateway,
            projectId
        )
        val request = Request.Builder().url(url)
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader(AUTH_HEADER_DEVOPS_USER_ID, userId)
            .addHeader(AUTH_HEADER_GATEWAY_TAG, bkTag.getLocalTag())
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body()!!.string()
            logger.info("[$projectId get devcloud resourceConfig responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["code"] == 0) {
                val dcUserPerformanceOptionsVO = objectMapper.readValue(JsonUtil.toJson(response["data"] ?: ""),
                    UserPerformanceOptionsVO::class.java)

                return UserDockerResourceOptionsVO(
                    default = dcUserPerformanceOptionsVO.default,
                    needShow = dcUserPerformanceOptionsVO.needShow,
                    dockerResourceOptionsMaps = getDockerResourceOptionsMap(dcUserPerformanceOptionsVO.performanceMaps)
                )
            } else {
                val msg = response["message"] as String
                logger.error("[$projectId] get devcloud resourceConfig failed, msg: $msg")
                throw DockerServiceException(errorType = ErrorCodeEnum.END_VM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.END_VM_ERROR.errorCode,
                    errorMsg = "Get devcloud resourceConfig failed, msg: $msg")
            }
        }
    }

    private fun getDockerResourceOptionsMap(performanceMaps: List<PerformanceMap>): List<DockerResourceOptionsMap> {
        val dockerResourceOptionsMaps = mutableListOf<DockerResourceOptionsMap>()
        performanceMaps.forEach {
            dockerResourceOptionsMaps.add(
                DockerResourceOptionsMap(
                    id = it.id,
                    dockerResourceOptionsShow = DockerResourceOptionsShow(
                        memory = it.performanceConfigVO.memory,
                        cpu = it.performanceConfigVO.cpu.toString(),
                        disk = it.performanceConfigVO.disk,
                        description = it.performanceConfigVO.description
                    )
            ))
        }

        return dockerResourceOptionsMaps
    }
}
