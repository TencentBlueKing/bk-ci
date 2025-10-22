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

import java.time.LocalDateTime

class PipelineTriggerEventBuilder {
    private var requestId: String? = null
    private var projectId: String? = null
    private var eventId: Long? = null
    private var triggerType: String? = null
    private var eventSource: String? = null
    private var eventType: String? = null
    private var triggerUser: String? = null
    private var eventDesc: String? = null
    private var requestParams: Map<String, String>? = null
    private var createTime: LocalDateTime? = null

    fun requestId(requestId: String) = apply {
        this.requestId = requestId
    }

    fun projectId(projectId: String) = apply {
        this.projectId = projectId
    }

    fun eventId(eventId: Long) = apply {
        this.eventId = eventId
    }

    fun triggerType(triggerType: String) = apply {
        this.triggerType = triggerType
    }

    fun eventSource(eventSource: String) = apply {
        this.eventSource = eventSource
    }

    fun eventType(eventType: String) = apply {
        this.eventType = eventType
    }

    fun triggerUser(triggerUser: String) = apply {
        this.triggerUser = triggerUser
    }

    fun eventDesc(eventDesc: String) = apply {
        this.eventDesc = eventDesc
    }

    fun requestParams(requestParams: Map<String, String>) = apply {
        this.requestParams = requestParams
    }

    fun createTime(createTime: LocalDateTime) = apply {
        this.createTime = createTime
    }

    fun build(): PipelineTriggerEvent {
        return PipelineTriggerEvent(
            requestId = requestId!!,
            projectId = projectId,
            eventId = eventId,
            triggerType = triggerType!!,
            eventSource = eventSource!!,
            eventType = eventType!!,
            triggerUser = triggerUser!!,
            eventDesc = eventDesc!!,
            requestParams = requestParams,
            createTime = createTime!!
        )
    }
}
