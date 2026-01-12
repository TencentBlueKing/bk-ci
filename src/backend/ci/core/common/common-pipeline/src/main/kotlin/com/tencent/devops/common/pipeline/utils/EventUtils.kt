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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_END
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_JOB_END
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_JOB_START
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_STAGE_END
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_STAGE_START
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_START
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_TASK_END
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_TASK_PAUSE
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType.BUILD_TASK_START
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.MetricsEvent

@Suppress("ComplexMethod")
object EventUtils {
    private val metricsEventMap = MetricsEvent.values().associateBy { it.name }
    fun PipelineBuildStatusBroadCastEvent.toEventType(): CallBackEvent? {
        return when (type) {
            BUILD_START -> CallBackEvent.BUILD_START
            BUILD_END -> CallBackEvent.BUILD_END
            BUILD_STAGE_START -> CallBackEvent.BUILD_STAGE_START
            BUILD_STAGE_END -> CallBackEvent.BUILD_STAGE_END
            BUILD_JOB_START -> CallBackEvent.BUILD_JOB_START
            BUILD_JOB_END -> CallBackEvent.BUILD_JOB_END
            BUILD_TASK_START -> CallBackEvent.BUILD_TASK_START
            BUILD_TASK_END -> CallBackEvent.BUILD_TASK_END
            BUILD_TASK_PAUSE -> CallBackEvent.BUILD_TASK_PAUSE
            else -> null
        }
    }

    fun PipelineBuildStatusBroadCastEvent.toMetricsEventType(): MetricsEvent? {
        if (type != null && metricsEventMap[type!!.name] != null) {
            return metricsEventMap[type!!.name]
        }
        return null
    }
}
