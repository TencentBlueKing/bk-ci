package com.tencent.devops.process.permission.notify

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd

class BluekingNotifyReceiversCmdImpl : NotifyReceiversCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val setting = commandContext.pipelineSetting
        val receivers = mutableSetOf<String>()
        if (commandContext.buildStatus.isFailure()) {
            val failReceivers = setting.failSubscription.users.split(",").toMutableSet()
            failReceivers.forEach {
                // 替换占位符
                receivers.add(EnvUtils.parseEnv(it, commandContext.variables, true))
            }
        } else if (commandContext.buildStatus.isSuccess()) {
            val successReceivers = setting.successSubscription.users.split(",").toMutableSet()
            successReceivers.forEach {
                // 替换占位符
                receivers.add(EnvUtils.parseEnv(it, commandContext.variables, true))
            }
        }
        commandContext.receivers = receivers
    }
}
