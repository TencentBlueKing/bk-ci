package com.tencent.devops.process.permission.notify

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd
import org.springframework.beans.factory.annotation.Value

class BluekingNotifyReceiversCmdImpl : NotifyReceiversCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    @Value("\${user.domain:#{null}}")
    private val userUseDomain: Boolean? = true

    override fun execute(commandContext: BuildNotifyContext) {
        val setting = commandContext.pipelineSetting
        val receivers = mutableSetOf<String>()
        if (commandContext.buildStatus.isFailure()) {
            val failReceivrs = findWeworkUser(setting.failSubscription.users.split(",").toMutableSet())
            failReceivrs.forEach {
                // 替换占位符
                receivers.add(EnvUtils.parseEnv(it, commandContext.variables, true))
            }
        } else if (commandContext.buildStatus.isSuccess()) {
            val successReceivrs = findWeworkUser(setting.successSubscription.users.split(",").toMutableSet())
            successReceivrs.forEach {
                // 替换占位符
                receivers.add(EnvUtils.parseEnv(it, commandContext.variables, true))
            }
        }
        commandContext.receivers = receivers
    }

    // #5318 为解决使用蓝鲸用户中心生成了带域名的用户名无法与企业微信账号对齐问题
    private fun findWeworkUser(userSet: Set<String>): Set<String> {
        if (userUseDomain!!) {
            val weworkUserSet = mutableSetOf<String>()
            userSet.forEach {
                // 若用户名包含域,取域前的用户名.
                if (it.contains("@")) {
                    weworkUserSet.add(it.substringBefore("@"))
                } else {
                    weworkUserSet.add(it)
                }
            }
            return weworkUserSet
        }
        return userSet
    }
}
