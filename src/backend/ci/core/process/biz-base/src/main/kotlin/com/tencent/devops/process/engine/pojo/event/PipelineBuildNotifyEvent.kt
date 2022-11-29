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

package com.tencent.devops.process.engine.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineRoutableEvent

/**
 * 流水线构建通知
 *
 * @version 1.0
 */
@Event(MQ.EXCHANGE_PIPELINE_MONITOR_DIRECT, MQ.ROUTE_PIPELINE_BUILD_NOTIFY)
data class PipelineBuildNotifyEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val notifyType: MutableSet<String>? = null, // 指定消息类型 枚举保护：使用NotifyType.name传值
    val receivers: List<String>, // 收信人
    val notifyTemplateEnum: String, // 模板, 枚举保护： PipelineNotifyTemplateEnum.name传值
    val titleParams: MutableMap<String, String>,
    val bodyParams: MutableMap<String, String>,
    val callbackData: Map<String, String>? = null,
    val notifyCompleteCheck: Boolean = false,
    val markdownContent: Boolean? = false, // 是否以markdown格式发送通知内容，为true时，会指向md格式的模板
    val position: String?,
    val stageId: String?,
    override var actionType: ActionType = ActionType.START,
    override var delayMills: Int = 0,
    override var routeKeySuffix: String? = null
) : IPipelineRoutableEvent(routeKeySuffix, actionType, source, projectId, pipelineId, userId, delayMills)
