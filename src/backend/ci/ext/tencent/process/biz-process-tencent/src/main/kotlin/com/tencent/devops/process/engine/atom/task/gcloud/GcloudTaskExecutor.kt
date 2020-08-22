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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.atom.task.gcloud

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class GcloudTaskExecutor(
    var buildLogPrinter: BuildLogPrinter,
    var gcloudApiUrl: String,
    var buildId: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GcloudTaskExecutor::class.java)

        private const val SUCCESS = "SUCCESS"
        private const val PENDING = "PENDING"
        private const val RUNNING = "RUNNING"
        private const val SUSPEND = "SUSPEND"
        private const val FAILURE = "FAILURE"
        private const val REVOKED = "REVOKED"

        private const val appCode = "ci_gcloud"
        private const val appSecret = "r.6E\$J+xKQ,kjz+k.4Ae<b<~ol0W6.ry!%,1fx05>R&JGQWqqL"
    }

    private fun executeTask(apiAuthCode: String, operator: String, taskId: Int?, elementId: String, containerId: String, executeCount: Int): CallResult<Int> {
        val requestData = mutableMapOf<String, Any>()

        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["username"] = operator

        requestData["operator"] = operator
        requestData["api_authorization_code"] = apiAuthCode

        val requestStr = ObjectMapper().writeValueAsString(requestData)

        try {
            val url = "$gcloudApiUrl/execute_task/$taskId/"
            val httpReq = Request.Builder()
                    .url(url)
                    .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
                    .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
                //            val responseStr = HttpUtils.postJson(url, requestStr)

                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                return if (response["res"] == false) {
                    val msg = response["msg"].toString()
                    CallResult(false, 0, msg)
                } else {
                    val data = response["data"] as Map<*, *>
                    val bpmTaskId = data["bpm_task_id"] as Int
                    CallResult(true, bpmTaskId, "success")
                }
            }
        } catch (e: Exception) {
            logger.error("start gcloud task error", e)
            buildLogPrinter.addRedLine(buildId, "start gcloud task error: ${e.message}", elementId, containerId, executeCount)
            throw RuntimeException("start gcloud task error")
        }
    }

    private fun createTask(
        operator: String,
        apiAuthCode: String,
        templateId: Int,
        params: Map<String, String>?,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): CallResult<Int> {
        val requestData = mutableMapOf<String, Any>()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["username"] = operator

        requestData["template_id"] = templateId
        requestData["operator"] = operator
        requestData["api_authorization_code"] = apiAuthCode

        if (params != null && params.isNotEmpty()) {
            requestData["task_parameters"] = params
        } else {
            requestData["task_parameters"] = emptyMap<String, Any>()
        }

        requestData["ignore_step_names"] = emptyList<Any>()
        requestData["task_circumstance"] = "test"
        requestData["task_name"] = "task_" + System.currentTimeMillis()

        val requestStr = jacksonObjectMapper().writeValueAsString(requestData)
        try {
            val url = "$gcloudApiUrl/simple_create_task/"
            logger.info("http request url: $url")
            logger.info("http request body: $requestStr")

            val httpReq = Request.Builder()
                    .url(url)
                    .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
                    .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.postJson(url, requestStr)
                logger.info("http response: $responseStr")

                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                return if (response["res"] == false) {
                    val msg = response["msg"].toString()
                    CallResult(false, 0, msg)
                } else {
                    val taskId = response["data"] as Int
                    CallResult(true, taskId, "success")
                }
            }
        } catch (e: Exception) {
            logger.error("create gcloud task error", e)
            buildLogPrinter.addRedLine(buildId, "create gcloud task error: ${e.message}", elementId, containerId, executeCount)
            throw RuntimeException("create gcloud task error: ${e.message}")
        }
    }

    private fun getTaskExecuteResult(operator: String, bpmTaskId: Int?): CallResult<Boolean> {
        try {
            val url = "$gcloudApiUrl/get_task_execute_result/$bpmTaskId/"

            val params = HashMap<String, String>()
            params["app_code"] = appCode
            params["app_secret"] = appSecret
            params["username"] = operator

            logger.info("get_task_execute_result url:$url")
//            val responseStr = HttpUtils.get(url, params)
            OkhttpUtils.doGet("$url?${OkhttpUtils.joinParams(params)}").use { resp ->
                val responseStr = resp.body()!!.string()
                logger.info("get_task_execute_result response:$responseStr")

                // 响应示例
                // 			"task_state_summary": "BLOCKED",
                // 		    "task_step": "FAILURE",
                // 		    "step_time_list": {},
                // 		    "result": true,
                // 		    "steps_dict": {
                // 		        "发布": {
                // 		            "node_tasks": [
                // 		                {
                // 		                    "is_text_node": false,
                // 		                    "state": "FAILURE",
                // 		                    "sleep_time": "",
                // 		                    "node_num": 0,
                // 		                    "is_sleep_node": false,
                // 		                    "is_chg_renovate_node": false,
                // 		                    "id": 84951577,
                // 		                    "retry_times": 0,
                // 		                    "auto_ignore": false
                // 		                }
                // 		            ],
                // 		            "stage_name": "发布",
                // 		            "is_stop_after": false,
                // 		            "state": "FAILURE",
                // 		            "is_timer_run": false,
                // 		            "id": 84951568
                // 		        }
                // 		    },

                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["result"] == true) {
                    val taskState = response["task_state_summary"] as String
                    when {
                        SUCCESS == taskState -> return CallResult(true, true, "success")
                        FAILURE == taskState -> return CallResult(false, true, "task failed")
                        REVOKED == taskState -> return CallResult(false, true, "task revoked")
                        else -> {
                            if (FAILURE == response["task_step"]) {
                                return CallResult(false, true, "task step failed")
                            } else {
                                val stepsDickObj = response["steps_dict"] as Map<*, *>
                                for (key in stepsDickObj.keys) {
                                    val itemObj = stepsDickObj[key] as Map<*, *>
                                    if (FAILURE == itemObj["state"]) {
                                        return CallResult(false, true, "task step failed")
                                    } // 如果还不满足要求，继续往下解析node_tasks
                                }
                            }
                            // 非以上三种状态，继续轮询结果
                            return CallResult(true, false, "running")
                        }
                    }
                } else {
                    logger.error("get task result failed")
                    return CallResult(false, true, "get task result failed")
                }
            }
        } catch (e: Exception) {
            logger.error("execute gcloud task error", e)
            return CallResult(false, true, "execute gcloud task error: ${e.message}")
        }
    }

    private fun threadSleep(second: Int) {
        try {
            Thread.sleep((second * 1000).toLong())
        } catch (e: InterruptedException) {
            //
        }
    }

    fun syncRunGcloudTask(
        appId: Int,
        operator: String,
        apiAuthCode: String,
        tmplId: Int,
        params: Map<String, String>?,
        timeoutInSeconds: Int,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): CallResult<Any> {
        val createTaskResult = createTask(operator, apiAuthCode, tmplId, params, elementId, containerId, executeCount)
        if (createTaskResult.success) {
            val taskId = createTaskResult.data
            logger.info("create gcloud task success, taskId: $taskId")
            buildLogPrinter.addLine(buildId, "create gcloud task success, taskId: $taskId", elementId, containerId, executeCount)
            buildLogPrinter.addLine(buildId, "gcloud task: http://open.oa.com/?app=gcloud&url=/s/gcloud/custom/get_flow_info_by_task/$taskId/$appId/?pagetype=0",
                    elementId, containerId, executeCount)
            val runTaskResult = executeTask(apiAuthCode, operator, taskId, elementId, containerId, executeCount)
            if (!runTaskResult.success) {
                buildLogPrinter.addRedLine(buildId, "start gcloud task failed: ${runTaskResult.message}", elementId, containerId, executeCount)
                return CallResult(false, null, "start gcloud task failed")
            }

            val bpmTaskId = runTaskResult.data
            buildLogPrinter.addLine(buildId, "start gcloud task success, bpmTaskId: $bpmTaskId", elementId, containerId, executeCount)
            val taskResult = waitUtilTaskDone(operator, bpmTaskId, timeoutInSeconds, elementId, containerId, executeCount)
            if (!taskResult.success) {
                logger.info("execute gcloud task failed")
                buildLogPrinter.addRedLine(buildId, "execute gcloud task failed: ${taskResult.message}", elementId, containerId, executeCount)
                return CallResult(false, null, "execute gcloud task failed")
            } else {
                logger.info("execute gcloud task success")
                buildLogPrinter.addLine(buildId, "execute gcloud task success", elementId, containerId, executeCount)
            }

            return CallResult(true, null, "execute gcloud task success")
        } else {
            logger.info("create gcloud task failed: ${createTaskResult.message}")
            buildLogPrinter.addRedLine(buildId, "create gcloud task failed: ${createTaskResult.message}", elementId, containerId, executeCount)
            return CallResult(false, null, "create gcloud task failed: ${createTaskResult.message}")
        }
    }

    private fun waitUtilTaskDone(operator: String, bpmTaskId: Int?, timeoutInSeconds: Int, elementId: String, containerId: String, executeCount: Int): CallResult<Any> {
        buildLogPrinter.addLine(buildId, "waiting for task done, timeout setting: ${timeoutInSeconds}s", elementId, containerId, executeCount)
        val startTime = System.currentTimeMillis()
        while (true) {
            if (System.currentTimeMillis() - startTime > timeoutInSeconds * 1000) {
                return CallResult(false, null, "execute gcloud task timeout")
            }

            threadSleep(10)
            val result = getTaskExecuteResult(operator, bpmTaskId)
            if (result.data == false) {
                continue
            } else if (!result.success) {
                return CallResult(false, null, result.message)
            } else if (result.data!!) {
                return CallResult(true, null, result.message)
            }
        }
    }

    data class CallResult<T>(val success: Boolean, val data: T?, val message: String)
}
