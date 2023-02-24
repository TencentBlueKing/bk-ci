/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

    override fun buildExt(task: PipelineBuildTask, variable: Map<String, String>): Map<String, String> {
        val taskType = task.taskType
        val extMap = mutableMapOf<String, String>()
        if (taskType.contains("linuxPaasCodeCCScript") || taskType.contains("linuxScript")) {
            logger.info("task need turbo, ${task.buildId}, ${task.taskName}, ${task.taskType}")
            val turboTaskId = getTurboTask(task.projectId, task.pipelineId, task.taskId)
            extMap[PIPELINE_TURBO_TASK_ID] = turboTaskId
        }

        extMap.putAll(pipelineContextService.buildContext(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId,
            taskId = null,
            variables = variable
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
        val logger = LoggerFactory.getLogger(this :: class.java)
    }
}
