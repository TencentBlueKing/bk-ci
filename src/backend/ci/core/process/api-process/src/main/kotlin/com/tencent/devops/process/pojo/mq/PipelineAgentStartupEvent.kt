/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo.mq

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineRoutableEvent
import com.tencent.devops.common.pipeline.type.DispatchType

@Event(MQ.EXCHANGE_AGENT_LISTENER_DIRECT, MQ.ROUTE_AGENT_STARTUP)
data class PipelineAgentStartupEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    val pipelineName: String,
    override val userId: String,
    val buildId: String,
    val buildNo: Int,
    val vmSeqId: String,
    val taskName: String,
    val os: String,
    val vmNames: String,
    @Deprecated("废弃字段")
    val startTime: Long? = null,
    val channelCode: String,
    val dispatchType: DispatchType,
    @Deprecated("废弃字段")
    val zone: Zone? = null,
    @Deprecated("废弃字段")
    val stageId: String? = null,
    val containerId: String,
    val containerHashId: String?,
    val queueTimeoutMinutes: Int? = null,
    @Deprecated("废弃字段")
    val containerType: String? = null,
    val atoms: Map<String, String> = mapOf(), // 用插件框架开发的插件信息 key为插件code，value为下载路径
    val executeCount: Int?,
    val customBuildEnv: Map<String, String>? = null,
    val dockerRoutingType: String? = "VM",
    override var actionType: ActionType = ActionType.REFRESH,
    override var delayMills: Int = 0,
    override var routeKeySuffix: String? = null
) : IPipelineRoutableEvent(routeKeySuffix, actionType, source, projectId, pipelineId, userId, delayMills)
