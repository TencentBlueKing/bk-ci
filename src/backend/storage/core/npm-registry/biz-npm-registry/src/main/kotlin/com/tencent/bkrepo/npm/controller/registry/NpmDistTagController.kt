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

import com.fasterxml.jackson.databind.node.TextNode
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.constant.MODIFIED
import com.tencent.bkrepo.npm.pojo.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmDistTagInfo
import com.tencent.bkrepo.npm.pojo.response.NpmResponse
import com.tencent.bkrepo.npm.service.NpmDistTagService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class NpmDistTagController(
    private val distTagService: NpmDistTagService
) {
    /**
     * returns the package's dist-tags
     * npm dist-tag ls [<pkg>]
     */
    @GetMapping(value = ["/-/package/{name}/dist-tags", "/-/package/@{scope}/{name}/dist-tags"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun listTags(artifactInfo: NpmArtifactInfo): Map<String, String> {
        return distTagService.listTags(artifactInfo)
    }

    /**
     * Set package's dist-tags
     */
    @PutMapping(value = ["/-/package/{name}/dist-tags", "/-/package/@{scope}/{name}/dist-tags"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun saveTags(artifactInfo: NpmDistTagInfo): ResponseEntity<NpmResponse> {
        distTagService.saveTags(artifactInfo)
        return ResponseEntity.status(HttpStatus.CREATED).body(NpmResponse.distTagSuccess())
    }

    /**
     * Add/modify package's dist-tags
     */
    @PostMapping(value = ["/-/package/{name}/dist-tags", "/-/package/@{scope}/{name}/dist-tags"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun updateTags(artifactInfo: NpmDistTagInfo): ResponseEntity<NpmResponse> {
        distTagService.updateTags(artifactInfo)
        return ResponseEntity.status(HttpStatus.CREATED).body(NpmResponse.distTagSuccess())
    }

    /**
     * Set package's dist-tags
     * npm dist-tag add <pkg>@<version> [<tag>]
     */
    @PutMapping(value = ["/-/package/{name}/dist-tags/{tag}", "/-/package/@{scope}/{name}/dist-tags/{tag}"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun putTags(artifactInfo: NpmArtifactInfo, @RequestBody version: TextNode): ResponseEntity<NpmResponse> {
        distTagService.setTags(artifactInfo, version.asText())
        return ResponseEntity.status(HttpStatus.CREATED).body(NpmResponse.distTagSuccess())
    }

    /**
     * Set package's dist-tags, same as putTags
     * npm dist-tag add <pkg>@<version> [<tag>]
     */
    @PostMapping(value = ["/-/package/{name}/dist-tags/{tag}", "/-/package/@{scope}/{name}/dist-tags/{tag}"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun postTags(artifactInfo: NpmArtifactInfo, @RequestBody version: TextNode): ResponseEntity<NpmResponse> {
        distTagService.setTags(artifactInfo, version.asText())
        return ResponseEntity.status(HttpStatus.CREATED).body(NpmResponse.distTagSuccess())
    }

    /**
     * Remove tag from dist-tags
     * npm dist-tag rm <pkg> <tag>
     */
    @DeleteMapping(value = ["/-/package/{name}/dist-tags/{tag}", "/-/package/@{scope}/{name}/dist-tags/{tag}"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun deleteTags(artifactInfo: NpmArtifactInfo): ResponseEntity<NpmResponse> {
        distTagService.deleteTags(artifactInfo)
        return ResponseEntity.ok(NpmResponse.distTagSuccess())
    }

    /**
     * add tag
     */
    @PutMapping(value = ["/{name}/{tag}", "/@{scope}/{name}/{tag}"])
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun addTag(artifactInfo: NpmArtifactInfo, @RequestBody version: TextNode): ResponseEntity<NpmResponse> {
        distTagService.setTags(artifactInfo, version.asText())
        val npmResponse = NpmResponse.success().apply { set(MODIFIED, LocalDateTime.now()) }
        return ResponseEntity.status(HttpStatus.CREATED).body(npmResponse)
    }
}
