package com.tencent.devops.process.permission.notify

import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean

@Configurable
@ConditionalOnMissingBean(NotifyReceiversCmd::class)
class BluekingNotifyReceiversCmdImpl : NotifyReceiversCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val setting = commandContext.pipelineSetting
        if (commandContext.buildStatus.isFailure() || commandContext.buildStatus.isCancel()) {
            commandContext.receivers = setting.successSubscription.users.split(",").toMutableSet()
        } else if (commandContext.buildStatus.isSuccess()) {
            commandContext.receivers = setting.failSubscription.users.split(",").toMutableSet()
        }
    }
}
