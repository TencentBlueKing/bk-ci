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
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.pojo.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmPublishInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmUpdateInfo
import com.tencent.bkrepo.npm.pojo.response.NpmRegistrySummary
import com.tencent.bkrepo.npm.pojo.response.NpmResponse
import com.tencent.bkrepo.npm.service.NpmPackageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class NpmPackageController(
    private val npmPackageService: NpmPackageService
) {

    @GetMapping
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun info(artifactInfo: DefaultArtifactInfo): NpmRegistrySummary {
        return npmPackageService.info(artifactInfo)
    }

    /**
     * npm publish
     * npm deprecate
     * npm star
     */
    @PutMapping("/{name}", "/@{scope}/{name}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun savePackage(publishInfo: NpmPublishInfo) {
        if (publishInfo.isDeprecated) {
            npmPackageService.deprecate(publishInfo)
        } else {
            npmPackageService.publish(publishInfo)
        }
    }

    /**
     * 更新包数据，npm unpublish依赖此接口
     * unpublish的请求体中不带attachments, 并将需要删除的版本从version中移除
     */
    @PutMapping("/{name}/-rev/{rev}", "/@{scope}/{name}/-rev/{rev}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun updatePackage(updateInfo: NpmUpdateInfo): ResponseEntity<NpmResponse> {
        npmPackageService.updatePackage(updateInfo)
        return ResponseEntity.status(HttpStatus.CREATED).body(NpmResponse.success())
    }

    /**
     * npm install
     */
    @GetMapping(
        "/{name}/{delimiter:-|download}/{filename}",
        "/@{scope}/{name}/{delimiter:-|download}/@{scope}/{filename}"
    )
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun download(artifactInfo: NpmArtifactInfo) {
        npmPackageService.download(artifactInfo)
    }

    /**
     * delete tarball and remove one version
     * npm unpublish test@1.0.0 执行流程：
     *   1. [getVersionMetadata]    GET /:name                  获取package.json
     *   2. [updatePackage]         PUT /:name/-rev/:rev        删除package.json
     *   3. [getVersionMetadata]    GET /:name                  获取package.json
     *   4. [deletePackage]         DELETE /:name/-/:filename   删除tgz文件
     */
    @DeleteMapping(
        "/{name}/{delimiter:-|download}/{filename}/-rev/{rev}",
        "/@{scope}/{name}/{delimiter:-|download}/@{scope}/{filename}/-rev/{rev}"
    )
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deleteVersion(artifactInfo: NpmArtifactInfo): ResponseEntity<NpmResponse> {
        npmPackageService.deleteVersion(artifactInfo)
        return ResponseEntity.ok(NpmResponse.success())
    }

    /**
     * remove all versions
     * npm检测到只有一个版本时候，会调用该接口删除整个包
     */
    @DeleteMapping("/{name}/-rev/{rev}", "/@{scope}/{name}/-rev/{rev}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deletePackage(artifactInfo: NpmArtifactInfo): ResponseEntity<NpmResponse> {
        npmPackageService.deletePackage(artifactInfo)
        return ResponseEntity.ok(NpmResponse.success())
    }

    /**
     * 获取包信息package.json
     */
    @GetMapping("/{name}", "/@{scope}/{name}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getPackageMetadata(artifactInfo: NpmArtifactInfo) {
        npmPackageService.getPackageMetadata(artifactInfo)
    }

    /**
     * get the special version or tag of a package. It is a deprecate api
     * @param version 版本或tag
     */
    @GetMapping("/{name}/{version}", "/@{scope}/{name}/{version}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getVersionMetadata(artifactInfo: NpmArtifactInfo) {
        npmPackageService.getVersionMetadata(artifactInfo)
    }
}
