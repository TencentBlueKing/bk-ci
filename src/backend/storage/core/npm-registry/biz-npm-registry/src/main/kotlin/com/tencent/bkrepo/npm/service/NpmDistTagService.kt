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

package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.manager.PackageManager
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.util.version.SemVersion
import com.tencent.bkrepo.npm.constant.LATEST
import com.tencent.bkrepo.npm.constant.NpmMessageCode
import com.tencent.bkrepo.npm.constant.TAG
import com.tencent.bkrepo.npm.pojo.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmDistTagInfo
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import org.springframework.stereotype.Service

@Suppress("UNCHECKED_CAST")
@Service
class NpmDistTagService(
    private val packageClient: PackageClient,
    private val packageManager: PackageManager
) : ArtifactService() {

    fun listTags(artifactInfo: NpmArtifactInfo): Map<String, String> {
        with(artifactInfo) {
            return packageManager.findPackageByKey(projectId, repoName, packageName).versionTag
        }
    }

    fun saveTags(artifactInfo: NpmDistTagInfo) {
        with(artifactInfo) {
            val request = PackageUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                versionTag = distTags
            )
            packageClient.updatePackage(request)
        }
    }

    fun updateTags(artifactInfo: NpmDistTagInfo) {
        with(artifactInfo) {
            val old = packageManager.findPackageByKey(projectId, repoName, packageName).versionTag
            val new = old.toMutableMap().apply { putAll(distTags) }
            val request = PackageUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                versionTag = new
            )
            packageClient.updatePackage(request)
        }
    }

    fun setTags(artifactInfo: NpmArtifactInfo, version: String) {
        val tag = artifactInfo.version
        Preconditions.checkNotBlank(tag, TAG)
        if (!SemVersion.validate(version)) {
            throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, version)
        }
        with(artifactInfo) {
            val old = packageManager.findPackageByKey(projectId, repoName, packageName).versionTag
            val versionInfo = packageManager.findVersionByName(projectId, repoName, packageName, version)
            val new = old.toMutableMap().apply { put(tag, versionInfo.name) }
            val request = PackageUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                versionTag = new
            )
            packageClient.updatePackage(request)
        }
    }

    fun deleteTags(artifactInfo: NpmArtifactInfo) {
        val tag = artifactInfo.version
        if (tag == LATEST) {
            throw ErrorCodeException(NpmMessageCode.DELETE_LATEST_TAG_FORBIDDEN)
        }
        with(artifactInfo) {
            val old = packageManager.findPackageByKey(projectId, repoName, packageName).versionTag
            val new = old.toMutableMap().apply { remove(tag) }
            val request = PackageUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                versionTag = new
            )
            packageClient.updatePackage(request)
        }
    }
}
