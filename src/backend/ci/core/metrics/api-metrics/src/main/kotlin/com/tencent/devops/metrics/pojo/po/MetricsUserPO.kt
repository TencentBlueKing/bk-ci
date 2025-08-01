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

package com.tencent.devops.metrics.pojo.po

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.event.MetricsEvent
import com.tencent.devops.common.pipeline.utils.EventUtils.toMetricsEventType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class MetricsUserPO(
    var startTime: LocalDateTime,
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val jobId: String?,
    val stepId: String?,
    val status: String,
    val atomCode: String?,
    val eventType: MetricsEvent,
    var endTime: LocalDateTime?,
    val labels: String?
) {
    constructor(event: PipelineBuildStatusBroadCastEvent) : this(
        startTime = event.eventTime ?: LocalDateTime.now(),
        projectId = event.projectId,
        pipelineId = event.pipelineId,
        buildId = event.buildId,
        jobId = event.jobId,
        stepId = event.stepId,
        status = checkNotNull(event.buildStatus),
        atomCode = event.atomCode,
        eventType = checkNotNull(event.toMetricsEventType()),
        endTime = null,
        labels = event.labels?.entries?.joinToString(separator = ";") { "${it.key}=${it.value}" }
    )

    companion object {
        const val DELIMITER = ","
        fun load(str: String?): MetricsUserPO? {
            if (str.isNullOrBlank()) return null
            val list = str.split(DELIMITER)
            if (list.size < 10) return null
            return MetricsUserPO(
                LocalDateTime.ofInstant(Instant.ofEpochSecond(list[0].toLong()), ZoneOffset.ofHours(8)),
                list[1],
                list[2],
                list[3],
                list[4].ifEmpty { null },
                list[5].ifEmpty { null },
                list[6],
                list[7].ifEmpty { null },
                MetricsEvent.valueOf(list[8]),
                list[9].ifEmpty { null }?.let {
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(it.toLong()), ZoneOffset.ofHours(8))
                },
                list.getOrNull(10)?.ifEmpty { null }
            )
        }
    }

    override fun toString(): String {
        return startTime.toInstant(ZoneOffset.ofHours(8)).epochSecond.toString() + DELIMITER +
            projectId + DELIMITER +
            pipelineId + DELIMITER +
            buildId + DELIMITER +
            (jobId ?: "") + DELIMITER +
            (stepId ?: "") + DELIMITER +
            status + DELIMITER +
            (atomCode ?: "") + DELIMITER +
            eventType.name + DELIMITER +
            (endTime?.toInstant(ZoneOffset.ofHours(8))?.epochSecond?.toString() ?: "") + DELIMITER +
            (labels ?: "")
    }
}
