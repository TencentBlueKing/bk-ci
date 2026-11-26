package com.tencent.devops.process.pojo.webhook

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion

/**
 * webhook 启动流水线请求
 *
 * 新请求只传 version 和 startParams，由 process 按版本加载编排并转换启动参数；
 * pipelineParamMap 为已转换好的参数，会覆盖 startParams 转换后的同名参数。
 * pipelineInfo、channelCode、resource 只为兼容滚动发布期间的旧请求，resource 不为空时按旧逻辑启动。
 */
data class WebhookStartPipelineRequest(
    val pipelineInfo: PipelineInfo? = null,
    val startType: StartType,
    val pipelineParamMap: MutableMap<String, BuildParameters>? = null,
    val channelCode: ChannelCode? = null,
    val resource: PipelineResourceVersion? = null,
    val signPipelineVersion: Int? = null,
    val frequencyLimit: Boolean = false,
    val version: Int? = null,
    val startParams: Map<String, Any>? = null
)
