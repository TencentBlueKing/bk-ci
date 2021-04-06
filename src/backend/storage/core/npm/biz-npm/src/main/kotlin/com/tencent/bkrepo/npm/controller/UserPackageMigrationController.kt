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

import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.service.DataMigrationService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("npm 用户接口")
@RequestMapping("/ext")
@RestController
class UserPackageMigrationController(
    private val dataMigrationService: DataMigrationService
) {

    @ApiOperation("data migration by file")
    @GetMapping("/{projectId}/{repoName}/dataMigrationByFile")
    fun dataMigrationByFile(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @RequestParam(defaultValue = "false") useErrorData: Boolean
    ): MigrateDetail {
        return dataMigrationService.dataMigrationByFile(artifactInfo, useErrorData)
    }

    @ApiOperation("data migration by url")
    @GetMapping("/{projectId}/{repoName}/dataMigrationByUrl")
    fun dataMigrationByUrl(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @RequestParam(defaultValue = "false") useErrorData: Boolean
    ): MigrateDetail {
        return dataMigrationService.dataMigrationByUrl(artifactInfo, useErrorData)
    }

    @ApiOperation("data migration by PkgName")
    @GetMapping("/{projectId}/{repoName}/dataMigrationByPkgName")
    fun dataMigrationByPkgName(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @RequestParam(defaultValue = "false") useErrorData: Boolean,
        @RequestParam pkgName: String
    ): MigrateDetail {
        return dataMigrationService.dataMigrationByPkgName(artifactInfo, useErrorData, pkgName)
    }
}
