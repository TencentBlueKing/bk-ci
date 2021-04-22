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

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest
import com.tencent.bkrepo.repository.service.packages.PackageService
import org.springframework.web.bind.annotation.RestController

@RestController
class PackageController(
    private val packageService: PackageService
) : PackageClient {

    override fun findPackageByKey(projectId: String, repoName: String, packageKey: String): Response<PackageSummary?> {
        val packageSummary = packageService.findPackageByKey(projectId, repoName, packageKey)
        return ResponseBuilder.success(packageSummary)
    }

    override fun findVersionByName(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): Response<PackageVersion?> {
        val packageVersion = packageService.findVersionByName(projectId, repoName, packageKey, version)
        return ResponseBuilder.success(packageVersion)
    }

    override fun findVersionNameByTag(
        projectId: String,
        repoName: String,
        packageKey: String,
        tag: String
    ): Response<String?> {
        val versionName = packageService.findVersionNameByTag(projectId, repoName, packageKey, tag)
        return ResponseBuilder.success(versionName)
    }

    override fun findLatestBySemVer(
        projectId: String,
        repoName: String,
        packageKey: String
    ): Response<PackageVersion?> {
        val packageVersion = packageService.findLatestBySemVer(projectId, repoName, packageKey)
        return ResponseBuilder.success(packageVersion)
    }

    override fun createVersion(request: PackageVersionCreateRequest): Response<Void> {
        packageService.createPackageVersion(request)
        return ResponseBuilder.success()
    }

    override fun deletePackage(projectId: String, repoName: String, packageKey: String): Response<Void> {
        packageService.deletePackage(projectId, repoName, packageKey)
        return ResponseBuilder.success()
    }

    override fun deleteVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): Response<Void> {
        packageService.deleteVersion(projectId, repoName, packageKey, version)
        return ResponseBuilder.success()
    }

    override fun updatePackage(request: PackageUpdateRequest): Response<Void> {
        packageService.updatePackage(request)
        return ResponseBuilder.success()
    }

    override fun updateVersion(request: PackageVersionUpdateRequest): Response<Void> {
        packageService.updateVersion(request)
        return ResponseBuilder.success()
    }

    override fun searchPackage(queryModel: QueryModel): Response<Page<MutableMap<*, *>>> {
        return ResponseBuilder.success(packageService.searchPackage(queryModel))
    }

    override fun listVersionPage(
        projectId: String,
        repoName: String,
        packageKey: String,
        option: VersionListOption
    ): Response<Page<PackageVersion>> {
        val pageResult = packageService.listVersionPage(projectId, repoName, packageKey, option)
        return ResponseBuilder.success(pageResult)
    }

    override fun listAllVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        option: VersionListOption
    ): Response<List<PackageVersion>> {
        val versions = packageService.listAllVersion(projectId, repoName, packageKey, option)
        return ResponseBuilder.success(versions)
    }

    override fun listPackagePage(
        projectId: String,
        repoName: String,
        option: PackageListOption
    ): Response<Page<PackageSummary>> {
        val pageResult = packageService.listPackagePage(projectId, repoName, option)
        return ResponseBuilder.success(pageResult)
    }

    override fun listAllPackageNames(projectId: String, repoName: String): Response<List<String>> {
        val names = packageService.listAllPackageName(projectId, repoName)
        return ResponseBuilder.success(names)
    }

    override fun getPackageCount(projectId: String, repoName: String): Response<Long> {
        val count = packageService.getPackageCount(projectId, repoName)
        return ResponseBuilder.success(count)
    }

    override fun populatePackage(request: PackagePopulateRequest): Response<Void> {
        packageService.populatePackage(request)
        return ResponseBuilder.success()
    }
}
