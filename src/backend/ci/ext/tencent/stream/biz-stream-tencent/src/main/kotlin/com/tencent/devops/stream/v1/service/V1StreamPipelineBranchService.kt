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

package com.tencent.devops.stream.v1.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.stream.v1.dao.V1StreamPipelineBranchDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class V1StreamPipelineBranchService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamPipelineBranchDao: V1StreamPipelineBranchDao
) {
    fun saveOrUpdate(
        gitProjectId: Long,
        pipelineId: String,
        branch: String
    ) {
        streamPipelineBranchDao.saveOrUpdate(
            dslContext = dslContext, gitProjectId = gitProjectId, pipelineId = pipelineId, branch = branch
        )
    }

    fun deleteBranch(
        gitProjectId: Long,
        pipelineId: String,
        branch: String?
    ): Boolean {
        return if (branch.isNullOrBlank()) {
            streamPipelineBranchDao.deletePipeline(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId
            ) > 0
        } else {
            streamPipelineBranchDao.deleteBranch(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                branch = branch
            ) > 0
        }
    }

    fun hasBranchExist(
        gitProjectId: Long,
        pipelineId: String
    ): Boolean {
        return streamPipelineBranchDao.pipelineBranchCount(dslContext, gitProjectId, pipelineId) > 0
    }

    fun getBranchPipelines(
        gitProjectId: Long,
        branch: String
    ): List<String> {
        return streamPipelineBranchDao.getBranchPipelines(dslContext, gitProjectId, branch).map { it.pipelineId }
    }

    fun getProjectBranches(
        gitProjectId: Long,
        pipelineId: String,
        search: String?,
        page: Int,
        pageSize: Int,
        orderBy: GitCodeBranchesOrder,
        sort: GitCodeBranchesSort
    ): Page<String> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

        val count = streamPipelineBranchDao.getProjectPipelineCount(
            dslContext, gitProjectId, pipelineId, search, limit, orderBy, sort
        ).let {
            if (it <= 0) {
                return Page(0, 0, 0, emptyList())
            }
            it
        }

        val result = streamPipelineBranchDao.getProjectPipeline(
            dslContext, gitProjectId, pipelineId, search, limit, orderBy, sort
        )
        return Page(
            page = page,
            pageSize = pageSize,
            count = count.toLong(),
            records = result.map {
                it.branch
            }
        )
    }
}
