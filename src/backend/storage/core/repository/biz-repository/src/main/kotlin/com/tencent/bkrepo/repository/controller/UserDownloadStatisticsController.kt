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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo.Companion.DEFAULT_MAPPING_URI
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.download.DownloadStatisticsMetricResponse
import com.tencent.bkrepo.repository.pojo.download.DownloadStatisticsResponse
import com.tencent.bkrepo.repository.service.DownloadStatisticsService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Api("构建下载量统计用户接口")
@RestController
@RequestMapping("/api/download/statistics")
class UserDownloadStatisticsController(
    private val downloadStatisticsService: DownloadStatisticsService
) {

    @ApiOperation("查询构建下载量")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/query/$DEFAULT_MAPPING_URI")
    fun query(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo,
        @ApiParam("构建名称", required = true) artifact: String,
        @ApiParam("构建版本", required = false) version: String? = null,
        @ApiParam("开始日期", required = true)
        @RequestParam startDate: LocalDate,
        @ApiParam("结束日期", required = true)
        @RequestParam endDate: LocalDate
    ): Response<DownloadStatisticsResponse> {
        with(artifactInfo) {
            val downloadStatisticsInfo =
                downloadStatisticsService.query(projectId, repoName, artifact, version, startDate, endDate)
            return ResponseBuilder.success(downloadStatisticsInfo)
        }
    }

    @ApiOperation("查询构建在 日、周、月 的下载量")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/query/special/$DEFAULT_MAPPING_URI")
    fun queryForSpecial(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo,
        @ApiParam("构建名称", required = true) artifact: String,
        @ApiParam("构建版本", required = false) version: String? = null
    ): Response<DownloadStatisticsMetricResponse> {
        with(artifactInfo) {
            val downloadStatisticsInfo =
                downloadStatisticsService.queryForSpecial(projectId, repoName, artifact, version)
            return ResponseBuilder.success(downloadStatisticsInfo)
        }
    }
}
