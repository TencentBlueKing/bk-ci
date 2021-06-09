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

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.PipelineWithModel
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineListFacadeService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNUSED")
@RestResource
class ServicePipelineResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : ServicePipelineResource {
    override fun status(userId: String, projectId: String, pipelineId: String): Result<Pipeline?> {
        checkParams(userId, projectId, pipelineId)
        return Result(
            data = pipelineListFacadeService.getSinglePipelineStatus(
                userId = userId, projectId = projectId, pipeline = pipelineId
            )
        )
    }

    override fun create(
        userId: String,
        projectId: String,
        pipeline: Model,
        channelCode: ChannelCode,
        useTemplateSettings: Boolean?
    ): Result<PipelineId> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(
            data = PipelineId(
                pipelineInfoFacadeService.createPipeline(
                    userId = userId,
                    projectId = projectId,
                    model = pipeline,
                    channelCode = channelCode,
                    checkPermission = ChannelCode.isNeedAuth(channelCode),
                    useTemplateSettings = useTemplateSettings
                )
            )
        )
    }

    override fun edit(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: Model,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkParams(userId, projectId, pipelineId)
        pipelineInfoFacadeService.editPipeline(
            userId, projectId, pipelineId, pipeline,
            channelCode, ChannelCode.isNeedAuth(channelCode)
        )
        // pipelineGroupService.setPipelineGroup(userId, pipelineId,projectId,pipeline.group)
        return Result(true)
    }

    override fun get(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): Result<Model> {
        checkParams(userId, projectId, pipelineId)
        return Result(pipelineInfoFacadeService.getPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            checkPermission = false
        ))
    }

    override fun getWithPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): Result<Model> {
        checkParams(userId, projectId, pipelineId)
        return Result(data = pipelineInfoFacadeService.getPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            checkPermission = checkPermission
        ))
    }

    override fun getBatch(
        userId: String,
        projectId: String,
        pipelineIds: List<String>,
        channelCode: ChannelCode
    ): Result<List<PipelineWithModel>> {
        checkParams(userId, projectId, pipelineIds)
        return Result(data = pipelineListFacadeService.getBatchPipelinesWithModel(
            userId = userId,
            projectId = projectId,
            pipelineIds = pipelineIds,
            channelCode = channelCode,
            checkPermission = false
        ))
    }

    override fun getPipelineInfo(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode?
    ): Result<PipelineInfo?> {
        checkProjectId(projectId)
        checkPipelineId(pipelineId)
        return Result(pipelineRepositoryService.getPipelineInfo(projectId, pipelineId))
    }

    override fun delete(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkParams(userId, projectId, pipelineId)
        pipelineInfoFacadeService.deletePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            checkPermission = ChannelCode.isNeedAuth(channelCode)
        )
        return Result(true)
    }

    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?,
        checkPermission: Boolean?
    ): Result<Page<Pipeline>> {
        checkUserId(userId)
        checkProjectId(projectId)
        val result = pipelineListFacadeService.listPermissionPipeline(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            sortType = PipelineSortType.CREATE_TIME,
            channelCode = channelCode ?: ChannelCode.BS,
            checkPermission = false
        )
        return Result(Page(result.page, result.pageSize, result.count, result.records))
    }

    override fun count(projectId: Set<String>?, channelCode: ChannelCode?): Result<Long> {
        val data = pipelineListFacadeService.count(projectId ?: setOf(), channelCode)
        return Result(0, "", data.toLong())
    }

    override fun isPipelineRunning(projectId: String, buildId: String, channelCode: ChannelCode): Result<Boolean> {
        checkProjectId(projectId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(pipelineListFacadeService.isPipelineRunning(projectId, buildId, channelCode))
    }

    override fun isRunning(projectId: String, buildId: String, channelCode: ChannelCode): Result<Boolean> {
        checkProjectId(projectId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(pipelineListFacadeService.isRunning(projectId, buildId, channelCode))
    }

    override fun getPipelineByIds(projectId: String, pipelineIds: Set<String>): Result<List<SimplePipeline>> {
        return Result(pipelineListFacadeService.getPipelineByIds(projectId = projectId, pipelineIds = pipelineIds))
    }

    override fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Result<Map<String, String>> {
        return Result(pipelineListFacadeService.getPipelineNameByIds(projectId, pipelineIds))
    }

    override fun getBuildNoByBuildIds(buildIds: Set<String>): Result<Map<String, String>> {
        return Result(pipelineListFacadeService.getBuildNoByByPair(buildIds))
    }

    override fun getAllstatus(userId: String, projectId: String, pipelineId: String): Result<List<Pipeline>?> {
        return Result(pipelineListFacadeService.getPipelineAllStatus(userId, projectId, pipelineId))
    }

    override fun rename(userId: String, projectId: String, pipelineId: String, name: PipelineName): Result<Boolean> {
        checkParams(userId, projectId, pipelineId)
        pipelineInfoFacadeService.renamePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            name = name.name,
            channelCode = ChannelCode.BS
        )
        return Result(true)
    }

    override fun restore(userId: String, projectId: String, pipelineId: String): Result<Boolean> {
        checkParams(userId, projectId, pipelineId)
        pipelineInfoFacadeService.restorePipeline(
            userId = userId, projectId = projectId, pipelineId = pipelineId, channelCode = ChannelCode.BS
        )
        return Result(true)
    }

    override fun getProjectPipelineIds(projectCode: String): Result<List<PipelineIdInfo>> {
        return Result(pipelineListFacadeService.getPipelineId(projectCode))
    }

    private fun checkParams(userId: String, projectId: String, pipelineId: String) {
        checkUserId(userId)
        checkProjectId(projectId)
        checkPipelineId(pipelineId)
    }

    private fun checkParams(userId: String, projectId: String, pipelineIds: List<String>) {
        checkUserId(userId)
        checkProjectId(projectId)
        checkPipelineIds(pipelineIds)
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    private fun checkProjectId(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    @Suppress("ALL")
    private fun checkPipelineIds(pipelineIds: List<String>) {
        if (pipelineIds.isEmpty()) {
            throw ParamBlankException("Invalid projectId list")
        }
        if (pipelineIds.size > 100) {
            throw InvalidParamException("Number of pipelines is too large, size:${pipelineIds.size}")
        }
    }

    private fun checkPipelineId(pipelineId: String) {
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
    }
}
