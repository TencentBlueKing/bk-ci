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

package com.tencent.bkrepo.npm.controller.registry

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.pojo.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.service.NpmPackageService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * npm search逻辑
 * 1. [search] GET /-/v1/search 服务器直接返回数据
 * 2. 如果服务器未提供/-/v1/search接口，则通过[listAll] GET /-/all 返回到服务器所有的包并在客户端缓存，客户端在本地搜索
 * 3. 如果/-/all 出错，则通过[listSince] GET /-/all/since 过滤数据
 */
@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class NpmSearchController(
    private val npmPackageService: NpmPackageService
) {

    /**
     * list all package, for npm legacy search
     */
    @GetMapping("/-/all")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun listAll(artifactInfo: NpmArtifactInfo): ResponseEntity<Map<String, Any>> {
        val result = mutableMapOf<String, Any>("_updated" to System.currentTimeMillis())
        npmPackageService.listAll(artifactInfo).forEach { result[it["name"].toString()] = it }
        val headers = HttpHeaders()
        return ResponseEntity.ok()
            .headers(headers)
            .body(result)
    }

    /**
     * list all package
     * npm search
     */
    @GetMapping("/-/all/since")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun listSince(artifactInfo: NpmArtifactInfo): ResponseEntity<Map<String, Any>> {
        val result = mutableMapOf<String, Any>("_updated" to System.currentTimeMillis())
        npmPackageService.listAll(artifactInfo).forEach { result[it["name"].toString()] = it }
        val headers = HttpHeaders()
        return ResponseEntity.ok()
            .headers(headers)
            .body(result)
    }

    /**
     * list all package names, for auto completion
     */
    @GetMapping("/-/short")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun listShorts(artifactInfo: NpmArtifactInfo): List<String> {
        return npmPackageService.listShorts(artifactInfo)
    }
    /**
     * {
    name: String,
    version: SemverString,
    description: String || null,
    maintainers: [
    {
    username: String,
    email: String
    },
    ...etc
    ] || null,
    keywords: [String] || null,
    date: Date || null
    }
     */
    //
    // /**
    //  * search
    //  */

    // @GetMapping("/-/v1/search")
    // @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    // fun search(artifactInfo: NpmArtifactInfo, request: NpmSearchRequest): List<String> {
    //     return npmPackageService.search(artifactInfo)
    // }
}
