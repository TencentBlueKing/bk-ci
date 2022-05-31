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

package com.tencent.bkrepo.maven.api

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.artifact.MavenDeleteArtifactInfo
import com.tencent.bkrepo.maven.pojo.response.MavenGAVCResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api("Maven 产品接口")
@RequestMapping("/ext")
interface MavenWebResource {
    @ApiOperation("maven jar 包删除接口")
    @DeleteMapping(MavenArtifactInfo.MAVEN_EXT_PACKAGE_DELETE)
    fun deletePackage(
        @ArtifactPathVariable mavenArtifactInfo: MavenDeleteArtifactInfo,
        @RequestParam packageKey: String
    ): Response<Void>

    @ApiOperation("maven jar 包版本删除接口")
    @DeleteMapping(MavenArtifactInfo.MAVEN_EXT_VERSION_DELETE)
    fun deleteVersion(
        @ArtifactPathVariable mavenArtifactInfo: MavenDeleteArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String?
    ): Response<Void>

    @ApiOperation("maven jar 版本详情接口")
    @GetMapping(MavenArtifactInfo.MAVEN_EXT_DETAIL)
    fun artifactDetail(
        @ArtifactPathVariable mavenArtifactInfo: MavenArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String?
    ): Response<Any?>

    @ApiOperation("maven gavc 搜索接口")
    @GetMapping("/search/gavc/{projectId}/{pageNumber}/{pageSize}")
    fun gavc(
        @PathVariable projectId: String,
        @PathVariable pageNumber: Int,
        @PathVariable pageSize: Int,
        @RequestParam g: String?,
        @RequestParam a: String?,
        @RequestParam v: String?,
        @RequestParam c: String?,
        @RequestParam repos: String?
    ): Response<Page<MavenGAVCResponse.UriResult>>
}
