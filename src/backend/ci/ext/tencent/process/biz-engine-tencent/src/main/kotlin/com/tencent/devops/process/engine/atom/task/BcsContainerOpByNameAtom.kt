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

@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode.USER_TASK_OPERATE_FAIL
import com.tencent.devops.common.api.pojo.ErrorCode.USER_INPUT_INVAILD
import com.tencent.devops.common.api.pojo.ErrorCode.USER_RESOURCE_NOT_FOUND
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.BcsContainerOpByNameElement
import com.tencent.devops.common.pipeline.element.bcs.BcsCommandStatus
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.third.enum.BcsCategory
import com.tencent.devops.process.pojo.third.enum.BcsOperation
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class BcsContainerOpByNameAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val bkAuthTokenApi: BSAuthTokenApi,
    private val serviceCode: PipelineAuthServiceCode
) : IAtomTask<BcsContainerOpByNameElement> {

    override fun getParamElement(task: PipelineBuildTask): BcsContainerOpByNameElement {
        return JsonUtil.mapTo(task.taskParams, BcsContainerOpByNameElement::class.java)
    }

    @Value("\${project.url}")
    private val projectUrl = "http://paas-cc.apigw.o.oa.com/uat/projects/"

    @Value("\${bcsApp.url}")
    private val bcsAppUrl = "http://paas-cd.apigw.o.oa.com/staging/apps/"

    override fun execute(
        task: PipelineBuildTask,
        param: BcsContainerOpByNameElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        if (param.opType.isBlank()) {
            buildLogPrinter.addRedLine(task.buildId, "opType is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "opType is not init"
            )
        }
        if (param.category.isNullOrBlank()) {
            logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| category is not init")
            buildLogPrinter.addRedLine(task.buildId, "category is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "category is not init"
            )
        }

        val projectCode = task.projectId
        val projectId = getProjectOriginId(projectCode)
        val opType = parseVariable(param.opType, runVariables)
        val bcsOperation = BcsOperation.parse(opType)

        return if (bcsOperation == BcsOperation.CREATE) {
            createInstance(task, projectId, param, runVariables)
        } else {
            doBcsOperation(param, bcsOperation, projectId, task, runVariables)
        }
    }

    private fun doBcsOperation(
        param: BcsContainerOpByNameElement,
        opType: BcsOperation,
        projectId: String,
        task: PipelineBuildTask,
        runVariables: Map<String, String>
    ): AtomResponse {

        // 公共的参数校验
        if (param.bcsAppInstName.isNullOrBlank()) {
            logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| bcsAppInstName is not init")
            buildLogPrinter.addRedLine(task.buildId, "bcsAppInstName is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "bcsAppInstName is not init"
            )
        }
        if (param.namespace.isNullOrBlank()) {
            logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| namespace is not init")
            buildLogPrinter.addRedLine(task.buildId, "namespace is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "namespace is not init"
            )
        }

        val instVarList = param.instVar ?: listOf()
        val appIdStr = parseVariable(param.ccAppId, runVariables)
        val category = BcsCategory.parse(parseVariable(param.category, runVariables))
        val namespace = parseVariable(param.namespace, runVariables)
        val bcsAppInstName = parseVariable(param.bcsAppInstName, runVariables)
        val bcsAppInstId = getBcsAppInstIdByName(task, appIdStr, projectId, category, bcsAppInstName, namespace)
        val timeout = param.timeout

        val varMap = mutableMapOf<String, String>()
        instVarList.forEach {
            varMap[parseVariable(it.key, runVariables)] = parseVariable(it.value, runVariables)
        }
        val instVar = mutableMapOf<String, Map<String, String>>()
        instVar[bcsAppInstId] = varMap

        lateinit var result: Pair<Int, String>
        when (opType) {
            BcsOperation.RECREATE -> {
                buildLogPrinter.addLine(
                        task.buildId,
                        "BCS opType is reCreate, instanceId : $bcsAppInstId",
                        task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                )
                result = reCreateInstance(category, appIdStr, projectId, bcsAppInstId)
            }
            BcsOperation.SCALE -> {
                if (param.bcsInstNum.isNullOrBlank()) {
                    logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| bcsInstNum is not init")
                    buildLogPrinter.addRedLine(task.buildId, "bcsInstNum is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(
                        buildStatus = BuildStatus.FAILED,
                        errorType = ErrorType.USER,
                        errorCode = USER_INPUT_INVAILD,
                        errorMsg = "bcsInstNum is not init"
                    )
                }
                val instNum = parseVariable(param.bcsInstNum, runVariables).toInt()
                buildLogPrinter.addLine(
                        task.buildId,
                        "BCS opType is scale, instanceId : $bcsAppInstId and instanceNum: $instNum",
                        task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                )
                result = scaleInstance(category, appIdStr, projectId, bcsAppInstId, instNum)
            }
            BcsOperation.ROLLINGUPDATE -> {
                if (param.instVersionName.isNullOrBlank()) {
                    logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| instVersionName is not init")
                    buildLogPrinter.addRedLine(task.buildId, "instVersionName is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(
                        buildStatus = BuildStatus.FAILED,
                        errorType = ErrorType.USER,
                        errorCode = USER_INPUT_INVAILD,
                        errorMsg = "instVersionName is not init"
                    )
                }
                val instVersionName = parseVariable(param.instVersionName, runVariables)
                val versionId = getInstVersionIdByName(appIdStr, projectId, bcsAppInstId, instVersionName)

                result = when (category) {
                    BcsCategory.APPLICATION -> {
                        buildLogPrinter.addLine(
                                task.buildId,
                                "BCS opType is application update, instanceId : $bcsAppInstId",
                                task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                        )
                        applicationUpdate(appIdStr, projectId, versionId, bcsAppInstId, instVar)
                    }
                    else -> {
                        if (param.bcsInstNum.isNullOrBlank()) {
                            logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| bcsInstNum is not init")
                            buildLogPrinter.addRedLine(task.buildId, "bcsInstNum is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
                            return AtomResponse(
                                buildStatus = BuildStatus.FAILED,
                                errorType = ErrorType.USER,
                                errorCode = USER_INPUT_INVAILD,
                                errorMsg = "bcsInstNum is not init"
                            )
                        }
                        val instNum = parseVariable(param.bcsInstNum, runVariables).toInt()

                        buildLogPrinter.addLine(
                                task.buildId,
                                "BCS opType is batch update, instanceId : $bcsAppInstId",
                                task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                        )
                        updateInstance(category, appIdStr, projectId, versionId, instNum, bcsAppInstId, instVar)
                    }
                }
            }
            BcsOperation.DELETE -> {
                buildLogPrinter.addLine(task.buildId, "BCS opType is delete", task.taskId, task.containerHashId, task.executeCount ?: 1)
                result = deleteInstance(category, appIdStr, projectId, bcsAppInstId)
            }
            BcsOperation.SIGNAL -> {
                if (param.signal.isNullOrBlank()) {
                    logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| signal is not init")
                    buildLogPrinter.addRedLine(task.buildId, "signal is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(
                        buildStatus = BuildStatus.FAILED,
                        errorType = ErrorType.USER,
                        errorCode = USER_INPUT_INVAILD,
                        errorMsg = "signal is not init"
                    )
                }
                if (param.processName.isNullOrBlank()) {
                    logger.warn("[${task.buildId}]|TASK_BcsContainerOpAtom| processName is not init")
                    buildLogPrinter.addRedLine(task.buildId, "processName is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(
                        buildStatus = BuildStatus.FAILED,
                        errorType = ErrorType.USER,
                        errorCode = USER_INPUT_INVAILD,
                        errorMsg = "processName is not init"
                    )
                }

                val processName = parseVariable(param.processName, runVariables)
                val signal = parseVariable(param.signal, runVariables).toInt()
                val signalInfoObj = mapOf("process_name" to processName, "signal" to signal)

                buildLogPrinter.addLine(
                        task.buildId,
                        "BCS opType is signal, processName : $processName , signal : $signal",
                        task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                )

                result = when (category) {
                    BcsCategory.APPLICATION -> {
                        buildLogPrinter.addLine(
                                task.buildId,
                                "BCS opType is application signal, instanceId : $bcsAppInstId",
                                task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                        )
                        sendSignal("applications", appIdStr, projectId, bcsAppInstId, signalInfoObj)
                    }
                    BcsCategory.DEPLOYMENT -> {
                        buildLogPrinter.addLine(
                                task.buildId,
                                "BCS opType is deployment signal, instanceId : $bcsAppInstId",
                                task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                        )
                        sendSignal("deployments", appIdStr, projectId, bcsAppInstId, signalInfoObj)
                    }
                    else -> {
                        logger.warn("BCS signal operate failed msg: BCS $category signal Type is illegal")
                        buildLogPrinter.addRedLine(
                                task.buildId,
                                "BCS operate result: BCS $category signal Type is illegal",
                                task.taskId,
                task.containerHashId,
                task.executeCount ?: 1
                        )
                        return AtomResponse(
                            buildStatus = BuildStatus.FAILED,
                            errorType = ErrorType.USER,
                            errorCode = USER_TASK_OPERATE_FAIL,
                            errorMsg = "BCS $category signal Type is illegal"
                        )
                    }
                }
            }
            BcsOperation.COMMAND -> {
                if (param.command.isNullOrBlank()) {
                    logger.warn("BCS command is not init of build(${task.buildId})")
                    buildLogPrinter.addRedLine(task.buildId, "BCS command is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    throw throw TaskExecuteException(
                        errorCode = USER_TASK_OPERATE_FAIL,
                        errorType = ErrorType.USER,
                        errorMsg = "BCS command is not init of build(${task.buildId})"
                    )
                }

                val command = parseVariable(param.category, runVariables)
                buildLogPrinter.addLine(task.buildId,
                        "BCS opType is command,command : $command", task.taskId, task.containerHashId, task.executeCount ?: 1
                )

                result = executeCommand(task, runVariables, param, appIdStr, projectId, bcsAppInstId, timeout)
            }
            else -> {
            }
        }
        if (result.first != 0) {
            logger.warn("BCS operate failed msg: ${result.second}")
            buildLogPrinter.addRedLine(task.buildId, "BCS operate result:${result.second}", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_TASK_OPERATE_FAIL,
                errorMsg = "BCS operation failed"
            )
        }

        Thread.sleep(5 * 1000)

        if (opType != BcsOperation.DELETE) {
            val appResult = waitForRunning(appIdStr, projectId, bcsAppInstId, timeout.toLong())
            if (!appResult.first) {
                buildLogPrinter.addRedLine(task.buildId, "BCS operation failed", task.taskId, task.containerHashId, task.executeCount ?: 1)
                buildLogPrinter.addRedLine(task.buildId, appResult.second, task.taskId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorMsg = "BCS operation failed"
                )
            }
        }

        buildLogPrinter.addLine(task.buildId, "BCS operation success!", task.taskId, task.containerHashId, task.executeCount ?: 1)
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun getInstVersionIdByName(
        appIdStr: String,
        projectId: String,
        instId: String,
        instVersionName: String
    ): Int {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = bcsAppUrl + "cc_app_ids/$appIdStr/projects/$projectId/instances/$instId/versions/?access_token=$token"
        logger.info("Get instVersionId, request url: $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Get instVersionId, response: $data")
            if (!response.isSuccessful) {
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get instVersionId, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            if (code != 0) {
                val message = responseData["code"] as String
                logger.warn("Get instVersionId failed, message : $message")
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get instVersionId failed, response: $data"
                )
            }

            val responseDataData: List<Map<String, Any>> = responseData["data"] as List<Map<String, Any>>
            val instVersionObjs = responseDataData.filter { (it["name"] as String).equals(instVersionName, true) }
            if (instVersionObjs.isEmpty()) {
                logger.warn("Get instVersionId failed , instVersionName is mismatching. instVersionName : $instVersionName")
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get instVersionId failed , instVersionName is mismatching. instVersionName : $instVersionName"
                )
            }
            val instVersionObj = instVersionObjs[0]
            return instVersionObj["id"] as Int
        }
    }

    private fun waitForRunning(
        appidStr: String,
        projectId: String,
        instanceId: String,
        timeout: Long
    ): Pair<Boolean, String> {
        logger.info("waiting for bcsApp running, timeout setting: ${timeout}min")
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > timeout * 60 * 1000) {
                logger.warn("waiting for bcsApp running timeout")
                return Pair(false, "Waiting for bcs app running timeout")
            }

            val (isFinish, success, msg) = getInstanceStatus(appidStr, projectId, instanceId)
            return when {
                !isFinish -> {
                    Thread.sleep(5 * 1000)
                    continue@loop
                }
                !success -> {
                    logger.warn("Waiting for bcs app running failed, msg: $msg")
                    Pair(false, "Waiting for bcs app running failed, msg: $msg")
                }
                else -> Pair(true, "Success!")
            }
        }
    }

    private fun getInstanceStatus(appidStr: String, projectId: String, instanceId: String): TaskResult {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/$instanceId/status/?access_token=$token"
        logger.info("Get instance status, request url: $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Get instance status, response: $data")
            if (!response.isSuccessful) {
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get instance status, response: $data"
                )
            }

            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            if (0 != code) {
                val message = responseData["message"].toString()
                return TaskResult(true, false, message)
            }
            val statusMap = responseData["data"] as Map<String, Any>
            if (null == statusMap["status"]) {
                return TaskResult(false, false, "")
            }
            val status = statusMap["status"] as String
            return when {
                "running".equals(status, false) -> TaskResult(true, true, "running")
                "unnormal".equals(status, false) -> TaskResult(true, false, "unnormal")
                else -> TaskResult(false, false, "")
            }
        }
    }

    private fun createInstance(
        task: PipelineBuildTask,
        projectId: String,
        param: BcsContainerOpByNameElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val timeout = param.timeout
        if (param.namespaceVar == null) {
            logger.warn("namespaceVar is not init of build(${task.buildId})")
            buildLogPrinter.addRedLine(task.buildId, "命名空间没有被赋值", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "命名空间没有被赋值"
            )
        }
        val variableInfo = param.namespaceVar!!

        if (param.templateName.isNullOrBlank()) {
            logger.warn("instanceEntity is not init of build(${task.buildId})")
            buildLogPrinter.addRedLine(task.buildId, "instanceEntity is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "instanceEntity is not init"
            )
        }
        if (param.showVersionName.isNullOrBlank()) {
            logger.warn("showVersionName is not init of build(${task.buildId})")
            buildLogPrinter.addRedLine(task.buildId, "showVersionName is not init", task.taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "showVersionName is not init"
            )
        }

        val category = parseVariable(param.category, runVariables)
        val appIdStr = parseVariable(param.ccAppId, runVariables)
        val clusterId = parseVariable(param.clusterId, runVariables)
        val templateName = parseVariable(param.templateName, runVariables)
        val showVersionName = parseVariable(param.showVersionName, runVariables)
        val instanceEntityObj = mapOf(
                category to listOf(
                        mapOf("name" to templateName)
                )
        )

        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = bcsAppUrl + "cc_app_ids/$appIdStr/projects/$projectId/instances/?access_token=$token"
        logger.info("Create instance, request url: $url")

        val clusterNsInfo = mutableMapOf<String, MutableMap<String, String>>()
        variableInfo.forEach { it ->
            val namespace = parseVariable(it.namespace, runVariables)
            val map: MutableMap<String, String> = clusterNsInfo.computeIfAbsent(namespace) { mutableMapOf() }
            val key = parseVariable(it.varKey, runVariables)
            val value = parseVariable(it.varValue, runVariables)
            map[key] = value
        }

        val requestData = mapOf(
                "cluster_ns_info" to mapOf(clusterId to clusterNsInfo),
                "show_version_name" to showVersionName,
                "instance_entity" to instanceEntityObj
        )

        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("Create instance, request body: $requestBody")
        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Create instance, response: $data")
            if (!response.isSuccessful) {
                logger.warn("Create instance failed, msg: $data")
                buildLogPrinter.addRedLine(task.buildId, "创建实例失败，详情： $data", task.taskId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorMsg = "创建实例失败，详情： $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            if (code != 0) {
                val message = responseData["message"].toString()
                logger.warn("Create instance failed, msg:$message")
                buildLogPrinter.addRedLine(task.buildId, "创建实例失败，详情：$message", task.taskId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorMsg = "创建实例失败，详情：$message"
                )
            }
            val instData = responseData["data"] as Map<String, Any>
            val instIdList = instData["inst_id_list"] as List<Int>

            Thread.sleep(5 * 1000)

            instIdList.forEach {
                val appResult = waitForRunning(appIdStr, projectId, it.toString(), timeout.toLong())
                if (!appResult.first) {
                    buildLogPrinter.addRedLine(task.buildId, "BCS operation failed", task.taskId, task.containerHashId, task.executeCount ?: 1)
                    buildLogPrinter.addRedLine(task.buildId, appResult.second, task.taskId, task.containerHashId, task.executeCount ?: 1)
                    return AtomResponse(
                        buildStatus = BuildStatus.FAILED,
                        errorType = ErrorType.USER,
                        errorCode = USER_TASK_OPERATE_FAIL,
                        errorMsg = "BCS operation failed"
                    ) }
            }
            buildLogPrinter.addLine(task.buildId, "BCS operation success!", task.taskId, task.containerHashId, task.executeCount ?: 1)
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun reCreateInstance(
        category: BcsCategory,
        appidStr: String,
        projectId: String,
        instIdList: String
    ): Pair<Int, String> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/batch_recreate/?access_token=$token&category=${category.getValue()}"
        logger.info("Recreate instance, request url: $url")
        val requestData = mapOf("inst_id_list" to listOf(instIdList))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("Recreate instance, request body: $requestBody")
        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Recreate instance, response: $data")
            if (!response.isSuccessful) {
                throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "Recreate instance, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            var message = ""
            if (responseData["message"] != null) message = responseData["message"].toString()
            return Pair(code, message)
        }
    }

    private fun deleteInstance(
        category: BcsCategory,
        appidStr: String,
        projectId: String,
        instIdList: String
    ): Pair<Int, String> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/batch_delete/?access_token=$token&category=${category.getValue()}"
        logger.info("delete instance, request url: $url")
        val requestData = mapOf("inst_id_list" to listOf(instIdList))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("delete instance, request body: $requestBody")
        val request = Request.Builder()
                .url(url)
                .delete(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("delete instance, response: $data")
            if (!response.isSuccessful) {
                throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "delete instance, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            var message = ""
            if (responseData["message"] != null) message = responseData["message"].toString()
            return Pair(code, message)
        }
    }

    private fun getBcsAppInstIdByName(
        task: PipelineBuildTask,
        appidStr: String,
        projectId: String,
        category: BcsCategory,
        bcsAppInstName: String,
        namespace: String
    ): String {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instance/detail/?access_token=$token&category=${category.getValue()}&name=$bcsAppInstName&namespace=$namespace"
        logger.info("Get bcsAppInstId, request url: $url")

        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        lateinit var dataMap: Map<String, Any>
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Get bcsAppInstId by bcsAppInstName, response: $data")
            if (!response.isSuccessful) {
                logger.warn("Get bcsAppInstId by bcsAppInstName($bcsAppInstName) failed, msg:$data")
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get bcsAppInstId faild, response: $data"
                )
            }

            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            if (0 != code) {
                val message = responseData["message"].toString()
                logger.warn("Get bcsAppInstId by bcsAppInstName($bcsAppInstName) failed, msg:$message")
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get bcsAppInstId faild, response: $data"
                )
            }
            dataMap = responseData["data"] as Map<String, Any>
            if (dataMap["id"] == null || dataMap["id"] == "") {
                logger.warn("Get bcsAppInstId by bcsAppInstName($bcsAppInstName) failed, msg:$data")
                buildLogPrinter.addRedLine(task.buildId, "实例名称或命名空间错误", task.taskId, task.containerHashId, task.executeCount ?: 1)
                throw TaskExecuteException(
                    errorCode = USER_RESOURCE_NOT_FOUND,
                    errorType = ErrorType.USER,
                    errorMsg = "Get bcsAppInstId by bcsAppInstName($bcsAppInstName) failed, response:$data"
                )
            }
        }
        return dataMap["id"].toString()
    }

    private fun sendSignal(
        signalCategory: String,
        appidStr: String,
        projectId: String,
        instIdList: String,
        signalInfoObj: Map<String, Any>
    ): Pair<Int, String> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/$signalCategory/batch_signal/?access_token=$token"
        logger.info("Send $signalCategory signal, request url: $url")

        val requestData = mapOf("inst_id_list" to listOf(instIdList), "signal_info" to signalInfoObj)
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("Send $signalCategory signal, request body: $requestBody")

        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Send $signalCategory signal, response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "Send $signalCategory signal, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            var message = ""
            if (responseData["message"] != null) message = responseData["message"].toString()
            return Pair(code, message)
        }
    }

    // 发送command命令，返回task_id
    private fun sendCommands(
        runVariables: Map<String, String>,
        param: BcsContainerOpByNameElement,
        appidStr: String,
        projectId: String,
        instId: String
    ): String {
        val commandVar = parseVariable(param.command, runVariables)
        val commandList = mutableListOf(commandVar)

        val envVarList = mutableListOf<String>()
        var usernameVar: String? = null
        var workDirVar: String? = null
        var reserveTimeVar = 24607
        val privilegedVar: Boolean = param.privileged

        if (!param.commandParam.isNullOrBlank()) {
            val commandParam = parseVariable(param.commandParam, runVariables)
            commandList.add(commandParam)
        }

        val envVar = param.env
        if (null != envVar && envVar.isNotEmpty()) {
            envVar.forEach {
                val key = parseVariable(it.key, runVariables)
                val value = parseVariable(it.value, runVariables)
                envVarList.add("$key=$value")
            }
        }
        if (!param.username.isNullOrBlank()) {
            usernameVar = parseVariable(param.username, runVariables)
        }
        if (!param.workDir.isNullOrBlank()) {
            workDirVar = parseVariable(param.workDir, runVariables)
        }
        if (!param.reserveTime.isNullOrBlank()) {
            reserveTimeVar = parseVariable(param.reserveTime, runVariables).toInt()
        }

        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/$instId/command/"
        logger.info("SendCommand, request url: $url")

        val requestData = mutableMapOf<String, Any?>(
                "access_token" to token,
                "command" to commandList,
                "privileged" to privilegedVar,
                "reserve_time" to reserveTimeVar
        )
        if (envVarList.isNotEmpty()) requestData["env"] = envVarList
        if (!usernameVar.isNullOrBlank()) requestData["username"] = usernameVar
        if (!workDirVar.isNullOrBlank()) requestData["work_dir"] = workDirVar

        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("SendCommand, request body: $requestBody")

        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("SendCommand, response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "SendCommand, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)

            val code = responseData["code"] as Int
            if (0 != code) {
                val message = responseData["message"].toString()
                logger.warn("SendCommand($commandVar) failed, msg:$message")
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "SendCommand($commandVar) failed, msg:$message")
            }
            val dataMap = responseData["data"] as Map<*, *>
            if (null == dataMap["task_id"]) {
                logger.warn("SendCommand($commandVar) failed, response:$data")
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "SendCommand($commandVar) failed, response:$data")
            }
            return dataMap["task_id"].toString()
        }
    }

    // 查询命令状态
    private fun getCommandStatus(
        pipelintTask: PipelineBuildTask,
        appidStr: String,
        projectId: String,
        instId: String,
        taskId: String
    ): TaskResult {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/$instId/command/status/?access_token=$token&task_id=$taskId"
        logger.info("Get Command Status, request url: $url")

        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Get Command Status, task_id($taskId), response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = data)
            }

            val responseObj: BcsCommandStatus = jacksonObjectMapper().readValue(data)
            if (0 != responseObj.code) {
                val message = responseObj.message
                logger.warn("Get Command Status, task_id($taskId) failed, msg:$message")
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "Get Command Status, task_id($taskId) failed, msg:$message")
            }

            val taskgroups = responseObj.data.status.taskgroups
            if (null == taskgroups || taskgroups.isEmpty()) {
                logger.warn("Get Command Status, taskgroups is null, msg:${responseObj.message}")
                return TaskResult(false, false, "Query the command state again")
            }

            val taskResult = TaskResult(true, true, "success!")
            taskgroups.forEach { taskgroup ->
                val tasks = taskgroup.tasks
                if (null == tasks || tasks.isEmpty()) {
                    logger.warn("Get Command Status, tasks is null, msg:${responseObj.message}")
                    return TaskResult(false, false, "Query the command state again")
                }

                tasks.forEach { task ->
                    val commInspect = task.commInspect
                    if (null == commInspect) {
                        logger.warn("Get Command Status, commInspect is null, msg:${task.message}")
                        return TaskResult(false, false, "command is running , status: ${task.status}")
                    }

                    // 判断命令执行是否成功
                    when (task.status) {
                        "failed" -> {
                            return TaskResult(true, false, commInspect.toString())
                        }
                        "finish" -> {
                            if (0 != commInspect.exitCode) {
                                return TaskResult(true, false, commInspect.toString())
                            } else {
                                logger.info("Get Command Status, status is finish, msg:${task.message}")
                                buildLogPrinter.addLine(pipelintTask.buildId, commInspect.stdout, pipelintTask.taskId, null, pipelintTask.executeCount ?: 1)
                            }
                        }
                        // 其他状态表示未完成
                        else -> {
                            buildLogPrinter.addLine(pipelintTask.buildId, commInspect.stdout, pipelintTask.taskId, null, pipelintTask.executeCount ?: 1)
                            taskResult.isFinish = false
                        }
                    }
                }
            }
            return taskResult
        }
    }

    // 执行命令，并返回命令最终结果
    private fun executeCommand(
        pipelintTask: PipelineBuildTask,
        runVariables: Map<String, String>,
        param: BcsContainerOpByNameElement,
        appidStr: String,
        projectId: String,
        instId: String,
        timeout: Int
    ): Pair<Int, String> {
        val taskId = sendCommands(runVariables, param, appidStr, projectId, instId)

        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > timeout * 60 * 1000) {
                logger.warn("Send Command for bcsApp running timeout, taskId:($taskId)")
                return Pair(400, "Send Command for bcsApp running timeout, taskId:($taskId)")
            }

            val (isFinish, success, msg) = getCommandStatus(pipelintTask, appidStr, projectId, instId, taskId)
            return when {
                !isFinish -> {
                    Thread.sleep(5 * 1000)
                    continue@loop
                }
                !success -> {
                    logger.warn("Send Command for bcsApp running failed, msg: $msg")
                    buildLogPrinter.addRedLine(pipelintTask.buildId, "命令执行失败，详情： $msg", pipelintTask.taskId, null, pipelintTask.executeCount ?: 1
                    )
                    Pair(400, "Send Command for bcsApp running failed, msg: $msg")
                }
                else -> {
                    logger.info("SendCommand Success!")
                    buildLogPrinter.addLine(pipelintTask.buildId, "命令执行成功，详情： $msg", pipelintTask.taskId, null, pipelintTask.executeCount ?: 1)
                    Pair(0, msg)
                }
            }
        }
    }

    private fun scaleInstance(
        category: BcsCategory,
        appidStr: String,
        projectId: String,
        instIdList: String,
        instNum: Int
    ): Pair<Int, String> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/batch_scale/?access_token=$token&category=${category.getValue()}&instance_num=$instNum"
        logger.info("Scale instance, request url: $url")

        val requestData = mapOf("inst_id_list" to listOf(instIdList))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("Scale instance, request body: $requestBody")

        val request = Request.Builder()
                .url(url)
                .put(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Scale instance, response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = data)
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            var message = ""
            if (responseData["message"] != null) message = responseData["message"].toString()
            return Pair(code, message)
        }
    }

    private fun applicationUpdate(
        appidStr: String,
        projectId: String,
        versionId: Int,
        instIdList: String,
        namespaceVar: Map<String, Map<String, String>>
    ): Pair<Int, String> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/batch_application_update/?access_token=$token&category=${BcsCategory.APPLICATION.getValue()}&version_id=$versionId"
        logger.info("Update application instance, request url: $url")
        val requestData = mapOf(
                "inst_id_list" to listOf(instIdList),
                "inst_variables" to namespaceVar
        )
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("Update application instance, request body: $requestBody")
        val request = Request.Builder()
                .url(url)
                .put(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Update application instance, response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = data)
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            var message = ""
            if (responseData["message"] != null) message = responseData["message"].toString()
            return Pair(code, message)
        }
    }

    private fun updateInstance(
        category: BcsCategory,
        appidStr: String,
        projectId: String,
        versionId: Int,
        instNum: Int,
        instIdList: String,
        instVar: Map<String, Map<String, String>>
    ): Pair<Int, String> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url =
                bcsAppUrl + "cc_app_ids/$appidStr/projects/$projectId/instances/batch_update/?access_token=$token&category=${category.getValue()}&version_id=$versionId&instance_num=$instNum"
        logger.info("Update instance, request url: $url")
        val requestData = mapOf(
                "inst_id_list" to listOf(instIdList),
                "inst_variables" to instVar
        )
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("Update instance, request body: $requestBody")
        val request = Request.Builder()
                .url(url)
                .put(RequestBody.create(JSON, requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Update instance, response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "Update instance, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            var message = ""
            if (responseData["message"] != null) message = responseData["message"].toString()
            return Pair(code, message)
        }
    }

    private fun getProjectOriginId(projectCode: String): String {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = "$projectUrl$projectCode/?access_token=$token"
        logger.info("Get project info, request url: $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            logger.info("Get project info, response: $data")
            if (!response.isSuccessful) {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "Get project info, response: $data"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
            val code = responseData["code"] as Int
            if (0 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return dataMap["project_id"] as String
            } else {
                throw throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = data
                )
            }
        }
    }

    private fun getList(strings: String?): List<String> {
        if (strings.isNullOrBlank()) {
            return mutableListOf()
        }
        return jacksonObjectMapper().readValue(strings!!) as List<String>
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BcsContainerOpByNameAtom::class.java)
        private val JSON = MediaType.parse("application/json;charset=utf-8")
    }

    data class TaskResult(var isFinish: Boolean, var success: Boolean, var msg: String)
}
