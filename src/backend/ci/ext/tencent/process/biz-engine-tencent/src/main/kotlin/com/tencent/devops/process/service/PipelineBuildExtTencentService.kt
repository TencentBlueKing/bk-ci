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
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_CONTENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
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
            val turboTaskId = getTurboTask(task.pipelineId, task.taskId)
            extMap[PIPELINE_TURBO_TASK_ID] = turboTaskId
        }
        val buildVar = pipelineContextService.buildContext(task.buildId, task.containerId, variable).toMutableMap()

        if (buildVar[PIPELINE_GIT_REF].isNullOrBlank()) {
            buildVar["ci.ref"] = buildVar[PIPELINE_GIT_REF]!!
            buildVar.remove(PIPELINE_GIT_REF)
        }
        if (buildVar[PIPELINE_GIT_HEAD_REF].isNullOrBlank()) {
            buildVar["ci.head_ref"] = buildVar[PIPELINE_GIT_HEAD_REF]!!
            buildVar.remove(PIPELINE_GIT_HEAD_REF)
        }
        if (buildVar[PIPELINE_GIT_BASE_REF].isNullOrBlank()) {
            buildVar["ci.base_ref"] = buildVar[PIPELINE_GIT_BASE_REF]!!
            buildVar.remove(PIPELINE_GIT_BASE_REF)
        }
        if (buildVar[PIPELINE_GIT_REPO].isNullOrBlank()) {
            buildVar["ci.repo"] = buildVar[PIPELINE_GIT_REPO]!!
            buildVar.remove(PIPELINE_GIT_REPO)
        }
        if (buildVar[PIPELINE_GIT_REPO_NAME].isNullOrBlank()) {
            buildVar["ci.repo_name"] = buildVar[PIPELINE_GIT_REPO_NAME]!!
            buildVar.remove(PIPELINE_GIT_REPO_NAME)
        }
        if (buildVar[PIPELINE_GIT_REPO_GROUP].isNullOrBlank()) {
            buildVar["ci.repo_group"] = buildVar[PIPELINE_GIT_REPO_GROUP]!!
            buildVar.remove(PIPELINE_GIT_REPO_GROUP)
        }
        if (buildVar[PIPELINE_GIT_EVENT].isNullOrBlank()) {
            buildVar["ci.event"] = buildVar[PIPELINE_GIT_EVENT]!!
            buildVar.remove(PIPELINE_GIT_EVENT)
        }
        if (buildVar[PIPELINE_GIT_EVENT_CONTENT].isNullOrBlank()) {
            buildVar["ci.event_content"] = buildVar[PIPELINE_GIT_EVENT_CONTENT]!!
            buildVar.remove(PIPELINE_GIT_EVENT_CONTENT)
        }
        if (buildVar[PIPELINE_GIT_SHA].isNullOrBlank()) {
            buildVar["ci.sha"] = buildVar[PIPELINE_GIT_SHA]!!
            buildVar.remove(PIPELINE_GIT_SHA)
        }
        if (buildVar[PIPELINE_GIT_SHA_SHORT].isNullOrBlank()) {
            buildVar["ci.sha_short"] = buildVar[PIPELINE_GIT_SHA_SHORT]!!
            buildVar.remove(PIPELINE_GIT_SHA_SHORT)
        }
        if (buildVar[PIPELINE_GIT_COMMIT_MESSAGE].isNullOrBlank()) {
            buildVar["ci.commit_message"] = buildVar[PIPELINE_GIT_COMMIT_MESSAGE]!!
            buildVar.remove(PIPELINE_GIT_COMMIT_MESSAGE)
        }
        extMap.putAll(buildVar)
        return extMap
    }

    override fun endBuild(task: PipelineBuildTask) = Unit

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
                LogUtils.costTime("call turbo ", startTime)
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
