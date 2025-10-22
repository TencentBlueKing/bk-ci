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

class PipelineTriggerDetailBuilder {
    private lateinit var projectId: String
    private var detailId: Long? = null
    private var eventId: Long? = null
    private var eventSource: String? = null
    private var status: String = ""
    private var pipelineId: String? = null
    private var pipelineName: String? = null
    private var buildId: String? = null
    private var buildNum: String? = null
    private var reason: String? = null
    private var reasonDetail: PipelineTriggerReasonDetail? = null

    fun projectId(projectId: String) = apply {
        this.projectId = projectId
    }

    fun detailId(detailId: Long) = apply {
        this.detailId = detailId
    }

    fun eventId(eventId: Long) = apply {
        this.eventId = eventId
    }

    fun pipelineId(pipelineId: String) = apply {
        this.pipelineId = pipelineId
    }

    fun pipelineName(pipelineName: String) = apply {
        this.pipelineName = pipelineName
    }

    fun eventSource(eventSource: String) = apply {
        this.eventSource = eventSource
    }

    fun getEventSource() = eventSource

    fun status(status: String) = apply {
        this.status = status
    }

    fun buildId(buildId: String) = apply {
        this.buildId = buildId
    }

    fun reason(reason: String) = apply {
        this.reason = reason
    }

    fun reasonDetail(reasonDetail: PipelineTriggerReasonDetail) = apply {
        this.reasonDetail = reasonDetail
    }

    fun buildNum(buildNum: String) = apply {
        this.buildNum = buildNum
    }

    fun build(): PipelineTriggerDetail {
        return PipelineTriggerDetail(
            projectId = projectId,
            detailId = detailId,
            eventId = eventId!!,
            status = status,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            buildId = buildId,
            buildNum = buildNum,
            reason = reason,
            reasonDetail = if (status == PipelineTriggerStatus.SUCCEED.name) {
                null
            } else {
                reasonDetail
            }
        )
    }
}
