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
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.service.PackageService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 包管理接口
 */
@RestController
@RequestMapping("/api")
class UserPackageController(
    private val packageService: PackageService
) {

    @ApiOperation("分页查询包")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/package/page/{projectId}/{repoName}")
    fun listPackagePage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        option: PackageListOption
    ): Response<Page<PackageSummary>> {
        val pageResult = packageService.listPackagePage(projectId, repoName, option)
        return ResponseBuilder.success(pageResult)
    }

    @ApiOperation("分页查询版本")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/version/page/{projectId}/{repoName}")
    fun listVersionPage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        option: VersionListOption
    ): Response<Page<PackageVersion>> {
        val pageResult = packageService.listVersionPage(projectId, repoName, packageKey, option)
        return ResponseBuilder.success(pageResult)
    }

    @ApiOperation("查询包信息")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/package/info/{projectId}/{repoName}")
    fun findPackageByKey(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String
    ): Response<PackageSummary?> {
        return ResponseBuilder.success(packageService.findPackageByKey(projectId, repoName, packageKey))
    }

    @ApiOperation("删除包")
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    @DeleteMapping("/package/delete/{projectId}/{repoName}")
    fun deletePackage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String
    ): Response<Void> {
        packageService.deletePackage(projectId, repoName, packageKey)
        return ResponseBuilder.success()
    }

    @ApiOperation("删除版本")
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    @DeleteMapping("/version/delete/{projectId}/{repoName}")
    fun deleteVersion(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ): Response<Void> {
        packageService.deleteVersion(projectId, repoName, packageKey, version)
        return ResponseBuilder.success()
    }

    @ApiOperation("搜索包")
    @PostMapping("/package/search")
    fun searchPackage(
        @RequestBody queryModel: QueryModel
    ): Response<Page<MutableMap<*, *>>> {
        return ResponseBuilder.success(packageService.searchPackage(queryModel))
    }

    @ApiOperation("下载版本")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/version/download/{projectId}/{repoName}")
    fun downloadVersion(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ) {
        packageService.downloadVersion(projectId, repoName, packageKey, version)
    }
}
