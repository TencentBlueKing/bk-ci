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

package com.tencent.bkrepo.generic.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo.Companion.DELTA_MAPPING_URI
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo.Companion.GENERIC_MAPPING_URI
import com.tencent.bkrepo.generic.constant.HEADER_OLD_FILE_PATH
import com.tencent.bkrepo.generic.pojo.TemporaryAccessToken
import com.tencent.bkrepo.generic.pojo.TemporaryAccessUrl
import com.tencent.bkrepo.generic.pojo.TemporaryUrlCreateRequest
import com.tencent.bkrepo.generic.service.TemporaryAccessService
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.repository.pojo.token.TokenType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/temporary/")
class TemporaryAccessController(
    private val temporaryAccessService: TemporaryAccessService,
    private val permissionManager: PermissionManager
) {

    @PostMapping("/token/create")
    fun createToken(@RequestBody request: TemporaryTokenCreateRequest): Response<List<TemporaryAccessToken>> {
        with(request) {
            fullPathSet.forEach {
                permissionManager.checkNodePermission(PermissionAction.WRITE, projectId, repoName, it)
            }
            return ResponseBuilder.success(temporaryAccessService.createToken(request))
        }
    }

    @PostMapping("/url/create")
    fun createUrl(@RequestBody request: TemporaryUrlCreateRequest): Response<List<TemporaryAccessUrl>> {
        with(request) {
            fullPathSet.forEach {
                permissionManager.checkNodePermission(PermissionAction.WRITE, projectId, repoName, it)
            }
            return ResponseBuilder.success(temporaryAccessService.createUrl(request))
        }
    }

    @GetMapping("/download/$GENERIC_MAPPING_URI")
    fun downloadByToken(
        artifactInfo: GenericArtifactInfo,
        @RequestParam token: String
    ) {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.DOWNLOAD)
        temporaryAccessService.download(artifactInfo)
        temporaryAccessService.decrementPermits(tokenInfo)
    }

    @CrossOrigin
    @PutMapping("/upload/$GENERIC_MAPPING_URI")
    fun uploadByToken(
        artifactInfo: GenericArtifactInfo,
        file: ArtifactFile,
        @RequestParam token: String
    ) {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.UPLOAD)
        temporaryAccessService.upload(artifactInfo, file)
        temporaryAccessService.decrementPermits(tokenInfo)
    }

    /**
     * 下载sign file
     * */
    @GetMapping("/sign/$DELTA_MAPPING_URI")
    fun downloadSignFile(
        artifactInfo: GenericArtifactInfo,
        @RequestParam token: String,
        @RequestParam md5: String? = null
    ) {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.DOWNLOAD)
        temporaryAccessService.sign(artifactInfo, md5)
        temporaryAccessService.decrementPermits(tokenInfo)
    }

    /**
     * 上传sign file
     * */
    @PutMapping("/sign/$DELTA_MAPPING_URI")
    fun uploadSignFile(
        @ArtifactPathVariable artifactInfo: GenericArtifactInfo,
        @RequestParam token: String,
        @RequestParam md5: String,
        signFile: ArtifactFile
    ) {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.UPLOAD)
        temporaryAccessService.uploadSignFile(signFile, artifactInfo, md5)
        temporaryAccessService.decrementPermits(tokenInfo)
    }

    /**
     * 增量上传patch
     * */
    @PatchMapping("/patch/$DELTA_MAPPING_URI")
    fun patch(
        artifactInfo: GenericArtifactInfo,
        @RequestHeader(HEADER_OLD_FILE_PATH) oldFilePath: String,
        @RequestParam token: String,
        deltaFile: ArtifactFile
    ): SseEmitter {
        val tokenInfo = temporaryAccessService.validateToken(token, artifactInfo, TokenType.UPLOAD)
        val emitter = temporaryAccessService.patch(artifactInfo, oldFilePath, deltaFile)
        temporaryAccessService.decrementPermits(tokenInfo)
        return emitter
    }
}
