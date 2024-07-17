package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
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
                        notifyType = successSubscription.types.filter {
                            it != PipelineSubscriptionType.WEWORK_GROUP
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
                            it != PipelineSubscriptionType.WEWORK_GROUP
                        }.map { it.name }.toMutableSet(),
                        titleParams = commandContext.notifyValue,
                        bodyParams = commandContext.notifyValue,
                        markdownContent = failSubscription.wechatGroupMarkdownFlag
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
                }
            }
            else -> Result<Any>(0)
        }
    }

    companion object {
        const val TYPE_STARTUP = 1
        const val TYPE_SHUTDOWN_SUCCESS = 2
        const val TYPE_SHUTDOWN_FAILURE = 3
        const val TYPE_SHUTDOWN_CANCEL = 4
    }
}
