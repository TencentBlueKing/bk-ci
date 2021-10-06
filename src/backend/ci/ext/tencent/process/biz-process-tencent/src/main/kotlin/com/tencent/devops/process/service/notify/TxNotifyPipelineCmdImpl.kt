package com.tencent.devops.process.service.notify

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.ExecutionVariables
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
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
class TxNotifyPipelineCmdImpl @Autowired constructor(
    override val pipelineRepositoryService: PipelineRepositoryService,
    override val pipelineRuntimeService: PipelineRuntimeService,
    override val pipelineBuildFacadeService: PipelineBuildFacadeService,
    override val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : NotifyPipelineCmd(
    pipelineRepositoryService, pipelineRuntimeService, pipelineBuildFacadeService, client
){
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
        if (triggerType == StartType.PIPELINE.name) {
            checkPipelineCall(buildId = buildId!!, vars = vars) // 通知父流水线状态
        }

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
        val parentTaskId = vars[PIPELINE_START_PARENT_BUILD_TASK_ID] ?: return
        val parentBuildId = vars[PIPELINE_START_PARENT_BUILD_ID] ?: return
        val parentBuildTask = pipelineRuntimeService.getBuildTask(parentBuildId, parentTaskId)
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
