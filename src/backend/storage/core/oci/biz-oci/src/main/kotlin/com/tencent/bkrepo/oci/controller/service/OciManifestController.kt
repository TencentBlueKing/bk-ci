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

package com.tencent.bkrepo.oci.controller.service

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo.Companion.MANIFEST_URL
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.service.OciManifestService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

/**
 * oci manifest controller
 */
@RestController
@Suppress("MVCPathVariableInspection")
class OciManifestController(
    private val ociManifestService: OciManifestService
) {

    /**
     * 上传manifest文件
     * 可以通过digest或者tag去上传
     */
    @PutMapping(MANIFEST_URL)
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun uploadManifests(
        artifactInfo: OciManifestArtifactInfo,
        artifactFile: ArtifactFile
    ) {
        ociManifestService.uploadManifest(artifactInfo, artifactFile)
    }

    /**
     * 下载manifest文件
     * 可以通过digest或者tag去下载
     */
    @GetMapping(MANIFEST_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun downloadManifests(
        artifactInfo: OciManifestArtifactInfo
    ) {
        ociManifestService.downloadManifests(artifactInfo)
    }

    /**
     * 删除manifest文件
     * 可以通过digest和tag删除
     */
    @DeleteMapping(MANIFEST_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun deleteManifests(
        artifactInfo: OciManifestArtifactInfo
    ) {
        ociManifestService.deleteManifests(artifactInfo)
    }
}
