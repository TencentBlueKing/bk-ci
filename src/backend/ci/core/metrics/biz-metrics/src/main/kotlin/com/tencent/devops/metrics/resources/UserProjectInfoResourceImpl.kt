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

package com.tencent.devops.metrics.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.metrics.api.UserProjectInfoResource
import com.tencent.devops.metrics.service.ProjectInfoManageService
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineErrorTypeInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineLabelInfo
import com.tencent.devops.metrics.pojo.dto.QueryProjectAtomListDTO
import com.tencent.devops.metrics.pojo.dto.QueryProjectPipelineLabelDTO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectInfoResourceImpl @Autowired constructor(
    private val projectInfoManageService: ProjectInfoManageService
) : UserProjectInfoResource {

    override fun queryProjectAtomList(
        projectId: String,
        userId: String,
        pipelineIds: List<String>?,
        page: Int,
        pageSize: Int,
        keyword: String?
    ): Result<Page<AtomBaseInfoDO>> {
        return Result(
            projectInfoManageService.queryProjectAtomList(
                QueryProjectAtomListDTO(
                    projectId = projectId,
                    page = page,
                    pageSize = pageSize,
                    keyword = keyword
                )
            )
        )
    }
    override fun queryProjectPipelineLabels(
        projectId: String,
        userId: String,
        pipelineIds: List<String>?,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<PipelineLabelInfo>> {
        return Result(
            projectInfoManageService.queryProjectPipelineLabels(
                QueryProjectPipelineLabelDTO(
                    pipelineIds = pipelineIds,
                    projectId = projectId,
                    page = page,
                    pageSize = pageSize,
                    keyword = keyword
                )
            )
        )
    }

    override fun queryProjectPipelineErrorTypes(
        userId: String,
        page: Int,
        pageSize: Int,
        keyword: String?
    ): Result<Page<PipelineErrorTypeInfoDO>> {
        return Result(projectInfoManageService.queryPipelineErrorTypes(page, pageSize, keyword))
    }
}
