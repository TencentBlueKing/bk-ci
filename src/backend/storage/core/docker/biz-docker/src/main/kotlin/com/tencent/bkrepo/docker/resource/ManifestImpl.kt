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

package com.tencent.bkrepo.docker.resource

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.api.Manifest
import com.tencent.bkrepo.docker.constant.MANIFEST_PATTERN
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.service.DockerV2LocalRepoService
import com.tencent.bkrepo.docker.util.PathUtil
import com.tencent.bkrepo.docker.util.UserUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

/**
 *  ManifestImpl validates and impl the manifest interface
 */
@RestController
class ManifestImpl @Autowired constructor(val dockerRepo: DockerV2LocalRepoService) : Manifest {

    override fun putManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        tag: String,
        contentType: String,
        artifactFile: ArtifactFile
    ): ResponseEntity<Any> {
        val uId = UserUtil.getContextUserId(userId)
        val name = PathUtil.artifactName(request, MANIFEST_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.uploadManifest(pathContext, tag, contentType, artifactFile)
    }

    override fun getManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        reference: String
    ): ResponseEntity<Any> {
        val name = PathUtil.artifactName(request, MANIFEST_PATTERN, projectId, repoName)
        val uId = UserUtil.getContextUserId(userId)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.getManifest(pathContext, reference)
    }

    override fun existManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        reference: String
    ): ResponseEntity<Any> {
        val name = PathUtil.artifactName(request, MANIFEST_PATTERN, projectId, repoName)
        val uId = UserUtil.getContextUserId(userId)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.getManifest(pathContext, reference)
    }
}
