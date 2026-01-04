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
 */

package com.tencent.devops.stream.constant

object StreamMQ {

    // 接受流水线结束的广播事件
    const val QUEUE_PIPELINE_BUILD_FINISH_STREAM = "q.engine.pipeline.build.stream"

    // 接受stage审核和红线的广播事件
    const val QUEUE_PIPELINE_BUILD_REVIEW_STREAM = "q.engine.pipeline.build.review.stream"
    const val QUEUE_PIPELINE_BUILD_QUALITY_CHECK_STREAM = "q.engine.pipeline.build.quality.check.stream"

    // Stream webhook请求
    const val QUEUE_STREAM_REQUEST_EVENT = "q.stream.request.event"

    // Stream Mr webhook 冲突检查
    const val QUEUE_STREAM_MR_CONFLICT_CHECK_EVENT = "q.stream.mr.conflict.check.event"

    // Stream 每条流水线的触发构建请求
    const val QUEUE_STREAM_TRIGGER_PIPELINE_EVENT = "q.stream.trigger.pipeline.event"

    // 定时变更广播exchange ====================================
    const val ENGINE_STREAM_LISTENER_EXCHANGE = "e.engine.stream.listener"
    const val EXCHANGE_STREAM_TIMER_CHANGE_FANOUT = "e.engine.stream.timer.change"

    const val QUEUE_STREAM_TIMER = "q.engine.stream.timer"
}
