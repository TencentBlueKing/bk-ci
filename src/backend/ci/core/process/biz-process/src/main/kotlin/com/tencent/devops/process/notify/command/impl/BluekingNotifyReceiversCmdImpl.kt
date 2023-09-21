package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.process.notify.command.BuildNotifyContext

class BluekingNotifyReceiversCmdImpl : NotifyReceiversCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val setting = commandContext.pipelineSetting
        if (commandContext.buildStatus.isFailure()) {
            setting.failSubscription.users.split(",").forEach {
                commandContext.receivers.add(EnvUtils.parseEnv(
                    command = it,
                    data = commandContext.variables,
                    replaceWithEmpty = true))
            }
        } else if (commandContext.buildStatus.isSuccess()) {
            setting.successSubscription.users.split(",").forEach {
                commandContext.receivers.add(EnvUtils.parseEnv(
                    command = it,
                    data = commandContext.variables,
                    replaceWithEmpty = true))
            }
        }
    }
}
