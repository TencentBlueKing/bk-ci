package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.tencent.devops.process.yaml.v2.models.on.TriggerOn

/**
 * 触发后的结果
 * @param trigger 触发结果
 * @param triggerOn 触发器->用于生成启动参数
 * @param timeTrigger 是否注册定时触发事件
 * @param deleteTrigger 是否注册删除触发的事件
 * @param repoHookName 是否注册跨项目触发事件
 */
data class TriggerResult(
    val trigger: TriggerBody,
    val triggerOn: TriggerOn?,
    val timeTrigger: Boolean,
    val deleteTrigger: Boolean,
    val repoHookName: List<String>? = null
) {
    fun noNeedTrigger(): Boolean = !trigger.trigger && !timeTrigger && !deleteTrigger && repoHookName.isNullOrEmpty()
}

/**
 * 触发后的结果
 * @param trigger 是否正常触发
 * @param notTriggerReason 未触发原因
 */
data class TriggerBody(
    var trigger: Boolean = true,
    var notTriggerReason: String? = null
) {
    fun triggerFail(path: String, message: String): TriggerBody {
        trigger = false
        notTriggerReason = "$path: $message"
        return this
    }
}
