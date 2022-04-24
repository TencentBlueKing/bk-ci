package com.tencent.devops.stream.trigger.parsers.triggerMatch

/**
 * 触发后的结果
 * @param trigger 是否正常触发
 * @param startParams 流水线需要的启动参数
 * @param timeTrigger 是否注册定时触发事件
 * @param deleteTrigger 是否注册删除触发的事件
 * @param repoHookName 是否注册跨项目触发事件
 */
data class TriggerResult(
    val trigger: Boolean,
    val startParams: Map<String, String>,
    val timeTrigger: Boolean,
    val deleteTrigger: Boolean,
    val repoHookName: List<String>? = null
)
