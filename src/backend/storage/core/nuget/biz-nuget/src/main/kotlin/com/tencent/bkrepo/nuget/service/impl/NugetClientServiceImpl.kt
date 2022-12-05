/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.v2.search.NuGetSearchRequest
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetPublishArtifactInfo
import com.tencent.bkrepo.nuget.service.NugetClientService
import com.tencent.bkrepo.nuget.util.NugetUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class NugetClientServiceImpl : NugetClientService, ArtifactService() {

    override fun getServiceDocument(artifactInfo: NugetArtifactInfo) {
        val response = HttpContextHolder.getResponse()
        try {
            var serviceDocument = NugetUtils.getServiceDocumentResource()
            serviceDocument = serviceDocument.replace(
                "\$\$baseUrl\$\$",
                HttpContextHolder.getRequest().requestURL.toString()
            )
            response.contentType = MediaTypes.APPLICATION_XML
            response.writer.write(serviceDocument)
        } catch (exception: IOException) {
            logger.error("unable to read resource: $exception")
            throw exception
        }
    }

    override fun publish(userId: String, publishInfo: NugetPublishArtifactInfo) {
        logger.info("user [$userId] handling publish package request in repo [${publishInfo.getRepoIdentify()}]")
        val context = ArtifactUploadContext(publishInfo.artifactFile)
        repository.upload(context)
        logger.info(
            "user [$userId] publish nuget package [${publishInfo.nuspecPackage.metadata.id}] with version " +
                "[${publishInfo.nuspecPackage.metadata.version}] success to repo [${publishInfo.getRepoIdentify()}]"
        )
//        context.response.status = HttpStatus.CREATED.value
//        context.response.writer.write("Successfully published NuPkg to: ${publishInfo.getArtifactFullPath()}")
    }

    override fun download(userId: String, artifactInfo: NugetDownloadArtifactInfo) {
        repository.download(ArtifactDownloadContext())
    }

    override fun findPackagesById(artifactInfo: NugetArtifactInfo, searchRequest: NuGetSearchRequest) {
        // todo
    }

    override fun delete(userId: String, artifactInfo: NugetDeleteArtifactInfo) {
        with(artifactInfo) {
            logger.info(
                "handling delete package version request for package [$packageName] and version [$version] " +
                        "in repo [${artifactInfo.getRepoIdentify()}]"
            )
            val context = ArtifactRemoveContext()
            repository.remove(context)
            logger.info(
                "userId [$userId] delete version [$version] for package [$packageName] " +
                        "in repo [${this.getRepoIdentify()}] success."
            )
            context.response.status = HttpStatus.NO_CONTENT.value
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NugetClientServiceImpl::class.java)
    }
}
