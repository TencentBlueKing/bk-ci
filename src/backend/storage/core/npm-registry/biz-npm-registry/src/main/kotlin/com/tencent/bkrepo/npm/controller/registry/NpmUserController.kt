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

package com.tencent.bkrepo.npm.controller.registry

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.npm.constant.USERNAME
import com.tencent.bkrepo.npm.pojo.response.NpmResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class NpmUserController {

    /**
     * whoami
     */
    @GetMapping("/-/whoami")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun whoami(artifactInfo: DefaultArtifactInfo): NpmResponse {
        return NpmResponse.success().apply {
            set(USERNAME, SecurityUtils.getUserId())
        }
    }

    /**
     * ping
     */
    @GetMapping("/-/ping")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun ping(artifactInfo: DefaultArtifactInfo) = NpmResponse.success()

    /**
     * show User
     */
    @GetMapping("/-/user/org.couchdb.user:{name}")
    fun showUser(artifactInfo: DefaultArtifactInfo) {
        throw MethodNotAllowedException()
    }

    /**
     * update User
     */
    @PutMapping("/-/user/org.couchdb.user:{name}/-rev/{rev}")
    fun updateUser(artifactInfo: DefaultArtifactInfo) {
        throw MethodNotAllowedException()
    }

    /**
     * delete token
     * npm logout
     */
    @DeleteMapping("/-/user/token/{token}")
    fun deleteToken(
        artifactInfo: DefaultArtifactInfo,
        @PathVariable token: String
    ): ResponseEntity<NpmResponse> {
        return ResponseEntity.ok(NpmResponse.success())
    }
}
