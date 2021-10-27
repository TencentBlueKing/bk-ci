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
            val failReceivrs = setting.failSubscription.users.split(",").toMutableSet()
            failReceivrs.forEach {
                receivers.add(EnvUtils.parseEnv(it, commandContext.variables, true))
            }
        } else if (commandContext.buildStatus.isSuccess()) {
            val successReceivrs = setting.successSubscription.users.split(",").toMutableSet()
            successReceivrs.forEach {
                receivers.add(EnvUtils.parseEnv(it, commandContext.variables, true))
            }
        }
        commandContext.receivers = receivers
    }
}
