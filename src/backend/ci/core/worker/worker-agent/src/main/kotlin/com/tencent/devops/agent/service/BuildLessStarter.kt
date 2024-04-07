package com.tencent.devops.agent.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.env.DockerEnv
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object BuildLessStarter {
    private val logger = LoggerFactory.getLogger(BuildLessStarter::class.java)

    fun waitK8sBuildLessJobStart() {
        var startFlag = false

        val podName = System.getenv("pod_name")
        val kubernetesManagerHost = System.getenv("kubernetes_manager_host")
        val loopUrl = "$kubernetesManagerHost/api/buildless/build/claim?podId=${strongPodName(podName)}"

        val request = Request.Builder()
            .url(loopUrl)
            .headers(
                mapOf(
                    "Accept" to "application/json",
                    "Devops-Token" to "landun"
                ).toHeaders()
            )
            .get()
            .build()
        do {
            logger.info("${LocalDateTime.now()} BuildLess loopUrl: $loopUrl")

            try {
                OkhttpUtils.doHttp(request).use { resp ->
                    startFlag = doK8sBuildLessResponse(resp)
                }
            } catch (e: Exception) {
                logger.error("${LocalDateTime.now()} Get buildLessTask error. continue loop... \n$e")
            }

            if (!startFlag) {
                Thread.sleep(1000)
            }
        } while (!startFlag)
    }

    private fun doK8sBuildLessResponse(
        resp: Response
    ): Boolean {
        val responseBody = resp.body?.string() ?: ""
        logger.info("${LocalDateTime.now()} Get buildLessTask response: $responseBody")
        if (resp.isSuccessful && responseBody.isNotBlank()) {
            val result = JsonUtil.to(responseBody, object : TypeReference<Result<BuildLessTask>>() {})
            if (result.status != 0 || result.data == null) {
                logger.warn("${LocalDateTime.now()} No buildLessTask, resp: ${resp.body} continue loop...")
                return false
            }

            DockerEnv.setAgentId(result.data!!.agentId)
            DockerEnv.setAgentSecretKey(result.data!!.secretKey)
            DockerEnv.setProjectId(result.data!!.projectId)
            DockerEnv.setBuildId(result.data!!.buildId)

            return true
        } else {
            logger.info("${LocalDateTime.now()} No buildLessTask, resp: ${resp.body} continue loop...")
            return false
        }
    }

    private fun strongPodName(podName: String): String {
        val strongPodName = System.getenv("random_str")
        return "$podName-$strongPodName"
    }
}

data class BuildLessTask(
    val projectId: String,
    val agentId: String,
    val pipelineId: String,
    val buildId: String,
    val vmSeqId: Int,
    val secretKey: String,
    val executionCount: Int
)
