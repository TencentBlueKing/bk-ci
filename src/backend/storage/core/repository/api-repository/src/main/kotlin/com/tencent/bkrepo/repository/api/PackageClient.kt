/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "PackageClient")
@RequestMapping("/service")
interface PackageClient {

    @ApiOperation("查询包信息")
    @GetMapping("/package/info/{projectId}/{repoName}")
    fun findPackageByKey(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String
    ): Response<PackageSummary?>

    @ApiOperation("根据版本名称查询版本信息")
    @GetMapping("/version/info/{projectId}/{repoName}")
    fun findVersionByName(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ): Response<PackageVersion?>

    @ApiOperation("根据版本标签查询版本信息")
    @GetMapping("/package/tag/{projectId}/{repoName}")
    fun findVersionNameByTag(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam tag: String
    ): Response<String?>

    @ApiOperation("根据语义化版本排序查找latest版本")
    @GetMapping("/version/semver/latest/{projectId}/{repoName}")
    fun findLatestBySemVer(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String
    ): Response<PackageVersion?>

    @ApiOperation("创建包版本")
    @PostMapping("/version/create")
    fun createVersion(
        @RequestBody request: PackageVersionCreateRequest
    ): Response<Void>

    @ApiOperation("删除包")
    @DeleteMapping("/package/delete/{projectId}/{repoName}")
    fun deletePackage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String
    ): Response<Void>

    @ApiOperation("删除版本")
    @DeleteMapping("/version/delete/{projectId}/{repoName}")
    fun deleteVersion(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ): Response<Void>

    @ApiOperation("更新包")
    @PostMapping("/package/update")
    fun updatePackage(
        @RequestBody request: PackageUpdateRequest
    ): Response<Void>

    @ApiOperation("更新版本")
    @PostMapping("/version/update")
    fun updateVersion(
        @RequestBody request: PackageVersionUpdateRequest
    ): Response<Void>

    @ApiOperation("搜索包")
    @PostMapping("/package/search")
    fun searchPackage(
        @RequestBody queryModel: QueryModel
    ): Response<Page<MutableMap<*, *>>>

    @ApiOperation("分页查询版本")
    @PostMapping("/version/page/{projectId}/{repoName}")
    fun listVersionPage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestBody option: VersionListOption = VersionListOption()
    ): Response<Page<PackageVersion>>

    @ApiOperation("查询所有版本")
    @PostMapping("/version/list/{projectId}/{repoName}")
    fun listAllVersion(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestBody option: VersionListOption = VersionListOption()
    ): Response<List<PackageVersion>>

    @ApiOperation("分页查询包")
    @PostMapping("/package/page/{projectId}/{repoName}")
    fun listPackagePage(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestBody option: PackageListOption = PackageListOption()
    ): Response<Page<PackageSummary>>

    @ApiOperation("查询所有包名称")
    @PostMapping("/package/list/{projectId}/{repoName}")
    fun listAllPackageNames(
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<List<String>>

    @ApiOperation("查询包数量")
    @PostMapping("/package/count/{projectId}/{repoName}")
    fun getPackageCount(
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<Long>

    /**
     * 包版本数据填充，该过程会自动累加downloads和version数量到包信息中
     *
     * 1. 如果包已经存在则不会更新包，跳到步骤2，否则创建新包
     * 2. 遍历versionList进行版本创建，如果版本已经存在则跳过。
     *
     */
    @ApiOperation("包版本数据填充")
    @PostMapping("/package/populate")
    fun populatePackage(
        @RequestBody request: PackagePopulateRequest
    ): Response<Void>
}
