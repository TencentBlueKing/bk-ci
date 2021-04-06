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

package com.tencent.bkrepo.generic.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo.Companion.GENERIC_MAPPING_URI
import com.tencent.bkrepo.generic.pojo.TemporaryAccessToken
import com.tencent.bkrepo.generic.pojo.TemporaryAccessUrl
import com.tencent.bkrepo.generic.service.TemporaryAccessService
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.repository.pojo.token.TokenType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/temporary/")
class TemporaryAccessController(
    private val temporaryAccessService: TemporaryAccessService,
    private val permissionManager: PermissionManager
) {

    @PostMapping("/token/create")
    fun createToken(
        @RequestAttribute userId: String,
        @RequestBody request: TemporaryTokenCreateRequest
    ): Response<List<TemporaryAccessToken>> {
        with(request) {
            fullPathSet.forEach {
                permissionManager.checkNodePermission(PermissionAction.WRITE, projectId, repoName, it)
            }
            return temporaryAccessService.createToken(request)
        }
    }

    @PostMapping("/url/create")
    fun createUrl(
        @RequestAttribute userId: String,
        @RequestBody request: TemporaryTokenCreateRequest
    ): Response<List<TemporaryAccessUrl>> {
        with(request) {
            fullPathSet.forEach {
                permissionManager.checkNodePermission(PermissionAction.WRITE, projectId, repoName, it)
            }
            return temporaryAccessService.createUrl(request)
        }
    }

    @GetMapping("/download/$GENERIC_MAPPING_URI")
    fun downloadByToken(
        @ArtifactPathVariable artifactInfo: GenericArtifactInfo,
        @RequestParam token: String
    ) {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.DOWNLOAD)
        temporaryAccessService.download(artifactInfo)
        temporaryAccessService.decrementPermits(tokenInfo)
    }

    @PutMapping("/upload/$GENERIC_MAPPING_URI")
    fun uploadByToken(
        @ArtifactPathVariable artifactInfo: GenericArtifactInfo,
        @RequestParam token: String,
        file: ArtifactFile
    ) {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.UPLOAD)
        temporaryAccessService.upload(artifactInfo, file)
        temporaryAccessService.decrementPermits(tokenInfo)
    }
}
