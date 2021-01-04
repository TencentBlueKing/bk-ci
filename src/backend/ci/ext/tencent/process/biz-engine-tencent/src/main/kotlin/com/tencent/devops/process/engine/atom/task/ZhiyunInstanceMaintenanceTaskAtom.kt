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

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.element.ZhiyunInstanceMaintenanceElement
import com.tencent.devops.common.pipeline.element.enums.ZhiyunOperation
import com.tencent.devops.common.pipeline.zhiyun.ZhiyunConfig
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ZHIYUN_FAIL
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ZhiyunInstanceMaintenanceTaskAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val zhiyunConfig: ZhiyunConfig
) : IAtomTask<ZhiyunInstanceMaintenanceElement> {
    override fun getParamElement(task: PipelineBuildTask): ZhiyunInstanceMaintenanceElement {
        return JsonUtil.mapTo(task.taskParams, ZhiyunInstanceMaintenanceElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: ZhiyunInstanceMaintenanceElement, runVariables: Map<String, String>): AtomResponse {
        val product = parseVariable(param.product, runVariables)
        val pkgName = parseVariable(param.pkgName, runVariables)
        val operation = param.operation.name
        val installPath = parseVariable(param.installPath, runVariables)
        val ips = parseVariable(param.ips, runVariables)
        val graceful = param.graceful?.toString() ?: ""
        val batchNum = if (!param.batchNum.isNullOrBlank()) parseVariable(param.batchNum, runVariables).toInt() else 0
        val batchInterval = if (!param.batchInterval.isNullOrBlank()) parseVariable(param.batchInterval, runVariables).toInt() else 0
        val curVersion = parseVariable(param.curVersion, runVariables)

        val userId = task.starter

        val instanceId = if (operation == ZhiyunOperation.ROLLBACK.name) {
            val url = "${zhiyunConfig.esbUrl}/rollbackEX"
            val requestData = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "caller" to zhiyunConfig.caller,
                "password" to zhiyunConfig.password,
                "operator" to userId,
                "para" to mapOf(
                    "product" to product,
                    "name" to pkgName,
                    "install_path" to installPath,
                    "curVersion" to curVersion,
                    "ips" to ips.split(",")
                )
            )
            createRollbackTask(requestData, url, task)
        } else {
            val url = "${zhiyunConfig.esbUrl}/instanceMaintenance"
            val requestData = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "caller" to zhiyunConfig.caller,
                "password" to zhiyunConfig.password,
                "operator" to userId,
                "para" to mapOf(
                    "product" to product,
                    "name" to pkgName,
                    "install_path" to installPath,
                    "operation" to operation.toLowerCase(),
                    "ips" to ips.split(","),
                    "graceful" to if ("true".equals(graceful, true)) "true" else "",
                    "batch_num" to batchNum,
                    "batch_interval" to batchInterval
                )
            )
            createTask(requestData, url, task)
        }

        // 等待返回结果
        logger.info("waiting for task done, timeout 10 minutes.")
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 11 * 60 * 1000) {
                logger.error("Wait for zhiyun timeout")
                buildLogPrinter.addRedLine(task.buildId, "织云操作失败,织云任务执行超时", task.taskId, task.containerHashId, task.executeCount ?: 1)
                throw BuildTaskException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ERROR_BUILD_TASK_ZHIYUN_FAIL.toInt(),
                    errorMsg = "织云操作失败,织云任务执行超时"
                )
            }
            Thread.sleep(2 * 1000)
            val isFinish = getInstanceInfoById(instanceId, userId, task)
            if (!isFinish) {
                continue@loop
            } else {
                return defaultSuccessAtomResponse
            }
        }
    }

    private fun getInstanceInfoById(instanceIds: List<Int>, userId: String, task: PipelineBuildTask): Boolean {
        val url = "${zhiyunConfig.esbUrl}/getInstanceInfoById"
        val requestData = mapOf(
            "app_code" to appCode,
            "app_secret" to appSecret,
            "caller" to zhiyunConfig.caller,
            "password" to zhiyunConfig.password,
            "operator" to userId,
            "para" to mapOf(
                "instanceId" to instanceIds
            )
        )
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody))
            .addHeader("apikey", zhiyunConfig.apiKey).build()
        OkhttpUtils.doHttp(request).use { res ->
            val responseBody = res.body()!!.string()
            logger.info("responseBody: $responseBody")

            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
            if ((responseData["code"] as String).toInt() != 0) {
                val msg = responseData["msg"]
                logger.error("zhiyun updateAsyncEX getInstanceInfo failed msg:$msg")
                buildLogPrinter.addRedLine(task.buildId, "织云操作失败,织云返回错误信息：$msg", task.taskId, task.containerHashId, task.executeCount ?: 1)
                throw BuildTaskException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ERROR_BUILD_TASK_ZHIYUN_FAIL.toInt(),
                    errorMsg = "织云操作失败,织云返回错误信息：$msg"
                )
            } else {
                val responseData = responseData["data"] as Map<String, Any>
                val instanceIdResults = (responseData["instanceIdResult"] as List<Map<String, Any>>)
                instanceIdResults.forEach {
                    val status = it["status"] as Int
                    if (0 != status && 1 != status) { // 非0 非1 失败
                        val errmsg = it["errmsg"] as String
                        val lastErrmsg = it["lastErrmsg"] as String
                        logger.error("zhiyun instanceMaintenance getInstanceInfo failed errmsg:$errmsg, lastErrmsg: $lastErrmsg")
                        buildLogPrinter.addRedLine(task.buildId, "织云操作失败,织云返回错误信息: errmsg：$errmsg, lastErrmsg: $lastErrmsg", task.taskId, task.containerHashId, task.executeCount ?: 1)
                        throw BuildTaskException(
                            errorType = ErrorType.SYSTEM,
                            errorCode = ERROR_BUILD_TASK_ZHIYUN_FAIL.toInt(),
                            errorMsg = "织云操作失败,织云返回错误信息: errmsg：$errmsg, lastErrmsg: $lastErrmsg"
                        )
                    } else if (0 == status) {
                        logger.info("zhiyun instanceMaintenance getInstanceInfo is running, continue waiting...")
                        return false
                    }
                }

                logger.info("zhiyun instanceMaintenance getInstanceInfo finished and success")
                buildLogPrinter.addLine(task.buildId, "织云操作成功！", task.taskId, task.containerHashId, task.executeCount ?: 1)
                return true
            }
        }
    }

    private fun createTask(requestData: Map<String, Any>, url: String, task: PipelineBuildTask): List<Int> {
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")
        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody))
            .addHeader("apikey", zhiyunConfig.apiKey).build()

        OkhttpUtils.doHttp(request).use { res ->
            val responseBody = res.body()!!.string()
            logger.info("responseBody: $responseBody")

            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
            if ((responseData["code"] as String).toInt() != 0) {
                val msg = responseData["msg"]
                logger.error("zhiyun instanceMaintenance failed msg:$msg")
                buildLogPrinter.addRedLine(task.buildId, "织云操作失败,织云返回错误信息：$msg", task.taskId, task.containerHashId, task.executeCount ?: 1)
                throw BuildTaskException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ERROR_BUILD_TASK_ZHIYUN_FAIL.toInt(),
                    errorMsg = "织云操作失败,织云返回错误信息：$msg"
                )
            } else {
                val responseData = responseData["data"] as Map<String, Any>
                val mids = (responseData["mid"] as List<String>).map { it.toInt() }
                logger.info("Zhiyun updateAsyncEX success, mid: $mids")
                buildLogPrinter.addLine(task.buildId, "织云操作开始，等待任务结束...【<a target='_blank' href='http://ccc.oa.com/package/tasks'>查看详情</a>】", task.taskId, task.containerHashId, task.executeCount ?: 1)
                return mids
            }
        }
    }

    private fun createRollbackTask(requestData: Map<String, Any>, url: String, task: PipelineBuildTask): List<Int> {
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")
        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody))
            .addHeader("apikey", zhiyunConfig.apiKey).build()

        OkhttpUtils.doHttp(request).use { res ->
            val responseBody = res.body()!!.string()
            logger.info("responseBody: $responseBody")

            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
            if ((responseData["code"] as String).toInt() != 0) {
                val msg = responseData["msg"]
                logger.error("zhiyun rollback failed msg:$msg")
                buildLogPrinter.addRedLine(task.buildId, "织云操作失败,织云返回错误信息：$msg", task.taskId, task.containerHashId, task.executeCount ?: 1)
                throw BuildTaskException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ERROR_BUILD_TASK_ZHIYUN_FAIL.toInt(),
                    errorMsg = "织云操作失败,织云返回错误信息：$msg"
                )
            } else {
                val responseData = responseData["data"] as Map<String, Any>
                val instanceIds = (responseData["instanceId"] as List<String>).map { it.toInt() }
                logger.info("Zhiyun rollback success, instanceIds: $instanceIds")
                buildLogPrinter.addLine(task.buildId, "织云操作开始，等待任务结束...【<a target='_blank' href='http://ccc.oa.com/package/tasks'>查看详情</a>】", task.taskId, task.containerHashId, task.executeCount ?: 1)
                return instanceIds
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ZhiyunInstanceMaintenanceTaskAtom::class.java)
        private val JSON = MediaType.parse("application/json;charset=utf-8")
        private const val appCode = "bkci"
        private const val appSecret = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
    }
}
