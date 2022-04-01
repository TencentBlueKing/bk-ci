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

package com.tencent.devops.stream.mq.streamTrigger

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.stream.constant.MQ
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import org.slf4j.MDC

@Event(MQ.EXCHANGE_STREAM_TRIGGER_PIPELINE_EVENT, MQ.ROUTE_STREAM_TRIGGER_PIPELINE_EVENT)
data class StreamTriggerEvent(
    // TODO 为了保证消息生产消费兼容，下次发布再去掉event的token字段
    val gitToken: GitToken,
    val forkGitToken: GitToken?,
    val gitRequestEventForHandle: GitRequestEventForHandle,
    val gitProjectPipeline: GitProjectPipeline,
    val event: GitEvent,
    val originYaml: String?,
    val filePath: String,
    val gitCIBasicSetting: GitCIBasicSetting,
    val changeSet: Set<String>? = null,
    val forkGitProjectId: Long? = null,
    val traceId: String? = MDC.get(TraceTag.BIZID)
)
