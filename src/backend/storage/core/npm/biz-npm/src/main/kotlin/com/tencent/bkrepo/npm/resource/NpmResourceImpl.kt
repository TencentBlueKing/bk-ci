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

package com.tencent.bkrepo.npm.resource

import com.tencent.bkrepo.npm.api.NpmResource
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.pojo.NpmDeleteResponse
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.service.NpmService
import com.tencent.bkrepo.npm.utils.GsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class NpmResourceImpl : NpmResource {

    @Autowired
    private lateinit var npmService: NpmService

    override fun publish(userId: String, artifactInfo: NpmArtifactInfo, body: String): NpmSuccessResponse {
        return npmService.publish(userId, artifactInfo, body)
    }

    override fun searchPackageInfo(artifactInfo: NpmArtifactInfo): Map<String, Any> {
        val fileInfo = npmService.searchPackageInfo(artifactInfo)
        return fileInfo?.let { GsonUtils.gsonToMaps<Any>(it) }
            ?: throw NpmArtifactNotFoundException("document not found")
    }

    override fun download(artifactInfo: NpmArtifactInfo) {
        npmService.download(artifactInfo)
    }

    override fun unpublish(userId: String, artifactInfo: NpmArtifactInfo): NpmDeleteResponse {
        return npmService.unpublish(userId, artifactInfo)
    }

    override fun updatePkg(artifactInfo: NpmArtifactInfo, body: String): NpmSuccessResponse {
        return npmService.updatePkg(artifactInfo, body)
    }

    override fun unPublishPkgWithVersion(artifactInfo: NpmArtifactInfo): NpmDeleteResponse {
        return npmService.unPublishPkgWithVersion(artifactInfo)
    }

    override fun search(artifactInfo: NpmArtifactInfo, searchRequest: MetadataSearchRequest): NpmSearchResponse {
        return npmService.search(artifactInfo, searchRequest)
    }

    override fun getDistTagsInfo(artifactInfo: NpmArtifactInfo): Map<String, String> {
        return npmService.getDistTagsInfo(artifactInfo)
    }

    override fun addDistTags(artifactInfo: NpmArtifactInfo, body: String): NpmSuccessResponse {
        return npmService.addDistTags(artifactInfo, body)
    }

    override fun deleteDistTags(artifactInfo: NpmArtifactInfo) {
        return npmService.deleteDistTags(artifactInfo)
    }
}
