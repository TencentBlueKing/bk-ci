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

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmDeleteResponse
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.pojo.metadata.disttags.DistTags
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.utils.NpmUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * npm 客户端操作指令
 */
@RestController
class NpmClientController(
    private val npmClientService: NpmClientService
) {

    /**
     * npm service info
     */
    @GetMapping("/{projectId}/{repoName}")
    fun repoInfo(): ResponseEntity<Void> {
        return ResponseEntity.ok().build<Void>()
    }

    /**
     * npm publish or update package
     */
    @PutMapping("/{projectId}/{repoName}/{name}")
    fun publishOrUpdatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable name: String
    ): NpmSuccessResponse {
        return npmClientService.publishOrUpdatePackage(userId, artifactInfo, name)
    }

    @PutMapping("/{projectId}/{repoName}/@{scope}/{name}")
    fun publishOrUpdatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String,
        @PathVariable name: String
    ): NpmSuccessResponse {
        val pkgName = String.format("@%s/%s", scope, name)
        return npmClientService.publishOrUpdatePackage(userId, artifactInfo, pkgName)
    }

    /**
     * query package.json info
     */
    @GetMapping("/{projectId}/{repoName}/{name}", produces = [MediaTypes.APPLICATION_JSON])
    fun packageInfo(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable name: String
    ): NpmPackageMetaData {
        return npmClientService.packageInfo(artifactInfo, name)
    }

    @GetMapping("/{projectId}/{repoName}/@{scope}/{name}", produces = [MediaTypes.APPLICATION_JSON])
    fun packageInfo(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String,
        @PathVariable name: String
    ): NpmPackageMetaData {
        val pkgName = String.format("@%s/%s", scope, name)
        return npmClientService.packageInfo(artifactInfo, pkgName)
    }

    /**
     * query package-version.json info
     */
    @GetMapping("/{projectId}/{repoName}/{name}/{version}", produces = [MediaTypes.APPLICATION_JSON])
    fun packageVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable name: String,
        @PathVariable version: String
    ): NpmVersionMetadata {
        return npmClientService.packageVersionInfo(artifactInfo, name, version)
    }

    @GetMapping(
        "/{projectId}/{repoName}/@{scope}/{name}/{version}", produces = [MediaTypes.APPLICATION_JSON]
    )
    fun packageVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String,
        @PathVariable name: String,
        @PathVariable version: String
    ): NpmVersionMetadata {
        val pkgName = String.format("@%s/%s", scope, name)
        return npmClientService.packageVersionInfo(artifactInfo, pkgName, version)
    }

    @RequestMapping(value = ["/{projectId}/{repoName}/**/*.tgz"], method = [RequestMethod.HEAD])
    fun downloadHead(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo
    ) {
        this.download(artifactInfo)
    }

    /**
     * download tgz file
     */
    @GetMapping("/{projectId}/{repoName}/**/*.tgz")
    fun download(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo
    ) {
        npmClientService.download(artifactInfo)
    }

    /**
     * npm search
     */
    @GetMapping("/{projectId}/{repoName}/-/v1/search")
    fun search(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        searchRequest: MetadataSearchRequest
    ): NpmSearchResponse {
        return npmClientService.search(artifactInfo, searchRequest)
    }

    /**
     * npm get dist-tag ls
     */
    @GetMapping(
        "/{projectId}/{repoName}/-/package/{name}/dist-tags",
        "/{projectId}/{repoName}/-/package/@{scope}/{name}/dist-tags"
    )
    fun getDistTags(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String
    ): DistTags {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        return npmClientService.getDistTags(artifactInfo, pkgName)
    }

    /**
     * npm dist-tag add
     */
    @PutMapping(
        "/{projectId}/{repoName}/-/package/{name}/dist-tags/{tag}",
        "/{projectId}/{repoName}/-/package/@{scope}/{name}/dist-tags/{tag}"
    )
    fun addDistTags(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable tag: String
    ): NpmSuccessResponse {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.addDistTags(userId, artifactInfo, pkgName, tag)
        return NpmSuccessResponse.createTagSuccess()
    }

    /**
     * npm dist-tag rm
     */
    @DeleteMapping(
        "/{projectId}/{repoName}/-/package/{name}/dist-tags/{tag}",
        "/{projectId}/{repoName}/-/package/@{scope}/{name}/dist-tags/{tag}"
    )
    fun deleteDistTags(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable tag: String
    ) {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.deleteDistTags(userId, artifactInfo, pkgName, tag)
    }

    /**
     * delete the version triggers the request
     */
    @PutMapping("/{projectId}/{repoName}/{name}/-rev/{rev}", "/{projectId}/{repoName}/@{scope}/{name}/-rev/{rev}")
    fun updatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable rev: String
    ): NpmSuccessResponse {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.updatePackage(userId, artifactInfo, pkgName)
        return NpmSuccessResponse.updatePkgSuccess()
    }

    /**
     * npm unpublish package@1.0.0
     */
    @DeleteMapping(
        "/**/{projectId}/{repoName}/{name}/{separator:-|download}/{filename}/-rev/{rev}",
        "/**/{projectId}/{repoName}/@{scope}/{name}/{separator:-|download}/@{scope}/{filename}/-rev/{rev}"
    )
    fun deleteVersion(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable separator: String,
        @PathVariable filename: String,
        @PathVariable rev: String
    ): NpmDeleteResponse {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        val tgzPath = HttpContextHolder.getRequest().requestURI.substringAfterLast(artifactInfo.getRepoIdentify())
            .substringBeforeLast("/-rev")
        val version = NpmUtils.analyseVersionFromPackageName(filename, name)
        npmClientService.deleteVersion(userId, artifactInfo, pkgName, version, tgzPath)
        return NpmDeleteResponse(true, pkgName, rev)
    }

    /**
     * npm unpublish package
     */
    @DeleteMapping("/{projectId}/{repoName}/{name}/-rev/{rev}", "/{projectId}/{repoName}/@{scope}/{name}/-rev/{rev}")
    fun deletePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable rev: String
    ): NpmDeleteResponse {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.deletePackage(userId, artifactInfo, pkgName)
        return NpmDeleteResponse(true, pkgName, rev)
    }

    companion object {
        fun isFullMetadata(): Boolean {
            val referer = HeaderUtils.getHeader("referer")
            return referer == null || referer.isEmpty() || !referer.startsWith("install ")
        }
    }
}
