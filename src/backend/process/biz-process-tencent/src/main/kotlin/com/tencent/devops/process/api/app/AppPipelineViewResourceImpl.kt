/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.service.view.PipelineViewService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppPipelineViewResourceImpl @Autowired constructor(
    private val pipelineService: PipelineService,
    private val pipelineViewService: PipelineViewService
) : AppPipelineViewResource {
    override fun listViewPipelines(userId: String, projectId: String, page: Int?, pageSize: Int?, sortType: PipelineSortType?, filterByPipelineName: String?, filterByCreator: String?, filterByLabels: String?, viewId: String): Result<PipelineViewPipelinePage<Pipeline>> {
        return Result(pipelineService.listViewPipelines(userId, projectId, page, pageSize, sortType ?: PipelineSortType.CREATE_TIME,
                ChannelCode.BS, viewId, true, filterByPipelineName, filterByCreator, filterByLabels))
    }

    override fun getViewSettings(userId: String, projectId: String): Result<PipelineViewSettings> {
        return Result(pipelineViewService.getViewSettings(userId, projectId))
    }
}