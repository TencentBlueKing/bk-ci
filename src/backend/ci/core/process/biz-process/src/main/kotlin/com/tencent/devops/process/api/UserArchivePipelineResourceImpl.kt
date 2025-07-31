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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserArchivePipelineResource
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.service.ArchivePipelineFacadeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserArchivePipelineResourceImpl @Autowired constructor(
    private val archivePipelineFacadeService: ArchivePipelineFacadeService
) : UserArchivePipelineResource {

    override fun getAllPipelines(userId: String, projectId: String): Result<List<Map<String, String>>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }

        return Result(archivePipelineFacadeService.getDownloadAllPipelines(userId, projectId))
    }

    override fun getDownloadAllPipelines(userId: String, projectId: String): Result<List<Map<String, String>>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }

        return Result(archivePipelineFacadeService.getDownloadAllPipelines(userId, projectId))
    }

    override fun migrateArchivePipelineData(
        userId: String,
        projectId: String,
        pipelineId: String,
        cancelFlag: Boolean
    ): Result<Boolean> {
        return Result(
            archivePipelineFacadeService.migrateArchivePipelineData(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                cancelFlag = cancelFlag
            )
        )
    }

    override fun batchMigrateArchivePipelineData(
        userId: String,
        projectId: String,
        cancelFlag: Boolean,
        pipelineIds: Set<String>
    ): Result<Boolean> {
        return Result(
            archivePipelineFacadeService.batchMigrateArchivePipelineData(
                userId = userId,
                projectId = projectId,
                cancelFlag = cancelFlag,
                pipelineIds = pipelineIds
            )
        )
    }

    override fun getArchivedPipelineList(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        sortType: PipelineSortType?,
        collation: PipelineCollation?
    ): Result<Page<PipelineInfo>> {
        return Result(
            archivePipelineFacadeService.getArchivedPipelineList(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                filterByPipelineName = filterByPipelineName,
                filterByCreator = filterByCreator,
                filterByLabels = filterByLabels,
                sortType = sortType,
                collation = collation
            )
        )
    }

    override fun getAllBuildNo(
        userId: String,
        pipelineId: String,
        projectId: String,
        debugVersion: Int?
    ): Result<List<Map<String, String>>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }

        return Result(archivePipelineFacadeService.getAllBuildNo(userId, pipelineId, projectId, debugVersion))
    }
}
