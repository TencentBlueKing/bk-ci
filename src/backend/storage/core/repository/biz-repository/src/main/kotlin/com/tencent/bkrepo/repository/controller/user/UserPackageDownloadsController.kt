/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.download.DetailsQueryRequest
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadsDetails
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadsSummary
import com.tencent.bkrepo.repository.pojo.download.SummaryQueryRequest
import com.tencent.bkrepo.repository.service.packages.PackageDownloadsService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("构建下载量统计用户接口")
@RestController
@RequestMapping("/api/package/downloads/")
class UserPackageDownloadsController(
    private val packageDownloadsService: PackageDownloadsService,
    private val permissionManager: PermissionManager
) {

    @ApiOperation("查询包下载记录明细")
    @PostMapping("/details")
    fun queryDetails(
        @RequestAttribute userId: String,
        @RequestBody request: DetailsQueryRequest
    ): Response<PackageDownloadsDetails> {
        with(request) {
            permissionManager.checkRepoPermission(PermissionAction.READ, projectId, repoName)
            return ResponseBuilder.success(packageDownloadsService.queryDetails(this))
        }
    }

    @ApiOperation("查询包下载总览")
    @PostMapping("/summary")
    fun querySummary(
        @RequestAttribute userId: String,
        @RequestBody request: SummaryQueryRequest
    ): Response<PackageDownloadsSummary> {
        with(request) {
            permissionManager.checkRepoPermission(PermissionAction.READ, projectId, repoName)
            return ResponseBuilder.success(packageDownloadsService.querySummary(this))
        }
    }
}
