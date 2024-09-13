package com.tencent.devops.process.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import com.tencent.devops.process.utils.PIPELINE_TURBO_TASK_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.stereotype.Service
import java.util.Random

@Service
class PipelineBuildExtTencentService @Autowired constructor(
    private val consulClient: ConsulDiscoveryClient?,
    private val pipelineContextService: PipelineContextService
) : PipelineBuildExtService {

    override fun buildExt(task: PipelineBuildTask, variables: Map<String, String>): Map<String, String> {
        val taskType = task.taskType
        val extMap = mutableMapOf<String, String>()
        if (taskType.contains("linuxPaasCodeCCScript") || taskType.contains("linuxScript")) {
            logger.info("task need turbo, ${task.buildId}, ${task.taskName}, ${task.taskType}")
            val turboTaskId = getTurboTask(task.projectId, task.pipelineId, task.taskId)
            extMap[PIPELINE_TURBO_TASK_ID] = turboTaskId
            extMap["turbo.task.id"] = turboTaskId
        }

        extMap.putAll(pipelineContextService.buildContext(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId,
            taskId = null,
            variables = variables,
            executeCount = task.executeCount
        ))
        return extMap
    }

    override fun endBuild(task: PipelineBuildTask) = Unit

    fun getTurboTask(projectId: String, pipelineId: String, elementId: String): String {
        try {
            val instances = consulClient!!.getInstances("turbo-new")
                    ?: return ""
            if (instances.isEmpty()) {
                return ""
            }
            val instance = loadBalance(instances)
            val apiUrl = "/api/service/turboPlan/pipelineId/$pipelineId/pipelineElementId/$elementId"
            val url = "${
                if (instance.isSecure) "https" else
                    "http"
            }://${instance.host}:${instance.port}$apiUrl"

            logger.info("Get turbo task info, request url: $url")
            val startTime = System.currentTimeMillis()
            OkhttpUtils.doGet(url, mapOf(AUTH_HEADER_DEVOPS_PROJECT_ID to projectId)).use { response ->
                val data = response.body?.string() ?: return ""
                logger.info("Get turbo task info, response: $data")
                LogUtils.costTime("call turbo ", startTime)
                if (!response.isSuccessful) {
                    throw RemoteServiceException(data)
                }
                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
                val code = responseData["code"] as Int
                if (0 == code) {
                    return responseData["data"] as? String ?: ""
                } else {
                    throw RemoteServiceException(data)
                }
            }
        } catch (e: Throwable) {
            logger.warn("Get turbo task info failed, $e")
            return ""
        }
    }

    fun loadBalance(instances: List<ServiceInstance>): ServiceInstance {
        val random = Random()
        val index = random.nextInt(instances.size)
        return instances[index]
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildExtTencentService::class.java)
    }
}
