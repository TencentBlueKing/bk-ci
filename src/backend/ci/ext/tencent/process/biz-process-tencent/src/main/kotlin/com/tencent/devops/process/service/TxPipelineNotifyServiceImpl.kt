package com.tencent.devops.process.service

import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineNotifyService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.util.ServiceHomeUrlUtils
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
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ComplexMethod", "NestedBlockDepth")
class TxPipelineNotifyServiceImpl @Autowired constructor(
    override val buildVariableService: BuildVariableService,
    override val pipelineRuntimeService: PipelineRuntimeService,
    override val pipelineRepositoryService: PipelineRepositoryService,
    override val pipelineSettingDao: PipelineSettingDao,
    override val dslContext: DSLContext,
    override val client: Client,
    override val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val bsAuthProjectApi: AuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val wechatWorkService: WechatWorkService
) : PipelineNotifyService(
    buildVariableService,
    pipelineRuntimeService,
    pipelineRepositoryService,
    pipelineSettingDao,
    dslContext,
    client,
    pipelineBuildFacadeService
) {
    override fun getExecutionVariables(pipelineId: String, vars: Map<String, String>): ExecutionVariables {
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

    override fun sendWeworkGroupMsg(setting: PipelineSetting, buildStatus: BuildStatus, vars: Map<String, String>) {
        var groups = mutableSetOf<String>()
        var content = ""
        var markerDownFlag = false
        var detailFlag = false
        if (buildStatus.isCancel() || buildStatus.isFailure()) {
            groups.addAll(setting.failSubscription.wechatGroup.split("[,;]".toRegex()))
            content = setting.failSubscription.content
            markerDownFlag = setting.failSubscription.wechatGroupMarkdownFlag
            detailFlag = setting.failSubscription.detailFlag
        } else if (buildStatus.isSuccess()) {
            groups.addAll(setting.successSubscription.wechatGroup.split("[,;]".toRegex()))
            content = setting.successSubscription.content
            markerDownFlag = setting.successSubscription.wechatGroupMarkdownFlag
            detailFlag = setting.successSubscription.detailFlag
        }
        sendWeworkGroup(groups, markerDownFlag, content, vars, detailFlag)
        return
    }

    override fun getReceivers(setting: PipelineSetting, type: String, projectId: String): Set<String> {
        val users = mutableSetOf<String>()
        return if (type == SUCCESS_TYPE) {
            users.addAll(setting.successSubscription.users.split(",").toMutableSet())
            if (setting.successSubscription.groups.isNotEmpty()) {
                logger.info("success notify config group: ${setting.successSubscription.groups}")
                val projectRoleUsers = bsAuthProjectApi.getProjectGroupAndUserList(bsPipelineAuthServiceCode, projectId)
                projectRoleUsers.forEach {
                    if (it.roleName in setting.successSubscription.groups) {
                        users.addAll(it.userIdList)
                    }
                }
            }
            users
        } else {
            users.addAll(setting.failSubscription.users.split(",").toMutableSet())
            if (setting.failSubscription.groups.isNotEmpty()) {
                logger.info("fail notify config group: ${setting.successSubscription.groups}")
                val projectRoleUsers = bsAuthProjectApi.getProjectGroupAndUserList(bsPipelineAuthServiceCode, projectId)
                projectRoleUsers.forEach {
                    if (it.roleName in setting.failSubscription.groups) {
                        users.addAll(it.userIdList)
                    }
                }
            }
            users
        }
    }

    override fun buildUrl(projectId: String, pipelineId: String, buildId: String): Map<String, String> {
        val detailUrl = detailUrl(projectId, pipelineId, buildId)
        val detailOuterUrl = detailOuterUrl(projectId, pipelineId, buildId)
        val detailShortOuterUrl = client.get(ServiceShortUrlResource::class).createShortUrl(
            CreateShortUrlRequest(url = detailOuterUrl, ttl = SHORT_URL_TTL)).data!!
        return mutableMapOf(
            "detailUrl" to detailUrl,
            "detailOuterUrl" to detailOuterUrl,
            "detailShortOuterUrl" to detailShortOuterUrl
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

    private fun sendWeworkGroup(
        weworkGroup: Set<String>,
        markerDownFlag: Boolean,
        content: String,
        vars: Map<String, String>,
        detailFlag: Boolean
    ) {
        val pipelineName = vars["pipelineName"]
        val buildNum = vars["buildNum"]
        val detailUrl = vars["detailUrl"]
        weworkGroup.forEach {
            if (markerDownFlag) {
                wechatWorkService.sendMarkdownGroup(content!!, it)
            } else {
                val receiver = Receiver(ReceiverType.group, it)
                val richtextContentList = mutableListOf<RichtextContent>()
                richtextContentList.add(
                    RichtextText(RichtextTextText("蓝盾流水线【$pipelineName】#$buildNum 构建成功\n\n"))
                )
                // TODO: 此处也需传入
                richtextContentList.add(RichtextText(RichtextTextText("✔️$content\n")))
                if (detailFlag) {
                    richtextContentList.add(
                        RichtextView(
                            RichtextViewLink(text = "查看详情", key = detailUrl!!, browser = 1)
                        )
                    )
                }
                val richtextMessage = RichtextMessage(receiver, richtextContentList)
                wechatWorkService.sendRichText(richtextMessage)
            }
        }
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${ServiceHomeUrlUtils.server()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"

    private fun detailOuterUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html" +
            "?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$processInstanceId"

    companion object {
        val logger = LoggerFactory.getLogger(TxPipelineNotifyServiceImpl::class.java)
        private const val SHORT_URL_TTL = 24 * 3600 * 180
    }
}
