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
class TxNotifyReceiverCmdImpl @@Autowired constructor(
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
            if (commandContext.pipelineSetting.successSubscription.groups.isNotEmpty()) {
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
                command = commandContext.pipelineSetting.successSubscription.users,
                data = commandContext.variables,
                replaceWithEmpty = true)
            users.addAll(failReceiver.split(",").toMutableSet())
            if (commandContext.pipelineSetting.failSubscription.groups.isNotEmpty()) {
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

    companion object {
        val logger = LoggerFactory.getLogger(TxNotifyReceiverCmdImpl::class.java)
    }
}
