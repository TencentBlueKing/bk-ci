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
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 仓库服务接口
 */
@Api("仓库服务接口")
@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "RepositoryClient")
@RequestMapping("/service/repo")
interface RepositoryClient {

    @ApiOperation("查询仓库信息")
    @GetMapping("/info/{projectId}/{repoName}")
    fun getRepoInfo(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String
    ): Response<RepositoryInfo?>

    @ApiOperation("查询仓库详情")
    @GetMapping("/detail/{projectId}/{repoName}")
    fun getRepoDetail(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "仓库类型", required = true)
        @RequestParam type: String? = null
    ): Response<RepositoryDetail?>

    @ApiOperation("列表查询项目所有仓库")
    @GetMapping("/list/{projectId}")
    fun listRepo(
        @ApiParam(value = "项目id", required = true)
        @PathVariable projectId: String,
        @ApiParam("仓库名称", required = false)
        @RequestParam name: String? = null,
        @ApiParam("仓库类型", required = false)
        @RequestParam type: String? = null
    ): Response<List<RepositoryInfo>>

    @ApiOperation("仓库分页查询")
    @PostMapping("/rangeQuery")
    fun rangeQuery(@RequestBody request: RepoRangeQueryRequest): Response<Page<RepositoryInfo?>>

    @ApiOperation("创建仓库")
    @PostMapping("/create")
    fun createRepo(@RequestBody request: RepoCreateRequest): Response<RepositoryDetail>

    @ApiOperation("修改仓库")
    @PostMapping("/update")
    fun updateRepo(@RequestBody request: RepoUpdateRequest): Response<Void>

    @ApiOperation("删除仓库")
    @DeleteMapping("/delete")
    fun deleteRepo(@RequestBody request: RepoDeleteRequest): Response<Void>

    @ApiOperation("分页查询指定类型仓库")
    @GetMapping("/page/repoType/{page}/{size}/{repoType}")
    fun pageByType(
        @ApiParam(value = "当前页", required = true, example = "0")
        @PathVariable page: Int,
        @ApiParam(value = "分页大小", required = true, example = "20")
        @PathVariable size: Int,
        @ApiParam(value = "仓库类型", required = true)
        @PathVariable repoType: String
    ): Response<Page<RepositoryDetail>>

    @Deprecated("replace with getRepoDetail")
    @GetMapping("/query/{projectId}/{repoName}/{type}")
    fun query(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "仓库类型", required = true)
        @PathVariable type: String
    ): Response<RepositoryDetail?>

    @Deprecated("replace with getRepoDetail")
    @GetMapping("/query/{projectId}/{repoName}")
    fun query(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String
    ): Response<RepositoryDetail?>

    @Deprecated("replace with getRepoDetail")
    @ApiOperation("查询仓库详情")
    @GetMapping("/detail/{projectId}/{repoName}/{type}")
    fun getRepoDetailWithType(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "仓库类型", required = true)
        @PathVariable type: String? = null
    ): Response<RepositoryDetail?>

    @Deprecated("replace with createRepo")
    @ApiOperation("创建仓库")
    @PostMapping
    fun create(@RequestBody request: RepoCreateRequest): Response<RepositoryDetail>

    @Deprecated("replace with updateRepo")
    @ApiOperation("修改仓库")
    @PutMapping
    fun update(@RequestBody request: RepoUpdateRequest): Response<Void>

    @Deprecated("replace with deleteRepo")
    @ApiOperation("删除仓库")
    @DeleteMapping
    fun delete(@RequestBody request: RepoDeleteRequest): Response<Void>
}
