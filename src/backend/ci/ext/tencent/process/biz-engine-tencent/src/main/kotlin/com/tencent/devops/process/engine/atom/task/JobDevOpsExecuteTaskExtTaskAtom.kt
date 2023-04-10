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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.job.JobClient
import com.tencent.devops.common.job.api.pojo.ExecuteTaskRequest
import com.tencent.devops.common.job.api.pojo.GlobalVar
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.JobDevOpsExecuteTaskExtElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_VIEW_RESULT
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.ParameterUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class JobDevOpsExecuteTaskExtTaskAtom @Autowired constructor(
    private val jobClient: JobClient,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<JobDevOpsExecuteTaskExtElement> {

    override fun getParamElement(task: PipelineBuildTask): JobDevOpsExecuteTaskExtElement {
        return JsonUtil.mapTo(task.taskParams, JobDevOpsExecuteTaskExtElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: JobDevOpsExecuteTaskExtElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val buildId = task.buildId
        val taskId = task.taskId
        val projectId = task.projectId
        val executeCount = task.executeCount ?: 1
        val containerId = task.containerHashId

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val operator = task.taskParams[STARTER] as String

        val timeout = getTimeout(param)

        logger.info("[$buildId]|LOOP|$taskId|JOB_TASK_ID=$taskInstanceId|startTime=$startTime")

        return AtomResponse(
            checkStatus(
                startTime = startTime,
                maxRunningMills = timeout,
                projectId = projectId,
                taskId = taskId,
                containerId = containerId,
                taskInstanceId = taskInstanceId,
                operator = operator,
                buildId = buildId,
                executeCount = executeCount
            )
        )
    }

    private fun getTimeout(param: JobDevOpsExecuteTaskExtElement): Long {
        var timeout = param.timeout * 1000L
        if (timeout <= 0) {
            timeout = 600 * 1000L
        }
        return timeout
    }

    override fun execute(
        task: PipelineBuildTask,
        param: JobDevOpsExecuteTaskExtElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        val projectId = task.projectId
        val containerId = task.containerHashId
        val executeCount = task.executeCount ?: 1

        if (param.taskId < 0) {
            logger.warn("taskId is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "taskId is not init", taskId, containerId, executeCount)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = "taskId is not init"
            )
        }

        val jobTaskTemplateId = param.taskId

        val globalVar = if (param.globalVar.isNotEmpty()) {
            parseVariable(JsonUtil.toJson(param.globalVar), runVariables)
        } else {
            "{}"
        }
        val map: Map<Int, String> = jacksonObjectMapper().readValue(globalVar)
        val globalVars = mutableListOf<GlobalVar>()
        for ((k, v) in map) {
            val globalVarTmp = GlobalVar(k, ParameterUtils.parseTemplate(runVariables, v))
            globalVars.add(globalVarTmp)
        }

        val timeout = getTimeout(param)

        // 以作业的最后修改人为操作人来执行作业，防止出现有流水线权限但是没有作业权限的问题
        val operator = jobClient.getTaskLastModifyUser(projectId, jobTaskTemplateId)
        logger.info("Execute job by operator: $operator")

        val executeTaskReq = ExecuteTaskRequest(operator, arrayListOf(), globalVars, jobTaskTemplateId, timeout)
        val taskInstanceId = jobClient.executeTaskDevops(executeTaskReq, projectId)
        buildLogPrinter.addLine(buildId, MessageUtil.getMessageByLocale(
            messageCode = BK_VIEW_RESULT,
            language = I18nUtil.getDefaultLocaleLanguage()
        ) + "${jobClient.getDetailUrl(projectId, taskInstanceId)}", task.taskId, containerId, executeCount)
        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            projectId = projectId,
            taskId = taskId,
            containerId = containerId,
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

        return if (buildStatus == BuildStatus.FAILED) AtomResponse(
            buildStatus = BuildStatus.FAILED,
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
            errorMsg = "start execute task failed"
        ) else AtomResponse(buildStatus)
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
                "Job getTimeout:${maxRunningMills / 60000} Minutes",
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
                buildLogPrinter.addLine(
                    buildId,
                    "Job devops execute task success! detail: ${taskResult.msg}",
                    taskId,
                    containerId,
                    executeCount
                )
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addRedLine(
                    buildId,
                    "start execute task failed! detail: ${taskResult.msg}",
                    taskId,
                    containerId,
                    executeCount
                )
                BuildStatus.FAILED
            }
        } else {
            BuildStatus.LOOP_WAITING
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobDevOpsExecuteTaskExtTaskAtom::class.java)
        private const val JOB_TASK_ID = "_JOB_TASK_ID"
        private const val STARTER = "_STARTER"
    }
}
