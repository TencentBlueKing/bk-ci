/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.normalizedLevel
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.pojo.request.ArtifactVulnerabilityRequest
import com.tencent.bkrepo.scanner.pojo.request.FileScanResultDetailRequest
import com.tencent.bkrepo.scanner.pojo.request.FileScanResultOverviewRequest
import com.tencent.bkrepo.scanner.pojo.response.ArtifactScanResultOverview
import com.tencent.bkrepo.scanner.pojo.response.ArtifactVulnerabilityInfo
import com.tencent.bkrepo.scanner.pojo.response.FileScanResultDetail
import com.tencent.bkrepo.scanner.pojo.response.FileScanResultOverview
import com.tencent.bkrepo.scanner.service.ScanPlanService
import com.tencent.bkrepo.scanner.service.ScanService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("扫描报告")
@RestController
@RequestMapping("/api/scan")
class UserScanReportController(
    private val scanService: ScanService,
    private val scanPlanService: ScanPlanService
) {

    @ApiOperation("获取文件扫描报告详情")
    @Permission(type = ResourceType.NODE, action = PermissionAction.READ)
    @PostMapping("/reports/detail${DefaultArtifactInfo.DEFAULT_MAPPING_URI}")
    fun artifactReport(
        @ArtifactPathVariable artifactInfo: ArtifactInfo,
        @RequestBody request: FileScanResultDetailRequest
    ): Response<FileScanResultDetail> {
        request.artifactInfo = artifactInfo
        return ResponseBuilder.success(scanService.resultDetail(request))
    }

    @ApiOperation("文件扫描结果预览")
    @Principal(PrincipalType.ADMIN)
    @PostMapping("/reports/overview")
    fun artifactReports(
        @RequestBody request: FileScanResultOverviewRequest
    ): Response<List<FileScanResultOverview>> {
        return ResponseBuilder.success(scanService.resultOverview(request))
    }

    @ApiOperation("制品详情--漏洞数据")
    @GetMapping("/artifact/leak/{projectId}/{subScanTaskId}")
    @Deprecated("切换为使用artifactReports接口", ReplaceWith("artifactReport"))
    fun artifactLeak(
        @ApiParam(value = "projectId") @PathVariable projectId: String,
        @ApiParam(value = "扫描记录id") @PathVariable subScanTaskId: String,
        request: ArtifactVulnerabilityRequest
    ): Response<Page<ArtifactVulnerabilityInfo>> {
        request.projectId = projectId
        request.subScanTaskId = subScanTaskId
        request.leakType = request.leakType?.let { normalizedLevel(it) }
        return ResponseBuilder.success(scanService.resultDetail(request))
    }

    @ApiOperation("制品详情--漏洞数据")
    @GetMapping("/artifact/count/{projectId}/{subScanTaskId}")
    @Deprecated("切换为使用artifactReports接口", ReplaceWith("artifactReport"))
    fun artifactCount(
        @ApiParam(value = "projectId") @PathVariable projectId: String,
        @ApiParam(value = "扫描记录id") @PathVariable subScanTaskId: String
    ): Response<ArtifactScanResultOverview> {
        return ResponseBuilder.success(scanPlanService.planArtifact(projectId, subScanTaskId))
    }
}
