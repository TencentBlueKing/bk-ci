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

package com.tencent.devops.process.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线构建完成事件
 * @author irwinsun
 * @version 1.0
 */
@Component
class MeasurePipelineBuildFinishListener @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildFinishBroadCastEvent>(pipelineEventDispatcher) {

    @Value("\${measure.execute:#{null}}")
    private val measureExecute: String? = null

    override fun run(event: PipelineBuildFinishBroadCastEvent) {
        // 是否触发开关, gitci,auto集群无需次listener
        if (!measureExecute.isNullOrEmpty() && measureExecute == "false") {
            return
        }
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        if (buildInfo == null) {
            logger.warn("[$pipelineId] build ($buildId) is not exist")
            return
        }
        logger.info("[$buildId]|[${event.source}]|status=(${event.status}|errorInfoList=${event.errorInfoList}")
    }
}
