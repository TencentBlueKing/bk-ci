package com.tencent.devops.dispatch.codecc.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dispatch.codecc.exception.DockerServiceException
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.codecc.pojo.codecc.DockerHostBuildInfo
import com.tencent.devops.dispatch.codecc.pojo.PipelineTaskStatus
import com.tencent.devops.dispatch.codecc.utils.DockerHostUtils
import com.tencent.devops.dispatch.codecc.utils.RedisUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DockerHostClient @Autowired constructor(
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisUtils: RedisUtils,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostClient::class.java)

        private const val TLINUX1_2_IMAGE = "/bkdevops/docker-builder1.2:v1"
        private const val TLINUX2_2_IMAGE = "/bkdevops/docker-builder2.2:v1"
    }

    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    fun startBuild(
        dispatchMessage: DispatchMessage,
        dockerIp: String,
        dockerHostPort: Int,
        poolNo: Int,
        driftIpInfo: String
    ) {
        pipelineDockerBuildDao.startBuild(
            dslContext = dslContext,
            projectId = dispatchMessage.projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId.toInt(),
            secretKey = dispatchMessage.secretKey,
            status = PipelineTaskStatus.RUNNING,
            zone = Zone.SHENZHEN.name,
            dockerIp = dockerIp,
            poolNo = poolNo
        )

        val secretKey = dispatchMessage.secretKey
        val agentId = dispatchMessage.id
        logger.info("secretKey: $secretKey")
        logger.info("agentId: $agentId")

        val requestBody = DockerHostBuildInfo(
            projectId = dispatchMessage.projectId,
            agentId = agentId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = Integer.valueOf(dispatchMessage.vmSeqId),
            secretKey = secretKey,
            status = PipelineTaskStatus.RUNNING.status,
            imageName = dockerBuildImagePrefix + TLINUX2_2_IMAGE,
            containerId = "",
            wsInHost = true,
            poolNo = poolNo,
            registryUser = "",
            registryPwd = "",
            imageType = ImageType.BKDEVOPS.type,
            imagePublicFlag = true,
            imageRDType = "SELF_DEVELOPED",
            containerHashId = ""
        )

        dockerBuildStart(dockerIp, dockerHostPort, requestBody, dispatchMessage, driftIpInfo)
    }

    fun endBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        containerId: String,
        dockerIp: String
    ) {
        val requestBody = DockerHostBuildInfo(
            projectId = projectId,
            agentId = "",
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            secretKey = "",
            status = 0,
            imageName = "",
            containerId = containerId,
            wsInHost = true,
            poolNo = 0,
            registryUser = "",
            registryPwd = "",
            imageType = "",
            imagePublicFlag = false,
            imageRDType = null,
            containerHashId = ""
        )

        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/build/end", dockerIp)
        val request = Request.Builder().url(proxyUrl)
            .delete(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(requestBody)
                )
            )
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        OkhttpUtils.doHttp(request).use { resp ->
            val responseBody = resp.body!!.string()
            logger.info("[$projectId|$pipelineId|$buildId] End build Docker VM $dockerIp responseBody: $responseBody")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (response["status"] == 0) {
                response["data"] as Boolean
            } else {
                val msg = response["message"] as String
                logger.error("[$projectId|$pipelineId|$buildId] End build Docker VM failed, msg: $msg")
                throw DockerServiceException("End build Docker VM failed, msg: $msg")
            }
        }
    }

    private fun dockerBuildStart(
        dockerIp: String,
        dockerHostPort: Int,
        requestBody: DockerHostBuildInfo,
        dispatchMessage: DispatchMessage,
        driftIpInfo: String,
        retryTime: Int = 0,
        unAvailableIpList: Set<String>? = null
    ) {
        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl(
            "/api/docker/build/start",
            dockerIp,
            dockerHostPort
        )
        val request = Request.Builder().url(proxyUrl)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(),
                                     JsonUtil.toJson(requestBody)))
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}|" +
                        "$retryTime] Start build Docker VM $dockerIp, url: $proxyUrl, requestBody: $requestBody")
        OkhttpUtils.doLongHttp(request).use { resp ->
            if (resp.isSuccessful) {
                val responseBody = resp.body!!.string()
                logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}" +
                                "|${dispatchMessage.buildId}|$retryTime] Start build Docker VM $dockerIp " +
                                "responseBody: $responseBody")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                when {
                    response["status"] == 0 -> {
                        val containerId = response["data"] as String
                        logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}" +
                                        "|${dispatchMessage.buildId}|$retryTime] update container: $containerId")
                        // 更新task状态以及构建历史记录，并记录漂移日志
                        dockerHostUtils.updateTaskSimpleAndRecordDriftLog(
                            dispatchMessage = dispatchMessage,
                            containerId = containerId,
                            newIp = dockerIp,
                            driftIpInfo = driftIpInfo
                        )
                    }
                    response["status"] == 2 -> {
                        // 异常重试三次
                        if (retryTime < 3) {
                            logger.warn("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|" +
                                            "${dispatchMessage.buildId}|$retryTime] Start build Docker VM " +
                                            "in $dockerIp failed with status 2, retry startBuild. " +
                                            "message: ${resp.message}")
                            val retryTimeLocal = retryTime + 1
                            val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp)
                                ?: setOf(dockerIp)
                            // 当前IP不可用，保险起见将当前ip可用性置为false，并重新获取可用ip
                            pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)
                            val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIp(
                                dispatchMessage,
                                unAvailableIpListLocal)
                            dockerBuildStart(dockerIpLocalPair.first, dockerIpLocalPair.second, requestBody,
                                             dispatchMessage, driftIpInfo, retryTimeLocal, unAvailableIpListLocal)
                        } else {
                            logger.error("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|" +
                                             "${dispatchMessage.buildId}|$retryTime] Start build Docker VM failed " +
                                             "with status 2, retry $retryTime times. message: ${resp.message}")
                            throw DockerServiceException("Start build Docker VM failed, retry $retryTime times.")
                        }
                    }
                    else -> {
                        val msg = response["message"] as String
                        logger.error("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}" +
                                         "|${dispatchMessage.buildId}|$retryTime] " +
                                         "Start build Docker VM failed, msg: $msg")
                        throw DockerServiceException("Start build Docker VM failed, msg: $msg")
                    }
                }
            } else {
                // 异常重试四次
                if (retryTime < 4) {
                    logger.warn("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|" +
                                    "${dispatchMessage.buildId}|$retryTime] Start build Docker VM in $dockerIp " +
                                    "failed, retry startBuild. message: ${resp.message}")
                    val retryTimeLocal = retryTime + 1
                    if (retryTime < 2) {
                        dockerBuildStart(dockerIp, dockerHostPort, requestBody, dispatchMessage,
                                         driftIpInfo, retryTimeLocal, unAvailableIpList)
                    } else {
                        val unAvailableIpListLocal: Set<String> = unAvailableIpList?.plus(dockerIp) ?: setOf(dockerIp)
                        // 当前IP不可用，保险起见将当前ip可用性置为false，并重新获取可用ip
                        pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, dockerIp, false)
                        val dockerIpLocalPair = dockerHostUtils.getAvailableDockerIp(
                            dispatchMessage,
                            unAvailableIpListLocal
                        )
                        dockerBuildStart(dockerIpLocalPair.first, dockerIpLocalPair.second, requestBody,
                                         dispatchMessage, driftIpInfo, retryTimeLocal, unAvailableIpListLocal)
                    }
                } else {
                    logger.error("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|" +
                                     "${dispatchMessage.buildId}|$retryTime] " +
                                     "Start build Docker VM failed, retry $retryTime times. message: ${resp.message}")
                    throw DockerServiceException("Start build Docker VM failed, retry $retryTime times.")
                }
            }
        }
    }
}
