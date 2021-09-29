package com.tencent.devops.stream.trigger

import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.git.GitEvent
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

// TODO:  统一的上下文参数，除了消息队列，逐步替代stream触发中的所有参数传递对象
// Stream 触发构建时的上下文参数
interface StreamContext {
    // gitWebHook过来的事件原文
    val gitEvent: GitEvent get() = gitEvent

    // stream 当前对git event 做了预处理的部分数据
    val requestEvent: GitRequestEvent get() = requestEvent

    // stream 项目的设置
    val streamSetting: GitCIBasicSetting get() = streamSetting
}


data class StreamRequestContext(
    val streamContext: StreamContext
) : StreamContext

data class StreamTriggerContext(
    val streamContext: StreamContext,
    val pipeline: GitProjectPipeline,
    val originYaml: String
) : StreamContext
