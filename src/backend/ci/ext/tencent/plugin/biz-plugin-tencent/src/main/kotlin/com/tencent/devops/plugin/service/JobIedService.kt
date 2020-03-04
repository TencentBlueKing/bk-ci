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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Files
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.plugin.client.WeTestClient
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import net.dongliu.apk.parser.ApkFile
import com.tencent.devops.common.client.Client
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class JobIedService @Autowired constructor(
    val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobIedService::class.java)
    }

    @Value("\${esb.url}")
    private val esbUrl = "http://open.oa.com/component/compapi/job/"

    @Value("\${esb.code}")
    private val appCode = "bkci"

    @Value("\${esb.secret}")
    private val appSecret = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"

    fun taskList(projectId: String, operator: String): String {
        val ccAppId = getCCAppId(projectId)
        if(ccAppId == null) {
            throw Exception("Project does not have cc appId.")
        }
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = ccAppId
        requestData["operator"] = operator

        val json = ObjectMapper().writeValueAsString(requestData)
        try {
            val httpReq = Request.Builder()
                .url(esbUrl + "get_task")
                .post(RequestBody.create(jsonMediaType, json))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.postJson(url, requestStr)

                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["code"] == "00") {
                    val responseData = response["data"] as Map<String, Any>
                    val isFinished = responseData["isFinished"] as Boolean
                    logger.error("request success. taskInstanceId: $taskInstanceId")
                    return if (isFinished) {
                        val taskInstanceObj = responseData["taskInstance"] as Map<String, Any>
                        val status = taskInstanceObj["status"] as Int
                        if (status == SUCCESS) {
                            logger.info("Job execute task finished and success")
                            TaskResult(isFinish = true, success = true, msg = "Success")
                        } else {
                            logger.info("Job execute task finished but failed")
                            TaskResult(isFinish = true, success = false, msg = "Job failed")
                        }
                    } else {
                        TaskResult(isFinish = false, success = false, msg = "Job Running")
                    }
                } else {
                    val msg = response["message"] as String
                    logger.error("request failed, msg: $msg")
                    return TaskResult(isFinish = true, success = false, msg = msg)
                }
            }
        } catch (e: Exception) {
            logger.error("execute job error", e)
            throw TaskExecuteException(
                errorCode = USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "execute job error: ${e.message}")
        }
    }

    private fun getCCAppId(projectId:String): Long?{
        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
        return projectInfo?.ccAppId?

    }


}
