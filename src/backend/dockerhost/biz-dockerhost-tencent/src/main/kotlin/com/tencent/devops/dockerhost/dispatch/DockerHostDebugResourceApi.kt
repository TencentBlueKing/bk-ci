package com.tencent.devops.dockerhost.dispatch

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.pojo.ContainerInfo
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class DockerHostDebugResourceApi : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(DockerHostDebugResourceApi::class.java)

    fun startDebug(hostTag: String): Result<ContainerInfo>? {
        val path = "/dispatch/api/dockerhost/startDebug?hostTag=$hostTag"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun reportDebugContainerId(pipelineId: String, vmSeqId: String, containerId: String): Result<Boolean>? {
        val path = "/dispatch/api/dockerhost/reportDebugContainerId?pipelineId=$pipelineId&vmSeqId=$vmSeqId&containerId=$containerId"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun endDebug(hostTag: String): Result<ContainerInfo>? {
        val path = "/dispatch/api/dockerhost/endDebug?hostTag=$hostTag"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun rollbackDebug(pipelineId: String, vmSeqId: String, shutdown: Boolean? = false, msg: String? = ""): Result<Boolean>? {
        val path = "/dispatch/api/dockerhost/rollbackDebug?pipelineId=$pipelineId&vmSeqId=$vmSeqId&shutdown=$shutdown"
        val request = if (msg.isNullOrBlank()) {
            buildPost(path)
        } else {
            buildPost(path, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), msg))
        }

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }
}