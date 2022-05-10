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

package com.tencent.bkrepo.docker.controller

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.constant.BLOB_PATTERN
import com.tencent.bkrepo.docker.constant.DOCKER_API_PREFIX
import com.tencent.bkrepo.docker.constant.DOCKER_BLOB_DIGEST_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_BLOB_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_BLOB_UUID_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_DIGEST
import com.tencent.bkrepo.docker.constant.DOCKER_PROJECT_ID
import com.tencent.bkrepo.docker.constant.DOCKER_REPO_NAME
import com.tencent.bkrepo.docker.constant.DOCKER_UUID
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.response.DockerResponse
import com.tencent.bkrepo.docker.service.DockerV2LocalRepoService
import com.tencent.bkrepo.docker.util.PathUtil
import com.tencent.bkrepo.docker.util.UserUtil.Companion.getContextUserId
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(DOCKER_API_PREFIX)
class BlobController @Autowired constructor(val dockerRepo: DockerV2LocalRepoService) {

    @PutMapping(DOCKER_BLOB_UUID_SUFFIX)
    fun uploadBlob(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @RequestHeader
        headers: HttpHeaders,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @PathVariable
        @ApiParam(value = DOCKER_UUID, required = true)
        uuid: String,
        @RequestParam
        @ApiParam(value = DOCKER_DIGEST, required = false)
        digest: String?,
        artifactFile: ArtifactFile
    ): DockerResponse {
        dockerRepo.httpHeaders = headers
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.uploadBlob(pathContext, DockerDigest(digest), uuid, artifactFile)
    }

    @RequestMapping(method = [RequestMethod.HEAD], value = [DOCKER_BLOB_DIGEST_SUFFIX])
    fun isBlobExists(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @PathVariable
        @ApiParam(value = DOCKER_DIGEST, required = true)
        digest: String
    ): DockerResponse {
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.isBlobExists(pathContext, DockerDigest(digest))
    }

    @RequestMapping(method = [RequestMethod.GET], value = [DOCKER_BLOB_DIGEST_SUFFIX])
    fun getBlob(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @PathVariable
        @ApiParam(value = DOCKER_DIGEST, required = true)
        digest: String
    ): ResponseEntity<Any> {
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.getBlob(pathContext, DockerDigest(digest))
    }

    @RequestMapping(method = [RequestMethod.POST], value = [DOCKER_BLOB_SUFFIX])
    fun startBlobUpload(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @RequestHeader
        headers: HttpHeaders,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @RequestParam
        @ApiParam(value = "mount", required = false)
        mount: String?
    ): ResponseEntity<Any> {
        dockerRepo.httpHeaders = headers
        val uId = getContextUserId(userId)
        val name = PathUtil.artifactName(request, BLOB_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.startBlobUpload(pathContext, mount)
    }

    @RequestMapping(method = [RequestMethod.PATCH], value = [DOCKER_BLOB_UUID_SUFFIX])
    fun patchUpload(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @RequestHeader
        headers: HttpHeaders,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @PathVariable
        @ApiParam(value = DOCKER_UUID, required = false)
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
