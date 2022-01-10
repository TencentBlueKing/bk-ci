package com.tencent.devops.stream.trigger

import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

// TODO:  统一的上下文参数，除了消息队列，逐步替代stream触发中的所有参数传递对象
// Stream 触发构建时的上下文参数
interface StreamContext {
    // gitWebHook过来的事件原文
    val gitEvent: GitEvent

    // stream 当前对git event 做了预处理的部分数据
    val requestEvent: GitRequestEvent

    // stream 项目的设置
    val streamSetting: GitCIBasicSetting
}

data class StreamRequestContext(
    override val gitEvent: GitEvent,
    override val requestEvent: GitRequestEvent,
    override val streamSetting: GitCIBasicSetting
) : StreamContext

data class StreamTriggerContext(
    override val gitEvent: GitEvent,
    override val requestEvent: GitRequestEvent,
    override val streamSetting: GitCIBasicSetting,
    // 用来构建的流水线
    val pipeline: GitProjectPipeline,
    // yaml原文配置
    val originYaml: String,
    // 变更文件列表
    val mrChangeSet: Set<String>?
) : StreamContext
