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

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_JSON
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.v2.search.NuGetSearchRequest
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetPublishArtifactInfo
import com.tencent.bkrepo.nuget.service.NugetClientService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}")
@RestController
class NugetClientController(
    private val nugetClientService: NugetClientService
) {
    @GetMapping(produces = [APPLICATION_JSON])
    fun getServiceDocument(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo
    ) {
        nugetClientService.getServiceDocument(artifactInfo)
    }

    /**
     * usage: NuGet push <程序包路径> [API 密钥] [ options ]
     * Content-Type multipart/form-data
     * A package with the provided ID and version already exists, status code 409
     */
    @PutMapping("/v2/package")
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun publish(
        @RequestAttribute userId: String,
        publishInfo: NugetPublishArtifactInfo
    ) {
        nugetClientService.publish(userId, publishInfo)
    }

    @GetMapping("/Download/{id}/{version}")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun download(
        @RequestAttribute userId: String,
        artifactInfo: NugetDownloadArtifactInfo
    ) {
        nugetClientService.download(userId, artifactInfo)
    }

    @GetMapping("/FindPackagesById()", produces = ["application/xml"])
    fun findPackagesById(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        searchRequest: NuGetSearchRequest
    ) {
        nugetClientService.findPackagesById(artifactInfo, searchRequest)
    }

    /**
     * nuget delete <packageID> <packageVersion> [ options ]
     */
    @DeleteMapping("/v2/package/{id}/{version}")
    @Permission(ResourceType.REPO, PermissionAction.DELETE)
    fun delete(
        @RequestAttribute userId: String,
        artifactInfo: NugetDeleteArtifactInfo
    ) {
        nugetClientService.delete(userId, artifactInfo)
    }
}
