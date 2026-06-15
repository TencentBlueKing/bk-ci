/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

/**
 * 按渠道生成 WebSocket 页面路径（注册/推送 key）的策略。
 * 使用方式：按顺序匹配 supports(channel)，第一个匹配的 strategy.build(info) 作为结果。
 *
 * 与 websocket 侧 buildNormalPage 的约定：此处 build() 产出的 key 须与前端同一逻辑页
 * 的 URL 经 buildNormalPage 规范化后的 key 一致，否则 changePage 注册与 push 查找的 key 不一致，会导致推送无法命中。
 */

interface RecordPagePathStrategy {
    fun supports(channel: ChannelCode): Boolean
    fun build(info: BuildPageInfo): String
}

interface HistoryPagePathStrategy {
    fun supports(channel: ChannelCode): Boolean
    fun build(info: BuildPageInfo): String
}

interface StatusPagePathStrategy {
    fun supports(channel: ChannelCode): Boolean
    fun build(info: BuildPageInfo): String
}

private const val CREATIVE_STREAM_PREFIX = "/console/creative-stream"

private object CreativeStreamRecordStrategy : RecordPagePathStrategy {
    override fun supports(channel: ChannelCode) = channel == ChannelCode.CREATIVE_STREAM
    override fun build(info: BuildPageInfo): String {
        val base =
            "$CREATIVE_STREAM_PREFIX/${info.projectId}/flow/${info.pipelineId}/execute/${info.buildId}/execute-detail"
        return if (info.executeCount != null) "$base?executeCount=${info.executeCount}" else base
    }
}

private object CreativeStreamHistoryStrategy : HistoryPagePathStrategy {
    override fun supports(channel: ChannelCode) = channel == ChannelCode.CREATIVE_STREAM
    override fun build(info: BuildPageInfo): String =
        "$CREATIVE_STREAM_PREFIX/${info.projectId}/flow/${info.pipelineId}/detail"
}

private object CreativeStreamStatusStrategy : StatusPagePathStrategy {
    override fun supports(channel: ChannelCode) = channel == ChannelCode.CREATIVE_STREAM
    override fun build(info: BuildPageInfo): String = "$CREATIVE_STREAM_PREFIX/${info.projectId}/list"
}

object PagePathStrategies {
    val recordStrategies: List<RecordPagePathStrategy> = listOf(CreativeStreamRecordStrategy)
    val historyStrategies: List<HistoryPagePathStrategy> = listOf(CreativeStreamHistoryStrategy)
    val statusStrategies: List<StatusPagePathStrategy> = listOf(CreativeStreamStatusStrategy)
}
