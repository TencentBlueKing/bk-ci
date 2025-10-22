/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 *
 */

package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.scm.api.pojo.webhook.Webhook
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "流水线触发事件")
data class PipelineTriggerEvent(
    @get:Schema(title = "请求ID")
    val requestId: String,
    @get:Schema(title = "项目ID")
    var projectId: String? = null,
    @get:Schema(title = "事件ID")
    var eventId: Long? = null,
    @get:Schema(title = "触发类型")
    val triggerType: String,
    @get:Schema(title = "事件源", required = false)
    var eventSource: String? = null,
    @get:Schema(title = "事件类型")
    val eventType: String,
    @get:Schema(title = "触发人")
    val triggerUser: String,
    @get:Schema(title = "事件描述")
    val eventDesc: String,
    @get:Schema(title = "重放事件ID")
    val replayRequestId: String? = null,
    @get:Schema(title = "事件请求参数, 记录手动/openapi/定时/远程触发启动参数")
    val requestParams: Map<String, String>? = null,
    @get:Schema(title = "触发时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "事件体")
    val eventBody: Webhook? = null
)
