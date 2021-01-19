/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.npm.api

import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PACKAGE_DIST_TAG_ADD_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PACKAGE_DIST_TAG_INFO_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PACKAGE_INFO_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PACKAGE_SEARCH_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PACKAGE_TGZ_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PACKAGE_VERSION_INFO_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_PKG_PUBLISH_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_SCOPE_PACKAGE_VERSION_INFO_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_SCOPE_PKG_PUBLISH_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_UNPUBLISH_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_UNPUBLISH_SCOPE_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_UNPUBLISH_VERSION_MAPPING_URI
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo.Companion.NPM_UNPUBLISH_VERSION_SCOPE_MAPPING_URI
import com.tencent.bkrepo.npm.pojo.NpmDeleteResponse
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus

@Api("npm接口定义")
interface NpmResource {
    @ApiOperation("publish package")
    @PutMapping(NPM_PKG_PUBLISH_MAPPING_URI, NPM_SCOPE_PKG_PUBLISH_MAPPING_URI)
    @ResponseStatus(HttpStatus.CREATED)
    fun publish(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @RequestBody body: String
    ): NpmSuccessResponse

    @ApiOperation("search package.json info")
    @GetMapping(
        NPM_SCOPE_PACKAGE_VERSION_INFO_MAPPING_URI,
        NPM_PACKAGE_INFO_MAPPING_URI,
        NPM_PACKAGE_VERSION_INFO_MAPPING_URI,
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun searchPackageInfo(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo
    ): Map<String, Any>

    @ApiOperation("install tgz file")
    @GetMapping(NPM_PACKAGE_TGZ_MAPPING_URI)
    fun download(@ArtifactPathVariable artifactInfo: NpmArtifactInfo)

    @ApiOperation("unpublish package")
    @DeleteMapping(NPM_UNPUBLISH_MAPPING_URI, NPM_UNPUBLISH_SCOPE_MAPPING_URI)
    fun unpublish(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo
    ): NpmDeleteResponse

    @ApiOperation("update package")
    @PutMapping(NPM_UNPUBLISH_MAPPING_URI, NPM_UNPUBLISH_SCOPE_MAPPING_URI)
    fun updatePkg(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @RequestBody body: String
    ): NpmSuccessResponse

    @ApiOperation("unpublish version package")
    @DeleteMapping(NPM_UNPUBLISH_VERSION_MAPPING_URI, NPM_UNPUBLISH_VERSION_SCOPE_MAPPING_URI)
    fun unPublishPkgWithVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo
    ): NpmDeleteResponse

    @ApiOperation("npm search")
    @GetMapping(NPM_PACKAGE_SEARCH_MAPPING_URI)
    fun search(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        searchRequest: MetadataSearchRequest
    ): NpmSearchResponse

    @ApiOperation("npm get dist-tag ls")
    @GetMapping(NPM_PACKAGE_DIST_TAG_INFO_MAPPING_URI)
    fun getDistTagsInfo(@ArtifactPathVariable artifactInfo: NpmArtifactInfo): Map<String, String>

    @ApiOperation("npm dist-tag add")
    @PutMapping(NPM_PACKAGE_DIST_TAG_ADD_MAPPING_URI)
    fun addDistTags(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @RequestBody body: String
    ): NpmSuccessResponse

    @ApiOperation("npm dist-tag rm")
    @DeleteMapping(NPM_PACKAGE_DIST_TAG_ADD_MAPPING_URI)
    fun deleteDistTags(@ArtifactPathVariable artifactInfo: NpmArtifactInfo)
}
