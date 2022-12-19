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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineInfoResource
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineDetailInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineInfoResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService
) : UserPipelineInfoResource {
    override fun list(userId: String, projectId: String, pipelineIdListString: String?): Result<List<Pipeline>> {
        checkParam(userId, projectId)
        val pipelineIdList = pipelineIdListString?.split(",")
        val result = pipelineListFacadeService.listPipelineInfo(userId, projectId, pipelineIdList)
        return Result(result)
    }

    override fun paginationGetIdAndName(
        userId: String,
        projectId: String,
        channelCodes: String,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<PipelineIdAndName>> {
        checkParam(userId, projectId)
        val result = pipelineListFacadeService.searchByProjectIdAndName(
            projectId = projectId,
            keyword = keyword,
            page = page,
            pageSize = pageSize,
            channelCodes = channelCodes.split(",").map { ChannelCode.getChannel(it)!! }
        )
        return Result(result)
    }

    override fun searchByName(
        userId: String,
        projectId: String,
        pipelineName: String?
    ): Result<List<PipelineIdAndName>> {
        checkParam(userId, projectId)
        val pipelineInfos = pipelineListFacadeService.searchIdAndName(
            projectId = projectId,
            pipelineName = pipelineName,
            page = null,
            pageSize = null
        )
        return Result(pipelineInfos)
    }

    override fun searchByPipelineName(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineIdAndName?> {
        checkParam(userId, projectId)
        return Result(
            pipelineListFacadeService.searchByPipeline(projectId, pipelineId)
        )
    }

    override fun getPipelineInfo(userId: String, projectId: String, pipelineId: String): Result<PipelineDetailInfo?> {
        return Result(pipelineListFacadeService.getPipelineDetail(userId, projectId, pipelineId))
    }

    fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}
