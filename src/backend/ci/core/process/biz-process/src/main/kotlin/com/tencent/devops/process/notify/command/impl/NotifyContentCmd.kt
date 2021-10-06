package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.util.NotifyTemplateUtils

class NotifyContentCmd: NotifyCmd {
    override fun canExecute(commandContextBuild: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContextBuild: BuildNotifyContext) {
        val replaceWithEmpty = true
        val setting = commandContextBuild.pipelineSetting

        // 内容为null的时候处理为空字符串
        var successContent = setting.successSubscription.content ?: NotifyTemplateUtils.COMMON_SHUTDOWN_SUCCESS_CONTENT
        var failContent = setting.failSubscription.content ?: NotifyTemplateUtils.COMMON_SHUTDOWN_FAILURE_CONTENT

        successContent = EnvUtils.parseEnv(successContent, commandContextBuild.variables, replaceWithEmpty)
        failContent = EnvUtils.parseEnv(failContent, commandContextBuild.variables, replaceWithEmpty)

        commandContextBuild.notifyValue["successContent"] = successContent
        commandContextBuild.notifyValue["failContent"] = failContent
        commandContextBuild.notifyValue["emailSuccessContent"] = successContent
        commandContextBuild.notifyValue["emailFailContent"] = failContent
    }
}
