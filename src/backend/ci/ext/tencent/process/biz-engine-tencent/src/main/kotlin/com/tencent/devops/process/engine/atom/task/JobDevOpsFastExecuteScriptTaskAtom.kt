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

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.job.JobClient
import com.tencent.devops.common.job.api.pojo.EnvSet
import com.tencent.devops.common.job.api.pojo.FastExecuteScriptRequest
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.JobDevOpsFastExecuteScriptElement
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ENV_ID_IS_NULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ENV_NAME_IS_NULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.Base64

@Component
@Scope(SCOPE_PROTOTYPE)
class JobDevOpsFastExecuteScriptTaskAtom @Autowired constructor(
    private val client: Client,
    private val jobClient: JobClient,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<JobDevOpsFastExecuteScriptElement> {

    override fun getParamElement(task: PipelineBuildTask): JobDevOpsFastExecuteScriptElement {
        return JsonUtil.mapTo(task.taskParams, JobDevOpsFastExecuteScriptElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: JobDevOpsFastExecuteScriptElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val buildId = task.buildId
        val taskId = task.taskId
        val projectId = task.projectId
        val executeCount = task.executeCount ?: 1

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val operator = task.taskParams[STARTER] as String

        logger.info("[$buildId]|LOOP|$taskId|JOB_TASK_ID=$taskInstanceId|startTime=$startTime")

        return AtomResponse(
            checkStatus(
                startTime = startTime,
                maxRunningMills = getTimeoutMills(param),
                projectId = projectId,
                taskId = taskId,
                containerId = task.containerHashId,
                taskInstanceId = taskInstanceId,
                operator = operator,
                buildId = buildId,
                executeCount = executeCount
            )
        )
    }

    private fun getTimeoutMills(param: JobDevOpsFastExecuteScriptElement) = param.scriptTimeout * 1000L

    override fun execute(
        task: PipelineBuildTask,
        param: JobDevOpsFastExecuteScriptElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val buildId = task.buildId
        val taskId = task.taskId
        val executeCount = task.executeCount ?: 1
        val containerId = task.containerHashId

        if (param.content.isBlank()) {
            logger.warn("content is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "content is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "content is not validate"
            )
        }
        val content = param.content

        if (param.type < 0) {
            logger.warn("type is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "type is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "type is not validate"
            )
        }
        if (param.envType.isBlank()) {
            logger.warn("envType is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "envType is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "envType is not validate"
            )
        }

        val envTypeValue = parseVariable(param.envType, runVariables)
        if (envTypeValue == "ENV" && (param.envId == null || param.envId!!.isEmpty())) {
            logger.warn("EnvId is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "EnvId is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "EnvId is not validate"
            )
        }
        if (envTypeValue == "ENV_NAME" && (param.envName == null || param.envName!!.isEmpty())) {
            logger.warn("EnvName is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "EnvName is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "EnvName is not validate"
            )
        }
        if (envTypeValue == "NODE" && (param.nodeId == null || param.nodeId!!.isEmpty())) {
            logger.warn("NodeId is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "NodeId is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "NodeId is not validate"
            )
        }

        if (param.account.isBlank()) {
            logger.warn("ipList is not init of build($buildId)")

            buildLogPrinter.addRedLine(buildId, "account is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "account is not validate"
            )
        }

        var contentValue: String
        var scriptParamsValue = ""
        try {
            // 先base64解出来，替换全局参数
            val contentScript = String(Base64.getDecoder().decode(content))
            contentValue = parseVariable(contentScript, runVariables)
            // 先base64 decode，然后传给job
            contentValue = Base64.getEncoder().encodeToString(contentValue.toByteArray())

            if (!param.scriptParams.isNullOrBlank()) {
                // 先base64解出来，替换全局参数
                val scriptParams = String(Base64.getDecoder().decode(param.scriptParams))
                scriptParamsValue = parseVariable(scriptParams, runVariables)
                // 先base64 decode，然后传给job
                scriptParamsValue = Base64.getEncoder().encodeToString(scriptParamsValue.toByteArray())
            }
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid content, it's not in valid Base64 scheme")
            buildLogPrinter.addRedLine(
                buildId,
                "Invalid content, it's not in valid Base64 scheme",
                taskId,
                containerId,
                executeCount
            )
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "Invalid content, it's not in valid Base64 scheme"
            )
        }
        val isParamSensitive = param.paramSensitive
        val scriptTimeout = param.scriptTimeout.toLong()
        val type = param.type
        val account = parseVariable(param.account, runVariables)
        var operator = task.starter
        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val lastModifyUser = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.lastModifyUser
        if (null != lastModifyUser && operator != lastModifyUser) {
            // 以流水线的最后一次修改人身份执行；如果最后一次修改人也没有这个环境的操作权限，这种情况不考虑，有问题联系产品!
            logger.info("operator:$operator, lastModifyUser:$lastModifyUser")
            buildLogPrinter.addLine(
                buildId,
                "Will use $lastModifyUser to distribute file...",
                taskId,
                containerId,
                executeCount
            )

            operator = lastModifyUser
        }

        val envSet = when (envTypeValue) {
            "NODE" -> {
                if (param.nodeId == null || param.nodeId!!.isEmpty()) {
                    buildLogPrinter.addRedLine(buildId, "EnvId is not init", taskId, containerId, executeCount)
                    throw BuildTaskException(
                        errorType = ErrorType.USER,
                        errorCode = ERROR_BUILD_TASK_ENV_ID_IS_NULL.toInt(),
                        errorMsg = "EnvId is not init"
                    ) }
                val targetNodeId = parseVariable(param.nodeId!!.joinToString(","), runVariables).split(",").toList()
                EnvSet(listOf(), targetNodeId, listOf())
            }
            "ENV" -> {
                if (param.envId == null || param.envId!!.isEmpty()) {
                    buildLogPrinter.addRedLine(buildId, "EnvId is not init", taskId, containerId, executeCount)
                    throw BuildTaskException(
                        errorType = ErrorType.USER,
                        errorCode = ERROR_BUILD_TASK_ENV_ID_IS_NULL.toInt(),
                        errorMsg = "EnvId is not init"
                    )
                }
                val targetEnvId = parseVariable(param.envId!!.joinToString(","), runVariables).split(",").toList()
                EnvSet(targetEnvId, listOf(), listOf())
            }
            "ENV_NAME" -> {
                if (param.envName == null || param.envName!!.isEmpty()) {
                    buildLogPrinter.addRedLine(buildId, "EnvName is not init", taskId, containerId, executeCount)
                    throw BuildTaskException(
                        errorType = ErrorType.USER,
                        errorCode = ERROR_BUILD_TASK_ENV_NAME_IS_NULL.toInt(),
                        errorMsg = "EnvName is not init"
                    ) }
                val targetEnvName = parseVariable(param.envName!!.joinToString(","), runVariables).split(",").toList()
                val envIdList = checkAuth(buildId, taskId, containerId, executeCount, operator, projectId, targetEnvName, client)
                EnvSet(envIdList, listOf(), listOf())
            }
            else -> {
                buildLogPrinter.addRedLine(buildId, "Unsupported EnvType: $type ", taskId, containerId, executeCount)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD.toInt(),
                    errorMsg = "Unsupported EnvType: $type"
                )
            }
        }

        checkEnvNodeExists(buildId, taskId, containerId, executeCount, operator, projectId, envSet, client)

        val fastExecuteScriptReq = FastExecuteScriptRequest(
            operator, contentValue, scriptTimeout, scriptParamsValue,
            if (isParamSensitive) 1 else 0, type, envSet, account
        )
        val taskInstanceId = jobClient.fastExecuteScriptDevops(fastExecuteScriptReq, projectId)
        buildLogPrinter.addLine(buildId, "查看结果: ${jobClient.getDetailUrl(projectId, taskInstanceId)}", task.taskId, containerId, executeCount)
        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = getTimeoutMills(param),
            projectId = projectId,
            taskId = taskId,
            containerId = task.containerHashId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (!BuildStatus.isFinish(buildStatus)) {
            buildLogPrinter.addLine(buildId, "Waiting for job:$taskInstanceId", task.taskId, containerId, executeCount)
        }

        return AtomResponse(buildStatus)
    }

    private fun checkStatus(
        startTime: Long,
        maxRunningMills: Long,
        projectId: String,
        taskInstanceId: Long,
        operator: String,
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMills) {
            logger.warn("job getTimeout. getTimeout minutes:${maxRunningMills / 60000}")
            buildLogPrinter.addRedLine(
                buildId,
                "Job getTimeout: ${maxRunningMills / 60000} Minutes",
                taskId,
                containerId,
                executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = jobClient.getTaskResult(projectId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(buildId, taskResult.msg, taskId, containerId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addRedLine(buildId, taskResult.msg, taskId, containerId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            BuildStatus.LOOP_WAITING
        }
    }

    private fun checkAuth(
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int,
        operator: String,
        projectId: String,
        envNameList: List<String>,
        client: Client
    ): MutableList<String> {
        val envList =
            client.get(ServiceEnvironmentResource::class).listRawByEnvNames(operator, projectId, envNameList).data
        val envNameExistsList = mutableListOf<String>()
        val envIdList = mutableListOf<String>()
        envList!!.forEach {
            envNameExistsList.add(it.name)
            envIdList.add(it.envHashId)
        }
        val noExistsEnvNames = envNameList.subtract(envNameExistsList)
        if (noExistsEnvNames.isNotEmpty()) {
            logger.warn("The envNames not exists, name:$noExistsEnvNames")
            buildLogPrinter.addRedLine(buildId, "以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames", taskId, containerId, executeCount)
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS.toInt(),
                errorMsg = "以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames"
            )
        }

        // 校验权限
        val userEnvList = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(operator, projectId).data
        val userEnvIdList = mutableListOf<String>()
        userEnvList!!.forEach {
            userEnvIdList.add(it.envHashId)
        }

        val noAuthEnvIds = envIdList.subtract(userEnvIdList)
        if (noAuthEnvIds.isNotEmpty()) {
            logger.warn("User does not permit to access the env: $noAuthEnvIds")
            buildLogPrinter.addRedLine(buildId, "用户没有操作这些环境的权限！环境ID：$noAuthEnvIds", taskId, containerId, executeCount)
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI.toInt(),
                errorMsg = "用户没有操作这些环境的权限！环境ID：$noAuthEnvIds"
            )
        }
        return envIdList
    }

    private fun checkEnvNodeExists(
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int,
        operator: String,
        projectId: String,
        envSet: EnvSet,
        client: Client
    ) {
        if (envSet.envHashIds.isNotEmpty()) {
            val envList = client.get(ServiceEnvironmentResource::class)
                .listRawByEnvHashIds(operator, projectId, envSet.envHashIds).data
            val envIdList = mutableListOf<String>()
            envList!!.forEach {
                envIdList.add(it.envHashId)
            }
            val noExistsEnvIds = envSet.envHashIds.subtract(envIdList)
            if (noExistsEnvIds.isNotEmpty()) {
                logger.warn("The envIds not exists, id:$noExistsEnvIds")
                buildLogPrinter.addRedLine(
                    buildId,
                    "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds",
                    taskId,
                    containerId,
                    executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS.toInt(),
                    errorMsg = "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds"
                )
            }
        }
        if (envSet.nodeHashIds.isNotEmpty()) {
            val nodeList =
                client.get(ServiceNodeResource::class).listRawByHashIds(operator, projectId, envSet.nodeHashIds).data
            val nodeIdList = mutableListOf<String>()
            nodeList!!.forEach {
                nodeIdList.add(it.nodeHashId)
            }
            val noExistsNodeIds = envSet.nodeHashIds.subtract(nodeIdList)
            if (noExistsNodeIds.isNotEmpty()) {
                logger.warn("The nodeIds not exists, id:$noExistsNodeIds")
                buildLogPrinter.addRedLine(
                    buildId,
                    "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds",
                    taskId,
                    containerId,
                    executeCount
                )
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS.toInt(),
                    errorMsg = "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds"
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobDevOpsFastExecuteScriptTaskAtom::class.java)
        private const val JOB_TASK_ID = "_JOB_TASK_ID"
        private const val STARTER = "_STARTER"
    }
}
