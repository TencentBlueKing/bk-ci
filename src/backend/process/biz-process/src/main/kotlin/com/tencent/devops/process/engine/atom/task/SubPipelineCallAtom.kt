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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.service.SubPipelineCallElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_SUBPIPELINEID_NULL
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SubPipelineCallAtom @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : IAtomTask<SubPipelineCallElement> {

    override fun getParamElement(task: PipelineBuildTask): SubPipelineCallElement {
        return JsonUtil.mapTo(task.taskParams, SubPipelineCallElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: SubPipelineCallElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        logger.info("[${task.buildId}]|ATOM_SUB_PIPELINE_FINISH|status=${task.status}")
        if (BuildStatus.isFinish(task.status)) {
            return AtomResponse(task.status)
        }

        val (message, buildStatus) = pair(task)

        if (force) { // 强制终止时，将子流水线也一并终止
            if (!BuildStatus.isFinish(buildStatus)) {
                pipelineBuildService.serviceShutdown(
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    buildId = task.subBuildId!!,
                    channelCode = ChannelCode.BS
                )
            }
        }
        when {
            BuildStatus.isFailure(buildStatus) ->
                LogUtils.addRedLine(rabbitTemplate, task.buildId, message, task.taskId, task.executeCount ?: 1)
            BuildStatus.isCancel(buildStatus) ->
                LogUtils.addYellowLine(rabbitTemplate, task.buildId, message, task.taskId, task.executeCount ?: 1)
            message.isNotBlank() ->
                LogUtils.addLine(rabbitTemplate, task.buildId, message, task.taskId, task.executeCount ?: 1)
        }
        return AtomResponse(buildStatus)
    }

    private fun pair(task: PipelineBuildTask): Pair<String, BuildStatus> {

        return if (task.subBuildId == null || task.subBuildId.isNullOrBlank()) {
            "找不到对应子流水线构建信息" to BuildStatus.FAILED
        } else {
            val buildInfo = pipelineRuntimeService.getBuildInfo(task.subBuildId!!)
            when {
                buildInfo == null -> "找不到对应子流水线" to BuildStatus.FAILED
                BuildStatus.isCancel(buildInfo.status) -> "子流水线被取消" to buildInfo.status
                BuildStatus.isFailure(buildInfo.status) -> "子流水线执行失败" to buildInfo.status
                BuildStatus.isSuccess(buildInfo.status) -> "子流水线执行成功" to buildInfo.status
                else -> "" to task.status
            }
        }
    }

    override fun execute(
        task: PipelineBuildTask,
        param: SubPipelineCallElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        logger.info("Enter SubPipelineCallAtom run...")
        val executeCount = task.executeCount ?: 1
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val buildId = task.buildId

        val taskId = task.taskId
        val (subPipelineId, pipelineInfo) = getSubPipeline(param, runVariables, task, buildId, taskId, projectId)

        val channelCode =
            ChannelCode.valueOf(runVariables[PIPELINE_START_CHANNEL] ?: error("PIPELINE_START_CHANNEL is null"))

        val startType = runVariables[PIPELINE_START_TYPE] ?: error("BK_CI_START_TYPE is null")

        val userId = if (startType == StartType.PIPELINE.name) {
            runVariables[PIPELINE_START_PIPELINE_USER_ID] ?: error("BK_CI_START_PIPELINE_USER_ID is null")
        } else {
            runVariables[PIPELINE_START_USER_ID] ?: error("PIPELINE_START_USER_ID is null")
        }

        logger.info("[$buildId]| Start call sub pipeline($subPipelineId) by user $userId")

        val startParams = mutableMapOf<String, Any>()
        if (param.parameters != null) {
            param.parameters!!.forEach {
                startParams[it.key] = parseVariable(it.value, runVariables)
            }
        }
        return startSubPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            subPipelineId = subPipelineId,
            channelCode = channelCode,
            startParams = startParams,
            pipelineInfo = pipelineInfo,
            executeCount = executeCount,
            param = param
        )
    }

    private fun startSubPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        subPipelineId: String,
        channelCode: ChannelCode,
        startParams: MutableMap<String, Any>,
        pipelineInfo: PipelineInfo,
        executeCount: Int,
        param: SubPipelineCallElement
    ): AtomResponse {
        val subBuildId = pipelineBuildService.subpipelineStartup(
            userId = userId,
            startType = StartType.PIPELINE,
            projectId = projectId,
            parentPipelineId = pipelineId,
            parentBuildId = buildId,
            parentTaskId = taskId,
            pipelineId = subPipelineId,
            channelCode = channelCode,
            parameters = startParams,
            checkPermission = false,
            isMobile = false
        )

        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "已启动子流水线 - ${pipelineInfo.pipelineName}",
            tag = taskId,
            executeCount = executeCount
        )

        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$subPipelineId/detail/$subBuildId'>查看子流水线执行详情</a>",
            tag = taskId,
            executeCount = executeCount
        )

        return AtomResponse(if (param.asynchronous) BuildStatus.SUCCEED else BuildStatus.CALL_WAITING)
    }

    private fun getSubPipeline(
        param: SubPipelineCallElement,
        runVariables: Map<String, String>,
        task: PipelineBuildTask,
        buildId: String,
        taskId: String,
        projectId: String
    ): Pair<String, PipelineInfo> {
        val subPipelineId = parseVariable(param.subPipelineId, runVariables)
        if (subPipelineId.isEmpty())
            throw BuildTaskException(
                ERROR_BUILD_TASK_SUBPIPELINEID_NULL,
                "子流水线ID参数为空，请检查流水线重新保存后并重新执行", task.pipelineId, buildId, taskId
            )

        // 注意：加projectId限定了不允许跨项目，防止恶意传递了跨项目的项目id
        val pipelineInfo = (pipelineRepositoryService.getPipelineInfo(projectId, subPipelineId)
            ?: throw BuildTaskException(
                ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS,
                "子流水线[$subPipelineId]不存在,请检查流水线是否还存在", task.pipelineId, buildId, taskId
            ))
        return Pair(subPipelineId, pipelineInfo)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineCallAtom::class.java)
    }
}
