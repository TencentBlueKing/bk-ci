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

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.app.pipeline.AppPipeline
import com.tencent.devops.process.pojo.app.pipeline.AppPipelineHistory
import com.tencent.devops.process.pojo.app.pipeline.AppProject
import com.tencent.devops.process.service.app.AppPipelineService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.label.PipelineGroupService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class AppPipelineResourceImpl @Autowired constructor(
    private val appPipelineService: AppPipelineService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) : AppPipelineResource {

    @Value("\${gitCI.tag:#{null}}")
    private val gitCI: String? = null

    override fun listProjects(
        userId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?
    ): Result<Page<AppProject>> {
        return Result(
            appPipelineService.listProjects(
                userId,
                page ?: -1,
                pageSize ?: -1,
                channelCode ?: ChannelCode.BS
            )
        )
    }

    override fun listPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?,
        sortType: PipelineSortType?
    ): Result<PipelinePage<AppPipeline>> {
        return Result(
            appPipelineService.listPipelines(
                userId,
                projectId,
                page,
                pageSize,
                channelCode ?: ChannelCode.BS,
                sortType ?: PipelineSortType.CREATE_TIME
            )
        )
    }

    override fun listPipelineHistory(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?,
        materialBranch: List<String>?
    ): Result<Page<AppPipelineHistory>> {
        return Result(
            appPipelineService.listPipelineHistory(
                userId,
                projectId,
                pipelineId,
                page,
                pageSize,
                channelCode ?: ChannelCode.BS,
                true,
                materialBranch
            )
        )
    }

    override fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?
    ): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.getHistoryConditionBranch(userId, projectId, pipelineId, alias))
    }

    override fun getHistoryConditionStatus(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<IdValue>> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.getHistoryConditionStatus(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionRepo(userId: String, projectId: String, pipelineId: String): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.getHistoryConditionRepo(userId, projectId, pipelineId))
    }

    override fun listUserCollect(userId: String, page: Int?, pageSize: Int?): Result<Page<AppPipeline>> {
        val collectPipeline = pipelineGroupService.getFavorPipelinesPage(userId, page, pageSize)

        // 根据项目id获取流水线,返回项目下的所有的流水线
        val data = mutableListOf<AppPipeline>()
        val projectIds = collectPipeline?.map { it.projectId }?.toSet() ?: setOf()
        val pipelineIds = collectPipeline?.map { it.pipelineId }?.toSet() ?: setOf()
        projectIds.parallelStream().forEach { projectId ->
            data.addAll(appPipelineService.listPipelines(userId, projectId, page, pageSize).records)
        }
        val count = collectPipeline?.count() ?: 0

        return Result(Page(page ?: 1, pageSize ?: count, count.toLong(), data.filter { it.pipelineId in pipelineIds }))
    }

    override fun collectPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        isCollect: Boolean
    ): Result<Boolean> {
        return Result(0, "", pipelineGroupService.favorPipeline(userId, projectId, pipelineId, isCollect))
    }

    private fun checkParam(userId: String, projectId: String, pipelineId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}
