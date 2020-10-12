package com.tencent.devops.process.service.turbo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import com.tencent.devops.process.utils.PIPELINE_TURBO_TASK_ID
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.stereotype.Service
import java.util.*

@Service
class PipelineBuildTurboExtService @Autowired constructor(
		private val consulClient: ConsulDiscoveryClient?
) : PipelineBuildExtService {

	override fun buildExt(task: PipelineBuildTask): Map<String, String> {
		val taskType = task.taskType
		if(taskType.contains("linuxPaasCodeCCScript") || taskType.contains("linuxScript")) {
			logger.info("task need turbo, ${task.buildId}, ${task.taskName}, ${task.taskType}")
			val turboTask = getTurboTask(task.pipelineId, task.taskId)
			return mutableMapOf(
					PIPELINE_TURBO_TASK_ID to turboTask
			)
		}
		return emptyMap()
	}

	fun getTurboTask(pipelineId: String, elementId: String): String {
		try {
			val instances = consulClient!!.getInstances("turbo")
					?: return ""
			if (instances.isEmpty()) {
				return ""
			}
			val instance = loadBalance(instances)
			val url = "${if (instance.isSecure) "https" else
				"http"}://${instance.host}:${instance.port}/api/service/turbo/task/pipeline/$pipelineId/$elementId"

			logger.info("Get turbo task info, request url: $url")
			val startTime = System.currentTimeMillis()
			val request = Request.Builder().url(url).get().build()
			OkhttpUtils.doHttp(request).use { response ->
				val data = response.body()?.string() ?: return ""
				logger.info("Get turbo task info, response: $data")
				if (!response.isSuccessful) {
					throw RemoteServiceException(data)
				}
				val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
				val code = responseData["status"] as Int
				if (0 == code) {
					val dataMap = responseData["data"] as Map<String, Any>
					return dataMap["taskId"] as String? ?: ""
				} else {
					throw RemoteServiceException(data)
				}
			}
			LogUtils.costTime("call turbo cost", startTime)
		} catch (e: Throwable) {
			logger.warn("Get turbo task info failed, $e")
			return ""
		}
	}

	fun loadBalance(instances: List<ServiceInstance>) :  ServiceInstance{
		val random = Random()
		val index = random.nextInt(instances.size)
		return instances[index]
	}


	companion object {
		val logger = LoggerFactory.getLogger(this :: class.java)
	}
}