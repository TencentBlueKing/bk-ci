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

package com.tencent.bkrepo.docker.resource

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.api.Blob
import com.tencent.bkrepo.docker.constant.BLOB_PATTERN
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.response.DockerResponse
import com.tencent.bkrepo.docker.service.DockerV2LocalRepoService
import com.tencent.bkrepo.docker.util.PathUtil
import com.tencent.bkrepo.docker.util.UserUtil.Companion.getContextUserId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class BlobImpl @Autowired constructor(val dockerRepo: DockerV2LocalRepoService) : Blob {

    override fun uploadBlob(
        request: HttpServletRequest,
        userId: String?,
        headers: HttpHeaders,
        projectId: String,
        repoName: String,
        uuid: String,
        digest: String?,
        artifactFile: ArtifactFile
    ): DockerResponse {
        dockerRepo.httpHeaders = headers
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.uploadBlob(pathContext, DockerDigest(digest), uuid, artifactFile)
    }

    override fun isBlobExists(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        digest: String
    ): DockerResponse {
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.isBlobExists(pathContext, DockerDigest(digest))
    }

    override fun getBlob(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        digest: String
    ): ResponseEntity<Any> {
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.getBlob(pathContext, DockerDigest(digest))
    }

    override fun startBlobUpload(
        request: HttpServletRequest,
        userId: String?,
        headers: HttpHeaders,
        projectId: String,
        repoName: String,
        mount: String?
    ): ResponseEntity<Any> {
        dockerRepo.httpHeaders = headers
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.startBlobUpload(pathContext, mount)
    }

    override fun patchUpload(
        request: HttpServletRequest,
        userId: String?,
        headers: HttpHeaders,
        projectId: String,
        repoName: String,
        uuid: String,
        artifactFile: ArtifactFile
    ): DockerResponse {
        dockerRepo.httpHeaders = headers
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.patchUpload(pathContext, uuid, artifactFile)
    }
}
