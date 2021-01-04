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

package com.tencent.devops.process.esb

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class JobCloudsFastExecuteScript @Autowired constructor(buildLogPrinter: BuildLogPrinter) :
    JobFastExecuteScript(buildLogPrinter) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobCloudsFastExecuteScript::class.java)
    }

    @Value("\${clouds.esb.url}")
    private val cloudsEsbUrl = "http://api-t.o.bkclouds.cc/c/clouds/compapi/job/"

    fun cloudsFastExecuteScript(
        buildId: String,
        operator: String,
        content: String,
        scriptParam: String,
        scriptTimeout: Int,
        openstate: String,
        targetAppId: Int,
        elementId: String,
        containerHashId: String?,
        executeCount: Int,
        paramSensitive: Int = 0,
        type: Int = 1,
        account: String = System.getProperty("user.name")
    ): Long {
        checkParam(operator, targetAppId, content, account)

        val taskInstanceId = sendTaskRequest(
            buildId = buildId,
            operator = operator,
            appId = targetAppId,
            content = content,
            scriptParam = scriptParam,
            scriptTimeout = scriptTimeout,
            type = type,
            openstate = openstate,
            account = account,
            elementId = elementId,
            containerHashId = containerHashId,
            executeCount = executeCount
        )

        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("Job start execute script failed.")
            throw OperationException("Job执行脚本失败")
        }

        return taskInstanceId
    }

    private fun sendTaskRequest(
        buildId: String,
        operator: String,
        appId: Int,
        content: String,
        scriptParam: String,
        scriptTimeout: Int,
        type: Int,
        openstate: String,
        account: String,
        elementId: String,
        containerHashId: String?,
        executeCount: Int
    ): Long {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["content"] = content
        requestData["script_params"] = Base64.getEncoder().encodeToString(scriptParam.toByteArray())
        requestData["script_timeout"] = scriptTimeout
        requestData["type"] = type
        requestData["account"] = account
        requestData["openstate"] = openstate
        requestData["uin"] = operator
        val url = cloudsEsbUrl + "dev_ops_fast_execute_script"
        return doSendTaskRequest(url, requestData, buildId, elementId, containerHashId, executeCount)
    }

    override fun getTaskResult(appId: Int, taskInstanceId: Long, operator: String): TaskResult {
        val url = cloudsEsbUrl + "get_task_result"
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["task_instance_id"] = taskInstanceId
        requestData["uin"] = operator
        return doGetTaskResult(url, requestData, taskInstanceId)
    }
}
