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

import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.docker.api.User
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.service.DockerV2LocalRepoService
import com.tencent.bkrepo.docker.util.PathUtil
import com.tencent.bkrepo.docker.util.UserUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Principal(PrincipalType.PLATFORM)
@RestController
class UserImpl @Autowired constructor(val dockerRepo: DockerV2LocalRepoService) : User {

    override fun getManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        tag: String
    ): Response<String> {
        val artifactName = PathUtil.userArtifactName(request, projectId, repoName, tag)
        val uId = UserUtil.getContextUserId(userId)
        val context = RequestContext(uId, projectId, repoName, artifactName)
        val result = dockerRepo.getManifestString(context, tag)
        return ResponseBuilder.success(result)
    }

    override fun getLayer(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        id: String
    ): ResponseEntity<Any> {
        val uId = UserUtil.getContextUserId(userId)
        val artifactName = PathUtil.layerArtifactName(request, projectId, repoName, id)
        val context = RequestContext(uId, projectId, repoName, artifactName)
        return dockerRepo.buildLayerResponse(context, id)
    }

    override fun getRepo(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String
    ): Response<List<String>> {
        val uId = UserUtil.getContextUserId(userId)
        val context = RequestContext(uId, projectId, repoName, EMPTY)
        val result = dockerRepo.getRepoList(context)
        return ResponseBuilder.success(result)
    }

    override fun getRepoTag(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String
    ): Response<Map<String, String>> {
        val uId = UserUtil.getContextUserId(userId)
        val artifactName = PathUtil.tagArtifactName(request, projectId, repoName)
        val context = RequestContext(uId, projectId, repoName, artifactName)
        val result = dockerRepo.getRepoTagList(context)
        return ResponseBuilder.success(result)
    }
}
