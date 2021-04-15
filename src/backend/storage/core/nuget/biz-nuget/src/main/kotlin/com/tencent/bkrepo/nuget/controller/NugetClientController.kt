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

package com.tencent.bkrepo.nuget.controller

import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_JSON
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.v2.search.NuGetSearchRequest
import com.tencent.bkrepo.nuget.service.NugetClientService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestController

@RestController
class NugetClientController(
    private val nugetClientService: NugetClientService
) {
    @GetMapping("/{projectId}/{repoName}", produces = [APPLICATION_JSON])
    fun getServiceDocument(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo
    ): String {
        return nugetClientService.getServiceDocument(artifactInfo)
    }

    @PutMapping("/{projectId}/{repoName}/**")
    fun publish(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        artifactFileMap: ArtifactFileMap
    ): String {
        return nugetClientService.publish(userId, artifactInfo, artifactFileMap)
    }

    @GetMapping("/{projectId}/{repoName}/Download/{packageId}/{packageVersion}")
    fun download(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        @PathVariable packageId: String,
        @PathVariable packageVersion: String
    ) {
        nugetClientService.download(userId, artifactInfo, packageId, packageVersion)
    }

    @GetMapping("/{projectId}/{repoName}/FindPackagesById()", produces = ["application/xml"])
    fun findPackagesById(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        searchRequest: NuGetSearchRequest
    ) {
        nugetClientService.findPackagesById(artifactInfo, searchRequest)
    }

    @DeleteMapping("/{projectId}/{repoName}/{packageId}/{packageVersion}")
    fun delete(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        @PathVariable packageId: String,
        @PathVariable packageVersion: String
    ) {
        nugetClientService.delete(userId, artifactInfo, packageId, packageVersion)
    }
}
