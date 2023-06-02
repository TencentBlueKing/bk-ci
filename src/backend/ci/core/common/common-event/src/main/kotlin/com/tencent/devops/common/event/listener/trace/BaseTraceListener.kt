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

package com.tencent.devops.common.event.listener.trace

import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.event.pojo.trace.ITraceEvent
import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.LoggerFactory
import org.slf4j.MDC

abstract class BaseTraceListener<in T : ITraceEvent>(val traceEventDispatcher: TraceEventDispatcher) :
    Listener<T> {

    companion object {
        private const val TryTime = 3
        private const val TryInterval1 = 1000
        private const val TryInterval2 = 3000
        private const val TryInterval3 = 5000
    }

    protected val logger = LoggerFactory.getLogger(javaClass)!!

    override fun execute(event: T) {
        var result = false
        try {
            val traceId = MDC.get(TraceTag.BIZID)
            if (traceId.isNullOrEmpty()) {
                if (!event.traceId.isNullOrEmpty()) {
                    MDC.put(TraceTag.BIZID, event.traceId)
                } else {
                    MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                }
            }
            run(event)
            result = true
        } catch (ignored: Throwable) { // #5109 打开堆栈，追踪未知错误
            logger.error("[TRACE_MQ_SEVERE]|FAIL|e=$ignored", ignored)
        } finally {
            if (!result && event.retryTime > 0) {
                event.retryTime = event.retryTime - 1
                when (event.retryTime % TryTime) {
                    1 -> event.delayMills = TryInterval1
                    2 -> event.delayMills = TryInterval2
                    else -> {
                        event.delayMills = TryInterval3
                    }
                }
                traceEventDispatcher.dispatch(event)
                logger.warn("[TRACE_MQ_SEVERE]|FAIL_TO_RETRY")
            }
            MDC.remove(TraceTag.BIZID)
        }
    }

    abstract fun run(event: T)
}
