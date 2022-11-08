package com.tencent.devops.dockerhost.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.script.ShellUtil
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.pojo.LeakScanReq
import com.tencent.devops.dockerhost.pojo.LeakScanResponse
import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import com.tencent.devops.dockerhost.services.image.ImageHandlerContext
import com.tencent.devops.dockerhost.utils.CommonUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class TXDockerHostImageScanService(
    private val dockerHostConfig: DockerHostConfig
) : DockerHostImageScanService {

    @Value("\${dockerCli.dockerSavedPath:/data/dockersavedimages}")
    var dockerSavedPath: String? = "/data/dockersavedimages"

    override fun scanningDocker(
        imageHandlerContext: ImageHandlerContext,
        dockerClient: DockerClient
    ): String {
        val logSuffix = "[${imageHandlerContext.buildId}]|[${imageHandlerContext.vmSeqId}]"

        lateinit var longDockerClient: DockerClient
        try {
            val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerConfig(dockerHostConfig.dockerConfig)
                .withApiVersion(dockerHostConfig.apiVersion)
                .build()

            val longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
                .dockerHost(config.dockerHost)
                .sslConfig(config.sslConfig)
                .connectTimeout(5000)
                .readTimeout(600000)
                .build()

            longDockerClient = DockerClientBuilder.getInstance(config).withDockerHttpClient(longHttpClient).build()

            // 敏感信息扫描
            val sensitiveResult = scanSensitive(logSuffix, imageHandlerContext)

            // 漏洞扫描
            val leakResult = scanLeak(logSuffix, dockerClient, imageHandlerContext.imageId)

            // 返回结果map
            val resultMap = mapOf(
                SENSITIVE_RESULT to sensitiveResult,
                LEAK_RESULT to leakResult
            )

            return JsonUtil.toJson(resultMap)
        } catch (e: Exception) {
            logger.error("$logSuffix scan docker error.", e)
        } finally {
            try {
                longDockerClient.close()
            } catch (e: IOException) {
                logger.error("$logSuffix Long docker client close exception: ${e.message}")
            }
        }

        return ""
    }

    fun scanSensitive(
        logSuffix: String,
        imageHandlerContext: ImageHandlerContext
    ): String {
        // 取任意一个tag扫描就可以
        val imageTag = imageHandlerContext.imageTagSet.first()

        try {
            logger.info("$logSuffix start scan dockerimage, imageId: ${imageHandlerContext.imageId}")
            val script = "dockerscan -t ${imageHandlerContext.imageId} -p ${imageHandlerContext.pipelineId} " +
                "-u $imageTag -i dev -T ${imageHandlerContext.projectId} -b ${imageHandlerContext.buildId} " +
                "-n ${imageHandlerContext.userName}"
            val scanResult = ShellUtil.executeEnhance(script)
            logger.info("$logSuffix scan docker $imageTag result: $scanResult")

            logger.info("$logSuffix scan image success, now remove local image, " +
                            "image name and tag: $imageTag")

            return scanResult
        } catch (e: Throwable) {
            logger.error("$logSuffix Docker image scanSensitive failed, msg: ${e.message}")
            return ""
        }
    }

    fun scanLeak(
        logSuffix: String,
        dockerClient: DockerClient,
        imageId: String
    ): String {
        try {
            val script = "docker inspect $imageId"
            val scanResult = ShellUtil.executeEnhance(script)

            val leakScanReq = LeakScanReq(
                content = scanResult,
                format_type = SCAN_FORMAT
            )

            val dockerIp = CommonUtils.getInnerIP()
            val request = Request.Builder().url("http://$dockerIp:8887/scan/directory/")
                .addHeader("Accept", "application/json; charset=utf-8")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(
                    RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        JsonUtil.toJson(leakScanReq)))
                .build()

            OkhttpUtils.doHttp(request).use { resp ->
                val responseBody = resp.body()!!.string()
                logger.info("$logSuffix Leak scan imageId: $imageId responseBody: $responseBody")
                val response: LeakScanResponse = jacksonObjectMapper().readValue(responseBody)

                // 请求结果OK
                if (response.result) {
                    return response.data
                } else {
                    logger.warn("$logSuffix Leak scan result: false, msg: ${response.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("$logSuffix Docker image scanLeak failed, msg: ${e.message}")
        }

        return ""
    }

    fun toHexStr(byteArray: ByteArray) =
        with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) append("0").append(hexStr)
                else append(hexStr)
            }
            toString()
        }

    companion object {
        private val logger = LoggerFactory.getLogger(TXDockerHostImageScanService::class.java)
        private const val SCAN_FORMAT = "html"
        private const val SENSITIVE_RESULT = "sensitiveResult"
        private const val LEAK_RESULT = "leakResult"
    }
}
