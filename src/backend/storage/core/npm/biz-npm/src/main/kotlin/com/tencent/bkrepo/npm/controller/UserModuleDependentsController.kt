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

package com.tencent.bkrepo.npm.controller

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.npm.pojo.module.des.ModuleDepsInfo
import com.tencent.bkrepo.npm.service.ModuleDepsService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("npm 依赖查询接口")
@RequestMapping("/ext")
@RestController
class UserModuleDependentsController(
    private val moduleDepsService: ModuleDepsService
) {
    @ApiOperation("分页查询某个资源节点被依赖的所有资源名称")
    @GetMapping("/dependent/page/{projectId}/{repoName}")
    fun page(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "当前页", required = true, example = "1")
        @RequestParam pageNumber: Int = DEFAULT_PAGE_NUMBER,
        @ApiParam(value = "分页大小", required = true, example = "20")
        @RequestParam pageSize: Int = DEFAULT_PAGE_SIZE,
        @ApiParam(value = "资源名称", required = true)
        @RequestParam packageKey: String
    ): Response<Page<ModuleDepsInfo>> {
        val name = PackageKeys.resolveNpm(packageKey)
        return ResponseBuilder.success(moduleDepsService.page(projectId, repoName, pageNumber, pageSize, name))
    }
}
