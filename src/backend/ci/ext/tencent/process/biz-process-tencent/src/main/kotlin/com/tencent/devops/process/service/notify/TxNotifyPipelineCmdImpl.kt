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

package com.tencent.devops.process.service.notify

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.notify.command.ExecutionVariables
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.process.utils.PipelineVarUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("ComplexMethod")
class TxNotifyPipelineCmdImpl @Autowired constructor(
    override val pipelineRepositoryService: PipelineRepositoryService,
    override val pipelineRuntimeService: PipelineRuntimeService,
    override val pipelineBuildFacadeService: PipelineBuildFacadeService,
    override val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTaskService: PipelineTaskService,
    override val buildVariableService: BuildVariableService
) : NotifyPipelineCmd(
    pipelineRepositoryService, pipelineRuntimeService, pipelineBuildFacadeService, client, buildVariableService
) {
    override fun getExecutionVariables(pipelineId: String, vars: MutableMap<String, String>): ExecutionVariables {
        // 兼容旧流水线的旧变量
        PipelineVarUtil.fillOldVar(vars.toMutableMap())

        var buildUser = ""
        var triggerType = ""
        var buildNum: Int? = null
        var pipelineVersion: Int? = null
        var channelCode: ChannelCode? = null
        var webhookTriggerUser: String? = null
        var pipelineUserId: String? = null
        var isMobileStart: Boolean? = null

        vars.forEach { (key, value) ->
            when (key) {
                PIPELINE_VERSION -> pipelineVersion = value.toInt()
                PIPELINE_START_USER_ID -> buildUser = value
                PIPELINE_START_TYPE -> triggerType = value
                PIPELINE_BUILD_NUM -> buildNum = value.toInt()
                PIPELINE_START_CHANNEL -> channelCode = ChannelCode.valueOf(value)
                PIPELINE_START_WEBHOOK_USER_ID -> webhookTriggerUser = value
                PIPELINE_START_PIPELINE_USER_ID -> pipelineUserId = value
                PIPELINE_START_MOBILE -> isMobileStart = value.toBoolean()
            }
        }

        val buildId = vars[PIPELINE_BUILD_ID]

        // 对于是web hook 触发的构建，用户显示触发人
        if (triggerType == StartType.WEB_HOOK.name && !webhookTriggerUser.isNullOrBlank()) {
            buildUser = webhookTriggerUser!!
        }

        if (triggerType == StartType.PIPELINE.name && !pipelineUserId.isNullOrBlank()) {
            buildUser = pipelineUserId!!
        }

        val trigger = StartType.toReadableString(triggerType, channelCode)

        // TODO: 考虑是否有必要放在此处
//        if (triggerType == StartType.PIPELINE.name) {
//            checkPipelineCall(buildId = buildId!!, vars = vars) // 通知父流水线状态
//        }

        return ExecutionVariables(
            pipelineVersion = pipelineVersion,
            buildNum = buildNum,
            trigger = trigger,
            originTriggerType = triggerType,
            user = buildUser,
            isMobileStart = isMobileStart ?: false
        )
    }

    private fun checkPipelineCall(buildId: String, vars: Map<String, String>) {
        val parentProjectId = vars[PIPELINE_START_PARENT_PROJECT_ID] ?: return
        val parentTaskId = vars[PIPELINE_START_PARENT_BUILD_TASK_ID] ?: return
        val parentBuildId = vars[PIPELINE_START_PARENT_BUILD_ID] ?: return
        val parentBuildTask = pipelineTaskService.getBuildTask(parentProjectId, parentBuildId, parentTaskId)
        if (parentBuildTask == null) {
            logger.warn("The parent build($parentBuildId) task($parentTaskId) not exist ")
            return
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildAtomTaskEvent(
                source = "sub_pipeline_build_$buildId", // 来源
                projectId = parentBuildTask.projectId,
                pipelineId = parentBuildTask.pipelineId,
                userId = parentBuildTask.starter,
                buildId = parentBuildTask.buildId,
                stageId = parentBuildTask.stageId,
                containerId = parentBuildTask.containerId,
                containerHashId = parentBuildTask.containerHashId,
                containerType = parentBuildTask.containerType,
                taskId = parentBuildTask.taskId,
                taskParam = parentBuildTask.taskParams,
                actionType = ActionType.REFRESH
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxNotifyPipelineCmdImpl::class.java)
    }
}
