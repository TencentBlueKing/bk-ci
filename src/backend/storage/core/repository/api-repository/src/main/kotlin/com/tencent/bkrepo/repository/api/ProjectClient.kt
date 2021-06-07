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
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.project.ProjectRangeQueryRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Api(description = "服务-项目接口")
@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "ProjectClient")
@RequestMapping("/service/project")
interface ProjectClient {

    @ApiOperation("查询项目信息")
    @GetMapping("/info/{name}")
    fun getProjectInfo(@ApiParam(value = "项目名") @PathVariable name: String): Response<ProjectInfo?>

    @ApiOperation("列表查询项目")
    @GetMapping("/list")
    fun listProject(): Response<List<ProjectInfo>>

    @ApiOperation("分页查询项目")
    @PostMapping("/rangeQuery")
    fun rangeQuery(@RequestBody request: ProjectRangeQueryRequest): Response<Page<ProjectInfo?>>

    @ApiOperation("创建项目")
    @PostMapping("/create")
    fun createProject(@RequestBody request: ProjectCreateRequest): Response<ProjectInfo>

    @Deprecated("replace with getProjectInfo")
    @ApiOperation("查询项目")
    @GetMapping("/query/{name}")
    fun query(@ApiParam(value = "项目名") @PathVariable name: String): Response<ProjectInfo?>

    @Deprecated("replace with createProject")
    @ApiOperation("创建项目")
    @PostMapping
    fun create(@RequestBody request: ProjectCreateRequest): Response<ProjectInfo>
}
