package com.tencent.devops.process.permission.notify

import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean

@Configurable
@ConditionalOnMissingBean(NotifyReceiversCmd::class)
class BluekingNotifyReceiversCmdImpl : NotifyReceiversCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    @Value("\${user.domain:#{null}")
    private val userUseDomain: Boolean? = true

    override fun execute(commandContext: BuildNotifyContext) {
        val setting = commandContext.pipelineSetting
        if (commandContext.buildStatus.isFailure() || commandContext.buildStatus.isCancel()) {
            commandContext.receivers = findWeworkUser(setting.successSubscription.users.split(",").toMutableSet())
        } else if (commandContext.buildStatus.isSuccess()) {
            commandContext.receivers = findWeworkUser(setting.failSubscription.users.split(",").toMutableSet())
        }
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
