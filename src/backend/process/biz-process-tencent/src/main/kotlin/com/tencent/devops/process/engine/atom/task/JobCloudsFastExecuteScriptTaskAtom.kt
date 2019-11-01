@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.JobCloudsFastExecuteScriptElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.esb.JobCloudsFastExecuteScript
import com.tencent.devops.process.pojo.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.Base64

@Component
@Scope(SCOPE_PROTOTYPE)
class JobCloudsFastExecuteScriptTaskAtom @Autowired constructor(
    private val jobFastExecuteScript: JobCloudsFastExecuteScript,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<JobCloudsFastExecuteScriptElement> {

    override fun getParamElement(task: PipelineBuildTask): JobCloudsFastExecuteScriptElement {
        return JsonUtil.mapTo(task.taskParams, JobCloudsFastExecuteScriptElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: JobCloudsFastExecuteScriptElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        parseParam(param, runVariables)

        val buildId = task.buildId
        val executeCount = task.executeCount ?: 1

        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "JOB_TASK_ID is not correct"
            ) else AtomResponse(task.status)

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()

        logger.info("[$buildId]|LOOP|${task.taskId}|JOB_TASK_ID=taskInstanceId|startTime=$startTime")
        return AtomResponse(
            jobFastExecuteScript.checkStatus(
                startTime = startTime,
                timeoutSeconds = param.scriptTimeout,
                targetAppId = param.targetAppId,
                taskInstanceId = taskInstanceId,
                operator = task.starter,
                buildId = buildId,
                taskId = task.taskId,
                containerHashId = task.containerHashId,
                executeCount = executeCount
            )
        )
    }

    override fun execute(
        task: PipelineBuildTask,
        param: JobCloudsFastExecuteScriptElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        parseParam(param, runVariables)

        val buildId = task.buildId
        val executeCount = task.executeCount ?: 1
        val taskInstanceId = jobFastExecuteScript.cloudsFastExecuteScript(
            buildId = buildId,
            operator = task.starter,
            content = param.content,
            scriptParam = param.scriptParams ?: "",
            scriptTimeout = param.scriptTimeout,
            openstate = param.openState,
            targetAppId = param.targetAppId,
            elementId = task.taskId,
            containerHashId = task.containerHashId ?: "",
            executeCount = executeCount,
            paramSensitive = if (param.paramSensitive) 1 else 0,
            type = param.type,
            account = param.account
        )

        logger.info("[${task.buildId}]|cloudsFastExecuteScript|taskInstanceId=$taskInstanceId")

        val startTime = System.currentTimeMillis()
        val buildStatus = jobFastExecuteScript.checkStatus(
            startTime = startTime,
            timeoutSeconds = param.scriptTimeout,
            targetAppId = param.targetAppId,
            taskInstanceId = taskInstanceId,
            operator = task.starter,
            buildId = buildId,
            taskId = task.taskId,
            containerHashId = task.containerHashId,
            executeCount = executeCount
        )

        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (!BuildStatus.isFinish(buildStatus)) {
            LogUtils.addLine(rabbitTemplate, buildId, "Waiting for job:$taskInstanceId", task.taskId, task.containerHashId, executeCount)
        }
        return if (buildStatus == BuildStatus.FAILED)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "send cloud stone file to Svr fail"
            )
        else AtomResponse(buildStatus)
    }

    private fun parseParam(param: JobCloudsFastExecuteScriptElement, runVariables: Map<String, String>) {
        param.scriptTimeout = parseVariable(param.scriptTimeout.toString(), runVariables).toInt()

        val scriptParam = try {
            String(Base64.getDecoder().decode(param.scriptParams))
        } catch (e: IllegalArgumentException) {
            throw ParamBlankException("Invalid scriptParams, it's not in valid Base64 scheme")
        }
        param.scriptParams = parseVariable(scriptParam, runVariables)
        param.account = parseVariable(param.account, runVariables)
        param.openState = parseVariable(param.openState, runVariables)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobCloudsFastExecuteScriptTaskAtom::class.java)
        private const val JOB_TASK_ID = "_bsJobTaskInstanceId"
    }
}
