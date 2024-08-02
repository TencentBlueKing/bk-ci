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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.BluekingNotifySendCmd
import com.tencent.devops.process.notify.command.impl.NotifySendCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ComplexMethod", "NestedBlockDepth")
class TxNotifySendGroupMsgCmdImpl @Autowired constructor(
    client: Client,
    val authProjectApi: AuthProjectApi,
    val pipelineAuthServiceCode: PipelineAuthServiceCode
) : NotifySendCmd(client) {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val replaceWithEmpty = true
        val setting = commandContext.pipelineSetting
        val buildStatus = commandContext.buildStatus
        val shutdownType = when {
            buildStatus.isCancel() -> BluekingNotifySendCmd.TYPE_SHUTDOWN_CANCEL
            buildStatus.isFailure() -> BluekingNotifySendCmd.TYPE_SHUTDOWN_FAILURE
            else -> BluekingNotifySendCmd.TYPE_SHUTDOWN_SUCCESS
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
                    }.toMutableSet()
                    if (!emptyGroup(successSubscription.groups)) {
                        logger.info("success notify config group: ${successSubscription.groups}")
                        val projectRoleUsers = authProjectApi.getProjectGroupAndUserList(
                            serviceCode = pipelineAuthServiceCode,
                            projectCode = commandContext.projectId)
                        projectRoleUsers.forEach {
                            if (it.roleName in successSubscription.groups) {
                                receivers.addAll(it.userIdList)
                            }
                        }
                    }
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
                    }.toMutableSet()
                    if (!emptyGroup(failSubscription.groups)) {
                        logger.info("fail notify config group: ${failSubscription.groups}")
                        val projectRoleUsers = authProjectApi.getProjectGroupAndUserList(
                            serviceCode = pipelineAuthServiceCode,
                            projectCode = commandContext.projectId)
                        projectRoleUsers.forEach {
                            if (it.roleName in failSubscription.groups) {
                                receivers.addAll(it.userIdList)
                            }
                        }
                    }
                    sendNotifyByTemplate(
                        templateCode = getNotifyTemplateCode(shutdownType, failSubscription.detailFlag),
                        receivers = receivers,
                        notifyType = failSubscription.types.filter {
                            it != PipelineSubscriptionType.WEWORK_GROUP
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
                }
            }
            else -> Result<Any>(0)
        }
    }

    private fun emptyGroup(groups: Set<String>): Boolean {
        if (groups.isEmpty()) {
            return true
        } else {
            if (groups.size == 1 && groups.first().isEmpty()) {
                return true
            }
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxNotifySendGroupMsgCmdImpl::class.java)
    }
}
