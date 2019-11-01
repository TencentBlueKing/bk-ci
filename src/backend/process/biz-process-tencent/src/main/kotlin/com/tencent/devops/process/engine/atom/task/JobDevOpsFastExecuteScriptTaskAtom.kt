@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.job.JobClient
import com.tencent.devops.common.job.api.pojo.EnvSet
import com.tencent.devops.common.job.api.pojo.FastExecuteScriptRequest
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.JobDevOpsFastExecuteScriptElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.ERROR_BUILD_TASK_ENV_ID_IS_NULL
import com.tencent.devops.process.engine.common.ERROR_BUILD_TASK_ENV_NAME_IS_NULL
import com.tencent.devops.process.engine.common.ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS
import com.tencent.devops.process.engine.common.ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS
import com.tencent.devops.process.engine.common.ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.service.PipelineUserService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
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
    private val pipelineUserService: PipelineUserService,
    private val rabbitTemplate: RabbitTemplate
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
                containerHashId = task.containerHashId,
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

        if (param.content.isBlank()) {
            logger.error("content is not init of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "content is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "content is not validate"
            )
        }
        val content = param.content

        if (param.type < 0) {
            logger.warn("type is not init of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "type is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "type is not validate"
            )
        }
        if (param.envType.isBlank()) {
            logger.warn("envType is not init of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "envType is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "envType is not validate"
            )
        }

        val envTypeValue = parseVariable(param.envType, runVariables)
        if (envTypeValue == "ENV" && (param.envId == null || param.envId!!.isEmpty())) {
            logger.warn("EnvId is not init of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "EnvId is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "EnvId is not validate"
            )
        }
        if (envTypeValue == "ENV_NAME" && (param.envName == null || param.envName!!.isEmpty())) {
            logger.warn("EnvName is not init of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "EnvName is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "EnvName is not validate"
            )
        }
        if (envTypeValue == "NODE" && (param.nodeId == null || param.nodeId!!.isEmpty())) {
            logger.warn("NodeId is not init of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "NodeId is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "NodeId is not validate"
            )
        }

        if (param.account.isBlank()) {
            logger.warn("ipList is not init of build($buildId)")

            LogUtils.addRedLine(rabbitTemplate, buildId, "account is not init", taskId, task.containerHashId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
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
            LogUtils.addRedLine(
                rabbitTemplate,
                buildId,
                "Invalid content, it's not in valid Base64 scheme",
                taskId,
                task.containerHashId,
                executeCount
            )
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "Invalid content, it's not in valid Base64 scheme"
            )
        }
        val isParamSensitive = param.paramSensitive
        val scriptTimeout = param.scriptTimeout.toLong()
        val type = param.type
        val account = parseVariable(param.account, runVariables)
        var operator = task.starter
        val pipelineId = task.pipelineId
        val lastModifyUserMap = pipelineUserService.listUpdateUsers(setOf(pipelineId))
        val lastModifyUser = lastModifyUserMap[pipelineId]
        if (null != lastModifyUser && operator != lastModifyUser) {
            // 以流水线的最后一次修改人身份执行；如果最后一次修改人也没有这个环境的操作权限，这种情况不考虑，有问题联系产品!
            logger.info("operator:$operator, lastModifyUser:$lastModifyUser")
            LogUtils.addLine(
                rabbitTemplate,
                buildId,
                "Will use $lastModifyUser to distribute file...",
                taskId,
                task.containerHashId,
                executeCount
            )

            operator = lastModifyUser
        }

        val projectId = task.projectId

        val envSet = when (envTypeValue) {
            "NODE" -> {
                if (param.nodeId == null || param.nodeId!!.isEmpty()) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "EnvId is not init", taskId, task.containerHashId, executeCount)
                    throw BuildTaskException(ERROR_BUILD_TASK_ENV_ID_IS_NULL, "EnvId is not init")
                }
                val targetNodeId = parseVariable(param.nodeId!!.joinToString(","), runVariables).split(",").toList()
                EnvSet(listOf(), targetNodeId, listOf())
            }
            "ENV" -> {
                if (param.envId == null || param.envId!!.isEmpty()) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "EnvId is not init", taskId, task.containerHashId, executeCount)
                    throw BuildTaskException(ERROR_BUILD_TASK_ENV_ID_IS_NULL, "EnvId is not init")
                }
                val targetEnvId = parseVariable(param.envId!!.joinToString(","), runVariables).split(",").toList()
                EnvSet(targetEnvId, listOf(), listOf())
            }
            "ENV_NAME" -> {
                if (param.envName == null || param.envName!!.isEmpty()) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "EnvName is not init", taskId, task.containerHashId, executeCount)
                    throw BuildTaskException(ERROR_BUILD_TASK_ENV_NAME_IS_NULL, "EnvName is not init")
                }
                val targetEnvName = parseVariable(param.envName!!.joinToString(","), runVariables).split(",").toList()
                val envIdList = checkAuth(buildId, taskId, task.containerHashId, executeCount, operator, projectId, targetEnvName, client)
                EnvSet(envIdList, listOf(), listOf())
            }
            else -> {
                LogUtils.addRedLine(rabbitTemplate, buildId, "Unsupported EnvType: $type ", taskId, task.containerHashId, executeCount)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "Unsupported EnvType: $type"
                )
            }
        }

        checkEnvNodeExists(buildId, taskId, task.containerHashId, executeCount, operator, projectId, envSet, client)

        val fastExecuteScriptReq = FastExecuteScriptRequest(
            operator, contentValue, scriptTimeout, scriptParamsValue,
            if (isParamSensitive) 1 else 0, type, envSet, account
        )
        val taskInstanceId = jobClient.fastExecuteScriptDevops(fastExecuteScriptReq, projectId)
        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = getTimeoutMills(param),
            projectId = projectId,
            taskId = taskId,
            containerHashId = task.containerHashId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (!BuildStatus.isFinish(buildStatus)) {
            LogUtils.addLine(rabbitTemplate, buildId, "Waiting for job:$taskInstanceId", task.taskId, task.containerHashId, executeCount)
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
        containerHashId: String?,
        executeCount: Int
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMills) {
            logger.warn("job getTimeout. getTimeout minutes:${maxRunningMills / 60000}")
            LogUtils.addRedLine(
                rabbitTemplate,
                buildId,
                "Job getTimeout: ${maxRunningMills / 60000} Minutes",
                taskId,
                containerHashId,
                executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = jobClient.getTaskResult(projectId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addLine(rabbitTemplate, buildId, taskResult.msg, taskId, containerHashId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addRedLine(rabbitTemplate, buildId, taskResult.msg, taskId, containerHashId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            BuildStatus.LOOP_WAITING
        }
    }

    private fun checkAuth(
        buildId: String,
        taskId: String,
        containerHashId: String?,
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
            logger.error("The envNames not exists, name:$noExistsEnvNames")
            LogUtils.addRedLine(rabbitTemplate, buildId, "以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames", taskId, containerHashId, executeCount)
            throw BuildTaskException(ERROR_BUILD_TASK_ENV_NAME_NOT_EXISTS, "以下这些环境名称不存在,请重新修改流水线！$noExistsEnvNames")
        }

        // 校验权限
        val userEnvList = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(operator, projectId).data
        val userEnvIdList = mutableListOf<String>()
        userEnvList!!.forEach {
            userEnvIdList.add(it.envHashId)
        }

        val noAuthEnvIds = envIdList.subtract(userEnvIdList)
        if (noAuthEnvIds.isNotEmpty()) {
            logger.error("User does not permit to access the env: $noAuthEnvIds")
            LogUtils.addRedLine(rabbitTemplate, buildId, "用户没有操作这些环境的权限！环境ID：$noAuthEnvIds", taskId, containerHashId, executeCount)
            throw BuildTaskException(ERROR_BUILD_TASK_USER_ENV_NO_OP_PRI, "用户没有操作这些环境的权限！环境ID：$noAuthEnvIds")
        }
        return envIdList
    }

    private fun checkEnvNodeExists(
        buildId: String,
        taskId: String,
        containerHashId: String?,
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
                logger.error("The envIds not exists, id:$noExistsEnvIds")
                LogUtils.addRedLine(
                    rabbitTemplate,
                    buildId,
                    "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds",
                    taskId,
                containerHashId,
                executeCount
                )
                throw BuildTaskException(
                    ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS,
                    "以下这些环境id不存在,请重新修改流水线！id：$noExistsEnvIds"
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
                logger.error("The nodeIds not exists, id:$noExistsNodeIds")
                LogUtils.addRedLine(
                    rabbitTemplate,
                    buildId,
                    "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds",
                    taskId,
                containerHashId,
                executeCount
                )
                throw BuildTaskException(
                    ERROR_BUILD_TASK_USER_ENV_ID_NOT_EXISTS,
                    "以下这些节点id不存在,请重新修改流水线！id：$noExistsNodeIds"
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
