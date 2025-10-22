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

package com.tencent.devops.process.engine.pojo.event

import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.event.enums.ActionType

/**
 * Container事件
 *
 * @version 1.0
 */
@Event(StreamBinding.PIPELINE_BUILD_CONTAINER)
data class PipelineBuildContainerEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val stageId: String,
    val containerId: String, // model中的container.id，CONTAINER表中的seq id，TASK表中的containerId
    val executeCount: Int?,
    val containerHashId: String?,
    val containerType: String,
    val previousStageStatus: BuildStatus? = null, // 此仅在Stage下发处才会赋值，Job内/Task回调 等都会为null
    override var actionType: ActionType,
    override var delayMills: Int = 0,
    val reason: String? = null,
    @Deprecated(message = "errorCode=com.tencent.devop.common.api.pojo.ErrorCode.USER_JOB_OUTTIME_LIMIT")
    val timeout: Boolean? = false,
    /**
     * 0 表示 没有错误
     */
    var errorCode: Int = 0,
    /**
     * null 表示没有错误 see [com.tencent.devops.common.api.pojo.ErrorType.name]
     */
    var errorTypeName: String? = null
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
