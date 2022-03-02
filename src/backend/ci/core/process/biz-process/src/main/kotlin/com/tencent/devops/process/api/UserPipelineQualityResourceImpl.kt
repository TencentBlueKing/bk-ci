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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineListRequest
import com.tencent.devops.process.api.user.UserPipelineQualityResource
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.quality.api.v2.pojo.response.QualityPipeline
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineQualityResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : UserPipelineQualityResource {
    override fun getPipelineInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode?
    ): Result<PipelineInfo> {
        return Result(
            pipelineRepositoryService.getPipelineInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode
            ) ?: throw RuntimeException("pipeline info not found for $pipelineId")
        )
    }

    override fun list(userId: String, projectId: String, request: PipelineListRequest?): Result<List<Pipeline>> {
        return Result(
            pipelineListFacadeService.listPipelineInfo(
                userId = userId,
                projectId = projectId,
                pipelineIdList = request?.pipelineId,
                templateIdList = request?.templateId
            )
        )
    }

    override fun listQualityViewPipelines(
        userId: String,
        projectId: String,
        keywords: String?,
        page: Int?,
        pageSize: Int?,
        viewId: String?
    ): Result<PipelineViewPipelinePage<QualityPipeline>> {
        return Result(
            pipelineListFacadeService.listQualityViewPipelines(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                sortType = PipelineSortType.CREATE_TIME,
                channelCode = ChannelCode.BS,
                viewId = viewId ?: PIPELINE_VIEW_ALL_PIPELINES,
                checkPermission = true,
                filterByPipelineName = keywords
            )
        )
    }
}
