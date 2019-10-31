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

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.service.ArtifactorySearchService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceArtifactoryJfrogResourceImpl @Autowired constructor(
    private val artifactorySearchService: ArtifactorySearchService
) : ServiceArtifactoryResource {
    override fun getReportRootUrl(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<String> {
        val url = "${HomeHostUtil.innerApiHost()}/ms/artifactory/api-html/user/reports/$projectCode/$pipelineId/$buildId/$taskId"
        return Result(url)
    }

    override fun searchFile(
        userId: String,
        projectCode: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<Page<FileInfo>> {
        checkParam(projectCode)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val bkProps = mutableListOf<Property>()
        searchProps.props.forEach { (k, v) ->
            bkProps.add(Property(k, v))
        }
        val result = artifactorySearchService.serviceSearch(projectCode, bkProps, offset, pageSizeNotNull)
        return Result(
            Page(
                count = result.first,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                totalPages = (result.first / pageSizeNotNull).toInt(),
                records = result.second
            )
        )
    }

    private fun checkParam(projectCode: String) {
        if (projectCode.isBlank()) {
            throw ParamBlankException("Invalid projectCode")
        }
    }
}