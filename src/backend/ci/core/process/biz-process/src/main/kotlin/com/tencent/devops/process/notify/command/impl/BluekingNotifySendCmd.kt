package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.process.constant.PipelineBuildParamKey
import com.tencent.devops.process.notify.command.BuildNotifyContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BluekingNotifySendCmd @Autowired constructor(
    client: Client
) : NotifySendCmd(client) {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val replaceWithEmpty = true
        val setting = commandContext.pipelineSetting
        val buildStatus = commandContext.buildStatus
        val shutdownType = when {
            buildStatus.isCancel() -> TYPE_SHUTDOWN_CANCEL
            buildStatus.isFailure() -> TYPE_SHUTDOWN_FAILURE
            else -> TYPE_SHUTDOWN_SUCCESS
        }
        when {
            buildStatus.isSuccess() -> {
                setting.successSubscriptionList?.forEach { successSubscription ->
                    // 内容为null的时候处理为空字符串
                    val successContent = EnvUtils.parseEnv(
                        successSubscription.content, commandContext.variables, replaceWithEmpty
                    )
                    val group = EnvUtils.parseEnv(
                        command = successSubscription.wechatGroup,
                        data = commandContext.variables,
                        replaceWithEmpty = true
                    )
                    commandContext.notifyValue["successContent"] = successContent
                    commandContext.notifyValue["emailSuccessContent"] = successContent
                    commandContext.notifyValue[NotifyUtils.WEWORK_GROUP_KEY] = group
                    // 注入 IMate 会话ID（订阅未填则回退到启动时透传的 ci.imate_session_id）
                    commandContext.notifyValue[NotifyUtils.IMATE_SESSION_ID_KEY] = resolveImateSessionId(
                        subscriptionSessionId = successSubscription.imateSessionId,
                        variables = commandContext.variables
                    )
                    val receivers = successSubscription.users.split(",").map {
                        EnvUtils.parseEnv(
                            command = it,
                            data = commandContext.variables,
                            replaceWithEmpty = true
                        )
                    }.toSet()
                    sendNotifyByTemplate(
                        templateCode = getNotifyTemplateCode(shutdownType, successSubscription.detailFlag),
                        receivers = receivers,
                        // 企业微信群通知/IMate通知，因接收者特殊（群id/会话id），从主消息流剥离单发
                        notifyType = successSubscription.types.filter {
                            it != PipelineSubscriptionType.WEWORK_GROUP &&
                                it != PipelineSubscriptionType.IMATE
                        }.map { it.name }.toMutableSet(),
                        titleParams = commandContext.notifyValue,
                        bodyParams = commandContext.notifyValue,
                        markdownContent = false
                    )
                    // 企业微信通知组的模板和企业微信通知用的是同一个模板,但是企业微信通知没有markdown选项,所以需要单独发送
                    if (successSubscription.types.contains(PipelineSubscriptionType.WEWORK_GROUP)) {
                        sendNotifyByTemplate(
                            templateCode = getNotifyTemplateCode(shutdownType, successSubscription.detailFlag),
                            receivers = receivers,
                            notifyType = setOf(PipelineSubscriptionType.WEWORK_GROUP.name),
                            titleParams = commandContext.notifyValue,
                            bodyParams = commandContext.notifyValue,
                            markdownContent = successSubscription.wechatGroupMarkdownFlag
                        )
                    }
                    // IMate 单独发送：渠道隔离，不影响其它历史渠道
                    if (successSubscription.types.contains(PipelineSubscriptionType.IMATE)) {
                        attachImateContext(
                            commandContext = commandContext,
                            templateCode = NotifyUtils.IMATE_TPL_PIPELINE_SUCCESS
                        )
                        sendNotifyByTemplate(
                            templateCode = getNotifyTemplateCode(shutdownType, successSubscription.detailFlag),
                            receivers = receivers,
                            notifyType = setOf(PipelineSubscriptionType.IMATE.name),
                            titleParams = commandContext.notifyValue,
                            bodyParams = commandContext.notifyValue,
                            markdownContent = false
                        )
                    }
                }
            }
            buildStatus.isFailure() -> {
                setting.failSubscriptionList?.forEach { failSubscription ->
                    // 内容为null的时候处理为空字符串
                    val failContent = EnvUtils.parseEnv(
                        failSubscription.content, commandContext.variables, replaceWithEmpty
                    )
                    val group = EnvUtils.parseEnv(
                        command = failSubscription.wechatGroup,
                        data = commandContext.variables,
                        replaceWithEmpty = true
                    )
                    commandContext.notifyValue["failContent"] = failContent
                    commandContext.notifyValue["emailFailContent"] = failContent
                    commandContext.notifyValue[NotifyUtils.WEWORK_GROUP_KEY] = group
                    commandContext.notifyValue[NotifyUtils.IMATE_SESSION_ID_KEY] = resolveImateSessionId(
                        subscriptionSessionId = failSubscription.imateSessionId,
                        variables = commandContext.variables
                    )
                    val receivers = failSubscription.users.split(",").map {
                        EnvUtils.parseEnv(
                            command = it,
                            data = commandContext.variables,
                            replaceWithEmpty = true
                        )
                    }.toSet()
                    sendNotifyByTemplate(
                        templateCode = getNotifyTemplateCode(shutdownType, failSubscription.detailFlag),
                        receivers = receivers,
                        notifyType = failSubscription.types.filter {
                            it != PipelineSubscriptionType.WEWORK_GROUP &&
                                it != PipelineSubscriptionType.IMATE
                        }.map { it.name }.toMutableSet(),
                        titleParams = commandContext.notifyValue,
                        bodyParams = commandContext.notifyValue,
                        markdownContent = false
                    )
                    // 企业微信通知组的模板和企业微信通知用的是同一个模板,但是企业微信通知没有markdown选项,所以需要单独发送
                    if (failSubscription.types.contains(PipelineSubscriptionType.WEWORK_GROUP)) {
                        sendNotifyByTemplate(
                            templateCode = getNotifyTemplateCode(shutdownType, failSubscription.detailFlag),
                            receivers = receivers,
                            notifyType = setOf(PipelineSubscriptionType.WEWORK_GROUP.name),
                            titleParams = commandContext.notifyValue,
                            bodyParams = commandContext.notifyValue,
                            markdownContent = failSubscription.wechatGroupMarkdownFlag
                        )
                    }
                    if (failSubscription.types.contains(PipelineSubscriptionType.IMATE)) {
                        attachImateContext(
                            commandContext = commandContext,
                            templateCode = NotifyUtils.IMATE_TPL_PIPELINE_FAIL
                        )
                        sendNotifyByTemplate(
                            templateCode = getNotifyTemplateCode(shutdownType, failSubscription.detailFlag),
                            receivers = receivers,
                            notifyType = setOf(PipelineSubscriptionType.IMATE.name),
                            titleParams = commandContext.notifyValue,
                            bodyParams = commandContext.notifyValue,
                            markdownContent = false
                        )
                    }
                }
            }
            else -> Result<Any>(0)
        }
    }

    /**
     * 解析 IMate 会话ID：优先使用订阅设置中的字段，否则回退到启动时透传到 build 变量中的 ci.imate_session_id。
     * 字段中支持流水线变量占位符（${ci.imate_session_id}）。
     */
    private fun resolveImateSessionId(
        subscriptionSessionId: String?,
        variables: Map<String, String>
    ): String {
        if (!subscriptionSessionId.isNullOrBlank()) {
            return EnvUtils.parseEnv(
                command = subscriptionSessionId,
                data = variables,
                replaceWithEmpty = true
            )
        }
        return variables[PipelineBuildParamKey.CI_IMATE_SESSION_ID].orEmpty()
    }

    /**
     * 把 IMate 业务上下文（模板代码 + 项目/流水线/构建坐标）写入 notifyValue，
     * 由 [com.tencent.devops.notify.service.notifier.ImateNotifier] 从 bodyParams 取出后填给 IMate；
     * IMate 端必须保存并在按钮点击回调中原样回传到 stream 后台 Open 接口。
     * 流水线运行结束场景没有 stage 维度，因此不写 STAGE_ID。
     */
    private fun attachImateContext(commandContext: BuildNotifyContext, templateCode: String) {
        commandContext.notifyValue[NotifyUtils.IMATE_TEMPLATE_CODE_KEY] = templateCode
        commandContext.notifyValue[NotifyUtils.IMATE_CTX_PROJECT_ID] = commandContext.projectId
        commandContext.notifyValue[NotifyUtils.IMATE_CTX_PIPELINE_ID] = commandContext.pipelineId
        commandContext.notifyValue[NotifyUtils.IMATE_CTX_BUILD_ID] = commandContext.buildId
    }

    companion object {
        const val TYPE_STARTUP = 1
        const val TYPE_SHUTDOWN_SUCCESS = 2
        const val TYPE_SHUTDOWN_FAILURE = 3
        const val TYPE_SHUTDOWN_CANCEL = 4
    }
}
