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

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ComplexMethod", "NestedBlockDepth")
class TxNotifyReceiverCmdImpl @Autowired constructor(
    val bsAuthProjectApi: AuthProjectApi,
    val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
) : NotifyReceiversCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val users = mutableSetOf<String>()
        val receivers = if (commandContext.buildStatus.isSuccess()) {
            val successReceiver = EnvUtils.parseEnv(
                command = commandContext.pipelineSetting.successSubscription.users,
                data = commandContext.variables,
                replaceWithEmpty = true)
            users.addAll(successReceiver.split(",").toMutableSet())
            if (!emptyGroup(commandContext.pipelineSetting.successSubscription.groups)) {
                logger.info("success notify config group: ${commandContext.pipelineSetting.successSubscription.groups}")
                val projectRoleUsers = bsAuthProjectApi.getProjectGroupAndUserList(
                    serviceCode = bsPipelineAuthServiceCode,
                    projectCode = commandContext.projectId)
                projectRoleUsers.forEach {
                    if (it.roleName in commandContext.pipelineSetting.successSubscription.groups) {
                        users.addAll(it.userIdList)
                    }
                }
            }
            users
        } else {
            val failReceiver = EnvUtils.parseEnv(
                command = commandContext.pipelineSetting.failSubscription.users,
                data = commandContext.variables,
                replaceWithEmpty = true)
            users.addAll(failReceiver.split(",").toMutableSet())
            if (!emptyGroup(commandContext.pipelineSetting.failSubscription.groups)) {
                logger.info("fail notify config group: ${commandContext.pipelineSetting.failSubscription.groups}")
                val projectRoleUsers = bsAuthProjectApi.getProjectGroupAndUserList(
                    serviceCode = bsPipelineAuthServiceCode,
                    projectCode = commandContext.projectId)
                projectRoleUsers.forEach {
                    if (it.roleName in commandContext.pipelineSetting.failSubscription.groups) {
                        users.addAll(it.userIdList)
                    }
                }
            }
            users
        }
        commandContext.receivers = receivers
    }

    fun emptyGroup(groups: Set<String>): Boolean {
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
        val logger = LoggerFactory.getLogger(TxNotifyReceiverCmdImpl::class.java)
    }
}
