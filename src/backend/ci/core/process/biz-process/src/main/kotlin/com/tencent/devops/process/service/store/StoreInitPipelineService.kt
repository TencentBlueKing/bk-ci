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

package com.tencent.devops.process.service.store

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.StoreInitPipelineReq
import com.tencent.devops.common.pipeline.pojo.StoreInitPipelineResp
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreInitPipelineService @Autowired constructor(
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) {
    private val logger = LoggerFactory.getLogger(StoreInitPipelineService::class.java)

    /**
     * 初始化流水线
     */
    fun initPipeline(
        userId: String,
        projectId: String,
        storeInitPipelineReq: StoreInitPipelineReq
    ): Result<StoreInitPipelineResp> {
        // 保存流水线信息
        val model = JsonUtil.to(storeInitPipelineReq.pipelineModel, Model::class.java)
        val pipelineId = pipelineInfoFacadeService.createPipeline(userId, projectId, model, ChannelCode.AM).pipelineId
        // 异步启动流水线
        var buildId: String? = null
        try {
            buildId = pipelineBuildFacadeService.buildManualStartup(
                userId = userId,
                startType = StartType.SERVICE,
                projectId = projectId,
                pipelineId = pipelineId,
                values = storeInitPipelineReq.startParams,
                channelCode = ChannelCode.AM,
                checkPermission = false,
                isMobile = false,
                startByMessage = null
            ).id
        } catch (ignored: Throwable) {
            logger.error("pipeline[$pipelineId] buildManualStartup error!", ignored)
        }
        return Result(StoreInitPipelineResp(pipelineId, buildId))
    }
}
